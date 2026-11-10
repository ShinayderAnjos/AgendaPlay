package br.com.agendaplay;

import static org.assertj.core.api.Assertions.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;

class FinanceiroMigrationTest {
    @Test void preservaRecebimentosLegadosAoMigrarDaVersaoSeis() throws Exception {
        String url="jdbc:postgresql://127.0.0.1:55432/postgres";
        String schema="agendaplay_migracao_sprint2";
        try(var c=DriverManager.getConnection(url,"agendaplay_test","");var s=c.createStatement()) {
            try(var rs=s.executeQuery("SELECT inet_server_port()")) {
                rs.next(); assertThat(rs.getInt(1)).isEqualTo(55432);
            }
            s.execute("DROP SCHEMA IF EXISTS agendaplay_migracao_sprint2 CASCADE");
            try {
                Flyway.configure().dataSource(url,"agendaplay_test","").schemas(schema).defaultSchema(schema)
                    .target("6").load().migrate();
                s.execute("SET search_path TO agendaplay_migracao_sprint2, public");
                s.execute("""
                    INSERT INTO usuario(nome,email,cpf,senha_hash,perfil) VALUES
                        ('Dono','dono@migration.test','12345678909','hash','PROPRIETARIO'),
                        ('Cliente','cliente@migration.test','52998224725','hash','CLIENTE');
                    INSERT INTO estabelecimento(id_proprietario,nome,localizacao) VALUES(1,'Centro','Rua');
                    INSERT INTO quadra(id_proprietario,id_estabelecimento,nome,modalidade,localizacao,valor_hora,situacao)
                        VALUES(1,1,'Arena','Futsal','Rua',90,'ATIVA');
                    INSERT INTO reserva(id_cliente,id_quadra,data,hora_inicio,hora_fim,valor_total)
                        VALUES(2,1,'2026-10-20','10:00','11:00',90);
                    INSERT INTO recebimento(id_reserva,forma,recebido_em,estornado_em)
                        VALUES(1,'PIX','2026-10-20 10:00:00-03','2026-10-20 11:00:00-03');
                    """);
                Flyway.configure().dataSource(url,"agendaplay_test","").schemas(schema).defaultSchema(schema)
                    .load().migrate();
                try(var rs=s.executeQuery("SELECT valor,forma,recebido_em,estornado_em,id_caixa FROM recebimento")) {
                    assertThat(rs.next()).isTrue(); assertThat(rs.getBigDecimal("valor")).isEqualByComparingTo("90");
                    assertThat(rs.getString("forma")).isEqualTo("PIX");
                    assertThat(rs.getTimestamp("recebido_em").toInstant()).isEqualTo(java.time.Instant.parse("2026-10-20T13:00:00Z"));
                    assertThat(rs.getTimestamp("estornado_em").toInstant()).isEqualTo(java.time.Instant.parse("2026-10-20T14:00:00Z"));
                    assertThat(rs.getObject("id_caixa")).isNull(); assertThat(rs.next()).isFalse();
                }
            } finally { s.execute("DROP SCHEMA IF EXISTS agendaplay_migracao_sprint2 CASCADE"); }
        }
    }
}
