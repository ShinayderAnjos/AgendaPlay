package br.com.agendaplay.service;

import br.com.agendaplay.dto.QuadraForm;
import br.com.agendaplay.model.*;
import br.com.agendaplay.repository.QuadraRepository;
import br.com.agendaplay.security.UsuarioAutenticado;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class QuadraService {
    private final QuadraRepository quadras;

    public QuadraService(QuadraRepository quadras) {
        this.quadras = quadras;
    }

    public List<Quadra> ativas() {
        return quadras.listarAtivas();
    }

    public List<Quadra> minhas(long usuario) {
        return quadras.listarDoProprietario(usuario);
    }

    public Quadra buscar(long id) {
        return quadras.buscar(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public Quadra propria(long id, UsuarioAutenticado usuario) {
        var quadra = buscar(id);
        validarDono(quadra, usuario);
        return quadra;
    }

    public void validarDono(Quadra quadra, UsuarioAutenticado usuario) {
        if (usuario.getPerfil() != Perfil.PROPRIETARIO
                || quadra.idProprietario() != usuario.getId()) {
            throw new AccessDeniedException("Esta quadra pertence a outro proprietário.");
        }
    }

    @Transactional
    public long salvar(Long id, QuadraForm form, UsuarioAutenticado usuario) {
        if (usuario.getPerfil() != Perfil.PROPRIETARIO)
            throw new AccessDeniedException("Acesso exclusivo de proprietários.");
        if (id != null) {
            var existente =
                    quadras.bloquear(id)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            validarDono(existente, usuario);
        }
        return quadras.salvar(
                new Quadra(
                        id == null ? 0 : id,
                        usuario.getId(),
                        form.getNome().trim(),
                        form.getModalidade().trim(),
                        form.getLocalizacao().trim(),
                        form.getValorHora(),
                        form.getSituacao()));
    }
}
