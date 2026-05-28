package com.ufersa.caronas.model;

/**
 * Coordenada geografica (latitude/longitude) usada para posicionar
 * bairros e UFERSA no mapa.
 */
public class Coordenada {
    private double lat;
    private double lng;

    public Coordenada() {}

    public Coordenada(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }

    /**
     * Distancia em km via formula de Haversine (esfera terrestre).
     * Util como fallback quando nao temos peso explicito da aresta.
     */
    public double distanciaKm(Coordenada outra) {
        final double R = 6371.0;
        double dLat = Math.toRadians(outra.lat - this.lat);
        double dLng = Math.toRadians(outra.lng - this.lng);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(this.lat)) * Math.cos(Math.toRadians(outra.lat))
                 * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
