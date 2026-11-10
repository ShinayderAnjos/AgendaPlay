document.querySelectorAll("[data-entrada-metade]").forEach((button) => {
  button.addEventListener("click", () => {
    const input = button.form.querySelector('input[name="valor"]');
    input.value = button.dataset.valor;
    input.focus();
  });
});
