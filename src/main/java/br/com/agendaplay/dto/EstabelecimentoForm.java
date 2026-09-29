package br.com.agendaplay.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class EstabelecimentoForm {
    @NotBlank
    @Size(max = 120)
    private String nome;

    @NotBlank
    @Size(max = 240)
    private String localizacao;

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

    public String getNome() {
        return nome;
    }

    public void setNome(String v) {
        nome = v;
    }

    public String getLocalizacao() {
        return localizacao;
    }

    public void setLocalizacao(String v) {
        localizacao = v;
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
}
