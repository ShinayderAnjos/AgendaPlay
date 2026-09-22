package br.com.agendaplay.service;

import java.util.Locale;

/** Algoritmos oficiais de CPF e CNPJ numérico/alfanumérico. */
public final class Documento {
    private Documento() {}

    public static String normalizar(String v) {
        return v == null ? "" : v.toUpperCase(Locale.ROOT).replaceAll("[. /\\-]", "");
    }

    public static boolean valido(String valor, boolean aceitaCnpj) {
        String d = normalizar(valor);
        if (d.matches("([0-9])\\1+")) return false;
        if (d.matches("[0-9]{11}")) {
            for (int n = 9; n <= 10; n++) {
                int soma = 0;
                for (int i = 0; i < n; i++) soma += (d.charAt(i) - '0') * (n + 1 - i);
                int resto = soma % 11;
                if (d.charAt(n) - '0' != (resto < 2 ? 0 : 11 - resto)) return false;
            }
            return true;
        }
        if (!aceitaCnpj || !d.matches("[A-Z0-9]{12}[0-9]{2}")) return false;
        for (int n = 12; n <= 13; n++) {
            int soma = 0, peso = 2;
            for (int i = n - 1; i >= 0; i--) {
                soma += (d.charAt(i) - 48) * peso;
                peso = peso == 9 ? 2 : peso + 1;
            }
            int resto = soma % 11;
            if (d.charAt(n) - '0' != (resto < 2 ? 0 : 11 - resto)) return false;
        }
        return true;
    }
}
