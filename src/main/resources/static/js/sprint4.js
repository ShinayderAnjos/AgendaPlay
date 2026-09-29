// Passwords stay only in the current form; never use browser storage.
document.querySelectorAll("input[type=password]").forEach((input) => {
  const wrapper = document.createElement("div");
  wrapper.className = "senha-controle";
  input.before(wrapper);
  wrapper.append(input);
  const button = document.createElement("button");
  button.type = "button";
  button.className = "botao secundario pequeno";
  button.innerHTML = '<svg class="icone-olho" viewBox="0 0 24 24" aria-hidden="true" focusable="false"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/><path class="olho-risco" d="M3 3l18 18" hidden/></svg><span>Mostrar</span>';
  button.setAttribute("aria-label", "Mostrar senha");
  button.setAttribute("aria-controls", input.id);
  button.setAttribute("aria-pressed", "false");
  wrapper.append(button);
  button.addEventListener("click", () => {
    const show = input.type === "password";
    input.type = show ? "text" : "password";
    button.querySelector("span").textContent = show ? "Ocultar" : "Mostrar";
    button.querySelector(".olho-risco").toggleAttribute("hidden", !show);
    button.setAttribute("aria-label", show ? "Ocultar senha" : "Mostrar senha");
    button.setAttribute("aria-pressed", String(show));
  });
});
const password = document.getElementById("senha"),
  rules = document.getElementById("forca-senha");
if (password && rules) {
  const validate = () => {
    const value = password.value,
      missing = [];
    if (value.length < 8 || value.length > 60)
      missing.push("De 8 a 60 caracteres");
    if (!/[A-Z]/.test(value)) missing.push("Pelo menos uma letra maiúscula");
    if (!/[a-z]/.test(value)) missing.push("Pelo menos uma letra minúscula");
    if (!/[0-9]/.test(value)) missing.push("Pelo menos um número");
    if (!/[^a-zA-Z0-9\s]/.test(value)) missing.push("Pelo menos um símbolo");
    if (new TextEncoder().encode(value).length > 72)
      missing.push("Reduza a quantidade de caracteres especiais");
    rules.replaceChildren(
      ...(missing.length ? missing : ["Senha atende aos requisitos."]).map(
        (text) => {
          const li = document.createElement("li");
          li.textContent = text;
          return li;
        },
      ),
    );
    rules.classList.toggle("senha-valida", missing.length === 0);
    password.setCustomValidity(
      missing.length ? "Complete os requisitos da senha." : "",
    );
  };
  password.addEventListener("input", validate);
  validate();
  const confirmation = document.getElementById("confirmacaoSenha");
  if (confirmation) {
    const check = () =>
      confirmation.setCustomValidity(
        confirmation.value && confirmation.value !== password.value
          ? "A confirmação da senha não confere."
          : "",
      );
    password.addEventListener("input", check);
    confirmation.addEventListener("input", check);
  }
}
document.querySelectorAll("form[data-preservar-senhas]").forEach((form) => {
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const button = form.querySelector("button[type=submit]");
    if (button.disabled) return;
    button.disabled = true;
    let errors = form.querySelector(".erro-formulario");
    if (!errors) {
      errors = document.createElement("div");
      errors.className = "aviso erro erro-formulario";
      errors.setAttribute("role", "alert");
      errors.tabIndex = -1;
      form.prepend(errors);
    }
    errors.replaceChildren();
    try {
      const response = await fetch(form.action, {
        method: "POST",
        body: new URLSearchParams(new FormData(form)),
        credentials: "same-origin",
      });
      if (!response.ok)
        throw new Error(
          "Não foi possível enviar. Confira sua conexão e tente novamente. Se a sessão expirou, recarregue a página.",
        );
      const documentResponse = new DOMParser().parseFromString(
        await response.text(),
        "text/html",
      );
      const messages = [...documentResponse.querySelectorAll(".aviso.erro")];
      // A failed login also redirects. Inspect the returned page before leaving
      // the current form so an incorrect e-mail does not discard the password.
      if (response.redirected && !messages.length) {
        window.location.assign(response.url);
        return;
      }
      const csrf = documentResponse.querySelector('input[name="_csrf"]');
      const currentCsrf = form.querySelector('input[name="_csrf"]');
      if (csrf && currentCsrf) currentCsrf.value = csrf.value;
      if (messages.length) {
        messages.forEach((message) => {
          const p = document.createElement("div");
          p.textContent = message.textContent;
          errors.append(p);
        });
      } else
        errors.textContent =
          "Não foi possível concluir. Revise os dados ou solicite um novo link.";
      errors.focus();
    } catch (error) {
      errors.textContent = error.message;
      errors.focus();
    } finally {
      button.disabled = false;
    }
  });
});
const dialog = document.createElement("dialog");
dialog.className = "dialogo";
dialog.setAttribute("aria-labelledby", "confirmar-titulo");
dialog.setAttribute("aria-describedby", "confirmar-texto");
dialog.innerHTML =
  '<h2 id="confirmar-titulo">Confirmar ação</h2><p id="confirmar-texto"></p><div class="acoes"><button type="button" class="botao secundario" data-voltar>Voltar</button><button type="button" class="botao" data-confirmar-acao>Confirmar</button></div>';
document.body.append(dialog);
let pendingForm = null,
  previousFocus = null;
document
  .querySelectorAll("form.cancelar-reserva,form[data-confirmar]")
  .forEach((form) =>
    form.addEventListener("submit", (event) => {
      if (form.dataset.confirmado === "true") return;
      event.preventDefault();
      pendingForm = form;
      previousFocus = document.activeElement;
      dialog.querySelector("p").textContent =
        form.dataset.confirmar ||
        "Deseja cancelar esta reserva? O horário será liberado.";
      dialog.showModal();
      dialog.querySelector("[data-voltar]").focus();
    }),
  );
dialog
  .querySelector("[data-voltar]")
  .addEventListener("click", () => dialog.close());
dialog.addEventListener("close", () => {
  pendingForm = null;
  previousFocus?.focus();
});
dialog.querySelector("[data-confirmar-acao]").addEventListener("click", () => {
  const form = pendingForm;
  if (!form) return;
  form.dataset.confirmado = "true";
  form.requestSubmit();
  dialog.close();
});
document.querySelectorAll(".slider").forEach((slider) => {
  const slides = [...slider.querySelectorAll("figure")],
    position = slider.querySelector(".slider-posicao"),
    pause = slider.querySelector("[data-pausar]");
  let index = 0,
    paused = matchMedia("(prefers-reduced-motion: reduce)").matches;
  function render() {
    slides.forEach((slide, i) => (slide.hidden = i !== index));
    position.textContent = index + 1 + " / " + slides.length;
    pause.textContent = paused ? "Reproduzir" : "Pausar";
    pause.setAttribute("aria-pressed", String(paused));
  }
  const move = (n) => {
    index = (index + n + slides.length) % slides.length;
    render();
  };
  slider
    .querySelectorAll("[data-slide]")
    .forEach((button) =>
      button.addEventListener("click", () =>
        move(Number(button.dataset.slide)),
      ),
    );
  pause.addEventListener("click", () => {
    paused = !paused;
    render();
  });
  if (slides.length < 2)
    slider.querySelector(".slider-controles").hidden = true;
  setInterval(() => {
    if (
      !paused &&
      !document.hidden &&
      !slider.matches(":hover") &&
      !slider.contains(document.activeElement)
    )
      move(1);
  }, 5000);
  render();
});

// Explicit calendar controls beside the dates, with keyboard fallback.
document.querySelectorAll('input[type=date]').forEach(input=>{
 const wrapper=document.createElement('div');wrapper.className='data-controle';input.before(wrapper);wrapper.append(input);
 const button=document.createElement('button');button.type='button';button.className='botao secundario pequeno';button.textContent='▦ Calendário';button.setAttribute('aria-label','Abrir calendário: '+(document.querySelector('label[for="'+input.id+'"]')?.textContent||'data'));wrapper.append(button);
 button.addEventListener('click',()=>{try{if(input.showPicker)input.showPicker();else input.focus();}catch{input.focus();}});
});
const dayFilter=document.getElementById('dia-agenda');
if(dayFilter){const filter=()=>{let count=0;document.querySelectorAll('article[data-dia]').forEach(item=>{item.hidden=Boolean(dayFilter.value&&item.dataset.dia!==dayFilter.value);if(!item.hidden)count++;});document.getElementById('dia-sem-horarios').hidden=count>0;if(dayFilter.value){document.getElementById('data').value=dayFilter.value;atualizarValor();}};dayFilter.addEventListener('change',filter);document.getElementById('todos-dias').addEventListener('click',()=>{dayFilter.value='';filter();});}
document.querySelectorAll('input[type=file][name=arquivos]').forEach(input=>input.addEventListener('change',()=>{const files=[...input.files];input.setCustomValidity(files.length>8?'Escolha até 8 fotos.':files.some(file=>file.size>5*1024*1024)?'Cada foto deve ter até 5 MB.':'');}));
