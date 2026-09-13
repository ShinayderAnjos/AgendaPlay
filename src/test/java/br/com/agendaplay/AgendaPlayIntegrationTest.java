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
                "Proprietário Teste", "dono@example.test", null, "", hash, Perfil.PROPRIETARIO);
        usuarios.cadastrar(
                "Outro Proprietário",
                "outrodono@example.test",
                null,
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
        quadra = quadras.salvar(null, quadraForm("ATIVA"), dono);
        agenda.disponibilizar(quadra, periodo("08:00", "18:00"), dono);
    }

    private UsuarioAutenticado autenticado(String email) {
        return new UsuarioAutenticado(usuarios.buscarPorEmail(email).orElseThrow());
    }

    private QuadraForm quadraForm(String situacao) {
        var f = new QuadraForm();
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
                                .param("cpf", "12345678909")
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
    void proprietarioPodeCadastrarSemCpf() throws Exception {
        mvc.perform(
                        post("/cadastro/proprietario")
                                .with(csrf())
                                .param("nome", "Proprietário Novo")
                                .param("email", "novo@example.test")
                                .param("senha", "SenhaTeste123!")
                                .param("confirmacaoSenha", "SenhaTeste123!"))
                .andExpect(redirectedUrl("/login?cadastrado"));
        assertThat(usuarios.buscarPorEmail("novo@example.test").orElseThrow().getPerfil())
                .isEqualTo(Perfil.PROPRIETARIO);
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
                                .param("cpf", "12345678909")
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
                                .param("cpf", "12345678909")
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
                        new HorarioLivre(data, LocalTime.of(8, 0), LocalTime.of(10, 0)),
                        new HorarioLivre(data, LocalTime.of(11, 30), LocalTime.of(18, 0)));
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
                .containsExactly(new HorarioLivre(data, LocalTime.of(8, 0), LocalTime.of(18, 0)));
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
}
