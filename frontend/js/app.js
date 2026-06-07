/* =========================================================================
   Carona Universidades - UI logic
   ========================================================================= */
(function () {
  const $  = (sel, root = document) => root.querySelector(sel);
  const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

  const state = {
    usuario: JSON.parse(localStorage.getItem("usuario") || "null"),
    bairros: [],
    universidades: [],
    coordenadas: {},          // { bairro: {lat,lng} }
    ultimoMatches: [],        // cache do ultimo resultado
    mapa: null,               // instancia Leaflet
    camadaTrajeto: null,      // LayerGroup do trajeto atual
    trajetoAtivo: null,       // {passageiroId, rotaId}
    cadastroLocal: null,      // {cep, latitude, longitude} resolvido pelo CEP
  };

  // ============================================================ bootstrap
  document.addEventListener("DOMContentLoaded", async () => {
    bindEvents();
    refreshUserChip();
    try {
      const [bairros, coords, universidades] = await Promise.all([
        api.listarBairros(),
        api.coordenadasBairros(),
        api.listarUniversidades(),
      ]);
      state.bairros = bairros;
      state.coordenadas = coords;
      state.universidades = universidades;
      preencherSelectsBairro();
      preencherSelectsUniversidade();
      preencherFiltroCaronasUniversidade();
    } catch (err) {
      console.error("Falha ao carregar dados iniciais:", err);
      showToast("Backend offline. Inicie o Spring Boot em http://localhost:8080.", "error");
    }
    // A lista de caronas e as stats nao dependem do login — carrega ja,
    // sem ficar preso atras da validacao do usuario.
    atualizarStats();
    carregarCaronas();
    // Valida que o usuario do localStorage ainda existe no backend.
    // Se o backend reiniciou (docker compose down/up), os IDs custom somem.
    await validarUsuarioLogado();
    // Com os selects ja preenchidos e o login validado, aplica o modo do
    // formulario (criar conta x atualizar meus dados).
    aplicarModoCadastro();
  });

  async function validarUsuarioLogado() {
    if (!state.usuario) return;
    try {
      const u = await api.buscarUsuario(state.usuario.id);
      // re-sincroniza com os dados mais recentes do backend
      state.usuario = u;
      localStorage.setItem("usuario", JSON.stringify(u));
      refreshUserChip();
    } catch (err) {
      // 404 ou similar: usuario nao existe mais
      console.warn("Usuario logado nao existe mais no backend, limpando sessao.");
      localStorage.removeItem("usuario");
      state.usuario = null;
      refreshUserChip();
      const root = $("#results");
      if (root) {
        root.innerHTML = `<div class="results__empty fadein">
          <span class="numeral numeral--ghost">↺</span>
          <p><strong>Sua sessão expirou.</strong> O backend foi reiniciado e os usuários criados anteriormente não estão mais cadastrados. Faça um novo cadastro acima.</p>
        </div>`;
      }
    }
  }

  // ============================================================ helpers
  function preencherSelectsBairro() {
    const opts = state.bairros
      .map((b) => `<option value="${b}">${b}</option>`)
      .join("");
    ["selectBairroUsuario", "selectBairroOrigem"].forEach((id) => {
      const el = document.getElementById(id);
      if (el) el.innerHTML = `<option value="">Selecione…</option>` + opts;
    });
  }

  function preencherSelectsUniversidade() {
    const opts = state.universidades
      .map((u, i) => `<option value="${u}"${i === 0 ? " selected" : ""}>${u}</option>`)
      .join("");
    ["selectUniversidadeUsuario", "selectDestinoRota", "selectDestinoBusca"].forEach((id) => {
      const el = document.getElementById(id);
      if (el) el.innerHTML = opts;
    });
  }

  function preencherFiltroCaronasUniversidade() {
    const el = document.getElementById("selectUniversidadeCaronas");
    if (!el) return;
    const opts = state.universidades
      .map((u) => `<option value="${u}">${u}</option>`)
      .join("");
    el.innerHTML = `<option value="">Todas</option>` + opts;
  }

  function refreshUserChip() {
    const chip = $("#userchip");
    const semLogin = $("#rotaSemLogin");
    if (state.usuario) {
      chip.hidden = false;
      const u = state.usuario;
      // mostra nome curto + universidade + bairro para o usuario saber
      // exatamente como esta logado
      $("#userchipName").textContent =
        `${u.nome.split(" ")[0]} · ${u.universidade || "—"} · ${u.bairro || "—"}`;
      semLogin.hidden = true;
    } else {
      chip.hidden = true;
      semLogin.hidden = false;
    }
  }

  function showToast(msg, type = "ok") {
    console[type === "error" ? "warn" : "log"](msg);
  }

  async function atualizarStats() {
    try {
      const s = await api.statsUsuarios();
      $("#statUsuarios").textContent = s.totalUsuarios ?? "—";
      $("#statBairros").textContent  = s.totalBairros  ?? "—";
      $("#statBaldes").textContent   = s.baldesOcupadosNoIndiceBairro ?? "—";
      const rotas = await api.listarRotas();
      $("#statRotas") && ($("#statRotas").textContent = rotas.length);
    } catch (e) { console.warn(e); }
  }

  // ============================================================ events
  function bindEvents() {
    $("#checkMotorista").addEventListener("change", (e) => {
      $("#formVeiculo").hidden = !e.target.checked;
    });

    const cepEl = $("#inputCep");
    if (cepEl) {
      cepEl.addEventListener("input", onCepInput);
      cepEl.addEventListener("blur", onCepLookup);
    }
    // ao informar/alterar o numero, refina a coordenada do endereco
    $("#inputNumero")?.addEventListener("blur", () => {
      if (($("#inputCep")?.value || "").replace(/\D/g, "").length === 8) onCepLookup();
    });

    $("#logoutBtn").addEventListener("click", () => {
      localStorage.removeItem("usuario");
      state.usuario = null;
      state.cadastroLocal = null;
      refreshUserChip();
      $("#formUsuario").reset();
      $("#formVeiculo").hidden = true;
      aplicarModoCadastro(); // volta para "Criar conta"
    });

    $("#formUsuario").addEventListener("submit", onSubmitUsuario);
    $("#formRota").addEventListener("submit", onSubmitRota);
    $("#formBusca").addEventListener("submit", onSubmitBusca);
    $("#formCaronas").addEventListener("submit", (e) => {
      e.preventDefault();
      carregarCaronas();
    });

    // Modal
    $$("[data-close]").forEach((el) =>
      el.addEventListener("click", fecharModal)
    );
    document.addEventListener("keydown", (e) => {
      if (e.key === "Escape") fecharModal();
    });
    $("#btnConfirmar").addEventListener("click", confirmarCarona);
  }

  // ============================================================ CEP
  // Mascara simples: 00000-000
  function onCepInput(e) {
    const dig = e.target.value.replace(/\D/g, "").slice(0, 8);
    e.target.value = dig.length > 5 ? `${dig.slice(0, 5)}-${dig.slice(5)}` : dig;
    // qualquer edicao invalida a coordenada resolvida anteriormente
    state.cadastroLocal = null;
  }

  function setCepFeedback(msg, cls = "") {
    const fb = $("#cepFeedback");
    if (!fb) return;
    fb.className = "form__hint" + (cls ? " " + cls : "");
    fb.textContent = msg;
  }

  // Normaliza para comparar bairros (sem acento, minusculo)
  function normalizar(str) {
    return String(str ?? "")
      .normalize("NFD").replace(/[̀-ͯ]/g, "")
      .trim().toLowerCase();
  }

  function selecionarBairro(nomeViaCep) {
    const alvo = normalizar(nomeViaCep);
    if (!alvo) return null;
    const match = state.bairros.find((b) => normalizar(b) === alvo);
    if (match) {
      const sel = $("#selectBairroUsuario");
      if (sel) sel.value = match;
      return match;
    }
    return null;
  }

  // Consulta ViaCEP -> preenche bairro; depois geocodifica (Nominatim/OSM)
  // para obter lat/lng precisos do endereco.
  async function onCepLookup() {
    const cepEl = $("#inputCep");
    if (!cepEl) return;
    const cep = cepEl.value.replace(/\D/g, "");
    if (cep.length !== 8) {
      if (cep.length > 0) setCepFeedback("CEP incompleto — use 8 dígitos.", "error");
      return;
    }

    setCepFeedback("Buscando endereço…", "loading");
    let endereco;
    try {
      const res = await fetch(`https://viacep.com.br/ws/${cep}/json/`);
      endereco = await res.json();
      if (endereco.erro) {
        setCepFeedback("CEP não encontrado. Selecione o bairro manualmente.", "error");
        return;
      }
    } catch (err) {
      setCepFeedback("Não foi possível consultar o CEP agora. Selecione o bairro manualmente.", "error");
      return;
    }

    const bairroMatch = selecionarBairro(endereco.bairro);

    // Preenche a rua automaticamente (sem sobrescrever o que o usuario digitou)
    const ruaEl = $("#inputLogradouro");
    if (ruaEl && !ruaEl.value.trim()) ruaEl.value = endereco.logradouro || "";
    const rua = ruaEl ? ruaEl.value.trim() : (endereco.logradouro || "");
    const numero = ($("#inputNumero")?.value || "").trim();

    const partes = [
      numero ? `${rua}, ${numero}` : rua,
      endereco.bairro, endereco.localidade, endereco.uf,
    ].filter(Boolean).join(", ");

    // Geocodifica o endereco para coordenada precisa (opcional, melhora a rota)
    let coord = null;
    try {
      coord = await geocodificar({ rua, numero, cidade: endereco.localidade, uf: endereco.uf, cep });
    } catch (err) {
      console.warn("Geocodificação falhou:", err);
    }

    if (coord) {
      state.cadastroLocal = { cep, latitude: coord.lat, longitude: coord.lng };
      const ondeBairro = bairroMatch
        ? `Bairro <strong>${bairroMatch}</strong> selecionado.`
        : `Bairro “${endereco.bairro || "?"}” não está na lista — escolha o mais próximo.`;
      setCepFeedback("", "ok");
      const fb = $("#cepFeedback");
      if (fb) fb.innerHTML =
        `📍 ${partes || cep} — localização precisa salva. ${ondeBairro}`;
    } else {
      // sem coordenada, mas ainda guardamos o CEP
      state.cadastroLocal = { cep, latitude: null, longitude: null };
      if (bairroMatch) {
        setCepFeedback(`Bairro ${bairroMatch} selecionado pelo CEP.`, "ok");
      } else {
        setCepFeedback(`Endereço: ${partes || cep}. Selecione o bairro na lista.`, "");
      }
    }
  }

  // Nominatim (OpenStreetMap): tenta pela rua (+numero); cai pro CEP se preciso.
  async function geocodificar({ rua, numero, cidade, uf, cep }) {
    const tentativas = [];
    if (rua) {
      tentativas.push(
        `https://nominatim.openstreetmap.org/search?format=json&limit=1&` +
        new URLSearchParams({
          street: numero ? `${numero} ${rua}` : rua,
          city: cidade || "",
          state: uf || "",
          country: "Brasil",
        })
      );
    }
    tentativas.push(
      `https://nominatim.openstreetmap.org/search?format=json&limit=1&` +
      new URLSearchParams({ postalcode: cep, country: "Brasil" })
    );

    for (const url of tentativas) {
      try {
        const res = await fetch(url, { headers: { "Accept": "application/json" } });
        const data = await res.json();
        if (Array.isArray(data) && data.length) {
          return { lat: Number(data[0].lat), lng: Number(data[0].lon) };
        }
      } catch (e) { /* tenta a proxima */ }
    }
    return null;
  }

  // ============================================================ submit usuario
  async function onSubmitUsuario(e) {
    e.preventDefault();
    const fb = $("#usuarioFeedback");
    fb.className = "form__feedback"; fb.textContent = "";

    const fd = new FormData(e.target);
    const motorista = fd.get("motorista") === "on";
    const payload = {
      nome: fd.get("nome"),
      email: fd.get("email"),
      curso: fd.get("curso"),
      universidade: fd.get("universidade"),
      bairro: fd.get("bairro"),
      cep: (fd.get("cep") || "").replace(/\D/g, "") || null,
      logradouro: (fd.get("logradouro") || "").trim() || null,
      numero: (fd.get("numero") || "").trim() || null,
      motorista,
    };
    // coordenada precisa resolvida pelo CEP (se o CEP no estado bate com o digitado)
    if (state.cadastroLocal && state.cadastroLocal.cep === payload.cep) {
      payload.latitude = state.cadastroLocal.latitude;
      payload.longitude = state.cadastroLocal.longitude;
    }
    if (motorista) {
      payload.veiculoModelo = fd.get("veiculoModelo");
      payload.veiculoPlaca  = fd.get("veiculoPlaca");
      payload.veiculoCor    = fd.get("veiculoCor");
      payload.veiculoVagas  = Number(fd.get("veiculoVagas")) || 1;
    }

    const editando = !!state.usuario;
    try {
      const u = editando
        ? await api.atualizarUsuario(state.usuario.id, payload)
        : await api.criarUsuario(payload);
      state.usuario = u;
      localStorage.setItem("usuario", JSON.stringify(u));
      refreshUserChip();
      atualizarStats();
      carregarCaronas();

      if (editando) {
        const end = u.logradouro
          ? `${u.logradouro}${u.numero ? ", " + u.numero : ""} - ${u.bairro}`
          : u.bairro;
        fb.textContent = u.latitude != null
          ? `Dados atualizados! Seu endereço (${end}) foi localizado com precisão — a rota da carona agora parte do seu ponto exato.`
          : `Dados atualizados! Informe o CEP para localizar seu endereço com precisão e melhorar a rota.`;
        aplicarModoCadastro(); // re-sincroniza o form com os dados salvos
      } else {
        fb.textContent = `Conta criada! Você é ${u.motorista ? "motorista" : "passageiro"} no bairro ${u.bairro}. Agora cadastre sua rota.`;
        aplicarModoCadastro(); // passa para modo "atualizar meus dados"
        setTimeout(() => document.querySelector("#rota").scrollIntoView({ behavior: "smooth" }), 600);
      }
    } catch (err) {
      fb.className = "form__feedback error";
      fb.textContent = `Erro ao ${editando ? "atualizar" : "criar conta"}: ` + err.message;
    }
  }

  // Pre-preenche o formulario de cadastro com os dados do usuario logado e
  // muda o botao para "Atualizar meus dados". Sem login, volta ao modo criar.
  function aplicarModoCadastro() {
    const f = $("#formUsuario");
    if (!f) return;
    const btn = f.querySelector("button[type=submit]");
    const titulo = $("#cadastroTitulo");
    const u = state.usuario;
    if (u) {
      f.nome.value = u.nome || "";
      f.email.value = u.email || "";
      f.curso.value = u.curso || "";
      if (u.universidade) f.universidade.value = u.universidade;
      if (u.bairro) f.bairro.value = u.bairro;
      f.cep.value = u.cep ? formatarCep(u.cep) : "";
      f.logradouro.value = u.logradouro || "";
      f.numero.value = u.numero || "";
      $("#checkMotorista").checked = !!u.motorista;
      $("#formVeiculo").hidden = !u.motorista;
      if (u.veiculo) {
        f.veiculoModelo.value = u.veiculo.modelo || "";
        f.veiculoPlaca.value  = u.veiculo.placa || "";
        f.veiculoCor.value    = u.veiculo.cor || "";
        f.veiculoVagas.value  = u.veiculo.vagas || 3;
      }
      // mantem a coordenada salva para reenvio se o CEP nao for re-digitado
      if (u.latitude != null && u.longitude != null && u.cep) {
        state.cadastroLocal = {
          cep: String(u.cep).replace(/\D/g, ""),
          latitude: u.latitude, longitude: u.longitude,
        };
      }
      if (btn) btn.textContent = "Atualizar meus dados";
      if (titulo) titulo.textContent = "Meus dados";
    } else {
      if (btn) btn.textContent = "Criar conta";
      if (titulo) titulo.textContent = "Cadastro";
    }
  }

  function formatarCep(v) {
    const d = String(v).replace(/\D/g, "").slice(0, 8);
    return d.length > 5 ? `${d.slice(0, 5)}-${d.slice(5)}` : d;
  }

  // ============================================================ submit rota
  async function onSubmitRota(e) {
    e.preventDefault();
    const fb = $("#rotaFeedback");
    fb.className = "form__feedback"; fb.textContent = "";
    if (!state.usuario) {
      fb.className = "form__feedback error";
      fb.textContent = "Cadastre-se primeiro acima."; return;
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
      atualizarStats();
      carregarCaronas();
      setTimeout(() => document.querySelector("#buscar").scrollIntoView({ behavior: "smooth" }), 500);
    } catch (err) {
      fb.className = "form__feedback error";
      fb.textContent = "Erro: " + err.message;
    }
  }

  // ============================================================ busca
  async function onSubmitBusca(e) {
    e.preventDefault();
    if (!state.usuario) {
      renderResults({ erro: "Você precisa estar cadastrado para buscar caronas." });
      return;
    }
    const fd = new FormData(e.target);
    const horario = fd.get("horario");
    const destino = fd.get("destino");
    // re-valida o usuario logado antes de buscar
    try {
      await api.buscarUsuario(state.usuario.id);
    } catch {
      await validarUsuarioLogado();
      return;
    }
    renderResults({ carregando: true, contexto: { horario, destino } });
    try {
      const matches = await api.buscarMatch(state.usuario.id, horario, destino);
      state.ultimoMatches = matches;
      renderResults({ matches, contexto: { horario, destino } });
    } catch (err) {
      renderResults({ erro: err.message });
    }
  }

  // ============================================================ render results
  function renderResults({ carregando, erro, matches, contexto }) {
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
      const u = state.usuario || {};
      const dest = contexto?.destino || "—";
      const matchaDest = !contexto?.destino || contexto.destino === u.universidade;
      let dica = "";
      if (!matchaDest) {
        dica = `Você é da <strong>${escapeHtml(u.universidade)}</strong>
                mas está buscando carona pra <strong>${escapeHtml(dest)}</strong>.
                Pode ser que ninguém da sua região vá pra lá nesse horário.
                Tente buscar com destino <strong>${escapeHtml(u.universidade)}</strong>.`;
      } else {
        dica = `Tente um horário diferente (±30 min), outro destino,
                ou volte ao topo e cadastre uma rota como motorista.`;
      }
      root.innerHTML = `<div class="results__empty fadein">
        <span class="numeral numeral--ghost">◯</span>
        <p><strong>Nenhum match encontrado.</strong></p>
        <p style="margin-top:8px;font-size:.85rem;color:var(--muted);">
          Você está logado como <strong>${escapeHtml(u.nome || "?")}</strong>
          (${escapeHtml(u.universidade || "?")} · ${escapeHtml(u.bairro || "?")})
          buscando pra <strong>${escapeHtml(dest)}</strong>
          às <strong>${escapeHtml(contexto?.horario || "?")}</strong>.
        </p>
        <p style="margin-top:12px;font-size:.88rem;line-height:1.55;">${dica}</p>
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

    // hook "Ver no mapa"
    $$(".match__action").forEach((btn) => {
      btn.addEventListener("click", () => {
        const rotaId = Number(btn.dataset.rota);
        const match = state.ultimoMatches.find((m) => m.rota.id === rotaId);
        if (match) abrirTrajeto(match);
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
          <br />
          <button class="match__action" data-rota="${m.rota.id}">Ver trajeto no mapa</button>
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

  // ============================================================ todas as caronas
  async function carregarCaronas() {
    const root = $("#ridesList");
    if (!root) return;
    const universidade = $("#selectUniversidadeCaronas")?.value || "";
    const tipo = $("#selectTipoCaronas")?.value || "";

    root.innerHTML = `<div class="results__empty fadein">
      <span class="numeral numeral--ghost">⌛</span>
      <p>Carregando caronas disponíveis…</p>
    </div>`;

    try {
      const [rotas, usuarios] = await Promise.all([
        api.listarRotas(),
        api.listarUsuarios(),
      ]);
      // indexa motoristas por id (join rota -> usuario)
      const usuariosPorId = {};
      usuarios.forEach((u) => { usuariosPorId[u.id] = u; });

      let lista = rotas
        .map((r) => ({ rota: r, motorista: usuariosPorId[r.usuarioId] }))
        .filter((x) => x.motorista && x.motorista.motorista); // so caronas de motoristas

      if (universidade) lista = lista.filter((x) => x.rota.destino === universidade);
      if (tipo) lista = lista.filter((x) => x.rota.tipo === tipo);

      // ordena por universidade, depois horario de saida
      lista.sort((a, b) => {
        const d = a.rota.destino.localeCompare(b.rota.destino);
        if (d !== 0) return d;
        return a.rota.horarioSaida.localeCompare(b.rota.horarioSaida);
      });

      if (!lista.length) {
        root.innerHTML = `<div class="results__empty fadein">
          <span class="numeral numeral--ghost">◯</span>
          <p><strong>Nenhuma carona disponível</strong> com esse filtro.</p>
        </div>`;
        return;
      }

      root.innerHTML = lista.map((x, i) => rideCard(x, i + 1)).join("");
    } catch (err) {
      root.innerHTML = `<div class="results__empty fadein">
        <span class="numeral numeral--ghost">!</span>
        <p>Erro ao carregar caronas: ${escapeHtml(err.message)}</p>
      </div>`;
    }
  }

  function rideCard({ rota, motorista }, rank) {
    const v = motorista.veiculo || {};
    const horario = rota.horarioSaida.slice(0, 5);
    const avaliacao = Number(motorista.avaliacao || 0).toFixed(1);
    const tipoLabel = rota.tipo === "VOLTA" ? "Volta" : "Ida";
    return `
      <article class="match fadein">
        <div class="match__rank">${rank}.</div>
        <div class="match__main">
          <h4>${escapeHtml(motorista.nome)}</h4>
          <p class="match__sub">${escapeHtml(motorista.curso)} · ${escapeHtml(motorista.universidade)}</p>
          <div class="match__meta">
            <span>${iconClock()} ${horario} · ${tipoLabel}</span>
            <span>${iconCar()} ${escapeHtml(v.modelo || "—")} ${v.cor ? "(" + escapeHtml(v.cor) + ")" : ""}</span>
            <span>${iconSeat()} ${rota.vagasDisponiveis} vagas</span>
            <span>${iconStar()} ${avaliacao} (${motorista.totalAvaliacoes || 0})</span>
          </div>
          <span class="match__compat">${escapeHtml(rota.bairroOrigem)} → ${escapeHtml(rota.destino)}</span>
        </div>
        <div class="match__score">
          <span class="match__scorenum" style="font-size:1.5rem;">${escapeHtml(rota.destino)}</span>
          <span class="match__scorelbl">DESTINO</span>
        </div>
      </article>
    `;
  }

  // ============================================================ MODAL DE TRAJETO
  async function abrirTrajeto(match) {
    const passageiroId = state.usuario.id;
    const rotaId = match.rota.id;
    state.trajetoAtivo = { passageiroId, rotaId, match };

    // Abre modal
    $("#modalTrajeto").hidden = false;
    document.body.style.overflow = "hidden";

    $("#trajetoTitle").textContent =
      `${match.motorista.nome} → você → UFERSA`;
    $("#trajetoAlgoritmoTxt").textContent = "Calculando caminho mínimo…";

    // Limpa stepper / metrics
    $("#stepper").innerHTML = `<li class="stepper__loading">Carregando trajetória...</li>`;
    ["metricDist","metricTempo","metricEcon"].forEach((id) => $("#" + id).textContent = "—");
    $("#bairrosTrail").innerHTML = "";
    const fb = $("#trajetoFeedback");
    fb.className = "trajeto__feedback"; fb.textContent = "";

    // Garante o mapa criado (no primeiro abrir, cria; depois reusa)
    ensureMap();

    try {
      const trajeto = await api.trajetoria(passageiroId, rotaId);
      renderTrajeto(trajeto, match);
    } catch (err) {
      fb.className = "trajeto__feedback error";
      fb.textContent = "Erro ao calcular trajeto: " + err.message;
    }
  }

  function fecharModal() {
    $("#modalTrajeto").hidden = true;
    document.body.style.overflow = "";
  }

  function ensureMap() {
    if (state.mapa) {
      // forca re-render do tile no proximo tick (modal acabou de ficar visivel)
      setTimeout(() => state.mapa.invalidateSize(), 100);
      return;
    }
    // centro aproximado de Mossoro
    state.mapa = L.map("mapa", {
      zoomControl: true,
      scrollWheelZoom: true,
    }).setView([-5.197, -37.345], 13);

    L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
      attribution: "&copy; <a href='https://openstreetmap.org/copyright'>OpenStreetMap</a>",
      maxZoom: 19,
    }).addTo(state.mapa);

    // marcadores de todos os bairros (cinza, sutil)
    Object.entries(state.coordenadas).forEach(([nome, c]) => {
      if (nome === "UFERSA") return;
      L.marker([c.lat, c.lng], {
        icon: L.divIcon({ className: "marker-other", iconSize: [10, 10] }),
        keyboard: false,
        interactive: true,
      })
        .bindTooltip(nome, { direction: "top", offset: [0, -4] })
        .addTo(state.mapa);
    });

    setTimeout(() => state.mapa.invalidateSize(), 200);
  }

  async function renderTrajeto(trajeto, match) {
    // Remove camada anterior
    if (state.camadaTrajeto) {
      state.camadaTrajeto.clearLayers();
      state.mapa.removeLayer(state.camadaTrajeto);
    }
    state.camadaTrajeto = L.layerGroup().addTo(state.mapa);

    // ------- stepper -------
    const stepHtml = trajeto.paradas.map((p, idx) => {
      const dotClass = p.tipo === "PICKUP" ? "stepper__dot--pickup"
                     : p.tipo === "DESTINO" ? "stepper__dot--destino"
                     : "";
      const label = p.tipo === "PARTIDA" ? "Início" :
                    p.tipo === "PICKUP"  ? "Pickup" : "Chegada";
      return `<li>
        <span class="stepper__dot ${dotClass}">${idx + 1}</span>
        <div class="stepper__title">${escapeHtml(p.bairro)}</div>
        <div class="stepper__sub">${escapeHtml(p.descricao)}</div>
        <span class="stepper__kind">${label}</span>
      </li>`;
    }).join("");
    $("#stepper").innerHTML = stepHtml;

    // ------- metrics -------
    $("#metricDist").textContent = trajeto.distanciaTotalKm.toFixed(1);
    $("#metricTempo").textContent = trajeto.tempoEstimadoMin;
    $("#metricEcon").textContent = "R$" + trajeto.economiaEstimadaReais.toFixed(2);

    // ------- bairros trail -------
    const bairros = trajeto.bairrosCaminho || [];
    $("#bairrosTrail").innerHTML = `
      <div class="bairros-trail__title">Caminho calculado pelo Dijkstra</div>
      <div class="bairros-trail__path">
        ${bairros.map((b) => escapeHtml(b)).join(' <span>→</span> ')}
      </div>
    `;
    $("#trajetoAlgoritmoTxt").textContent = trajeto.algoritmoUsado;

    // ------- mapa: marcadores das paradas -------
    const paradasLatLng = [];
    trajeto.paradas.forEach((p, idx) => {
      if (!p.coordenada) return;
      const pinClass = p.tipo === "PICKUP" ? "marker-pin--pickup"
                     : p.tipo === "DESTINO" ? "marker-pin--destino"
                     : "";
      const icon = L.divIcon({
        className: "",
        html: `<div class="marker-pin ${pinClass}"><span>${idx + 1}</span></div>`,
        iconSize: [32, 32],
        iconAnchor: [16, 32],
        popupAnchor: [0, -28],
      });
      const marker = L.marker([p.coordenada.lat, p.coordenada.lng], { icon })
        .bindPopup(`<strong>${escapeHtml(p.bairro)}</strong><br>${escapeHtml(p.descricao)}`)
        .addTo(state.camadaTrajeto);
      paradasLatLng.push([p.coordenada.lat, p.coordenada.lng]);
    });

    // ------- mapa: linha do trajeto -------
    // Tenta usar OSRM (rota real nas ruas). Fallback: liga os bairros do
    // caminho com linhas retas + Haversine.
    const fallbackLatLngs = (trajeto.caminhoPoligono || [])
      .filter((c) => c)
      .map((c) => [c.lat, c.lng]);

    // 1) Desenha ja uma linha tracejada com o caminho Dijkstra (mostra o
    //    "trajeto algoritmico" enquanto o roteamento real carrega)
    const linhaDijkstra = L.polyline(fallbackLatLngs, {
      color: "#2D5A4F",
      weight: 3,
      opacity: 0.55,
      dashArray: "6 8",
    }).addTo(state.camadaTrajeto);

    // 2) Tenta enriquecer com a rota real (OSRM publico). Se falhar, mantemos
    //    a polyline tracejada.
    try {
      const realLatLngs = await rotaRealOSRM(fallbackLatLngs);
      if (realLatLngs && realLatLngs.length) {
        L.polyline(realLatLngs, {
          color: "#B8451E",
          weight: 5,
          opacity: 0.85,
          lineCap: "round",
          lineJoin: "round",
        }).addTo(state.camadaTrajeto);
        linhaDijkstra.setStyle({ opacity: 0.25 });
      }
    } catch (e) {
      console.warn("OSRM indisponivel - usando linhas retas:", e);
    }

    // ------- mapa: ajusta zoom para encaixar tudo -------
    if (paradasLatLng.length) {
      state.mapa.fitBounds(paradasLatLng, { padding: [50, 50], maxZoom: 15 });
    }
    // garante refresh visual
    setTimeout(() => state.mapa.invalidateSize(), 50);
  }

  // Chama OSRM para obter rota real entre N pontos (via waypoints).
  async function rotaRealOSRM(pontos) {
    if (!pontos || pontos.length < 2) return null;
    const coords = pontos.map(([lat, lng]) => `${lng},${lat}`).join(";");
    const url = `https://router.project-osrm.org/route/v1/driving/${coords}?overview=full&geometries=geojson`;
    const res = await fetch(url);
    if (!res.ok) throw new Error("OSRM falhou: " + res.status);
    const data = await res.json();
    if (!data.routes || !data.routes.length) return null;
    // GeoJSON: [lng,lat]
    return data.routes[0].geometry.coordinates.map(([lng, lat]) => [lat, lng]);
  }

  async function confirmarCarona() {
    const fb = $("#trajetoFeedback");
    fb.className = "trajeto__feedback"; fb.textContent = "Carona confirmada! Boa viagem 🚗";
    setTimeout(() => fecharModal(), 1200);
  }

  // ============================================================ utils
  function escapeHtml(str) {
    return String(str ?? "").replace(/[&<>"']/g, (c) => ({
      "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;",
    })[c]);
  }

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
