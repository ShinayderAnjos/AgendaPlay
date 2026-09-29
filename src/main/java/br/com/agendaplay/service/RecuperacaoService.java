package br.com.agendaplay.service;

import br.com.agendaplay.exception.RegraNegocioException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service
public class RecuperacaoService {
    private final JdbcClient jdbc;
    private final PasswordEncoder senhas;
    private final JavaMailSender mail;
    private final Clock clock;
    private final String baseUrl, remetente;

    public RecuperacaoService(
            JdbcClient jdbc,
            PasswordEncoder senhas,
            JavaMailSender mail,
            Clock clock,
            @Value("${agendaplay.url-publica:http://localhost:8080}") String baseUrl,
            @Value("${agendaplay.email-remetente:nao-responda@agendaplay.local}")
                    String remetente) {
        this.jdbc = jdbc;
        this.senhas = senhas;
        this.mail = mail;
        this.clock = clock;
        this.baseUrl = baseUrl;
        this.remetente = remetente;
    }

    private String hash(String token) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Transactional
    public void solicitar(String email) {
        if (email == null || email.length() > 160) return;
        var id =
                jdbc.sql("SELECT id FROM usuario WHERE email=? FOR UPDATE")
                        .param(email.trim().toLowerCase(Locale.ROOT))
                        .query(Long.class)
                        .optional();
        if (id.isEmpty()) return;
        var agora = OffsetDateTime.now(clock);
        if (jdbc.sql("SELECT count(*) FROM recuperacao_senha WHERE id_usuario=? AND criado_em>?")
                        .params(id.get(), agora.minusMinutes(1))
                        .query(Long.class)
                        .single()
                > 0) return;
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.sql("DELETE FROM recuperacao_senha WHERE id_usuario=? OR expira_em<=?")
                .params(id.get(), agora)
                .update();
        jdbc.sql(
                        "INSERT INTO recuperacao_senha(token_hash,id_usuario,expira_em,criado_em)"
                            + " VALUES (?,?,?,?)")
                .params(hash(token), id.get(), agora.plusMinutes(30), agora)
                .update();
        var mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(email.trim().toLowerCase(Locale.ROOT));
        mensagem.setSubject("AgendaPlay — redefinir senha");
        mensagem.setText(
                "Para redefinir sua senha, abra o link abaixo. Ele expira em 30 minutos e funciona"
                    + " uma única vez.\n\n"
                        + baseUrl
                        + "/redefinir-senha?token="
                        + token
                        + "\n\nSe não pediu a alteração, ignore esta mensagem.");
        mail.send(mensagem);
    }

    public boolean valido(String token) {
        return token != null
                && token.length() <= 128
                && jdbc.sql(
                                        "SELECT count(*) FROM recuperacao_senha WHERE token_hash=?"
                                            + " AND expira_em>?")
                                .params(hash(token), OffsetDateTime.now(clock))
                                .query(Long.class)
                                .single()
                        > 0;
    }

    @Transactional
    public void redefinir(String token, String senha, String confirmacao) {
        UsuarioService.validarSenha(senha, confirmacao);
        if (token == null || token.length() > 128)
            throw new RegraNegocioException("Link inválido ou expirado. Solicite um novo link.");
        var id =
                jdbc.sql(
                                "DELETE FROM recuperacao_senha WHERE token_hash=? AND expira_em>?"
                                    + " RETURNING id_usuario")
                        .params(hash(token), OffsetDateTime.now(clock))
                        .query(Long.class)
                        .optional();
        if (id.isEmpty())
            throw new RegraNegocioException("Link inválido ou expirado. Solicite um novo link.");
        jdbc.sql("UPDATE usuario SET senha_hash=? WHERE id=?")
                .params(senhas.encode(senha), id.get())
                .update();
        jdbc.sql("DELETE FROM recuperacao_senha WHERE id_usuario=?").param(id.get()).update();
    }
}
