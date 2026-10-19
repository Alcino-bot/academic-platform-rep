package mz.plataformaacademica.instituicao;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import mz.plataformaacademica.comum.RegraDeNegocioException;

@Entity
@Table(name = "instituicao")
public class Instituicao {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Column(name = "sigla", nullable = false)
    private String sigla;

    @Column(name = "tipo_instituicao_id", nullable = false)
    private UUID tipoInstituicaoId;

    @Column(name = "modelo_origem_id")
    private UUID modeloOrigemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoInstituicao estado;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private Instant dataCriacao;

    protected Instituicao() {
    }

    public Instituicao(String nome, String sigla, UUID tipoInstituicaoId) {
        this.nome = nome;
        this.sigla = sigla;
        this.tipoInstituicaoId = tipoInstituicaoId;
        this.estado = EstadoInstituicao.PENDENTE;
        this.dataCriacao = Instant.now();
    }

    public void mudarEstado(EstadoInstituicao destino) {
        if (!estado.podeTransitarPara(destino)) {
            throw new RegraDeNegocioException(
                "Transição de estado não permitida: " + estado + " para " + destino);
        }
        this.estado = destino;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getSigla() {
        return sigla;
    }

    public UUID getTipoInstituicaoId() {
        return tipoInstituicaoId;
    }

    public UUID getModeloOrigemId() {
        return modeloOrigemId;
    }

    public EstadoInstituicao getEstado() {
        return estado;
    }

    public Instant getDataCriacao() {
        return dataCriacao;
    }
}
