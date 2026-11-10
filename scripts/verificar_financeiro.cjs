const {chromium}=require(process.env.PLAYWRIGHT_MODULE || "playwright");
const fs=require("node:fs"), assert=require("node:assert/strict");
const {execFileSync}=require("node:child_process");
const base=process.env.TEST_BASE_URL || "http://localhost:8084";
const schema="agendaplay_browser4";
const output="docs/evidencias/financeiro-sprint2";
fs.mkdirSync(output,{recursive:true});
function sql(query) {
  return execFileSync(process.env.TEST_PSQL,["-h","127.0.0.1","-p","55432","-U","agendaplay_test",
    "-d","postgres","-qAt","-v","ON_ERROR_STOP=1","-c",query],{encoding:"utf8"}).trim();
}
assert.equal(sql("SELECT inet_server_port()"),"55432");
const previous=JSON.parse(fs.readFileSync("tmp/sprint3-browser-result.json","utf8"));
assert.match(previous.owner,/^[a-z0-9@.]+$/);
assert.match(previous.client,/^[a-z0-9@.]+$/);
const fixture=JSON.parse(sql(`SELECT json_build_object('quadra',q.id,'estabelecimento',q.id_estabelecimento,
  'cliente',u.id) FROM ${schema}.quadra q CROSS JOIN ${schema}.usuario u
  JOIN ${schema}.usuario d ON d.email='${previous.owner}'
  WHERE q.id_proprietario=d.id AND u.email='${previous.client}' ORDER BY q.id LIMIT 1`));
const reserva=Number(sql(`WITH nova AS(INSERT INTO ${schema}.reserva(id_cliente,id_quadra,data,hora_inicio,hora_fim,valor_total)
  VALUES(${fixture.cliente},${fixture.quadra},current_date+90,'20:00','21:00',120) RETURNING id) SELECT id FROM nova`));
assert(Number.isSafeInteger(reserva));
(async()=>{
  const browser=await chromium.launch({channel:"msedge",headless:true});
  try {
    const ctx=await browser.newContext({viewport:{width:1365,height:900},locale:"pt-BR"});
    const owner=await ctx.newPage();
    const clientCtx=await browser.newContext({viewport:{width:1365,height:900},locale:"pt-BR"});
    const client=await clientCtx.newPage();
    const errors=[];
    for(const page of [owner,client]) page.on("pageerror",e=>errors.push(e.message));
    async function login(page,email) {
      await page.goto(base+"/login");
      await page.getByLabel("E-mail",{exact:true}).fill(email);
      await page.getByLabel("Senha",{exact:true}).fill("TesteSeguro123!");
      await page.getByRole("button",{name:"Entrar",exact:true}).click();
      await page.waitForURL("**/*/quadras");
    }
    const item=page=>page.locator("article.financeiro-item").filter({hasText:"Reserva #"+reserva+" ·"});
    async function noOverflow(page) {
      assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false,"Layout sem rolagem horizontal");
    }
    await login(owner,previous.owner);
    await owner.getByRole("link",{name:"Caixa",exact:true}).click();
    await owner.getByLabel("Estabelecimento",{exact:true}).selectOption(String(fixture.estabelecimento));
    await owner.getByLabel("Dinheiro na abertura (R$)").fill("100");
    await owner.getByRole("button",{name:"Abrir caixa",exact:true}).click();
    await owner.waitForURL(/\/proprietario\/caixa\/\d+$/);
    const caixaUrl=owner.url();
    await owner.getByRole("link",{name:"Financeiro",exact:true}).click();
    await owner.waitForLoadState("load");
    await item(owner).getByRole("button",{name:"Usar 50% do valor total",exact:true}).click();
    assert.equal(await item(owner).locator('[name=valor]').inputValue(),"60.00");
    await item(owner).locator('[name=forma]').selectOption("PIX");
    await item(owner).getByRole("button",{name:"Registrar recebimento",exact:true}).click();
    await owner.getByText("Recebimento registrado. O saldo restante foi atualizado.",{exact:true}).waitFor();
    assert.equal(await item(owner).locator(".badge").textContent(),"PARCIAL");
    await owner.screenshot({path:output+"/financeiro-parcial-desktop.png",fullPage:true});
    for(const forma of ["DINHEIRO","CARTAO"]) {
      await item(owner).locator('[name=valor]').fill("30");
      await item(owner).locator('[name=forma]').selectOption(forma);
      await item(owner).getByRole("button",{name:"Registrar recebimento",exact:true}).click();
      await owner.getByText("Recebimento registrado. O saldo restante foi atualizado.",{exact:true}).waitFor();
    }
    assert.equal(await item(owner).locator(".badge").textContent(),"RECEBIDO");
    await login(client,previous.client);
    await client.getByRole("link",{name:"Meu financeiro",exact:true}).click();
    assert.equal(await item(client).locator(".badge").textContent(),"RECEBIDO");
    await item(client).getByText("Histórico de recebimentos",{exact:true}).click();
    assert.equal(await item(client).locator("li").count(),3);
    assert.equal(await item(client).locator("form").count(),0);
    await client.screenshot({path:output+"/cliente-financeiro-desktop.png",fullPage:true});
    await client.setViewportSize({width:390,height:844}); await noOverflow(client);
    await client.screenshot({path:output+"/cliente-financeiro-mobile.png",fullPage:true});
    await owner.goto(caixaUrl);
    await owner.getByText("Dinheiro esperado no fechamento: R$ 130,00",{exact:true}).waitFor();
    await owner.setViewportSize({width:390,height:844}); await noOverflow(owner);
    await owner.screenshot({path:output+"/caixa-mobile.png",fullPage:true});
    await owner.setViewportSize({width:1365,height:900});
    await owner.getByLabel("Dinheiro contado no fechamento (R$)").fill("125");
    await owner.getByLabel("Observação da conferência (opcional)").fill("Conferência de teste: diferença de R$ 5.");
    await owner.getByRole("button",{name:"Confirmar fechamento",exact:true}).click();
    await owner.getByText("Diferença (contado − esperado): R$ -5,00",{exact:true}).waitFor();
    await owner.screenshot({path:output+"/caixa-fechado-desktop.png",fullPage:true});
    // Somente a reserva criada como fixture no schema isolado é cancelada aqui.
    sql(`UPDATE ${schema}.reserva SET situacao='CANCELADA' WHERE id=${reserva}`);
    await owner.goto(base+"/proprietario/caixa");
    await owner.getByLabel("Estabelecimento",{exact:true}).selectOption(String(fixture.estabelecimento));
    await owner.getByLabel("Dinheiro na abertura (R$)").fill("50");
    await owner.getByRole("button",{name:"Abrir caixa",exact:true}).click();
    await owner.waitForURL(/\/proprietario\/caixa\/\d+$/);
    const segundoCaixa=owner.url();
    await owner.goto(base+"/proprietario/financeiro");
    await item(owner).getByRole("button",{name:"Registrar estorno realizado",exact:true}).click();
    await owner.getByText("Estorno manual registrado.",{exact:true}).waitFor();
    await client.reload();
    assert.equal(await item(client).locator(".badge").textContent(),"ESTORNADO");
    await owner.goto(segundoCaixa);
    await owner.getByText("Dinheiro esperado no fechamento: R$ 20,00",{exact:true}).waitFor();
    await owner.screenshot({path:output+"/caixa-estorno-desktop.png",fullPage:true});
    await owner.goto(caixaUrl);
    await owner.getByText("Dinheiro esperado no fechamento: R$ 130,00",{exact:true}).waitFor();
    await owner.goto(base+"/proprietario/financeiro");
    await owner.setViewportSize({width:390,height:844}); await noOverflow(owner);
    await owner.screenshot({path:output+"/financeiro-mobile.png",fullPage:true});
    assert.deepEqual(errors,[]);
    fs.writeFileSync(output+"/navegador.json",JSON.stringify({resultado:"aprovado",data:new Date().toISOString(),
      errosJavaScript:errors,fluxos:["abertura por estabelecimento","entrada opcional de 50%","parcelas com formas diferentes",
      "saldo remanescente e quitação","consulta do cliente sem ações de recebimento","conferência de dinheiro físico",
      "fechamento com diferença","estorno em caixa posterior","preservação do fechamento anterior","390px sem overflow"]},null,2));
    console.log("Financeiro Sprint 2 aprovado no navegador.");
  } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
