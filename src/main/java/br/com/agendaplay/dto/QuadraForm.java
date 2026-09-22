package br.com.agendaplay.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.*;

public class QuadraForm {
    @NotNull(message = "Selecione um estabelecimento.")
    private Long idEstabelecimento;

    @Min(0)
    @Max(1440)
    private int toleranciaMinutos = 10;

    @DecimalMin("-90")
    @DecimalMax("90")
    private BigDecimal latitude;

    @DecimalMin("-180")
    @DecimalMax("180")
    private BigDecimal longitude;

    @AssertTrue(message = "Informe latitude e longitude juntas.")
    public boolean isCoordenadasValidas() {
        return (latitude == null) == (longitude == null);
    }

    public Long getIdEstabelecimento() {
        return idEstabelecimento;
    }

    public void setIdEstabelecimento(Long v) {
        idEstabelecimento = v;
    }

    public int getToleranciaMinutos() {
        return toleranciaMinutos;
    }

    public void setToleranciaMinutos(int v) {
        toleranciaMinutos = v;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal v) {
        latitude = v;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal v) {
        longitude = v;
    }

    @NotBlank(message = "Informe o nome da quadra.")
    @Size(max = 120)
    private String nome;

    @NotBlank(message = "Informe a modalidade.")
    @Size(max = 60)
    private String modalidade;

    @NotBlank(message = "Informe a localização.")
    @Size(max = 240)
    private String localizacao;

    @NotNull(message = "Informe o valor por hora.")
    @DecimalMin(value = "0.00", message = "O valor não pode ser negativo.")
    @Digits(integer = 8, fraction = 2, message = "Use até 8 dígitos e 2 casas decimais.")
    private BigDecimal valorHora;

    @NotBlank
    @Pattern(regexp = "ATIVA|INATIVA", message = "Selecione uma situação válida.")
    private String situacao = "ATIVA";

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getModalidade() {
        return modalidade;
    }

    public void setModalidade(String modalidade) {
        this.modalidade = modalidade;
    }

    public String getLocalizacao() {
        return localizacao;
    }

    public void setLocalizacao(String localizacao) {
        this.localizacao = localizacao;
    }

    public BigDecimal getValorHora() {
        return valorHora;
    }

    public void setValorHora(BigDecimal valorHora) {
        this.valorHora = valorHora;
    }

    public String getSituacao() {
        return situacao;
    }

    public void setSituacao(String situacao) {
        this.situacao = situacao;
    }
}
