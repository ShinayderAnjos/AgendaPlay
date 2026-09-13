package br.com.agendaplay.model;

public abstract class Usuario {
    private final long id;
    private final String nome;
    private final String email;
    private final String cpf;
    private final String telefone;
    private final String senhaHash;

    protected Usuario(
            long id, String nome, String email, String cpf, String telefone, String senhaHash) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.cpf = cpf;
        this.telefone = telefone;
        this.senhaHash = senhaHash;
    }

    public long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getCpf() {
        return cpf;
    }

    public String getTelefone() {
        return telefone;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public abstract Perfil getPerfil();
}
