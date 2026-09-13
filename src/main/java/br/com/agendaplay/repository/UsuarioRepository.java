package br.com.agendaplay.repository;

import br.com.agendaplay.model.*;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UsuarioRepository {
    private final JdbcClient jdbc;

    public UsuarioRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        return jdbc.sql("SELECT * FROM usuario WHERE email = :email")
                .param("email", email)
                .query(
                        (rs, row) -> {
                            if (rs.getString("perfil").equals("CLIENTE")) {
                                return (Usuario)
                                        new Cliente(
                                                rs.getLong("id"),
                                                rs.getString("nome"),
                                                rs.getString("email"),
                                                rs.getString("cpf"),
                                                rs.getString("telefone"),
                                                rs.getString("senha_hash"));
                            }
                            return new Proprietario(
                                    rs.getLong("id"),
                                    rs.getString("nome"),
                                    rs.getString("email"),
                                    rs.getString("cpf"),
                                    rs.getString("telefone"),
                                    rs.getString("senha_hash"));
                        })
                .optional();
    }

    public long cadastrar(
            String nome,
            String email,
            String cpf,
            String telefone,
            String senhaHash,
            Perfil perfil) {
        return jdbc.sql(
                        """
                        INSERT INTO usuario (nome, email, cpf, telefone, senha_hash, perfil)
                        VALUES (:nome, :email, :cpf, :telefone, :senha, :perfil) RETURNING id
                        """)
                .param("nome", nome)
                .param("email", email)
                .param("cpf", cpf)
                .param("telefone", telefone)
                .param("senha", senhaHash)
                .param("perfil", perfil.name())
                .query(Long.class)
                .single();
    }
}
