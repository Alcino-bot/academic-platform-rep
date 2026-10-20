package mz.plataformaacademica.administracao;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mz.plataformaacademica.comum.ConflitoException;
import mz.plataformaacademica.comum.NaoEncontradoException;
import mz.plataformaacademica.comum.RegraDeNegocioException;

@Service
public class ServicoDeModelosOrganizacionais {

    public record ModeloVista(
        UUID id, UUID tipoInstituicaoId, String nome, String descricao, String estado) {
    }

    public record TipoUnidadeVista(UUID id, String nome, int nivel, UUID tipoPaiId) {
    }

    public record ModeloDetalhe(ModeloVista modelo, List<TipoUnidadeVista> tiposUnidade) {
    }

    private record ModeloBloqueado(UUID tipoInstituicaoId, String estado) {
    }

    private static final RowMapper<ModeloVista> MAPEADOR_MODELO = (rs, linha) -> new ModeloVista(
        rs.getObject("id", UUID.class),
        rs.getObject("tipo_instituicao_id", UUID.class),
        rs.getString("nome"),
        rs.getString("descricao"),
        rs.getString("estado"));

    private static final RowMapper<TipoUnidadeVista> MAPEADOR_TIPO_UNIDADE = (rs, linha) -> new TipoUnidadeVista(
        rs.getObject("id", UUID.class),
        rs.getString("nome"),
        rs.getInt("nivel"),
        rs.getObject("tipo_pai_id", UUID.class));

    private static final String LISTAR =
        "SELECT id, tipo_instituicao_id, nome, descricao, estado FROM modelo_organizacional ORDER BY lower(nome), id";

    private static final String OBTER =
        "SELECT id, tipo_instituicao_id, nome, descricao, estado FROM modelo_organizacional WHERE id = ?";

    private static final String TIPOS_DO_MODELO =
        "SELECT id, nome, nivel, tipo_pai_id FROM modelo_tipo_unidade WHERE modelo_id = ? ORDER BY nivel, lower(nome), id";

    private static final String OBTER_TIPO_INSTITUICAO =
        "SELECT estado FROM tipo_instituicao WHERE id = ?";

    private static final String CRIAR =
        "INSERT INTO modelo_organizacional (tipo_instituicao_id, nome, descricao, estado) "
            + "VALUES (?, ?, ?, 'RASCUNHO') ON CONFLICT DO NOTHING RETURNING id";

    private static final String BLOQUEAR_MODELO =
        "SELECT tipo_instituicao_id, estado FROM modelo_organizacional WHERE id = ? FOR UPDATE";

    private static final String NIVEL_DO_TIPO_PAI =
        "SELECT nivel FROM modelo_tipo_unidade WHERE id = ? AND modelo_id = ?";

    private static final String CRIAR_TIPO_UNIDADE =
        "INSERT INTO modelo_tipo_unidade (modelo_id, nome, nivel, tipo_pai_id) VALUES (?, ?, ?, ?) "
            + "ON CONFLICT DO NOTHING RETURNING id";

    private static final String CONTAR_TIPOS =
        "SELECT count(*) FROM modelo_tipo_unidade WHERE modelo_id = ?";

    private static final String CONTAR_HIERARQUIAS_INVALIDAS =
        "SELECT count(*) FROM modelo_tipo_unidade filho "
            + "LEFT JOIN modelo_tipo_unidade pai ON pai.id = filho.tipo_pai_id AND pai.modelo_id = filho.modelo_id "
            + "WHERE filho.modelo_id = ? AND "
            + "((filho.nivel = 1 AND filho.tipo_pai_id IS NOT NULL) "
            + "OR (filho.nivel > 1 AND (filho.tipo_pai_id IS NULL OR pai.id IS NULL "
            + "OR filho.nivel <> pai.nivel + 1)))";

    private static final String ACTIVAR =
        "UPDATE modelo_organizacional SET estado = 'ACTIVO' WHERE id = ? AND estado = 'RASCUNHO'";

    private final JdbcTemplate jdbc;

    public ServicoDeModelosOrganizacionais(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<ModeloVista> listar() {
        return jdbc.query(LISTAR, MAPEADOR_MODELO);
    }

    @Transactional(readOnly = true)
    public ModeloDetalhe obter(UUID id) {
        ModeloVista modelo = obterModelo(id);
        List<TipoUnidadeVista> tipos = jdbc.query(TIPOS_DO_MODELO, MAPEADOR_TIPO_UNIDADE, id);
        return new ModeloDetalhe(modelo, tipos);
    }

    @Transactional
    public ModeloVista criar(UUID tipoInstituicaoId, String nome, String descricao) {
        if (tipoInstituicaoId == null) {
            throw new RegraDeNegocioException("O tipo de instituição é obrigatório");
        }
        String nomeLimpo = normalizarNome(nome);
        List<String> estados = jdbc.queryForList(OBTER_TIPO_INSTITUICAO, String.class, tipoInstituicaoId);
        if (estados.isEmpty()) {
            throw new NaoEncontradoException("Tipo de instituição não encontrado");
        }
        String descricaoLimpa = descricao == null || descricao.isBlank() ? null : descricao.strip();
        List<UUID> criados = jdbc.queryForList(
            CRIAR, UUID.class, tipoInstituicaoId, nomeLimpo, descricaoLimpa);
        if (criados.isEmpty()) {
            throw new ConflitoException("Já existe um modelo com esse nome para o tipo de instituição");
        }
        return obterModelo(criados.get(0));
    }

    @Transactional
    public TipoUnidadeVista adicionarTipoUnidade(
            UUID modeloId, String nome, Integer nivel, UUID tipoPaiId) {
        ModeloBloqueado modelo = bloquearModelo(modeloId);
        exigirRascunho(modelo.estado());
        String nomeLimpo = normalizarNomeTipo(nome);
        if (nivel == null || nivel < 1) {
            throw new RegraDeNegocioException("O nível do tipo de unidade tem de ser pelo menos 1");
        }
        if (nivel == 1 && tipoPaiId != null) {
            throw new RegraDeNegocioException("Um tipo de unidade de nível 1 não pode ter tipo pai");
        }
        if (nivel > 1 && tipoPaiId == null) {
            throw new RegraDeNegocioException("Um tipo de unidade acima do nível 1 tem de ter tipo pai");
        }
        if (tipoPaiId != null) {
            List<Integer> niveisPai = jdbc.queryForList(
                NIVEL_DO_TIPO_PAI, Integer.class, tipoPaiId, modeloId);
            if (niveisPai.isEmpty()) {
                throw new NaoEncontradoException("Tipo de unidade pai não encontrado neste modelo");
            }
            if (nivel != niveisPai.get(0) + 1) {
                throw new RegraDeNegocioException(
                    "O nível do tipo de unidade tem de ser o nível do tipo pai mais um");
            }
        }
        List<UUID> criados = jdbc.queryForList(
            CRIAR_TIPO_UNIDADE, UUID.class, modeloId, nomeLimpo, nivel, tipoPaiId);
        if (criados.isEmpty()) {
            throw new ConflitoException("Já existe um tipo de unidade com esse nome no modelo");
        }
        return obterTipoUnidade(criados.get(0), modeloId);
    }

    @Transactional
    public ModeloVista activar(UUID id) {
        ModeloBloqueado modelo = bloquearModelo(id);
        exigirRascunho(modelo.estado());
        List<String> estadosTipo = jdbc.queryForList(
            OBTER_TIPO_INSTITUICAO, String.class, modelo.tipoInstituicaoId());
        if (estadosTipo.isEmpty()) {
            throw new NaoEncontradoException("Tipo de instituição não encontrado");
        }
        if (!"APROVADA".equals(estadosTipo.get(0))) {
            throw new RegraDeNegocioException("O tipo de instituição ainda não foi aprovado");
        }
        Integer quantidade = jdbc.queryForObject(CONTAR_TIPOS, Integer.class, id);
        if (quantidade == null || quantidade == 0) {
            throw new RegraDeNegocioException("O modelo tem de incluir pelo menos um tipo de unidade");
        }
        Integer hierarquiasInvalidas = jdbc.queryForObject(CONTAR_HIERARQUIAS_INVALIDAS, Integer.class, id);
        if (hierarquiasInvalidas == null || hierarquiasInvalidas > 0) {
            throw new RegraDeNegocioException(
                "A hierarquia do modelo é inválida: o nível do tipo filho tem de ser o do tipo pai mais um");
        }
        if (jdbc.update(ACTIVAR, id) != 1) {
            throw new RegraDeNegocioException("O modelo organizacional não pôde ser activado");
        }
        return obterModelo(id);
    }

    private ModeloVista obterModelo(UUID id) {
        List<ModeloVista> modelos = jdbc.query(OBTER, MAPEADOR_MODELO, id);
        if (modelos.isEmpty()) {
            throw new NaoEncontradoException("Modelo organizacional não encontrado");
        }
        return modelos.get(0);
    }

    private TipoUnidadeVista obterTipoUnidade(UUID tipoId, UUID modeloId) {
        List<TipoUnidadeVista> tipos = jdbc.query(
            "SELECT id, nome, nivel, tipo_pai_id FROM modelo_tipo_unidade WHERE id = ? AND modelo_id = ?",
            MAPEADOR_TIPO_UNIDADE,
            tipoId,
            modeloId);
        if (tipos.isEmpty()) {
            throw new NaoEncontradoException("Tipo de unidade não encontrado neste modelo");
        }
        return tipos.get(0);
    }

    private ModeloBloqueado bloquearModelo(UUID id) {
        List<ModeloBloqueado> modelos = jdbc.query(
            BLOQUEAR_MODELO,
            (rs, linha) -> new ModeloBloqueado(
                rs.getObject("tipo_instituicao_id", UUID.class),
                rs.getString("estado")),
            id);
        if (modelos.isEmpty()) {
            throw new NaoEncontradoException("Modelo organizacional não encontrado");
        }
        return modelos.get(0);
    }

    private static void exigirRascunho(String estado) {
        if (!"RASCUNHO".equals(estado)) {
            throw new RegraDeNegocioException("Só é possível alterar ou activar um modelo em rascunho");
        }
    }

    private static String normalizarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new RegraDeNegocioException("O nome do modelo é obrigatório");
        }
        String nomeLimpo = nome.strip();
        if (nomeLimpo.length() > 100) {
            throw new RegraDeNegocioException("O nome do modelo é demasiado longo");
        }
        return nomeLimpo;
    }

    private static String normalizarNomeTipo(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new RegraDeNegocioException("O nome do tipo de unidade é obrigatório");
        }
        String nomeLimpo = nome.strip();
        if (nomeLimpo.length() > 100) {
            throw new RegraDeNegocioException("O nome do tipo de unidade é demasiado longo");
        }
        return nomeLimpo;
    }
}
