package br.com.agendaplay.security;

import br.com.agendaplay.model.Perfil;
import br.com.agendaplay.model.Usuario;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

public class UsuarioAutenticado extends User {
    private final long id;
    private final String nome;
    private final Perfil perfil;

    public UsuarioAutenticado(Usuario usuario) {
        super(
                usuario.getEmail(),
                usuario.getSenhaHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil())));
        this.id = usuario.getId();
        this.nome = usuario.getNome();
        this.perfil = usuario.getPerfil();
    }

    public long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Perfil getPerfil() {
        return perfil;
    }
}
