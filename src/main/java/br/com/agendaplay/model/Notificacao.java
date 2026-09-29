package br.com.agendaplay.model;

public record Notificacao(long id, long idUsuario, long idReserva, String mensagem, boolean lida) {}
