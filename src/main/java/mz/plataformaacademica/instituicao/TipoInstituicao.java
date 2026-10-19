package mz.plataformaacademica.instituicao;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipo_instituicao")
public class TipoInstituicao {

    @Id
    private UUID id;

    @Column(name = "nome", nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoTipoInstituicao estado;

    protected TipoInstituicao() {
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public EstadoTipoInstituicao getEstado() {
        return estado;
    }
}
