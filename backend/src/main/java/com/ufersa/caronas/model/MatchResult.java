package com.ufersa.caronas.model;

public class MatchResult {
    private Usuario motorista;
    private Rota rota;
    private double score;          // quanto maior, melhor
    private int diferencaMinutos;  // diferenca entre horarios
    private String compatibilidade; // descricao legivel

    public MatchResult() {}

    public MatchResult(Usuario motorista, Rota rota, double score,
                       int diferencaMinutos, String compatibilidade) {
        this.motorista = motorista;
        this.rota = rota;
        this.score = score;
        this.diferencaMinutos = diferencaMinutos;
        this.compatibilidade = compatibilidade;
    }

    public Usuario getMotorista() { return motorista; }
    public void setMotorista(Usuario motorista) { this.motorista = motorista; }

    public Rota getRota() { return rota; }
    public void setRota(Rota rota) { this.rota = rota; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public int getDiferencaMinutos() { return diferencaMinutos; }
    public void setDiferencaMinutos(int diferencaMinutos) { this.diferencaMinutos = diferencaMinutos; }

    public String getCompatibilidade() { return compatibilidade; }
    public void setCompatibilidade(String compatibilidade) { this.compatibilidade = compatibilidade; }
}
