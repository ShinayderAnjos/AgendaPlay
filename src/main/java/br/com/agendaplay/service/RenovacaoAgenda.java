package br.com.agendaplay.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "agendaplay.renovacao-automatica",
        havingValue = "true",
        matchIfMissing = true)
@Component
public class RenovacaoAgenda {
    private final PadraoService padroes;
    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(RenovacaoAgenda.class);

    public RenovacaoAgenda(PadraoService p) {
        padroes = p;
    }

    @Scheduled(initialDelay = 60000, fixedDelay = 3600000)
    public void renovar() {
        for (long q : padroes.quadrasAtivas())
            try {
                padroes.renovar(q);
            } catch (RuntimeException e) {
                log.error("Falha ao renovar agenda da quadra {}", q, e);
            }
    }
}
