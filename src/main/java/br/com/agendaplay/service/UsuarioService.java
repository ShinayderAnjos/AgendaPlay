package br.com.agendaplay.service;

import br.com.agendaplay.dto.CadastroForm;
import br.com.agendaplay.exception.RegraNegocioException;
import br.com.agendaplay.model.Perfil;
import br.com.agendaplay.repository.UsuarioRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarios;
    private final PasswordEncoder senhas;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder senhas) {
        this.usuarios = usuarios;
        this.senhas = senhas;
    }

    @Transactional
    public long cadastrar(CadastroForm form, Perfil perfil) {
        validarSenha(form.getSenha(), form.getConfirmacaoSenha());
        String cpf = Documento.normalizar(form.getCpf());
        if (!Documento.valido(cpf, perfil == Perfil.PROPRIETARIO))
            throw new RegraNegocioException(
                    perfil == Perfil.CLIENTE
                            ? "Informe um CPF válido."
                            : "Informe um CPF ou CNPJ válido (também aceitamos letras).");
        return usuarios.cadastrar(
                form.getNome().trim(),
                form.getEmail().trim().toLowerCase(Locale.ROOT),
                cpf.isBlank() ? null : cpf,
                form.getTelefone(),
                senhas.encode(form.getSenha()),
                perfil);
    }

    public static void validarSenha(String senha, String confirmacao) {
        if (senha == null
                || senha.length() < 8
                || senha.length() > 60
                || !senha.matches("(?s)(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^a-zA-Z0-9\\s]).+"))
            throw new RegraNegocioException(
                    "Use de 8 a 60 caracteres, com maiúscula, minúscula, número e símbolo.");
        if (!senha.equals(confirmacao)) {
            throw new RegraNegocioException("A confirmação da senha não confere.");
        }
        if (senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new RegraNegocioException("A senha é muito longa. Use menos caracteres.");
        }
    }
}
