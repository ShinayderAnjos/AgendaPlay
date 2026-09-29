const { chromium } = require(process.env.PLAYWRIGHT_MODULE || "playwright");
const fs = require("node:fs");
const assert = require("node:assert/strict");
const net = require("node:net");
const { execFileSync } = require("node:child_process");
const base = process.env.TEST_BASE_URL || "http://localhost:8084";
const output = "docs/evidencias/sprint4";
fs.mkdirSync(output, { recursive: true });
const psql =
  process.env.TEST_PSQL || "C:/Program Files/PostgreSQL/17/bin/psql.exe";
function sql(query) {
  return execFileSync(
    psql,
    [
      "-h",
      "127.0.0.1",
      "-p",
      "55432",
      "-U",
      "agendaplay_test",
      "-d",
      "postgres",
      "-At",
      "-v",
      "ON_ERROR_STOP=1",
      "-c",
      query,
    ],
    { encoding: "utf8" },
  ).trim();
}
assert.equal(sql("SELECT inet_server_port()"), "55432");
assert.equal(
  sql(
    "SELECT count(*) FROM information_schema.schemata WHERE schema_name='agendaplay_browser4'",
  ),
  "1",
);
const schema = "agendaplay_browser4";
const previous = JSON.parse(
  fs.readFileSync("tmp/sprint3-browser-result.json", "utf8"),
);
const mail = [];
const smtp = net.createServer((socket) => {
  socket.write("220 localhost test SMTP\r\n");
  let buffer = "",
    data = false,
    message = "";
  socket.on("data", (chunk) => {
    buffer += chunk.toString();
    let end;
    while ((end = buffer.indexOf("\r\n")) >= 0) {
      const line = buffer.slice(0, end);
      buffer = buffer.slice(end + 2);
      if (data) {
        if (line === ".") {
          mail.push(message);
          message = "";
          data = false;
          socket.write("250 accepted\r\n");
        } else message += line + "\r\n";
      } else if (line.startsWith("EHLO") || line.startsWith("HELO"))
        socket.write("250 localhost\r\n");
      else if (line === "DATA") {
        data = true;
        socket.write("354 send mail\r\n");
      } else if (line === "QUIT") {
        socket.end("221 bye\r\n");
      } else socket.write("250 OK\r\n");
    }
  });
});
(async () => {
  await new Promise((resolve) => smtp.listen(2526, "127.0.0.1", resolve));
  const browser = await chromium.launch({ channel: "msedge", headless: true });
  try {
    const ctx = await browser.newContext({
      viewport: { width: 1365, height: 900 },
      locale: "pt-BR",
    });
    const page = await ctx.newPage();
    const errors = [],
      dialogs = [];
    page.on("pageerror", (e) => errors.push(e.message));
    page.on("dialog", async (d) => {
      dialogs.push(d.message());
      await d.dismiss();
    });
    await ctx.route("https://tile.openstreetmap.org/**", (route) =>
      route.abort(),
    );
    const password = "SprintQuatro123!";
    const suffix = Date.now(),
      email = "sprint4" + suffix + "@example.test";
    function cpf(prefix) {
      let a = prefix;
      for (let n = 9; n < 11; n++) {
        let sum = 0;
        for (let i = 0; i < n; i++) sum += Number(a[i]) * (n + 1 - i);
        const r = sum % 11;
        a += r < 2 ? "0" : String(11 - r);
      }
      return a;
    }
    async function login(who, secret) {
      await page.goto(base + "/login");
      await page.getByLabel("E-mail", { exact: true }).fill(who);
      await page.getByLabel("Senha", { exact: true }).fill(secret);
      await page.getByRole("button", { name: "Entrar", exact: true }).click();
      await page.waitForURL(/\/(cliente|proprietario)\/quadras$/);
    }
    async function logout() {
      await page.getByRole("button", { name: "Sair", exact: true }).click();
      await page.waitForURL("**/login?saiu");
    }
    async function noOverflow() {
      assert.equal(
        await page.evaluate(
          () => document.documentElement.scrollWidth > innerWidth,
        ),
        false,
        "Tela não deve ter rolagem horizontal",
      );
    }
    await page.goto(base + "/cadastro/cliente");
    assert.equal(await page.locator("#forca-senha li").count(), 5);
    assert.equal(await page.locator('.senha-controle .icone-olho').count(), 2);
    const rows = await page
      .locator("#forca-senha li")
      .evaluateAll((items) => items.map((i) => i.getBoundingClientRect().top));
    assert.equal(new Set(rows).size, 5);
    await page.getByLabel("Nome completo").fill("Cliente Sprint Quatro");
    await page.getByLabel("E-mail", { exact: true }).fill(email);
    await page.locator("#cpf").fill("11111111111");
    await page.locator("#senha").fill(password);
    await page.locator("#confirmacaoSenha").fill(password);
    await page
      .getByRole("button", { name: "Mostrar senha", exact: true })
      .first()
      .click();
    assert.equal(await page.locator("#senha").getAttribute("type"), "text");
    await page
      .getByRole("button", { name: "Ocultar senha", exact: true })
      .click();
    await page
      .getByRole("button", { name: "Criar conta", exact: true })
      .click();
    await page
      .locator(".erro-formulario")
      .filter({ hasText: "CPF válido" })
      .waitFor();
    assert.equal(await page.locator("#senha").inputValue(), password);
    assert.equal(
      await page.locator("#confirmacaoSenha").inputValue(),
      password,
    );
    await page.setViewportSize({ width: 390, height: 844 });
    await noOverflow();
    await page.screenshot({
      path: output + "/cadastro-mobile.png",
      fullPage: true,
    });
    await page.locator("#cpf").fill(cpf(String(suffix).slice(-9)));
    await page.locator("#confirmacaoSenha").fill("Diferente123!");
    await page
      .getByRole("button", { name: "Criar conta", exact: true })
      .click();
    assert.equal(await page.locator("#senha").inputValue(), password);
    assert.equal(
      await page
        .locator("#confirmacaoSenha")
        .evaluate((e) => e.validity.customError),
      true,
    );
    await page.locator("#confirmacaoSenha").fill(password);
    await page
      .getByRole("button", { name: "Criar conta", exact: true })
      .click();
    await page.waitForURL("**/login?cadastrado");
    await page.getByLabel("E-mail", { exact: true }).fill("inexistente@example.test");
    await page.getByLabel("Senha", { exact: true }).fill(password);
    await page.getByRole("button", { name: "Entrar", exact: true }).click();
    await page.locator(".erro-formulario").filter({ hasText: "E-mail ou senha inválidos" }).waitFor();
    assert.equal(await page.locator("#senha").inputValue(), password);
    assert.equal(await page.locator("#email").inputValue(), "inexistente@example.test");
    await page.getByLabel("E-mail", { exact: true }).fill(email);
    await page.getByRole("button", { name: "Entrar", exact: true }).click();
    await page.waitForURL("**/cliente/quadras");
    await logout();
    await page.setViewportSize({ width: 1365, height: 900 });
    await login(previous.owner, "TesteSeguro123!");
    const q = Number(
      sql(
        "SELECT q.id FROM " +
          schema +
          ".quadra q JOIN " +
          schema +
          ".usuario u ON u.id=q.id_proprietario WHERE u.email='" +
          previous.owner +
          "' ORDER BY q.id LIMIT 1",
      ),
    );
    assert(q > 0);
    await page.screenshot({path:"tmp/sprint4-upload-fixture.png"});
    sql("DELETE FROM "+schema+".foto_quadra WHERE id_quadra="+q);
    await page.goto(base + "/proprietario/quadras/" + q + "/fotos");
    await page
      .locator("#arquivos")
      .setInputFiles([
        "src/main/resources/static/imagens/logo.png",
        "tmp/sprint4-upload-fixture.png",
      ]);
    await page
      .getByRole("button", { name: "Salvar fotos", exact: true })
      .click();
    await page.getByText("Fotos salvas.", { exact: true }).waitFor();
    assert.equal(await page.locator(".grade article").count(), 2);
    await page
      .getByRole("button", { name: "Usar como capa", exact: true })
      .click();
    await page.getByText("Foto de capa", { exact: true }).waitFor();
    await page.screenshot({
      path: output + "/fotos-desktop.png",
      fullPage: true,
    });
    // Sprint 3 ends by disabling its weekly schedule. Add an independent
    // availability so the calendar is tested with a real selectable day.
    await page.goto(base + "/proprietario/quadras/" + q + "/agenda");
    await page.locator("#data").fill(previous.date);
    await page.locator("#horaInicio").fill("12:00");
    await page.locator("#horaFim").fill("13:00");
    await page.getByRole("button", { name: "Adicionar horário", exact: true }).click();
    await page.locator('article[data-dia="' + previous.date + '"]').first().waitFor();
    await page.goto(base + "/");
    assert.equal(await page.locator(".slider figure").count(), 2);
    assert.equal(await page.locator(".slider figure:visible").count(), 1);
    const initial = await page
      .locator(".slider figure:visible img")
      .getAttribute("src");
    await page
      .getByRole("button", { name: "Próxima foto", exact: true })
      .click();
    assert.notEqual(
      await page.locator(".slider figure:visible img").getAttribute("src"),
      initial,
    );
    await page.getByRole("button", { name: "Pausar", exact: true }).click();
    const pausedPhoto = await page.locator(".slider figure:visible img").getAttribute("src");
    await page.mouse.move(0, 0);
    await page.locator("[data-pausar]").blur();
    await page.waitForTimeout(5200);
    assert.equal(await page.locator(".slider figure:visible img").getAttribute("src"), pausedPhoto);
    await page.getByRole("button", { name: "Reproduzir", exact: true }).click();
    await page.locator("[data-pausar]").blur();
    await page.mouse.move(0, 0);
    await page.waitForFunction((src) => document.querySelector('.slider figure:not([hidden]) img').getAttribute('src') !== src, pausedPhoto);
    await page.getByRole("button", { name: "Pausar", exact: true }).click();
    await page.screenshot({
      path: output + "/inicio-desktop.png",
      fullPage: true,
    });
    await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
    assert.equal(
      Math.round(
        await page
          .locator(".cabecalho")
          .evaluate((e) => e.getBoundingClientRect().top),
      ),
      0,
    );
    await page.setViewportSize({ width: 390, height: 844 });
    await noOverflow();
    await page.evaluate(() => window.scrollTo(0,0));
    await page.screenshot({
      path: output + "/inicio-mobile.png",
      fullPage: true,
    });
    await page.setViewportSize({ width: 1365, height: 900 });
    await logout();
    await login(email, password);
    await page.goto(base + "/cliente/quadras/" + q + "/horarios");
    await page.locator("#data").fill(previous.date);
    await page.locator("#horaInicio").fill("12:00");
    await page.locator("#horaFim").fill("12:30");
    await page.getByRole("button", { name: "Confirmar reserva", exact: true }).click();
    await page.waitForURL("**/reservas");
    await page.getByText("Avaliação aguardando a baixa", { exact: false }).waitFor();
    assert.equal(await page.locator('[name=nota]').count(), 0);
    const rid = Number(sql("SELECT r.id FROM " + schema + ".reserva r JOIN " + schema + ".usuario u ON u.id=r.id_cliente WHERE u.email='" + email + "' AND r.id_quadra=" + q));
    assert(rid > 0);
    await logout();
    await login(previous.owner, "TesteSeguro123!");
    await page.goto(base + "/proprietario/agenda");
    await page.getByRole("heading", { name: "Reservas de todos os dias", exact: true }).waitFor();
    const booked = page.locator('article').filter({ hasText: "Reserva #" + rid + " ·" });
    await booked.waitFor();
    assert.equal(await booked.getByRole('button', { name: 'Dar baixa', exact: true }).isDisabled(), true);
    await page.screenshot({path: output + '/reserva-futura-proprietario.png', fullPage: true});
    assert(await page.locator('#data').evaluate(e => e.getBoundingClientRect().width >= 150), 'Data deve caber no filtro');
    await page.setViewportSize({ width: 390, height: 844 });
    await noOverflow();
    await page.screenshot({path: output + '/agenda-mobile.png', fullPage: true});
    await page.setViewportSize({ width: 1365, height: 900 });
    // Simulate the arrival time only in the isolated test schema, after a real
    // booking via the client UI. No real reservation is modified.
    sql("UPDATE " + schema + ".reserva SET data=current_date,hora_inicio='00:00',hora_fim='23:59' WHERE id=" + rid);
    await page.goto(base + "/reservas");
    await page.getByRole("button", { name: "Dar baixa", exact: true }).click();
    await page.keyboard.press("Escape");
    assert.equal(await page.getByRole("dialog").isVisible(), false);
    assert.equal(await page.getByRole("button", { name: "Dar baixa", exact: true }).evaluate(e => e === document.activeElement), true);
    await page
      .getByRole("button", { name: "Dar baixa", exact: true })
      .click();
    await page
      .getByRole("dialog")
      .getByRole("button", { name: "Voltar", exact: true })
      .click();
    assert.equal(await page.getByRole("dialog").isVisible(), false);
    await page
      .getByRole("button", { name: "Dar baixa", exact: true })
      .click();
    await page
      .getByRole("dialog")
      .getByRole("button", { name: "Confirmar", exact: true })
      .click();
    await page.getByText("Reserva concluída.", { exact: false }).waitFor();
    await logout();
    await login(email, password);
    await page.goto(base + "/reservas");
    await page.locator("[name=nota]").selectOption("5");
    await page
      .locator("[name=comentario]")
      .fill(("Ótima quadra! Avaliação de teste " + suffix));
    await page
      .getByRole("button", { name: "Enviar avaliação", exact: true })
      .click();
    await page.getByText("Sua avaliação: 5", { exact: false }).waitFor();
    await page.screenshot({
      path: output + "/avaliacao-desktop.png",
      fullPage: true,
    });
    await page.goto(base + "/cliente/quadras/" + q + "/horarios");
    await page
      .getByText(("Ótima quadra! Avaliação de teste " + suffix), { exact: true })
      .waitFor();
    assert.equal(await page.locator(".galeria-fotos img").count(), 2);
    assert.equal(await page.getByRole("button",{name:"Abrir calendário: Consultar um dia"}).count(),1);
    const availableDay = await page.locator('article[data-dia]').first().getAttribute('data-dia');
    await page.locator('#dia-agenda').fill(availableDay);
    assert.equal(await page.locator('#data').inputValue(), availableDay);
    assert.equal(await page.locator('article[data-dia]:visible').evaluateAll((items) => items.every(i => i.dataset.dia === document.querySelector('#dia-agenda').value)), true);
    await page.locator("#dia-agenda").fill("2099-01-01");
    await page.locator("#dia-sem-horarios").waitFor();
    await page.getByRole("button",{name:"Mostrar todos os dias",exact:true}).click();
    assert.equal(await page.locator("#dia-agenda").inputValue(), "");
    await page.setViewportSize({ width: 390, height: 844 });
    await noOverflow();
    await page.evaluate(() => window.scrollTo(0,0));
    await page.screenshot({
      path: output + "/quadra-mobile.png",
      fullPage: true,
    });
    await page.setViewportSize({ width: 1365, height: 900 });
    await logout();
    await page.goto(base + "/recuperar-senha");
    await page.getByLabel("E-mail da conta").fill(email);
    await page
      .getByRole("button", { name: "Enviar link de recuperação", exact: true })
      .click();
    await page
      .getByText("Se o e-mail estiver cadastrado", { exact: false })
      .waitFor();
    assert.equal(mail.length, 1);
    let raw = mail[0].replace(/=\r\n/g, "").replace(/=3D/g, "=");
    if (/Content-Transfer-Encoding: base64/i.test(raw))
      raw = Buffer.from(
        raw.split("\r\n\r\n").slice(1).join("\r\n\r\n"),
        "base64",
      ).toString();
    const match = raw.match(/token=([A-Za-z0-9_-]{43})/);
    assert(match, "E-mail deve conter o link com token");
    const resetUrl = base + "/redefinir-senha?token=" + match[1];
    await page.goto(resetUrl);
    // A backend validation error must also keep both password fields intact.
    await page.locator("#senha").fill("SenhaNovaSprint4!");
    await page.locator("#confirmacaoSenha").fill("OutraSenha123!");
    await page.locator('form[data-preservar-senhas]').evaluate(form => { form.noValidate = true; });
    await page.getByRole("button", { name: "Salvar nova senha", exact: true }).click();
    await page.locator(".erro-formulario").filter({ hasText: "confere" }).waitFor();
    assert.equal(await page.locator("#senha").inputValue(), "SenhaNovaSprint4!");
    assert.equal(await page.locator("#confirmacaoSenha").inputValue(), "OutraSenha123!");
    await page.locator("#senha").fill("SenhaNovaSprint4!");
    await page.locator("#confirmacaoSenha").fill("SenhaNovaSprint4!");
    await page
      .getByRole("button", { name: "Salvar nova senha", exact: true })
      .click();
    await page.waitForURL("**/login?redefinida");
    await page.getByText("Senha redefinida.", { exact: false }).waitFor();
    await login(email, "SenhaNovaSprint4!");
    await logout();
    await page.goto(resetUrl);
    await page
      .getByText("Link inválido, expirado ou já utilizado.", { exact: true })
      .waitFor();
    assert.deepEqual(errors, []);
    assert.deepEqual(dialogs, []);
    fs.writeFileSync(
      output + "/navegador.json",
      JSON.stringify(
        {
          resultado: "aprovado",
          data: new Date().toISOString(),
          errosJavaScript: errors,
          dialogosNativos: dialogs,
          fluxos: [
            "requisitos um por linha",
            "senha preservada em CPF inválido",
            "senhas diferentes preservadas",
            "mostrar/ocultar senha",
            "cadastro e login",
            "login inválido preserva senha e permite corrigir e-mail",
            "slider automático e pausa",
            "Escape fecha confirmação e restaura foco",
            "filtro de calendário seleciona dia disponível",
            "erro do servidor na redefinição preserva senhas",
            "upload e troca de capa",
            "galeria e slider",
            "cabeçalho fixo",
            "mobile 390px sem overflow",
            "confirmação e desistência em modal",
            "baixa pelo proprietário",
            "reserva criada pelo cliente aparece ao trocar para o proprietário sem filtro de data",
            "baixa bloqueada antes do início com explicação visível",
            "baixa durante o horário libera avaliação ao retornar à conta cliente",
            "avaliação pelo cliente",
            "e-mail capturado em SMTP local",
            "redefinição e login com senha nova",
            "link de uso único",
          ],
        },
        null,
        2,
      ),
    );
    console.log(
      "Sprint 4 aprovada no navegador: " +
        errors.length +
        " erros JavaScript; " +
        dialogs.length +
        " diálogos nativos.",
    );
  } finally {
    await browser.close();
    smtp.close();
  }
})().catch((e) => {
  console.error(e);
  smtp.close();
  process.exitCode = 1;
});

