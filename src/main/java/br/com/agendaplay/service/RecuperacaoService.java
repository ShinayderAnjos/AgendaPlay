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
    private final String baseUrl,remetente;
    private final SecureRandom random=new SecureRandom();
    private static final String ERRO="Código inválido, expirado ou sem tentativas disponíveis. Solicite um novo código.";

    public RecuperacaoService(JdbcClient jdbc,PasswordEncoder senhas,JavaMailSender mail,Clock clock,
            @Value("${agendaplay.url-publica:http://localhost:8080}") String baseUrl,
            @Value("${agendaplay.email-remetente:nao-responda@agendaplay.local}") String remetente) {
        this.jdbc=jdbc; this.senhas=senhas; this.mail=mail; this.clock=clock;
        this.baseUrl=baseUrl; this.remetente=remetente;
    }
    private String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
            .digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch(NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private String token() {
        byte[] bytes=new byte[32]; random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    private Optional<Long> usuarioBloqueado(String email) {
        if(email==null || email.length()>160) return Optional.empty();
        return jdbc.sql("SELECT id FROM usuario WHERE email=? FOR UPDATE")
            .param(email.trim().toLowerCase(Locale.ROOT)).query(Long.class).optional();
    }
    @Transactional
    public void solicitar(String email) {
        var id=usuarioBloqueado(email);
        if(id.isEmpty()) return;
        var agora=OffsetDateTime.now(clock);
        if(jdbc.sql("SELECT count(*) FROM recuperacao_envio WHERE id_usuario=? AND enviado_em>?")
            .params(id.get(),agora.minusMinutes(1)).query(Long.class).single()>0) return;
        if(jdbc.sql("SELECT count(*) FROM recuperacao_envio WHERE id_usuario=? AND enviado_em>?")
            .params(id.get(),agora.minusHours(1)).query(Long.class).single()>=5) return;
        String codigo=String.format(Locale.ROOT,"%06d",random.nextInt(1_000_000));
        jdbc.sql("DELETE FROM recuperacao_senha WHERE id_usuario=? OR expira_em<=?")
            .params(id.get(),agora).update();
        jdbc.sql("""
            INSERT INTO recuperacao_senha(token_hash,id_usuario,expira_em,criado_em,codigo_hash)
            VALUES (?,?,?,?,?)
            """).params(hash(token()),id.get(),agora.plusMinutes(10),agora,senhas.encode(codigo)).update();
        jdbc.sql("DELETE FROM recuperacao_envio WHERE enviado_em<=?").param(agora.minusDays(1)).update();
        jdbc.sql("INSERT INTO recuperacao_envio(id_usuario,enviado_em) VALUES (?,?)").params(id.get(),agora).update();
        var mensagem=new SimpleMailMessage();
        mensagem.setFrom(remetente); mensagem.setTo(email.trim().toLowerCase(Locale.ROOT));
        mensagem.setSubject("AgendaPlay — código para redefinir senha");
        mensagem.setText("Seu código de verificação é: "+codigo+
            "\n\nDigite o código em "+baseUrl+"/validar-codigo"+
            "\nO código expira em 10 minutos e permite até 5 tentativas. Ele só pode ser utilizado uma vez."+
            "\n\nSe não pediu a alteração, ignore esta mensagem.");
        mail.send(mensagem);
    }
    // A falha precisa confirmar o contador de tentativas, sem desfazer a transação.
    @Transactional(noRollbackFor=RegraNegocioException.class)
    public String validarCodigo(String email,String codigo) {
        var id=usuarioBloqueado(email);
        if(id.isEmpty()) throw new RegraNegocioException(ERRO);
        var agora=OffsetDateTime.now(clock);
        var registro=jdbc.sql("""
            SELECT token_hash,codigo_hash,tentativas FROM recuperacao_senha
            WHERE id_usuario=? AND codigo_hash IS NOT NULL AND expira_em>? FOR UPDATE
            """).params(id.get(),agora).query(Codigo.class).optional();
        if(registro.isEmpty() || registro.get().tentativas()>=5) throw new RegraNegocioException(ERRO);
        var c=registro.get();
        jdbc.sql("UPDATE recuperacao_senha SET tentativas=tentativas+1 WHERE token_hash=?")
            .param(c.tokenHash()).update();
        if(codigo==null || !codigo.matches("[0-9]{6}") || !senhas.matches(codigo,c.codigoHash()))
            throw new RegraNegocioException(ERRO);
        String permissao=token();
        jdbc.sql("DELETE FROM recuperacao_senha WHERE id_usuario=?").param(id.get()).update();
        jdbc.sql("INSERT INTO recuperacao_senha(token_hash,id_usuario,expira_em,criado_em) VALUES (?,?,?,?)")
            .params(hash(permissao),id.get(),agora.plusMinutes(10),agora).update();
        return permissao;
    }
    public boolean valido(String token) {
        return token!=null && !token.isBlank() && token.length()<=128
            && jdbc.sql("""
                SELECT count(*) FROM recuperacao_senha WHERE token_hash=? AND expira_em>?
                AND codigo_hash IS NULL
                """).params(hash(token),OffsetDateTime.now(clock)).query(Long.class).single()>0;
    }
    @Transactional
    public void redefinir(String token,String senha,String confirmacao) {
        UsuarioService.validarSenha(senha,confirmacao);
        if(token==null || token.isBlank() || token.length()>128)
            throw new RegraNegocioException("Validação inválida ou expirada. Solicite um novo código.");
        var usuario=jdbc.sql("SELECT id_usuario FROM recuperacao_senha WHERE token_hash=? AND codigo_hash IS NULL")
            .param(hash(token)).query(Long.class).optional();
        if(usuario.isEmpty()) throw new RegraNegocioException("Validação inválida ou expirada. Solicite um novo código.");
        jdbc.sql("SELECT id FROM usuario WHERE id=? FOR UPDATE").param(usuario.get()).query(Long.class).single();
        var id=jdbc.sql("""
            DELETE FROM recuperacao_senha WHERE token_hash=? AND expira_em>?
            AND codigo_hash IS NULL RETURNING id_usuario
            """).params(hash(token),OffsetDateTime.now(clock)).query(Long.class).optional();
        if(id.isEmpty()) throw new RegraNegocioException("Validação inválida ou expirada. Solicite um novo código.");
        jdbc.sql("UPDATE usuario SET senha_hash=? WHERE id=?").params(senhas.encode(senha),id.get()).update();
        jdbc.sql("DELETE FROM recuperacao_senha WHERE id_usuario=?").param(id.get()).update();
    }
    private record Codigo(String tokenHash,String codigoHash,int tentativas) {}
}
