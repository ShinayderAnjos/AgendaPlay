package br.com.agendaplay.model;

public class Proprietario extends Usuario {
    public Proprietario(
            long id, String nome, String email, String cpf, String telefone, String senhaHash) {
        super(id, nome, email, cpf, telefone, senhaHash);
    }

    @Override
    public Perfil getPerfil() {
        return Perfil.PROPRIETARIO;
    }
}
