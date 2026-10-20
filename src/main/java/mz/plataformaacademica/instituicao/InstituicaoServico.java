package mz.plataformaacademica.instituicao;

import java.util.List;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mz.plataformaacademica.comum.ConflitoException;
import mz.plataformaacademica.comum.NaoEncontradoException;
import mz.plataformaacademica.comum.RegraDeNegocioException;

@Service
public class InstituicaoServico {

    private final InstituicaoRepositorio instituicoes;
    private final TipoInstituicaoRepositorio tipos;

    public InstituicaoServico(InstituicaoRepositorio instituicoes, TipoInstituicaoRepositorio tipos) {
        this.instituicoes = instituicoes;
        this.tipos = tipos;
    }

    @Transactional
    public Instituicao criar(String nome, String sigla, UUID tipoInstituicaoId) {
        return criar(nome, sigla, tipoInstituicaoId, null);
    }

    @Transactional
    public Instituicao criar(String nome, String sigla, UUID tipoInstituicaoId, UUID modeloOrigemId) {
        String nomeLimpo = textoObrigatorio(nome, "O nome é obrigatório");
        String siglaLimpa = textoObrigatorio(sigla, "A sigla é obrigatória");
        if (tipoInstituicaoId == null) {
            throw new RegraDeNegocioException("O tipo de instituição é obrigatório");
        }
        TipoInstituicao tipo = tipos.findById(tipoInstituicaoId)
            .orElseThrow(() -> new NaoEncontradoException("Tipo de instituição não encontrado"));
        if (tipo.getEstado() != EstadoTipoInstituicao.APROVADA) {
            throw new RegraDeNegocioException("O tipo de instituição ainda não foi aprovado");
        }
        try {
            return instituicoes.saveAndFlush(new Instituicao(nomeLimpo, siglaLimpa, tipo.getId(), modeloOrigemId));
        } catch (DataIntegrityViolationException e) {
            String causa = String.valueOf(e.getMostSpecificCause().getMessage());
            if (causa.contains("uq_instituicao_nome")) {
                throw new ConflitoException("Já existe uma instituição com esse nome");
            }
            if (causa.contains("uq_instituicao_sigla")) {
                throw new ConflitoException("Já existe uma instituição com essa sigla");
            }
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Instituicao obter(UUID id) {
        return instituicoes.findById(id)
            .orElseThrow(() -> new NaoEncontradoException("Instituição não encontrada"));
    }

    @Transactional(readOnly = true)
    public List<Instituicao> listar() {
        return instituicoes.findAll(Sort.by("nome"));
    }

    @Transactional
    public Instituicao mudarEstado(UUID id, EstadoInstituicao destino) {
        Instituicao instituicao = obter(id);
        instituicao.mudarEstado(destino);
        return instituicao;
    }

    private static String textoObrigatorio(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new RegraDeNegocioException(mensagem);
        }
        return valor.strip();
    }
}
