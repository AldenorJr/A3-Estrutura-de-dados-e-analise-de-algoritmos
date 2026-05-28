package com.ufersa.caronas.model;

import java.util.concurrent.atomic.AtomicLong;

public class Usuario {
    private static final AtomicLong SEQ = new AtomicLong(1);

    private Long id;
    private String nome;
    private String email;
    private String curso;
    private String bairro;
    private String universidade;
    private boolean motorista;
    private double avaliacao; // 0.0 a 5.0
    private int totalAvaliacoes;
    private Veiculo veiculo; // null se nao for motorista

    public Usuario() {
        this.id = SEQ.getAndIncrement();
        this.avaliacao = 5.0;
        this.totalAvaliacoes = 0;
    }

    public Usuario(String nome, String email, String curso, String bairro,
                   String universidade, boolean motorista, Veiculo veiculo) {
        this();
        this.nome = nome;
        this.email = email;
        this.curso = curso;
        this.bairro = bairro;
        this.universidade = universidade;
        this.motorista = motorista;
        this.veiculo = veiculo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCurso() { return curso; }
    public void setCurso(String curso) { this.curso = curso; }

    public String getBairro() { return bairro; }
    public void setBairro(String bairro) { this.bairro = bairro; }

    public String getUniversidade() { return universidade; }
    public void setUniversidade(String universidade) { this.universidade = universidade; }

    public boolean isMotorista() { return motorista; }
    public void setMotorista(boolean motorista) { this.motorista = motorista; }

    public double getAvaliacao() { return avaliacao; }
    public void setAvaliacao(double avaliacao) { this.avaliacao = avaliacao; }

    public int getTotalAvaliacoes() { return totalAvaliacoes; }
    public void setTotalAvaliacoes(int totalAvaliacoes) { this.totalAvaliacoes = totalAvaliacoes; }

    public Veiculo getVeiculo() { return veiculo; }
    public void setVeiculo(Veiculo veiculo) { this.veiculo = veiculo; }

    public void registrarAvaliacao(double nota) {
        double soma = this.avaliacao * this.totalAvaliacoes + nota;
        this.totalAvaliacoes++;
        this.avaliacao = soma / this.totalAvaliacoes;
    }
}
