/**
 * API client - todas as chamadas pro backend Spring Boot.
 *
 * Por padrao, aponta para http://localhost:8080.
 * Pode ser sobrescrito via window.API_BASE antes do app.js carregar.
 */
(function () {
  const BASE = window.API_BASE || "http://localhost:8080";

  async function request(path, opts = {}) {
    const res = await fetch(BASE + path, {
      headers: { "Content-Type": "application/json", ...(opts.headers || {}) },
      ...opts,
    });
    if (!res.ok) {
      const text = await res.text().catch(() => "");
      throw new Error(`${res.status} ${res.statusText}${text ? ` — ${text}` : ""}`);
    }
    // 204 ou body vazio
    if (res.status === 204) return null;
    const ct = res.headers.get("content-type") || "";
    return ct.includes("application/json") ? res.json() : res.text();
  }

  window.api = {
    listarBairros: () => request("/api/bairros"),
    listarUniversidades: () => request("/api/universidades"),

    listarUsuarios: () => request("/api/usuarios"),
    criarUsuario:   (u) => request("/api/usuarios", { method: "POST", body: JSON.stringify(u) }),
    buscarUsuario:  (id) => request(`/api/usuarios/${id}`),
    avaliarMotorista: (motoristaId, nota) =>
      request("/api/usuarios/avaliar", {
        method: "POST",
        body: JSON.stringify({ motoristaId, nota }),
      }),
    statsUsuarios: () => request("/api/usuarios/stats"),

    listarRotas:    () => request("/api/rotas"),
    criarRota:      (r) => request("/api/rotas", { method: "POST", body: JSON.stringify(r) }),
    rotasDoUsuario: (id) => request(`/api/rotas/usuario/${id}`),

    buscarMatch: (usuarioId, horario, destino) => {
      const params = new URLSearchParams({ usuarioId, horario });
      if (destino) params.set("destino", destino);
      return request(`/api/match?${params.toString()}`);
    },

    trajetoria: (passageiroId, rotaId) =>
      request(`/api/trajetoria?passageiroId=${passageiroId}&rotaId=${rotaId}`),

    coordenadasBairros: () => request("/api/trajetoria/coordenadas"),
  };
})();
