package com.ufersa.caronas.model;

import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicLong;

public class Rota {
    private static final AtomicLong SEQ = new AtomicLong(1);

    private Long id;
    private Long usuarioId;
    private String bairroOrigem;
    private String destino; // universidade
    private LocalTime horarioSaida;
    private int vagasDisponiveis;
    private TipoRota tipo; // IDA ou VOLTA

    public Rota() {
        this.id = SEQ.getAndIncrement();
    }

    public Rota(Long usuarioId, String bairroOrigem, String destino,
                LocalTime horarioSaida, int vagasDisponiveis, TipoRota tipo) {
        this();
        this.usuarioId = usuarioId;
        this.bairroOrigem = bairroOrigem;
        this.destino = destino;
        this.horarioSaida = horarioSaida;
        this.vagasDisponiveis = vagasDisponiveis;
        this.tipo = tipo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getBairroOrigem() { return bairroOrigem; }
    public void setBairroOrigem(String bairroOrigem) { this.bairroOrigem = bairroOrigem; }

    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }

    public LocalTime getHorarioSaida() { return horarioSaida; }
    public void setHorarioSaida(LocalTime horarioSaida) { this.horarioSaida = horarioSaida; }

    public int getVagasDisponiveis() { return vagasDisponiveis; }
    public void setVagasDisponiveis(int vagasDisponiveis) { this.vagasDisponiveis = vagasDisponiveis; }

    public TipoRota getTipo() { return tipo; }
    public void setTipo(TipoRota tipo) { this.tipo = tipo; }
}
