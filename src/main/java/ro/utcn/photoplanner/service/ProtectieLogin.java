package ro.utcn.photoplanner.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limitează încercările de autentificare eșuate, ca parolele să nu poată fi ghicite prin
 * încercare după încercare.
 * <p>
 * Se numără pe două chei deodată:
 * <ul>
 *   <li><b>după adresa de email</b> — oprește insistența pe un singur cont;</li>
 *   <li><b>după adresa IP</b> — oprește încercarea câte unei parole comune pe multe conturi,
 *       caz în care numărătoarea pe cont nu s-ar umple niciodată.</li>
 * </ul>
 * Blocarea e <b>temporară</b> și intenționat așa: una permanentă ar deveni ea însăși o armă,
 * fiindcă oricine ți-ar putea bloca contul greșind parola în mod deliberat.
 * <p>
 * Numărătoarea stă în memorie, deci e per instanță. Pentru mai multe instanțe ar trebui mutată
 * într-un loc comun (Redis sau baza de date).
 */
@Service
public class ProtectieLogin {

    private static final Logger log = LoggerFactory.getLogger(ProtectieLogin.class);

    private final int esecuriPeCont;
    private final int esecuriPeIp;
    private final Duration durataBlocare;

    private final Map<String, Numaratoare> dupaCont = new ConcurrentHashMap<>();
    private final Map<String, Numaratoare> dupaIp = new ConcurrentHashMap<>();

    public ProtectieLogin(
            @Value("${photoplanner.login.esecuri-pe-cont:5}") int esecuriPeCont,
            @Value("${photoplanner.login.esecuri-pe-ip:20}") int esecuriPeIp,
            @Value("${photoplanner.login.minute-blocare:15}") int minuteBlocare) {
        this.esecuriPeCont = esecuriPeCont;
        this.esecuriPeIp = esecuriPeIp;
        this.durataBlocare = Duration.ofMinutes(minuteBlocare);
    }

    /** Câte eșecuri s-au adunat și când expiră fereastra de numărare. */
    private static final class Numaratoare {
        private int esecuri;
        private Instant expira;

        Numaratoare(Duration durata) {
            this.esecuri = 0;
            this.expira = Instant.now().plus(durata);
        }

        synchronized void adauga(Duration durata) {
            if (Instant.now().isAfter(expira)) {
                esecuri = 0;                       // fereastra veche s-a încheiat
            }
            esecuri++;
            expira = Instant.now().plus(durata);   // fiecare eșec prelungește blocarea
        }

        synchronized boolean atinge(int prag) {
            return esecuri >= prag && Instant.now().isBefore(expira);
        }

        synchronized boolean esteVeche() {
            return Instant.now().isAfter(expira);
        }
    }

    /**
     * Cheia pentru email se normalizează la fel ca la autentificare, altfel „A@x.ro” și
     * „a@x.ro” ar avea numărători separate și pragul nu s-ar atinge niciodată.
     */
    private static String cheieCont(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    public boolean esteBlocat(String email, String ip) {
        curata();
        Numaratoare peCont = dupaCont.get(cheieCont(email));
        Numaratoare peIp = ip != null ? dupaIp.get(ip) : null;

        return (peCont != null && peCont.atinge(esecuriPeCont))
                || (peIp != null && peIp.atinge(esecuriPeIp));
    }

    public void inregistreazaEsec(String email, String ip) {
        dupaCont.computeIfAbsent(cheieCont(email), k -> new Numaratoare(durataBlocare))
                .adauga(durataBlocare);
        if (ip != null) {
            dupaIp.computeIfAbsent(ip, k -> new Numaratoare(durataBlocare)).adauga(durataBlocare);
        }

        if (esteBlocat(email, ip)) {
            // Fără email în log: n-are rost să scriem în clar ce conturi sunt vizate.
            log.warn("Autentificări eșuate prea multe de la {} — blocat temporar.", ip);
        }
    }

    /** O autentificare reușită curăță istoricul contului, ca greșelile de ieri să nu se adune. */
    public void inregistreazaSucces(String email, String ip) {
        dupaCont.remove(cheieCont(email));
        if (ip != null) {
            dupaIp.remove(ip);
        }
    }

    /** Scoate numărătorile expirate, ca hărțile să nu crească la nesfârșit. */
    private void curata() {
        dupaCont.values().removeIf(Numaratoare::esteVeche);
        dupaIp.values().removeIf(Numaratoare::esteVeche);
    }
}
