const { chromium } = require(process.env.PLAYWRIGHT_MODULE || "playwright");
const fs = require("fs");
const assert = require("assert/strict");
(async () => {
  const browser = await chromium.launch({ channel: "msedge", headless: true });
  const ctx = await browser.newContext({
    viewport: { width: 1365, height: 900 },
    locale: "pt-BR",
  });
  const page = await ctx.newPage();
  const errors = [];
  page.on("pageerror", (e) => errors.push(e.message));
  page.on("console", (m) => {
    if (m.type() === "error" && !m.text().includes("tile.openstreetmap.org"))
      errors.push(m.text());
  });
  // Tiles are stubbed: browser tests exercise point selection without automated traffic to OSM.
  await ctx.route("https://tile.openstreetmap.org/**", (route) =>
    route.fulfill({
      status: 200,
      contentType: "image/png",
      body: Buffer.from(
        "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wl6rN8AAAAASUVORK5CYII=",
        "base64",
      ),
    }),
  );
  const base = "http://localhost:8081";
  const suffix = Date.now();
  const owner = `dono${suffix}@example.test`,
    client = `cliente${suffix}@example.test`;
  const cpf = (base) => {
    let a = base;
    for (let n = 9; n < 11; n++) {
      let sum = 0;
      for (let i = 0; i < n; i++) sum += Number(a[i]) * (n + 1 - i);
      let r = sum % 11;
      a += r < 2 ? "0" : String(11 - r);
    }
    return a;
  };
  const doc1 = cpf(String(suffix).slice(-9)),
    doc2 = cpf(String(suffix + 1).slice(-9));
  async function signup(type, email, doc) {
    await page.goto(base + "/cadastro/" + type);
    await page.getByLabel("Nome completo").fill("Pessoa Teste " + type);
    await page.getByLabel("E-mail", { exact: true }).fill(email);
    await page.locator("#cpf").fill(doc);
    await page.getByLabel("Senha", { exact: true }).fill("TesteSeguro123!");
    await page.getByLabel("Confirme a senha").fill("TesteSeguro123!");
    await page
      .getByRole("button", { name: "Criar conta", exact: true })
      .click();
    await page.waitForURL("**/login?cadastrado");
  }
  async function login(email) {
    await page.goto(base + "/login");
    await page.getByLabel("E-mail", { exact: true }).fill(email);
    await page.getByLabel("Senha", { exact: true }).fill("TesteSeguro123!");
    await page.getByRole("button", { name: "Entrar", exact: true }).click();
    await page.waitForURL("**/*/quadras");
  }
  await signup("proprietario", owner, doc1);
  await login(owner);
  await page
    .getByRole("link", { name: "Estabelecimentos", exact: true })
    .click();
  await page
    .getByRole("link", { name: "+ Cadastrar estabelecimento", exact: true })
    .click();
  await page
    .getByLabel("Nome do estabelecimento")
    .fill("Centro Esportivo Sprint 3");
  await page
    .getByLabel("Endereço", { exact: true })
    .fill("Rua dos Esportes, 100, Brasília");
  await page.getByText("Escolher localização no mapa", { exact: true }).click();
  await page.locator("#mapa").click({ position: { x: 150, y: 130 } });
  assert.notEqual(await page.locator("#latitude").inputValue(), "");
  const lat = await page.locator("#latitude").inputValue();
  await page
    .getByRole("button", { name: "Salvar estabelecimento", exact: true })
    .click();
  await page.waitForURL("**/proprietario/estabelecimentos");
  await page.getByRole("link", { name: "Quadras", exact: true }).click();
  await page
    .getByRole("link", { name: "+ Cadastrar quadra", exact: true })
    .click();
  await page
    .getByLabel("Estabelecimento", { exact: true })
    .selectOption({ label: "Centro Esportivo Sprint 3" });
  assert.equal(await page.locator("#latitude").inputValue(), lat);
  await page
    .getByLabel("Nome da quadra", { exact: true })
    .fill("Quadra Sprint 3");
  await page.getByLabel("Modalidade", { exact: true }).fill("Futsal");
  await page.getByLabel("Valor por hora (R$)", { exact: true }).fill("90");
  await page
    .getByRole("button", { name: "Salvar quadra", exact: true })
    .click();
  await page.waitForURL("**/proprietario/quadras");
  await page
    .getByRole("link", { name: "Disponibilidades", exact: true })
    .click();
  const quadraUrl = page.url();
  await page
    .getByRole("link", { name: "Padrões semanais", exact: true })
    .click();
  await page.getByLabel("Começar com um modelo").selectOption("noite");
  await page.getByLabel("Preço por hora (R$)", { exact: true }).fill("120");
  await page.screenshot({
    path: "tmp/sprint3-padroes-desktop.png",
    fullPage: true,
  });
  await page
    .getByRole("button", { name: "Criar padrão e gerar horários", exact: true })
    .click();
  await page.getByText("Padrão criado.", { exact: false }).waitFor();
  await page.goto(quadraUrl);
  let future = new Date();
  future.setDate(future.getDate() + 2);
  while ([0, 6].includes(future.getDay())) future.setDate(future.getDate() + 1);
  const date = [
    future.getFullYear(),
    String(future.getMonth() + 1).padStart(2, "0"),
    String(future.getDate()).padStart(2, "0"),
  ].join("-");
  await page.getByRole("button", { name: "Sair", exact: true }).click();
  await signup("cliente", client, doc2);
  await login(client);
  await page.getByLabel("Modalidade", { exact: true }).fill("Futsal");
  await page.locator("#data").fill(date);
  await page.locator("#inicio").fill("18:00");
  await page.locator("#fim").fill("18:30");
  await page.getByRole("button", { name: "Filtrar", exact: true }).click();
  await page.getByRole("link", { name: "Ver horários →", exact: true }).click();
  const clientUrl = page.url();
  await page
    .locator(`.escolher-horario[data-data="${date}"][data-inicio="18:00"]`)
    .click();
  await page.locator("#horaFim").fill("18:30");
  await page.locator("#horaFim").dispatchEvent("change");
  assert.match(await page.locator("#valor-previsto").innerText(), /60,00/);
  await page
    .getByRole("button", { name: "Confirmar reserva", exact: true })
    .click();
  await page.waitForURL("**/reservas");
  await page.getByText("Reserva confirmada!", { exact: false }).waitFor();
  await page.goto(clientUrl);
  assert.equal(
    await page
      .locator(`.escolher-horario[data-data="${date}"][data-inicio="18:40"]`)
      .count(),
    1,
  );
  await page.getByRole("button", { name: "Sair", exact: true }).click();
  await login(owner);
  await page.getByRole("link", { name: "Agenda", exact: true }).click();
  await page.locator("#data").fill(date);
  await page
    .getByRole("button", { name: "Consultar agenda", exact: true })
    .click();
  await page.screenshot({
    path: "tmp/sprint3-agenda-desktop.png",
    fullPage: true,
  });
  page.on("dialog", (d) => d.accept());
  await page
    .getByRole("button", { name: "Cancelar reserva", exact: true })
    .click();
  await page.waitForURL("**/reservas");
  await page.getByRole("button", { name: "Sair", exact: true }).click();
  await login(client);
  await page
    .getByText("cancelada pelo proprietário.", { exact: false })
    .waitFor();
  await page.screenshot({
    path: "tmp/sprint3-notificacao-desktop.png",
    fullPage: true,
  });
  await page.setViewportSize({ width: 390, height: 844 });
  await page.screenshot({
    path: "tmp/sprint3-filtros-mobile.png",
    fullPage: true,
  });
  assert.equal(
    await page.evaluate(
      () => document.documentElement.scrollWidth > innerWidth,
    ),
    false,
    "catálogo mobile sem overflow",
  );
  await page
    .getByRole("button", { name: "Marcar como lida", exact: true })
    .click();
  assert.equal(
    await page.getByText("Cancelamentos recentes", { exact: true }).count(),
    0,
  );
  await page.getByRole("button", { name: "Sair", exact: true }).click();
  await login(owner);
  await page.goto(quadraUrl.replace("/agenda", "/padroes"));
  assert.equal(
    await page.evaluate(
      () => document.documentElement.scrollWidth > innerWidth,
    ),
    false,
    "padrões mobile sem overflow",
  );
  await page.screenshot({
    path: "tmp/sprint3-padroes-mobile.png",
    fullPage: true,
  });
  await page
    .getByRole("button", { name: "Desativar padrão", exact: true })
    .click();
  await page.getByText("Padrão desativado.", { exact: false }).waitFor();
  assert.deepEqual(errors, []);
  fs.writeFileSync(
    "tmp/sprint3-browser-result.json",
    JSON.stringify(
      {
        resultado: "aprovado",
        errors,
        owner,
        client,
        date,
        fluxos: [
          "cadastro e login de ambos os perfis",
          "estabelecimento",
          "clique no mapa e coordenadas persistidas",
          "quadra vinculada",
          "prévia e criação de padrão",
          "filtros",
          "preço parcial 60,00",
          "tolerância de 10 minutos",
          "agenda diária",
          "cancelamento do proprietário",
          "notificação no login",
          "leitura da notificação",
          "desativação de padrão",
          "390px sem overflow",
        ],
      },
      null,
      2,
    ),
  );
  await browser.close();
  console.log("Fluxos Sprint 3 aprovados no navegador");
})().catch((e) => {
  console.error(e);
  process.exit(1);
});
