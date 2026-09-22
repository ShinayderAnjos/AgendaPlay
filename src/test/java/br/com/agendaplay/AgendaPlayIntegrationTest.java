package br.com.agendaplay;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.agendaplay.dto.*;
import br.com.agendaplay.exception.RegraNegocioException;
import br.com.agendaplay.model.*;
import br.com.agendaplay.repository.*;
import br.com.agendaplay.security.UsuarioAutenticado;
import br.com.agendaplay.service.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.concurrent.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AgendaPlayIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UsuarioRepository usuarios;
    @Autowired UsuarioService contas;
    @Autowired QuadraService quadras;
    @Autowired AgendaService agenda;
    @Autowired AgendaRepository repositorio;
    @Autowired PasswordEncoder senhas;
    private UsuarioAutenticado dono, outroDono, cliente, outroCliente;
    private long quadra;
    private long estabelecimento;
    @Autowired EstabelecimentoService estabelecimentos;
    private final LocalDate data = LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(3);

    @BeforeEach
    void preparar() {
        // Este teste só limpa a estrutura da instância isolada de testes.
        assertThat(jdbc.queryForObject("SELECT current_schema()", String.class))
                .isEqualTo("agendaplay_teste");
        assertThat(jdbc.queryForObject("SELECT inet_server_port()", Integer.class))
                .isEqualTo(55432);
        jdbc.execute("TRUNCATE reserva, disponibilidade, quadra, usuario RESTART IDENTITY CASCADE");
        String hash = senhas.encode("AgendaTeste123!");
        usuarios.cadastrar(
                "Proprietário Teste",
                "dono@example.test",
                "12345678909",
                "",
                hash,
                Perfil.PROPRIETARIO);
        usuarios.cadastrar(
                "Outro Proprietário",
                "outrodono@example.test",
                "12345678000195",
                "",
                hash,
                Perfil.PROPRIETARIO);
        usuarios.cadastrar(
                "Cliente Teste", "cliente@example.test", "52998224725", "", hash, Perfil.CLIENTE);
        usuarios.cadastrar(
                "Outro Cliente", "outro@example.test", "11144477735", "", hash, Perfil.CLIENTE);
        dono = autenticado("dono@example.test");
        outroDono = autenticado("outrodono@example.test");
        cliente = autenticado("cliente@example.test");
        outroCliente = autenticado("outro@example.test");
        var ef = new EstabelecimentoForm();
        ef.setNome("Centro esportivo");
        ef.setLocalizacao("Rua de Teste, 100");
        estabelecimento = estabelecimentos.salvar(null, ef, dono);
        quadra = quadras.salvar(null, quadraForm("ATIVA"), dono);
        agenda.disponibilizar(quadra, periodo("08:00", "18:00"), dono);
    }

    private UsuarioAutenticado autenticado(String email) {
        return new UsuarioAutenticado(usuarios.buscarPorEmail(email).orElseThrow());
    }

    private QuadraForm quadraForm(String situacao) {
        var f = new QuadraForm();
        f.setIdEstabelecimento(estabelecimento);
        f.setToleranciaMinutos(0);
        f.setNome("Arena de Testes");
        f.setModalidade("Futsal");
        f.setLocalizacao("Rua de Teste, 100");
        f.setValorHora(new BigDecimal("90.00"));
        f.setSituacao(situacao);
        return f;
    }

    private PeriodoForm periodo(String inicio, String fim) {
        var f = new PeriodoForm();
        f.setData(data);
        f.setHoraInicio(LocalTime.parse(inicio));
        f.setHoraFim(LocalTime.parse(fim));
        return f;
    }

    @Test
    void paginasPublicasRenderizam() throws Exception {
        for (String caminho : List.of("/", "/login", "/cadastro/cliente", "/cadastro/proprietario"))
            mvc.perform(get(caminho))
                    .andExpect(status().isOk())
                    .andExpect(
                            content().string(org.hamcrest.Matchers.containsString("AgendaPlay")));
    }

    @Test
    void paginasAutenticadasRenderizamComDados() throws Exception {
        agenda.reservar(quadra, periodo("10:00", "11:00"), cliente);
        for (String caminho :
                List.of(
                        "/proprietario/quadras",
                        "/proprietario/quadras/nova",
                        "/proprietario/quadras/" + quadra + "/editar",
                        "/proprietario/quadras/" + quadra + "/agenda",
                        "/reservas",
                        "/conta"))
            mvc.perform(get(caminho).with(user(dono))).andExpect(status().isOk());
        for (String caminho :
                List.of(
                        "/cliente/quadras",
                        "/cliente/quadras/" + quadra + "/horarios",
                        "/reservas",
                        "/conta"))
            mvc.perform(get(caminho).with(user(cliente))).andExpect(status().isOk());
    }

    @Test
    void cadastroClientePersisteHashEIgnoraPerfilForjado() throws Exception {
        mvc.perform(
                        post("/cadastro/cliente")
                                .with(csrf())
                                .param("nome", "Nova Pessoa")
                                .param("email", "NOVA@EXAMPLE.TEST")
                                .param("cpf", "93541134780")
                                .param("senha", "SenhaTeste123!")
                                .param("confirmacaoSenha", "SenhaTeste123!")
                                .param("perfil", "PROPRIETARIO"))
                .andExpect(redirectedUrl("/login?cadastrado"));
        var conta = usuarios.buscarPorEmail("nova@example.test").orElseThrow();
        assertThat(conta.getPerfil()).isEqualTo(Perfil.CLIENTE);
        assertThat(conta.getSenhaHash()).isNotEqualTo("SenhaTeste123!");
        assertThat(senhas.matches("SenhaTeste123!", conta.getSenhaHash())).isTrue();
    }

    @Test
    void proprietarioNaoPodeCadastrarSemDocumento() throws Exception {
        mvc.perform(
                        post("/cadastro/proprietario")
                                .with(csrf())
                                .param("nome", "Proprietário Novo")
                                .param("email", "novo@example.test")
                                .param("senha", "SenhaTeste123!")
                                .param("confirmacaoSenha", "SenhaTeste123!"))
                .andExpect(model().attributeHasErrors("form"));
        assertThat(usuarios.buscarPorEmail("novo@example.test")).isEmpty();
    }

    @Test
    void rejeitaCadastroInvalidoESenhasDiferentes() throws Exception {
        mvc.perform(
                        post("/cadastro/cliente")
                                .with(csrf())
                                .param("nome", "A")
                                .param("email", "invalido")
                                .param("cpf", "11111111111")
                                .param("senha", "123")
                                .param("confirmacaoSenha", "456"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasErrors("form"));
        mvc.perform(
                        post("/cadastro/cliente")
                                .with(csrf())
                                .param("nome", "Nome Válido")
                                .param("email", "novo@example.test")
                                .param("cpf", "93541134780")
                                .param("senha", "SenhaTeste123!")
                                .param("confirmacaoSenha", "OutraSenha123!"))
                .andExpect(model().attributeHasErrors("form"));
        assertThat(usuarios.buscarPorEmail("novo@example.test")).isEmpty();
    }

    @Test
    void rejeitaEmailDuplicadoSemExporSql() throws Exception {
        mvc.perform(
                        post("/cadastro/cliente")
                                .with(csrf())
                                .param("nome", "Cliente Duplicado")
                                .param("email", "CLIENTE@EXAMPLE.TEST")
                                .param("cpf", "93541134780")
                                .param("senha", "SenhaTeste123!")
                                .param("confirmacaoSenha", "SenhaTeste123!"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasErrors("form"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("já cadastrado")));
    }

    @Test
    void loginLogoutEProtecaoDeSessao() throws Exception {
        mvc.perform(get("/reservas")).andExpect(status().is3xxRedirection());
        mvc.perform(
                        post("/login")
                                .with(csrf())
                                .param("email", "cliente@example.test")
                                .param("senha", "errada"))
                .andExpect(redirectedUrl("/login?erro"));
        var resultado =
                mvc.perform(
                                post("/login")
                                        .with(csrf())
                                        .param("email", " CLIENTE@EXAMPLE.TEST ")
                                        .param("senha", "AgendaTeste123!"))
                        .andExpect(redirectedUrl("/painel"))
                        .andReturn();
        var sessao = (MockHttpSession) resultado.getRequest().getSession(false);
        mvc.perform(get("/reservas").session(sessao)).andExpect(status().isOk());
        mvc.perform(post("/sair").session(sessao).with(csrf()))
                .andExpect(redirectedUrl("/login?saiu"));
        assertThat(sessao.isInvalid()).isTrue();
        mvc.perform(get("/reservas")).andExpect(status().is3xxRedirection());
    }

    @Test
    void csrfObrigatorioEPerfisIsolados() throws Exception {
        mvc.perform(post("/cadastro/cliente")).andExpect(status().isForbidden());
        mvc.perform(get("/proprietario/quadras").with(user(cliente)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/cliente/quadras").with(user(dono))).andExpect(status().isForbidden());
        mvc.perform(post("/proprietario/quadras/nova").with(user(dono)))
                .andExpect(status().isForbidden());
    }

    @Test
    void quadraSempreVinculadaAoDonoAutenticado() throws Exception {
        mvc.perform(
                        post("/proprietario/quadras/nova")
                                .with(user(dono))
                                .with(csrf())
                                .param("idEstabelecimento", String.valueOf(estabelecimento))
                                .param("nome", "Segunda Arena")
                                .param("modalidade", "Vôlei")
                                .param("localizacao", "Rua Dois")
                                .param("valorHora", "120.00")
                                .param("situacao", "ATIVA")
                                .param("idProprietario", String.valueOf(outroDono.getId())))
                .andExpect(redirectedUrl("/proprietario/quadras"));
        assertThat(quadras.minhas(dono.getId())).hasSize(2);
        assertThat(quadras.minhas(outroDono.getId())).isEmpty();
    }

    @Test
    void rejeitaValorNegativoEAlteracaoDeOutroDono() throws Exception {
        mvc.perform(
                        post("/proprietario/quadras/nova")
                                .with(user(dono))
                                .with(csrf())
                                .param("nome", "Arena")
                                .param("modalidade", "Vôlei")
                                .param("localizacao", "Rua Dois")
                                .param("valorHora", "-1")
                                .param("situacao", "ATIVA"))
                .andExpect(model().attributeHasErrors("form"));
        mvc.perform(get("/proprietario/quadras/" + quadra + "/editar").with(user(outroDono)))
                .andExpect(status().isForbidden());
        assertThatThrownBy(() -> quadras.salvar(quadra, quadraForm("INATIVA"), outroDono))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void consultaNaoExpoeDadosDoProprietario() throws Exception {
        mvc.perform(get("/cliente/quadras").with(user(cliente)))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "dono@example.test"))))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.not(
                                                org.hamcrest.Matchers.containsString(
                                                        "senha_hash"))));
    }

    @Test
    void validaDatasDisponibilidadesEPropriedade() {
        assertThatThrownBy(() -> agenda.disponibilizar(quadra, periodo("09:00", "12:00"), dono))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> agenda.disponibilizar(quadra, periodo("20:00", "19:00"), dono))
                .isInstanceOf(RegraNegocioException.class);
        var passado = periodo("19:00", "20:00");
        passado.setData(data.minusDays(5));
        assertThatThrownBy(() -> agenda.disponibilizar(quadra, passado, dono))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(
                        () -> agenda.disponibilizar(quadra, periodo("19:00", "20:00"), outroDono))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(agenda.disponibilizar(quadra, periodo("18:00", "19:00"), dono)).isPositive();
    }

    @Test
    void reservaCalculaValorERecortaIntervalosLivres() {
        long id = agenda.reservar(quadra, periodo("10:00", "11:30"), cliente);
        var reserva = repositorio.buscarReserva(id).orElseThrow();
        assertThat(reserva.valorTotal()).isEqualByComparingTo("135.00");
        assertThat(reserva.idCliente()).isEqualTo(cliente.getId());
        assertThat(agenda.horariosLivres(quadra))
                .containsExactly(
                        new HorarioLivre(
                                data,
                                LocalTime.of(8, 0),
                                LocalTime.of(10, 0),
                                new BigDecimal("90.00"),
                                0),
                        new HorarioLivre(
                                data,
                                LocalTime.of(11, 30),
                                LocalTime.of(18, 0),
                                new BigDecimal("90.00"),
                                0));
    }

    @Test
    void rejeitaSobreposicaoParcialTotalEForaDaDisponibilidade() {
        agenda.reservar(quadra, periodo("10:00", "12:00"), cliente);
        for (String[] intervalo :
                List.of(
                        new String[] {"10:00", "12:00"},
                        new String[] {"09:00", "11:00"},
                        new String[] {"11:00", "13:00"},
                        new String[] {"09:00", "13:00"},
                        new String[] {"07:00", "09:00"}))
            assertThatThrownBy(
                            () ->
                                    agenda.reservar(
                                            quadra,
                                            periodo(intervalo[0], intervalo[1]),
                                            outroCliente))
                    .isInstanceOf(RegraNegocioException.class);
        assertThat(agenda.reservar(quadra, periodo("12:00", "13:00"), outroCliente)).isPositive();
    }

    @Test
    void bancoRejeitaSobreposicaoMesmoSemPassarPeloServico() {
        repositorio.reservar(
                cliente.getId(),
                quadra,
                data,
                LocalTime.of(10, 0),
                LocalTime.of(12, 0),
                BigDecimal.TEN);
        assertThatThrownBy(
                        () ->
                                repositorio.reservar(
                                        outroCliente.getId(),
                                        quadra,
                                        data,
                                        LocalTime.of(11, 0),
                                        LocalTime.of(13, 0),
                                        BigDecimal.TEN))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void bancoImpedeReferenciasComPerfilErrado() {
        assertThatThrownBy(
                        () ->
                                repositorio.reservar(
                                        dono.getId(),
                                        quadra,
                                        data,
                                        LocalTime.of(10, 0),
                                        LocalTime.of(11, 0),
                                        BigDecimal.TEN))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "INSERT INTO"
                                            + " quadra(id_proprietario,nome,modalidade,localizacao,valor_hora,situacao)"
                                            + " VALUES (?,?,?,?,?,?)",
                                        cliente.getId(),
                                        "Inválida",
                                        "Futsal",
                                        "Teste",
                                        90,
                                        "ATIVA"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void duasSolicitacoesSimultaneasConfirmamSomenteUma() throws Exception {
        var inicio = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> primeira =
                    () -> {
                        inicio.await();
                        try {
                            agenda.reservar(quadra, periodo("10:00", "11:00"), cliente);
                            return true;
                        } catch (RegraNegocioException e) {
                            return false;
                        }
                    };
            Callable<Boolean> segunda =
                    () -> {
                        inicio.await();
                        try {
                            agenda.reservar(quadra, periodo("10:00", "11:00"), outroCliente);
                            return true;
                        } catch (RegraNegocioException e) {
                            return false;
                        }
                    };
            var a = executor.submit(primeira);
            var b = executor.submit(segunda);
            inicio.countDown();
            assertThat(List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }
        assertThat(repositorio.reservasDaQuadra(quadra)).hasSize(1);
    }

    @Test
    void quadraInativaNaoAceitaReservaMasPreservaHistorico() {
        agenda.reservar(quadra, periodo("10:00", "11:00"), cliente);
        quadras.salvar(quadra, quadraForm("INATIVA"), dono);
        assertThat(quadras.ativas()).isEmpty();
        assertThat(agenda.horariosLivres(quadra)).isEmpty();
        assertThatThrownBy(() -> agenda.reservar(quadra, periodo("12:00", "13:00"), cliente))
                .isInstanceOf(RegraNegocioException.class);
        assertThat(agenda.minhasReservas(cliente, null, null)).hasSize(1);
    }

    @Test
    void consultasECancelamentoRespeitamDonoDaReserva() {
        long id = agenda.reservar(quadra, periodo("10:00", "11:00"), cliente);
        assertThat(agenda.minhasReservas(outroCliente, null, null)).isEmpty();
        assertThat(agenda.minhasReservas(outroDono, null, null)).isEmpty();
        assertThat(agenda.minhasReservas(dono, quadra, data)).hasSize(1);
        assertThat(agenda.minhasReservas(dono, quadra, data.plusDays(1))).isEmpty();
        assertThatThrownBy(() -> agenda.cancelar(id, outroCliente))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> agenda.cancelar(id, outroDono))
                .isInstanceOf(AccessDeniedException.class);
        agenda.cancelar(id, cliente);
        assertThat(repositorio.buscarReserva(id).orElseThrow().situacao()).isEqualTo("CANCELADA");
        assertThat(agenda.horariosLivres(quadra))
                .containsExactly(
                        new HorarioLivre(
                                data,
                                LocalTime.of(8, 0),
                                LocalTime.of(18, 0),
                                new BigDecimal("90.00"),
                                0));
        assertThat(agenda.reservar(quadra, periodo("10:00", "11:00"), outroCliente)).isPositive();
    }

    @Test
    void disponibilidadeComReservaNaoPodeSerInativada() {
        long id = agenda.disponibilidades(quadra, dono).getFirst().id();
        long reserva = agenda.reservar(quadra, periodo("10:00", "11:00"), cliente);
        assertThatThrownBy(() -> agenda.inativarDisponibilidade(quadra, id, dono))
                .isInstanceOf(RegraNegocioException.class);
        agenda.cancelar(reserva, dono);
        agenda.inativarDisponibilidade(quadra, id, dono);
        assertThat(agenda.horariosLivres(quadra)).isEmpty();
    }

    @Test
    void fluxoDeReservaPeloFormulario() throws Exception {
        mvc.perform(
                        post("/cliente/quadras/" + quadra + "/reservar")
                                .with(user(cliente))
                                .with(csrf())
                                .param("data", data.toString())
                                .param("horaInicio", "10:00")
                                .param("horaFim", "11:00")
                                .param("idCliente", String.valueOf(outroCliente.getId())))
                .andExpect(redirectedUrl("/reservas"));
        var reserva = agenda.minhasReservas(cliente, null, null).getFirst();
        assertThat(reserva.idCliente()).isEqualTo(cliente.getId());
        mvc.perform(
                        post("/cliente/quadras/" + quadra + "/reservar")
                                .with(user(outroCliente))
                                .with(csrf())
                                .param("data", data.toString())
                                .param("horaInicio", "10:30")
                                .param("horaFim", "11:30"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasErrors("form"));
    }

    @Autowired PadraoService padroes;
    @Autowired NotificacaoService notificacoes;

    private PadraoForm padrao() {
        var f = new PadraoForm();
        f.setDias(List.of(data.getDayOfWeek().getValue()));
        f.setHoraInicio(LocalTime.of(19, 0));
        f.setHoraFim(LocalTime.of(23, 0));
        f.setDuracaoMinutos(60);
        f.setToleranciaMinutos(10);
        f.setValorHora(new BigDecimal("120.00"));
        return f;
    }

    @Test
    void documentosNumericosEAlfanumericos() {
        assertThat(Documento.valido("529.982.247-25", false)).isTrue();
        assertThat(Documento.valido("12.345.678/0001-95", true)).isTrue();
        assertThat(Documento.valido("12.ABC.345/01DE-35", true)).isTrue();
        assertThat(Documento.valido("12.abc.345/01de-35", true)).isTrue();
        for (String d :
                List.of(
                        "",
                        "11111111111",
                        "00000000000000",
                        "12ABC34501DE36",
                        "52998224726",
                        "52998224725!")) assertThat(Documento.valido(d, true)).as(d).isFalse();
        assertThat(Documento.valido("12ABC34501DE35", false)).isFalse();
    }

    @Test
    void cadastraProprietarioComCnpjAlfanumerico() throws Exception {
        mvc.perform(
                        post("/cadastro/proprietario")
                                .with(csrf())
                                .param("nome", "Proprietário CNPJ")
                                .param("email", "cnpj@example.test")
                                .param("cpf", "12.abc.345/01de-35")
                                .param("senha", "SenhaForte123!")
                                .param("confirmacaoSenha", "SenhaForte123!"))
                .andExpect(redirectedUrl("/login?cadastrado"));
        assertThat(usuarios.buscarPorEmail("cnpj@example.test").orElseThrow().getCpf())
                .isEqualTo("12ABC34501DE35");
    }

    @Test
    void rejeitaSenhasFracasMesmoPeloServico() {
        for (String senha :
                List.of(
                        "12345678",
                        "abcdefgh",
                        "ABCDEFGH",
                        "Senha123",
                        "senha123!",
                        "SENHA123!",
                        "Senha 123",
                        "Aa1!")) {
            var f = new CadastroForm();
            f.setNome("Nome Teste");
            f.setEmail("fraca@example.test");
            f.setCpf("93541134780");
            f.setSenha(senha);
            f.setConfirmacaoSenha(senha);
            assertThatThrownBy(() -> contas.cadastrar(f, Perfil.CLIENTE))
                    .isInstanceOf(RegraNegocioException.class);
        }
    }

    @Test
    void novasTelasRenderizamEDadosInvalidosReapresentamFormulario() throws Exception {
        for (String url :
                List.of(
                        "/proprietario/estabelecimentos",
                        "/proprietario/estabelecimentos/novo",
                        "/proprietario/estabelecimentos/" + estabelecimento + "/editar",
                        "/proprietario/agenda",
                        "/proprietario/quadras/" + quadra + "/padroes"))
            mvc.perform(get(url).with(user(dono))).andExpect(status().isOk());
        mvc.perform(get("/cliente/estabelecimentos").with(user(cliente)))
                .andExpect(status().isOk());
        mvc.perform(
                        post("/proprietario/estabelecimentos/novo")
                                .with(user(dono))
                                .with(csrf())
                                .param("nome", "Centro")
                                .param("localizacao", "Rua")
                                .param("latitude", "95"))
                .andExpect(model().attributeHasErrors("form"));
        mvc.perform(
                        post("/proprietario/quadras/" + quadra + "/padroes")
                                .with(user(dono))
                                .with(csrf()))
                .andExpect(model().attributeHasErrors("form"));
    }

    @Test
    void estabelecimentoObrigatorioENaoPodeSerDeOutroDono() throws Exception {
        var f = quadraForm("ATIVA");
        f.setIdEstabelecimento(null);
        assertThatThrownBy(() -> quadras.salvar(null, f, dono))
                .isInstanceOf(RegraNegocioException.class);
        f.setIdEstabelecimento(estabelecimento);
        assertThatThrownBy(() -> quadras.salvar(null, f, outroDono))
                .isInstanceOf(AccessDeniedException.class);
        mvc.perform(
                        get("/proprietario/estabelecimentos/" + estabelecimento + "/editar")
                                .with(user(outroDono)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/proprietario/quadras/" + quadra + "/padroes").with(user(outroDono)))
                .andExpect(status().isForbidden());
    }

    @Test
    void coordenadasPersistemNoEstabelecimentoENaQuadra() throws Exception {
        mvc.perform(
                        post("/proprietario/estabelecimentos/" + estabelecimento + "/editar")
                                .with(user(dono))
                                .with(csrf())
                                .param("nome", "Centro no mapa")
                                .param("localizacao", "Rua do mapa")
                                .param("latitude", "-15.1234567")
                                .param("longitude", "-47.7654321"))
                .andExpect(redirectedUrl("/proprietario/estabelecimentos"));
        assertThat(estabelecimentos.propria(estabelecimento, dono).latitude())
                .isEqualByComparingTo("-15.1234567");
        var f = quadraForm("ATIVA");
        f.setLatitude(new BigDecimal("-15.1234567"));
        f.setLongitude(new BigDecimal("-47.7654321"));
        quadras.salvar(quadra, f, dono);
        assertThat(quadras.buscar(quadra).longitude()).isEqualByComparingTo("-47.7654321");
    }

    @Test
    void toleranciaPadraoDezMinutosProtegeReservasParciaisNosDoisSentidos() {
        var avulso = periodo("19:00", "23:00");
        avulso.setToleranciaMinutos(10);
        agenda.disponibilizar(quadra, avulso, dono);
        agenda.reservar(quadra, periodo("20:00", "20:30"), cliente);
        for (String[] h :
                List.of(
                        new String[] {"20:30", "21:00"},
                        new String[] {"20:39", "21:00"},
                        new String[] {"19:00", "19:51"}))
            assertThatThrownBy(() -> agenda.reservar(quadra, periodo(h[0], h[1]), outroCliente))
                    .isInstanceOf(RegraNegocioException.class);
        assertThat(agenda.reservar(quadra, periodo("20:40", "21:00"), outroCliente)).isPositive();
        assertThat(agenda.reservar(quadra, periodo("19:00", "19:50"), outroCliente)).isPositive();
    }

    @Test
    void toleranciaAtravessaMeiaNoiteSemLiberarConflito() {
        var a = periodo("23:00", "23:59");
        a.setToleranciaMinutos(10);
        agenda.disponibilizar(quadra, a, dono);
        agenda.reservar(quadra, periodo("23:30", "23:59"), cliente);
        var b = periodo("00:00", "02:00");
        b.setData(data.plusDays(1));
        agenda.disponibilizar(quadra, b, dono);
        var r = periodo("00:08", "01:00");
        r.setData(data.plusDays(1));
        assertThatThrownBy(() -> agenda.reservar(quadra, r, outroCliente))
                .isInstanceOf(RegraNegocioException.class);
        r.setHoraInicio(LocalTime.of(0, 9));
        assertThat(agenda.reservar(quadra, r, outroCliente)).isPositive();
    }

    @Test
    void precoDoHorarioPrevaleceEClienteNaoPodeForjarPrecoOuTolerancia() {
        var f = periodo("19:00", "23:00");
        f.setValorHora(new BigDecimal("120.00"));
        f.setToleranciaMinutos(15);
        agenda.disponibilizar(quadra, f, dono);
        var r = periodo("20:00", "20:30");
        r.setValorHora(BigDecimal.ZERO);
        r.setToleranciaMinutos(0);
        var reserva = repositorio.buscarReserva(agenda.reservar(quadra, r, cliente)).orElseThrow();
        assertThat(reserva.valorTotal()).isEqualByComparingTo("60.00");
        assertThat(reserva.toleranciaMinutos()).isEqualTo(15);
        assertThatThrownBy(() -> agenda.reservar(quadra, periodo("20:40", "21:00"), outroCliente))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void padraoGeraDiasCorretosAteFimDoMesSeguinteSemDuplicar() {
        long id = padroes.criar(quadra, padrao(), dono);
        padroes.renovar(quadra);
        padroes.renovar(quadra);
        var geradas =
                repositorio.disponibilidades(quadra).stream()
                        .filter(d -> d.idPadrao() != null && d.idPadrao() == id)
                        .toList();
        assertThat(geradas).isNotEmpty();
        var limite =
                LocalDate.now(ZoneId.of("America/Sao_Paulo"))
                        .plusMonths(2)
                        .withDayOfMonth(1)
                        .minusDays(1);
        assertThat(geradas)
                .allMatch(
                        d ->
                                d.data().getDayOfWeek() == data.getDayOfWeek()
                                        && !d.data().isAfter(limite));
        assertThat(
                        geradas.stream()
                                .filter(d -> d.data().equals(data))
                                .map(d -> d.horaInicio())
                                .toList())
                .containsExactly(LocalTime.of(19, 0), LocalTime.of(20, 10), LocalTime.of(21, 20));
        assertThat(geradas.stream().map(d -> d.data() + " " + d.horaInicio()).distinct().count())
                .isEqualTo(geradas.size());
        var r =
                repositorio
                        .buscarReserva(agenda.reservar(quadra, periodo("19:00", "19:30"), cliente))
                        .orElseThrow();
        assertThat(r.valorTotal()).isEqualByComparingTo("60.00");
    }

    @Test
    void padraoDesativadoPreservaReservasERetiraHorariosVazios() {
        long id = padroes.criar(quadra, padrao(), dono);
        long r = agenda.reservar(quadra, periodo("19:00", "20:00"), cliente);
        padroes.desativar(quadra, id, dono);
        padroes.renovar(quadra);
        assertThat(repositorio.buscarReserva(r).orElseThrow().situacao()).isEqualTo("CONFIRMADA");
        var geradas =
                repositorio.disponibilidades(quadra).stream()
                        .filter(d -> d.idPadrao() != null && d.idPadrao() == id)
                        .toList();
        assertThat(geradas.stream().filter(d -> d.ativo()).count()).isEqualTo(1);
        assertThat(agenda.horariosLivres(quadra))
                .noneMatch(
                        h -> h.data().equals(data) && h.horaInicio().isAfter(LocalTime.of(18, 0)));
    }

    @Test
    void horarioInativadoNaoRenasceNaRenovacao() {
        long id = padroes.criar(quadra, padrao(), dono);
        var d =
                repositorio.disponibilidades(quadra).stream()
                        .filter(
                                v ->
                                        v.idPadrao() != null
                                                && v.idPadrao() == id
                                                && v.data().equals(data))
                        .findFirst()
                        .orElseThrow();
        agenda.inativarDisponibilidade(quadra, d.id(), dono);
        padroes.renovar(quadra);
        assertThat(
                        repositorio.disponibilidades(quadra).stream()
                                .filter(v -> v.id() == d.id())
                                .findFirst()
                                .orElseThrow()
                                .ativo())
                .isFalse();
        var avulso = periodo("19:00", "20:00");
        avulso.setValorHora(new BigDecimal("150.00"));
        agenda.disponibilizar(quadra, avulso, dono);
        padroes.renovar(quadra);
        assertThat(
                        repositorio
                                .buscarReserva(agenda.reservar(quadra, avulso, cliente))
                                .orElseThrow()
                                .valorTotal())
                .isEqualByComparingTo("150.00");
    }

    @Test
    void padroesConflitantesEDeOutrosDonosSaoRejeitados() {
        padroes.criar(quadra, padrao(), dono);
        assertThatThrownBy(() -> padroes.criar(quadra, padrao(), dono))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> padroes.criar(quadra, padrao(), outroDono))
                .isInstanceOf(AccessDeniedException.class);
        var f = padrao();
        f.setDuracaoMinutos(400);
        assertThatThrownBy(() -> padroes.criar(quadra, f, dono))
                .isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void cancelamentoNotificaOutraParteUmaVezENaoVazaEntreContas() throws Exception {
        long r = agenda.reservar(quadra, periodo("10:00", "11:00"), cliente);
        agenda.cancelar(r, dono);
        agenda.cancelar(r, dono);
        assertThat(notificacoes.pendentes(cliente.getId())).hasSize(1);
        assertThat(notificacoes.pendentes(outroCliente.getId())).isEmpty();
        mvc.perform(get("/cliente/quadras").with(user(cliente)))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "cancelada pelo proprietário")));
        mvc.perform(get("/").with(user(cliente)))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "cancelada pelo proprietário")));
        var n = notificacoes.pendentes(cliente.getId()).getFirst();
        notificacoes.ler(n.id(), outroCliente.getId());
        assertThat(notificacoes.pendentes(cliente.getId())).hasSize(1);
        mvc.perform(post("/notificacoes/" + n.id() + "/ler").with(user(cliente)).with(csrf()))
                .andExpect(redirectedUrl("/painel"));
        assertThat(notificacoes.pendentes(cliente.getId())).isEmpty();
        r = agenda.reservar(quadra, periodo("10:00", "11:00"), cliente);
        agenda.cancelar(r, cliente);
        assertThat(notificacoes.pendentes(dono.getId())).hasSize(1);
    }

    @Test
    void filtrosCombinamModalidadeEstabelecimentoDataIntervaloEPreco() throws Exception {
        mvc.perform(
                        get("/cliente/quadras")
                                .with(user(cliente))
                                .param("modalidade", "futsal")
                                .param("localizacao", "teste")
                                .param("estabelecimento", "" + estabelecimento)
                                .param("data", data.toString())
                                .param("inicio", "10:00")
                                .param("fim", "11:00")
                                .param("valorMaximo", "90"))
                .andExpect(model().attribute("quadras", org.hamcrest.Matchers.hasSize(1)));
        agenda.reservar(quadra, periodo("10:00", "11:00"), cliente);
        mvc.perform(
                        get("/cliente/quadras")
                                .with(user(cliente))
                                .param("data", data.toString())
                                .param("inicio", "10:00")
                                .param("fim", "11:00"))
                .andExpect(model().attribute("quadras", org.hamcrest.Matchers.hasSize(0)));
        mvc.perform(get("/cliente/quadras").with(user(cliente)).param("valorMaximo", "89"))
                .andExpect(model().attribute("quadras", org.hamcrest.Matchers.hasSize(0)));
        mvc.perform(get("/cliente/quadras").with(user(cliente)).param("inicio", "10:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void agendaNavegavelFiltraDataQuadraEEstabelecimento() throws Exception {
        agenda.reservar(quadra, periodo("10:00", "11:00"), cliente);
        mvc.perform(
                        get("/proprietario/agenda")
                                .with(user(dono))
                                .param("data", data.toString())
                                .param("quadra", "" + quadra)
                                .param("estabelecimento", "" + estabelecimento)
                                .param("ordem", "horario"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("reservas", org.hamcrest.Matchers.hasSize(1)));
        mvc.perform(
                        get("/proprietario/agenda")
                                .with(user(outroDono))
                                .param("data", data.toString())
                                .param("quadra", "" + quadra))
                .andExpect(model().attribute("reservas", org.hamcrest.Matchers.hasSize(0)));
    }

    @Autowired org.springframework.jdbc.core.simple.JdbcClient jdbcClient;
    @Autowired QuadraRepository quadraRepository;
    @Autowired org.springframework.transaction.PlatformTransactionManager transacoes;

    @Test
    void renovacaoAvancaComORelogioAteNovoMesSeguinte() {
        padroes.criar(quadra, padrao(), dono);
        LocalDate futuro =
                LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusMonths(1).withDayOfMonth(2);
        Clock clock =
                Clock.fixed(
                        futuro.atStartOfDay(ZoneId.of("America/Sao_Paulo")).toInstant(),
                        ZoneId.of("America/Sao_Paulo"));
        var renovador = new PadraoService(jdbcClient, quadraRepository, quadras, clock);
        new org.springframework.transaction.support.TransactionTemplate(transacoes)
                .executeWithoutResult(status -> renovador.renovar(quadra));
        var max =
                repositorio.disponibilidades(quadra).stream()
                        .filter(d -> d.idPadrao() != null)
                        .map(d -> d.data())
                        .max(LocalDate::compareTo)
                        .orElseThrow();
        var limite = futuro.plusMonths(2).withDayOfMonth(1).minusDays(1);
        assertThat(max)
                .isAfter(futuro.withDayOfMonth(1).plusMonths(1).minusDays(1))
                .isBeforeOrEqualTo(limite);
    }

    @Test
    void renovacoesSimultaneasNaoDuplicamHorarios() throws Exception {
        long id = padroes.criar(quadra, padrao(), dono);
        long antes =
                repositorio.disponibilidades(quadra).stream()
                        .filter(d -> d.idPadrao() != null && d.idPadrao() == id)
                        .count();
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> padroes.renovar(quadra));
            var b = pool.submit(() -> padroes.renovar(quadra));
            a.get(15, TimeUnit.SECONDS);
            b.get(15, TimeUnit.SECONDS);
        }
        assertThat(
                        repositorio.disponibilidades(quadra).stream()
                                .filter(d -> d.idPadrao() != null && d.idPadrao() == id)
                                .count())
                .isEqualTo(antes);
    }

    @Test
    void bancoProtegeToleranciaMesmoSemPassarPeloServico() {
        repositorio.reservar(
                cliente.getId(),
                quadra,
                data,
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                BigDecimal.TEN,
                10);
        assertThatThrownBy(
                        () ->
                                repositorio.reservar(
                                        outroCliente.getId(),
                                        quadra,
                                        data,
                                        LocalTime.of(11, 9),
                                        LocalTime.of(12, 0),
                                        BigDecimal.TEN,
                                        0))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(
                        repositorio.reservar(
                                outroCliente.getId(),
                                quadra,
                                data,
                                LocalTime.of(11, 10),
                                LocalTime.of(12, 0),
                                BigDecimal.TEN,
                                0))
                .isPositive();
    }

    @Test
    void precoHistoricoNaoMudaQuandoQuadraEEditada() {
        long id = agenda.reservar(quadra, periodo("10:00", "11:00"), cliente);
        var f = quadraForm("ATIVA");
        f.setValorHora(new BigDecimal("250.00"));
        quadras.salvar(quadra, f, dono);
        assertThat(repositorio.buscarReserva(id).orElseThrow().valorTotal())
                .isEqualByComparingTo("90.00");
    }

    @Test
    void precoDePadroesVariaEntreDiasDaMesmaQuadra() {
        padroes.criar(quadra, padrao(), dono);
        var f = padrao();
        f.setDias(List.of(data.plusDays(1).getDayOfWeek().getValue()));
        f.setValorHora(new BigDecimal("180.00"));
        padroes.criar(quadra, f, dono);
        var r = periodo("19:00", "19:30");
        r.setData(data.plusDays(1));
        assertThat(
                        repositorio
                                .buscarReserva(agenda.reservar(quadra, r, cliente))
                                .orElseThrow()
                                .valorTotal())
                .isEqualByComparingTo("90.00");
    }
}
