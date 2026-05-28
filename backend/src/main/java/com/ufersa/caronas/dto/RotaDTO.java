package com.ufersa.caronas.dto;

public class RotaDTO {
    public Long usuarioId;
    public String bairroOrigem;
    public String destino;
    public String horarioSaida; // formato HH:mm
    public Integer vagasDisponiveis;
    public String tipo; // IDA ou VOLTA
}
