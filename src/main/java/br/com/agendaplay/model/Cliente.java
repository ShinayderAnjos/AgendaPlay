package br.com.agendaplay.model;

public class Cliente extends Usuario {
    public Cliente(
            long id, String nome, String email, String cpf, String telefone, String senhaHash) {
        super(id, nome, email, cpf, telefone, senhaHash);
    }

    @Override
    public Perfil getPerfil() {
        return Perfil.CLIENTE;
    }
}
