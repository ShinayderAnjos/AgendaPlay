document.querySelectorAll(".escolher-horario").forEach((botao) => {
  botao.addEventListener("click", () => {
    document.getElementById("data").value = botao.dataset.data;
    document.getElementById("horaInicio").value = botao.dataset.inicio;
    document.getElementById("horaFim").value = botao.dataset.fim;
    atualizarValor();
    document
      .getElementById("form-periodo")
      .scrollIntoView({ behavior: "smooth", block: "center" });
    document.getElementById("horaInicio").focus({ preventScroll: true });
  });
});
function atualizarValor() {
  const formulario = document.getElementById("form-periodo");
  const destino = document.getElementById("valor-previsto");
  if (!formulario || !destino) return;
  const inicio = document.getElementById("horaInicio").value;
  const fim = document.getElementById("horaFim").value;
  const emMinutos = (valor) => {
    const [h, m] = valor.split(":").map(Number);
    return h * 60 + m;
  };
  const duracao = emMinutos(fim) - emMinutos(inicio);
  const dia = document.getElementById("data").value;
  const janela = [...document.querySelectorAll(".escolher-horario")].find(
    (b) =>
      b.dataset.data === dia &&
      emMinutos(inicio) >= emMinutos(b.dataset.inicio) &&
      emMinutos(fim) <= emMinutos(b.dataset.fim),
  );
  if (!janela) {
    destino.textContent =
      "Selecione um intervalo dentro dos horários livres para consultar o valor.";
    return;
  }
  destino.textContent =
    duracao > 0
      ? "Valor previsto: " +
        ((duracao / 60) * Number(janela.dataset.valor)).toLocaleString(
          "pt-BR",
          { style: "currency", currency: "BRL" },
        )
      : "Selecione um horário final posterior ao inicial.";
}
document
  .querySelectorAll("#form-periodo input")
  .forEach((campo) => campo.addEventListener("change", atualizarValor));
document.querySelectorAll(".cancelar-reserva").forEach((formulario) =>
  formulario.addEventListener("submit", (evento) => {
    if (
      !window.confirm("Deseja cancelar esta reserva? O horário será liberado.")
    )
      evento.preventDefault();
  }),
);
atualizarValor();
