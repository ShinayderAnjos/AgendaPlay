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
        if (!form.getSenha().equals(form.getConfirmacaoSenha())) {
            throw new RegraNegocioException("A confirmação da senha não confere.");
        }
        if (form.getSenha().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new RegraNegocioException("A senha é muito longa. Use menos caracteres.");
        }
        String cpf = form.getCpf() == null ? "" : form.getCpf().replaceAll("[^0-9]", "");
        if (perfil == Perfil.CLIENTE && cpf.isBlank())
            throw new RegraNegocioException("Informe seu CPF.");
        return usuarios.cadastrar(
                form.getNome().trim(),
                form.getEmail().trim().toLowerCase(Locale.ROOT),
                cpf.isBlank() ? null : cpf,
                form.getTelefone(),
                senhas.encode(form.getSenha()),
                perfil);
    }
}
