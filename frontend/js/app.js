/* =========================================================================
   Caronas UFERSA - UI logic
   ========================================================================= */
(function () {
  const $  = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

  const state = {
    usuario: JSON.parse(localStorage.getItem("usuario") || "null"),
    bairros: [],
  };

  // ---------------------------------------------------------------- bootstrap
  document.addEventListener("DOMContentLoaded", async () => {
    bindEvents();
    refreshUserChip();
    try {
      state.bairros = await api.listarBairros();
      preencherSelectsBairro();
    } catch (err) {
      console.error("Falha ao carregar bairros:", err);
      showToast("Backend offline. Inicie o Spring Boot em http://localhost:8080.", "error");
    }
    atualizarStats();
  });

  // ---------------------------------------------------------------- helpers
  function preencherSelectsBairro() {
    const opts = state.bairros
      .map((b) => `<option value="${b}">${b}</option>`)
      .join("");
    ["selectBairroUsuario", "selectBairroOrigem"].forEach((id) => {
      const el = document.getElementById(id);
      if (el) el.innerHTML = `<option value="">Selecione…</option>` + opts;
    });
  }

  function refreshUserChip() {
    const chip = $("#userchip");
    const semLogin = $("#rotaSemLogin");
    if (state.usuario) {
      chip.hidden = false;
      $("#userchipName").textContent = state.usuario.nome.split(" ")[0];
      semLogin.hidden = true;
    } else {
      chip.hidden = true;
      semLogin.hidden = false;
    }
  }

  function showToast(msg, type = "ok") {
    // toast simples e discreto (reaproveita feedback no proximo form)
    console[type === "error" ? "warn" : "log"](msg);
  }

  async function atualizarStats() {
    try {
      const s = await api.statsUsuarios();
      $("#statUsuarios").textContent = s.totalUsuarios ?? "—";
      $("#statBairros").textContent  = s.totalBairros  ?? "—";
      $("#statBaldes").textContent   = s.baldesOcupadosNoIndiceBairro ?? "—";
    } catch (e) {
      console.warn(e);
    }
  }

  // ---------------------------------------------------------------- events
  function bindEvents() {
    $("#checkMotorista").addEventListener("change", (e) => {
      $("#formVeiculo").hidden = !e.target.checked;
    });

    $("#logoutBtn").addEventListener("click", () => {
      localStorage.removeItem("usuario");
      state.usuario = null;
      refreshUserChip();
    });

    $("#formUsuario").addEventListener("submit", onSubmitUsuario);
    $("#formRota").addEventListener("submit", onSubmitRota);
    $("#formBusca").addEventListener("submit", onSubmitBusca);
  }

  // ---------------------------------------------------------------- submit usuario
  async function onSubmitUsuario(e) {
    e.preventDefault();
    const fb = $("#usuarioFeedback");
    fb.className = "form__feedback";
    fb.textContent = "";

    const fd = new FormData(e.target);
    const motorista = fd.get("motorista") === "on";
    const payload = {
      nome: fd.get("nome"),
      email: fd.get("email"),
      curso: fd.get("curso"),
      universidade: fd.get("universidade"),
      bairro: fd.get("bairro"),
      motorista,
    };
    if (motorista) {
      payload.veiculoModelo = fd.get("veiculoModelo");
      payload.veiculoPlaca  = fd.get("veiculoPlaca");
      payload.veiculoCor    = fd.get("veiculoCor");
      payload.veiculoVagas  = Number(fd.get("veiculoVagas")) || 1;
    }

    try {
      const u = await api.criarUsuario(payload);
      state.usuario = u;
      localStorage.setItem("usuario", JSON.stringify(u));
      refreshUserChip();
      atualizarStats();
      fb.textContent = `Conta criada! Você é ${u.motorista ? "motorista" : "passageiro"} no bairro ${u.bairro}. Agora cadastre sua rota.`;
      e.target.reset();
      $("#formVeiculo").hidden = true;
      setTimeout(() => document.querySelector("#rota").scrollIntoView({ behavior: "smooth" }), 600);
    } catch (err) {
      fb.className = "form__feedback error";
      fb.textContent = "Erro ao criar conta: " + err.message;
    }
  }

  // ---------------------------------------------------------------- submit rota
  async function onSubmitRota(e) {
    e.preventDefault();
    const fb = $("#rotaFeedback");
    fb.className = "form__feedback"; fb.textContent = "";
    if (!state.usuario) {
      fb.className = "form__feedback error";
      fb.textContent = "Cadastre-se primeiro acima.";
      return;
    }
    const fd = new FormData(e.target);
    const payload = {
      usuarioId: state.usuario.id,
      bairroOrigem: fd.get("bairroOrigem"),
      destino: fd.get("destino"),
      horarioSaida: fd.get("horarioSaida"),
      vagasDisponiveis: Number(fd.get("vagasDisponiveis")) || 0,
      tipo: fd.get("tipo"),
    };
    try {
      const r = await api.criarRota(payload);
      fb.textContent = `Rota salva (#${r.id}) — ${r.bairroOrigem} → ${r.destino} às ${r.horarioSaida.slice(0,5)}.`;
      setTimeout(() => document.querySelector("#buscar").scrollIntoView({ behavior: "smooth" }), 500);
    } catch (err) {
      fb.className = "form__feedback error";
      fb.textContent = "Erro: " + err.message;
    }
  }

  // ---------------------------------------------------------------- busca
  async function onSubmitBusca(e) {
    e.preventDefault();
    if (!state.usuario) {
      renderResults({ erro: "Você precisa estar cadastrado para buscar caronas." });
      return;
    }
    const fd = new FormData(e.target);
    const horario = fd.get("horario");
    const destino = fd.get("destino");
    renderResults({ carregando: true });
    try {
      const matches = await api.buscarMatch(state.usuario.id, horario, destino);
      renderResults({ matches });
    } catch (err) {
      renderResults({ erro: err.message });
    }
  }

  // ---------------------------------------------------------------- render
  function renderResults({ carregando, erro, matches }) {
    const root = $("#results");
    if (carregando) {
      root.innerHTML = `<div class="results__empty fadein">
        <span class="numeral numeral--ghost">⌛</span>
        <p>Calculando matches via Hash → Grafo → QuickSort…</p>
      </div>`;
      return;
    }
    if (erro) {
      root.innerHTML = `<div class="results__empty fadein">
        <span class="numeral numeral--ghost">!</span>
        <p>${escapeHtml(erro)}</p>
      </div>`;
      return;
    }
    if (!matches || !matches.length) {
      root.innerHTML = `<div class="results__empty fadein">
        <span class="numeral numeral--ghost">◯</span>
        <p>Nenhum match encontrado neste horário. Tente expandir 30 min ou cadastre mais rotas.</p>
      </div>`;
      return;
    }
    root.innerHTML = matches.map((m, i) => matchCard(m, i + 1)).join("");
    // hook stars
    $$(".match__stars").forEach((stars) => {
      const motoristaId = Number(stars.dataset.motorista);
      $$("button", stars).forEach((btn) => {
        btn.addEventListener("click", async () => {
          const nota = Number(btn.dataset.nota);
          try {
            await api.avaliarMotorista(motoristaId, nota);
            $$("button", stars).forEach((b, idx) => b.classList.toggle("on", idx < nota));
          } catch (err) {
            alert("Erro ao avaliar: " + err.message);
          }
        });
      });
    });
  }

  function matchCard(m, rank) {
    const v = m.motorista.veiculo || {};
    const score = Number(m.score).toFixed(1);
    const avaliacao = Number(m.motorista.avaliacao).toFixed(1);
    const horario = m.rota.horarioSaida.slice(0, 5);
    return `
      <article class="match fadein">
        <div class="match__rank">${rank}.</div>
        <div class="match__main">
          <h4>${escapeHtml(m.motorista.nome)}</h4>
          <p class="match__sub">${escapeHtml(m.motorista.curso)} · ${escapeHtml(m.motorista.bairro)}</p>
          <div class="match__meta">
            <span>${iconClock()} ${horario}</span>
            <span>${iconCar()} ${escapeHtml(v.modelo || "—")} ${v.cor ? "(" + escapeHtml(v.cor) + ")" : ""}</span>
            <span>${iconSeat()} ${m.rota.vagasDisponiveis} vagas</span>
            <span>${iconStar()} ${avaliacao} (${m.motorista.totalAvaliacoes})</span>
          </div>
          <span class="match__compat">${escapeHtml(m.compatibilidade)}</span>
        </div>
        <div class="match__score">
          <span class="match__scorenum">${score}</span>
          <span class="match__scorelbl">SCORE</span>
          <div class="match__stars" data-motorista="${m.motorista.id}" title="Avaliar este motorista">
            ${[1,2,3,4,5].map((n) => `<button data-nota="${n}" title="${n} estrelas">★</button>`).join("")}
          </div>
        </div>
      </article>
    `;
  }

  function escapeHtml(str) {
    return String(str ?? "").replace(/[&<>"']/g, (c) => ({
      "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
    })[c]);
  }

  // icons (inline SVGs)
  function iconClock() {
    return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg>`;
  }
  function iconCar() {
    return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M3 13l2-5h14l2 5"/><rect x="3" y="13" width="18" height="6" rx="1.5"/><circle cx="7.5" cy="19" r="1.3"/><circle cx="16.5" cy="19" r="1.3"/></svg>`;
  }
  function iconSeat() {
    return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"><path d="M6 20v-3a3 3 0 013-3h6a3 3 0 013 3v3"/><circle cx="12" cy="7" r="3"/></svg>`;
  }
  function iconStar() {
    return `<svg viewBox="0 0 24 24" fill="currentColor"><path d="M12 2l3 7h7l-5.5 4.5L18 21l-6-4-6 4 1.5-7.5L2 9h7z"/></svg>`;
  }
})();
