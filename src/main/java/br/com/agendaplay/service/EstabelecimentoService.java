package br.com.agendaplay.service;

import br.com.agendaplay.dto.EstabelecimentoForm;
import br.com.agendaplay.model.*;
import br.com.agendaplay.security.UsuarioAutenticado;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EstabelecimentoService {
    private final JdbcClient jdbc;

    public EstabelecimentoService(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Estabelecimento> listar(Long dono) {
        return dono == null
                ? jdbc.sql(
                                "SELECT e.* FROM estabelecimento e WHERE EXISTS(SELECT 1 FROM"
                                    + " quadra q WHERE q.id_estabelecimento=e.id AND"
                                    + " q.situacao='ATIVA') ORDER BY e.nome,e.id")
                        .query(Estabelecimento.class)
                        .list()
                : jdbc.sql("SELECT * FROM estabelecimento WHERE id_proprietario=? ORDER BY nome,id")
                        .param(dono)
                        .query(Estabelecimento.class)
                        .list();
    }

    public Estabelecimento propria(long id, UsuarioAutenticado u) {
        return listar(u.getId()).stream()
                .filter(e -> e.id() == id && u.getPerfil() == Perfil.PROPRIETARIO)
                .findFirst()
                .orElseThrow(
                        () ->
                                new AccessDeniedException(
                                        "Este estabelecimento não pertence à sua conta."));
    }

    public long salvar(Long id, EstabelecimentoForm f, UsuarioAutenticado u) {
        if (u.getPerfil() != Perfil.PROPRIETARIO)
            throw new AccessDeniedException("Acesso exclusivo de proprietários.");
        if (id != null) {
            propria(id, u);
            jdbc.sql(
                            "UPDATE estabelecimento SET"
                                + " nome=:n,localizacao=:l,latitude=:a,longitude=:o WHERE id=:id"
                                + " AND id_proprietario=:u")
                    .param("n", f.getNome().trim())
                    .param("l", f.getLocalizacao().trim())
                    .param("a", f.getLatitude())
                    .param("o", f.getLongitude())
                    .param("id", id)
                    .param("u", u.getId())
                    .update();
            return id;
        }
        return jdbc.sql(
                        "INSERT INTO"
                            + " estabelecimento(id_proprietario,nome,localizacao,latitude,longitude)"
                            + " VALUES(:u,:n,:l,:a,:o) RETURNING id")
                .param("u", u.getId())
                .param("n", f.getNome().trim())
                .param("l", f.getLocalizacao().trim())
                .param("a", f.getLatitude())
                .param("o", f.getLongitude())
                .query(Long.class)
                .single();
    }
}
