package ro.utcn.photoplanner.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ro.utcn.photoplanner.model.Utilizator;

/**
 * Scrie linkul de resetare în log, în loc să-l trimită pe email.
 * <p>
 * Bun pentru dezvoltare, fiindcă merge fără niciun fel de configurare. <b>Nu e potrivit pentru
 * un server real</b>: oricine poate citi logurile ar putea folosi linkul ca să intre pe cont.
 * Pentru producție se pune {@code photoplanner.resetare.expeditor=email} și trimiterea trece
 * la {@link ExpeditorEmail}.
 */
@Service
@ConditionalOnProperty(name = "photoplanner.resetare.expeditor", havingValue = "log",
                       matchIfMissing = true)
public class ExpeditorInLog implements ExpeditorLinkResetare {

    private static final Logger log = LoggerFactory.getLogger(ExpeditorInLog.class);

    @Override
    public void trimite(Utilizator utilizator, String link) {
        log.warn("""

                ============================================================
                 LINK DE RESETARE A PAROLEI (mod dezvoltare — nu se trimite email)
                 cont: {}
                 link: {}
                ============================================================
                """, utilizator.getEmail(), link);
    }
}
