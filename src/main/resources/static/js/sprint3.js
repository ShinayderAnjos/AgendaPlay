const senha = document.getElementById("senha"),
  forca = document.getElementById("forca-senha");
if (senha && forca)
  senha.addEventListener("input", () => {
    const faltam = [];
    if (senha.value.length < 8) faltam.push("8 caracteres");
    if (!/[A-Z]/.test(senha.value)) faltam.push("maiúscula");
    if (!/[a-z]/.test(senha.value)) faltam.push("minúscula");
    if (!/[0-9]/.test(senha.value)) faltam.push("número");
    if (!/[^a-zA-Z0-9\s]/.test(senha.value)) faltam.push("símbolo");
    forca.textContent = faltam.length
      ? "Falta: " + faltam.join(", ")
      : "Senha atende aos requisitos.";
    senha.setCustomValidity(
      faltam.length ? "Complete os requisitos da senha." : "",
    );
  });
const padrao = document.getElementById("form-padrao");
const minutos = (v) => {
  const [h, m] = v.split(":").map(Number);
  return h * 60 + m;
};
const horario = (v) =>
  String(Math.floor(v / 60)).padStart(2, "0") +
  ":" +
  String(v % 60).padStart(2, "0");
function sugerir() {
  if (!padrao) return;
  const a = minutos(padrao.horaInicio.value),
    b = minutos(padrao.horaFim.value),
    d = Number(padrao.duracaoMinutos.value),
    t = Number(padrao.toleranciaMinutos.value);
  const lista = [];
  if (d > 0 && t >= 0 && b > a)
    for (let i = a; i + d <= b; i += d + t) {
      lista.push(horario(i) + "–" + horario(i + d));
      if (lista.length === 48) {
        lista.push("…");
        break;
      }
    }
  document.getElementById("sugestoes").textContent = lista.length
    ? "Sugestão: " + lista.join(" · ")
    : "Informe uma faixa que comporte a duração escolhida.";
}
if (padrao) {
  padrao.addEventListener("input", sugerir);
  document.getElementById("modelo").addEventListener("change", (e) => {
    const modelos = {
      manha: ["08:00", "12:00", 60, [1, 2, 3, 4, 5]],
      tarde: ["13:00", "18:00", 60, [1, 2, 3, 4, 5]],
      noite: ["18:00", "23:00", 60, [1, 2, 3, 4, 5]],
      fimsemana: ["08:00", "18:00", 90, [6, 7]],
    };
    const m = modelos[e.target.value];
    if (!m) return;
    padrao.horaInicio.value = m[0];
    padrao.horaFim.value = m[1];
    padrao.duracaoMinutos.value = m[2];
    padrao
      .querySelectorAll("[name=dias]")
      .forEach((c) => (c.checked = m[3].includes(Number(c.value))));
    sugerir();
  });
  sugerir();
}
document.querySelectorAll(".dias-padrao").forEach(
  (e) =>
    (e.textContent = e.textContent
      .split(",")
      .map(
        (d) => ["", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom"][Number(d)],
      )
      .join(", ")),
);
const mapaElemento = document.getElementById("mapa");
if (mapaElemento) {
  const lat = document.getElementById("latitude"),
    lng = document.getElementById("longitude"),
    status = document.getElementById("mapa-status");
  let mapa, marcador;
  function atualizarPonto(a, o) {
    lat.value = Number(a).toFixed(7);
    lng.value = Number(o).toFixed(7);
    if (mapa) {
      if (marcador) marcador.remove();
      marcador = L.circleMarker([a, o], {
        radius: 9,
        color: "#116b48",
        fillOpacity: 0.8,
      }).addTo(mapa);
    }
    status.textContent =
      "Localização selecionada: " + lat.value + ", " + lng.value;
  }
  function abrir() {
    if (!window.L) {
      status.textContent =
        "Mapa indisponível. Informe o endereço ou as coordenadas.";
      return;
    }
    if (mapa) {
      mapa.invalidateSize();
      return;
    }
    const a = lat.value ? Number(lat.value) : -15.78,
      o = lng.value ? Number(lng.value) : -47.93;
    mapa = L.map(mapaElemento).setView([a, o], lat.value ? 16 : 4);
    L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png", {
      maxZoom: 19,
      attribution:
        '© <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    })
      .addTo(mapa)
      .on(
        "tileerror",
        () =>
          (status.textContent =
            "Não foi possível carregar o mapa. Confira sua conexão ou informe as coordenadas."),
      );
    mapa.on("click", (e) => atualizarPonto(e.latlng.lat, e.latlng.lng));
    if (lat.value && lng.value) atualizarPonto(a, o);
  }
  mapaElemento.closest("details").addEventListener("toggle", (e) => {
    if (e.target.open) abrir();
  });
  document.getElementById("minha-localizacao").addEventListener("click", () => {
    if (!navigator.geolocation) {
      status.textContent = "Localização não disponível neste navegador.";
      return;
    }
    navigator.geolocation.getCurrentPosition(
      (p) => {
        abrir();
        atualizarPonto(p.coords.latitude, p.coords.longitude);
        if (mapa) mapa.setView([p.coords.latitude, p.coords.longitude], 16);
      },
      () =>
        (status.textContent =
          "Não foi possível obter sua localização. Escolha no mapa ou informe as coordenadas."),
    );
  });
  [lat, lng].forEach((e) =>
    e.addEventListener("change", () => {
      if (
        lat.value &&
        lng.value &&
        lat.checkValidity() &&
        lng.checkValidity()
      ) {
        atualizarPonto(Number(lat.value), Number(lng.value));
        if (mapa) mapa.setView([Number(lat.value), Number(lng.value)], 16);
      }
    }),
  );
  document.getElementById("abrir-google").addEventListener("click", (e) => {
    const q =
      lat.value && lng.value
        ? lat.value + "," + lng.value
        : document.getElementById("localizacao").value;
    e.currentTarget.href =
      "https://www.google.com/maps/search/?api=1&query=" +
      encodeURIComponent(q || "Brasil");
  });
  const estabelecimento = document.getElementById("idEstabelecimento");
  if (estabelecimento)
    estabelecimento.addEventListener("change", () => {
      const o = estabelecimento.selectedOptions[0];
      if (!o.value) return;
      document.getElementById("localizacao").value = o.dataset.endereco || "";
      lat.value = o.dataset.latitude || "";
      lng.value = o.dataset.longitude || "";
      if (marcador) {
        marcador.remove();
        marcador = null;
      }
      if (lat.value && lng.value) {
        atualizarPonto(Number(lat.value), Number(lng.value));
        if (mapa) mapa.setView([Number(lat.value), Number(lng.value)], 16);
      }
    });
}
