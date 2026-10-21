package mz.plataformaacademica.administracao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mz.plataformaacademica.administracao.ArvoreDeTiposDeUnidade.Entrada;
import mz.plataformaacademica.administracao.ArvoreDeTiposDeUnidade.TipoResolvido;
import mz.plataformaacademica.comum.ConflitoException;
import mz.plataformaacademica.comum.NaoEncontradoException;
import mz.plataformaacademica.comum.RegraDeNegocioException;

@Service
public class ServicoDeModelosOrganizacionais {

    public record TipoDeUnidadeVista(UUID id, String nome, int nivel, UUID paiId) {
    }

    public record ModeloResumo(
        UUID id, UUID tipoInstituicaoId, String nome, String descricao, String estado, int totalDeTiposDeUnidade) {
    }

    public record ModeloVista(
        UUID id,
        UUID tipoInstituicaoId,
        String nome,
        String descricao,
        String estado,
        List<TipoDeUnidadeVista> tiposDeUnidade) {
    }

    private record CabecalhoDoModelo(
        UUID id, UUID tipoInstituicaoId, String nome, String descricao, String estado) {
    }

    private static final Set<String> ESTADOS = Set.of("RASCUNHO", "ACTIVO", "INACTIVO");

    private static final RowMapper<CabecalhoDoModelo> CABECALHO = (rs, linha) -> new CabecalhoDoModelo(
        rs.getObject("id", UUID.class),
        rs.getObject("tipo_instituicao_id", UUID.class),
        rs.getString("nome"),
        rs.getString("descricao"),
        rs.getString("estado"));

    private static final RowMapper<TipoDeUnidadeVista> TIPO_DE_UNIDADE = (rs, linha) -> new TipoDeUnidadeVista(
        rs.getObject("id", UUID.class),
        rs.getString("nome"),
        rs.getInt("nivel"),
        rs.getObject("tipo_pai_id", UUID.class));

    private static final String LISTAR =
        "SELECT m.id, m.tipo_instituicao_id, m.nome, m.descricao, m.estado, "
            + "(SELECT count(*) FROM modelo_tipo_unidade t WHERE t.modelo_id = m.id) AS total "
            + "FROM modelo_organizacional m ORDER BY lower(m.nome), m.id";

    private static final String LISTAR_POR_ESTADO =
        "SELECT m.id, m.tipo_instituicao_id, m.nome, m.descricao, m.estado, "
            + "(SELECT count(*) FROM modelo_tipo_unidade t WHERE t.modelo_id = m.id) AS total "
            + "FROM modelo_organizacional m WHERE m.estado = ? ORDER BY lower(m.nome), m.id";

    private static final String OBTER_CABECALHO =
        "SELECT id, tipo_instituicao_id, nome, descricao, estado FROM modelo_organizacional WHERE id = ?";

    private static final String TIPOS_DO_MODELO =
        "SELECT id, nome, nivel, tipo_pai_id FROM modelo_tipo_unidade WHERE modelo_id = ? ORDER BY nivel, lower(nome)";

    private static final String ESTADO_DO_TIPO_DE_INSTITUICAO =
        "SELECT estado FROM tipo_instituicao WHERE id = ?";

    private static final String CRIAR_MODELO =
        "INSERT INTO modelo_organizacional (tipo_instituicao_id, nome, descricao) VALUES (?, ?, ?) "
            + "ON CONFLICT DO NOTHING RETURNING id";

    private static final String CRIAR_TIPO_DE_UNIDADE =
        "INSERT INTO modelo_tipo_unidade (modelo_id, nome, nivel, tipo_pai_id) VALUES (?, ?, ?, ?) "
            + "ON CONFLICT DO NOTHING RETURNING id";

    private static final String BLOQUEAR_MODELO =
        "SELECT m.estado AS estado_do_modelo, t.estado AS estado_do_tipo "
            + "FROM modelo_organizacional m JOIN tipo_instituicao t ON t.id = m.tipo_instituicao_id "
            + "WHERE m.id = ? FOR UPDATE OF m";

    private static final String CONTAR_TIPOS_DE_UNIDADE =
        "SELECT count(*) FROM modelo_tipo_unidade WHERE modelo_id = ?";

    private static final String CONTAR_NIVEIS_INCONSISTENTES =
        "SELECT count(*) FROM modelo_tipo_unidade f "
            + "JOIN modelo_tipo_unidade p ON p.id = f.tipo_pai_id AND p.modelo_id = f.modelo_id "
            + "WHERE f.modelo_id = ? AND f.nivel <> p.nivel + 1";

    private static final String MUDAR_ESTADO =
        "UPDATE modelo_organizacional SET estado = ? WHERE id = ?";

    private final JdbcTemplate jdbc;

    public ServicoDeModelosOrganizacionais(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<ModeloResumo> listar(String estado) {
        RowMapper<ModeloResumo> mapeador = (rs, linha) -> new ModeloResumo(
            rs.getObject("id", UUID.class),
            rs.getObject("tipo_instituicao_id", UUID.class),
            rs.getString("nome"),
            rs.getString("descricao"),
            rs.getString("estado"),
            rs.getInt("total"));
        if (estado == null || estado.isBlank()) {
            return jdbc.query(LISTAR, mapeador);
        }
        if (!ESTADOS.contains(estado)) {
            throw new RegraDeNegocioException("Estado de modelo organizacional inválido");
        }
        return jdbc.query(LISTAR_POR_ESTADO, mapeador, estado);
    }

    @Transactional(readOnly = true)
    public ModeloVista obter(UUID id) {
        List<CabecalhoDoModelo> cabecalhos = jdbc.query(OBTER_CABECALHO, CABECALHO, id);
        if (cabecalhos.isEmpty()) {
            throw new NaoEncontradoException("Modelo organizacional não encontrado");
        }
        CabecalhoDoModelo cabecalho = cabecalhos.get(0);
        List<TipoDeUnidadeVista> tipos = jdbc.query(TIPOS_DO_MODELO, TIPO_DE_UNIDADE, id);
        return new ModeloVista(
            cabecalho.id(),
            cabecalho.tipoInstituicaoId(),
            cabecalho.nome(),
            cabecalho.descricao(),
            cabecalho.estado(),
            tipos);
    }

    @Transactional
    public ModeloVista criar(UUID tipoInstituicaoId, String nome, String descricao, List<Entrada> entradas) {
        if (nome == null || nome.isBlank()) {
            throw new RegraDeNegocioException("O nome é obrigatório");
        }
        String nomeLimpo = nome.strip();
        if (nomeLimpo.length() > 100) {
            throw new RegraDeNegocioException("O nome é demasiado longo");
        }
        String descricaoLimpa = descricao == null || descricao.isBlank() ? null : descricao.strip();
        if (tipoInstituicaoId == null) {
            throw new RegraDeNegocioException("O tipo de instituição é obrigatório");
        }
        List<TipoResolvido> tipos = ArvoreDeTiposDeUnidade.resolver(entradas);
        List<String> estadosDoTipo = jdbc.queryForList(ESTADO_DO_TIPO_DE_INSTITUICAO, String.class, tipoInstituicaoId);
        if (estadosDoTipo.isEmpty()) {
            throw new NaoEncontradoException("Tipo de instituição não encontrado");
        }
        if (!"APROVADA".equals(estadosDoTipo.get(0))) {
            throw new RegraDeNegocioException("O tipo de instituição ainda não foi aprovado");
        }
        List<UUID> criados = jdbc.queryForList(CRIAR_MODELO, UUID.class, tipoInstituicaoId, nomeLimpo, descricaoLimpa);
        if (criados.isEmpty()) {
            throw new ConflitoException("Já existe um modelo organizacional com esse nome para este tipo de instituição");
        }
        UUID modeloId = criados.get(0);
        Map<String, UUID> idsPorChave = new HashMap<>();
        for (TipoResolvido tipo : tipos) {
            UUID paiId = tipo.paiNome() == null ? null : idsPorChave.get(ArvoreDeTiposDeUnidade.chave(tipo.paiNome()));
            List<UUID> criadosDoTipo = jdbc.queryForList(
                CRIAR_TIPO_DE_UNIDADE, UUID.class, modeloId, tipo.nome(), tipo.nivel(), paiId);
            if (criadosDoTipo.isEmpty()) {
                throw new RegraDeNegocioException(
                    "O tipo de unidade '" + tipo.nome() + "' aparece mais de uma vez");
            }
            idsPorChave.put(ArvoreDeTiposDeUnidade.chave(tipo.nome()), criadosDoTipo.get(0));
        }
        return obter(modeloId);
    }

    @Transactional
    public ModeloVista activar(UUID id) {
        String[] estados = bloquear(id);
        if ("ACTIVO".equals(estados[0])) {
            throw new RegraDeNegocioException("O modelo organizacional já está activo");
        }
        if (!"APROVADA".equals(estados[1])) {
            throw new RegraDeNegocioException("O tipo de instituição do modelo não está aprovado");
        }
        Integer total = jdbc.queryForObject(CONTAR_TIPOS_DE_UNIDADE, Integer.class, id);
        if (total == null || total == 0) {
            throw new RegraDeNegocioException("O modelo precisa de pelo menos um tipo de unidade para ser activado");
        }
        Integer inconsistentes = jdbc.queryForObject(CONTAR_NIVEIS_INCONSISTENTES, Integer.class, id);
        if (inconsistentes != null && inconsistentes > 0) {
            throw new RegraDeNegocioException(
                "O modelo é inconsistente: o nível de um tipo de unidade tem de ser o do tipo pai mais um");
        }
        jdbc.update(MUDAR_ESTADO, "ACTIVO", id);
        return obter(id);
    }

    @Transactional
    public ModeloVista inactivar(UUID id) {
        String[] estados = bloquear(id);
        if (!"ACTIVO".equals(estados[0])) {
            throw new RegraDeNegocioException(
                "Só um modelo activo pode ser inactivado (estado actual: " + estados[0] + ")");
        }
        jdbc.update(MUDAR_ESTADO, "INACTIVO", id);
        return obter(id);
    }

    private String[] bloquear(UUID id) {
        List<String[]> encontrados = jdbc.query(
            BLOQUEAR_MODELO,
            (rs, linha) -> new String[] {rs.getString("estado_do_modelo"), rs.getString("estado_do_tipo")},
            id);
        if (encontrados.isEmpty()) {
            throw new NaoEncontradoException("Modelo organizacional não encontrado");
        }
        return encontrados.get(0);
    }
}
