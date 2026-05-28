package com.ufersa.caronas.model;

public class Veiculo {
    private String modelo;
    private String placa;
    private String cor;
    private int vagas;

    public Veiculo() {}

    public Veiculo(String modelo, String placa, String cor, int vagas) {
        this.modelo = modelo;
        this.placa = placa;
        this.cor = cor;
        this.vagas = vagas;
    }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }

    public String getCor() { return cor; }
    public void setCor(String cor) { this.cor = cor; }

    public int getVagas() { return vagas; }
    public void setVagas(int vagas) { this.vagas = vagas; }
}
