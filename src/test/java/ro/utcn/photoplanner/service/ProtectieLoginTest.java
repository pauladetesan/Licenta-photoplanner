package ro.utcn.photoplanner.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Numărătoarea de autentificări eșuate și blocarea temporară. */
class ProtectieLoginTest {

    private static final String CONT = "cineva@example.com";
    private static final String IP = "192.0.2.10";

    private ProtectieLogin protectie;

    @BeforeEach
    void pregateste() {
        // 3 eșecuri pe cont, 5 pe IP, blocare 15 minute
        protectie = new ProtectieLogin(3, 5, 15);
    }

    @Test
    @DisplayName("la început nimic nu e blocat")
    void inceputCurat() {
        assertThat(protectie.esteBlocat(CONT, IP)).isFalse();
    }

    @Test
    @DisplayName("sub prag încă se poate încerca")
    void subPragNuBlocheaza() {
        protectie.inregistreazaEsec(CONT, IP);
        protectie.inregistreazaEsec(CONT, IP);

        assertThat(protectie.esteBlocat(CONT, IP)).isFalse();
    }

    @Test
    @DisplayName("la atingerea pragului contul e blocat")
    void pragulBlocheaza() {
        for (int i = 0; i < 3; i++) {
            protectie.inregistreazaEsec(CONT, IP);
        }
        assertThat(protectie.esteBlocat(CONT, IP)).isTrue();
    }

    /** Altfel greșelile dintr-o zi s-ar aduna peste cele de mâine și ar bloca pe nedrept. */
    @Test
    @DisplayName("o autentificare reușită șterge numărătoarea")
    void succesulResetează() {
        protectie.inregistreazaEsec(CONT, IP);
        protectie.inregistreazaEsec(CONT, IP);
        protectie.inregistreazaSucces(CONT, IP);
        protectie.inregistreazaEsec(CONT, IP);

        assertThat(protectie.esteBlocat(CONT, IP)).isFalse();
    }

    @Test
    @DisplayName("blocarea unui cont nu blochează alt cont de pe același IP")
    void blocareaEstePeCont() {
        for (int i = 0; i < 3; i++) {
            protectie.inregistreazaEsec(CONT, IP);
        }

        assertThat(protectie.esteBlocat(CONT, IP)).isTrue();
        assertThat(protectie.esteBlocat("altcineva@example.com", "198.51.100.7")).isFalse();
    }

    /**
     * Încercarea aceleiași parole pe multe conturi nu umple niciodată numărătoarea unui singur
     * cont — de asta există și pragul pe IP.
     */
    @Test
    @DisplayName("multe conturi încercate de pe același IP declanșează pragul pe IP")
    void pragulPeIpPrindeIncercareaPeMulteConturi() {
        for (int i = 0; i < 5; i++) {
            protectie.inregistreazaEsec("victima" + i + "@example.com", IP);
        }

        // Niciun cont nu a ajuns la 3 eșecuri, dar IP-ul a ajuns la 5.
        assertThat(protectie.esteBlocat("victima0@example.com", IP)).isTrue();
        assertThat(protectie.esteBlocat("cineva-nou@example.com", IP)).isTrue();
        // De pe alt IP, același cont e în regulă.
        assertThat(protectie.esteBlocat("cineva-nou@example.com", "203.0.113.5")).isFalse();
    }

    /**
     * Adresele se normalizează la fel ca la autentificare; altfel variantele cu majuscule ar
     * avea fiecare numărătoarea ei și pragul nu s-ar atinge niciodată.
     */
    @Test
    @DisplayName("majusculele și spațiile din email nu ocolesc blocarea")
    void emailulEsteNormalizat() {
        protectie.inregistreazaEsec("Cineva@Example.COM", IP);
        protectie.inregistreazaEsec("  cineva@example.com  ", IP);
        protectie.inregistreazaEsec("CINEVA@EXAMPLE.COM", IP);

        assertThat(protectie.esteBlocat(CONT, IP)).isTrue();
    }

    /**
     * Numărăm și încercările pe adrese neînregistrate: dacă doar conturile reale s-ar bloca,
     * chiar faptul blocării ar spune care adrese există.
     */
    @Test
    @DisplayName("și adresele neînregistrate se numără, ca blocarea să nu divulge ce conturi există")
    void siAdreseleNecunoscuteSeNumara() {
        for (int i = 0; i < 3; i++) {
            protectie.inregistreazaEsec("nu-exista@example.com", IP);
        }
        assertThat(protectie.esteBlocat("nu-exista@example.com", IP)).isTrue();
    }

    @Test
    @DisplayName("blocarea expiră după fereastra configurată")
    void blocareaExpira() {
        ProtectieLogin scurta = new ProtectieLogin(2, 50, 0);  // fereastră de 0 minute
        scurta.inregistreazaEsec(CONT, IP);
        scurta.inregistreazaEsec(CONT, IP);

        assertThat(scurta.esteBlocat(CONT, IP)).isFalse();
    }

    @Test
    @DisplayName("un email lipsă nu aruncă")
    void emailLipsaNuArunca() {
        protectie.inregistreazaEsec(null, IP);
        assertThat(protectie.esteBlocat(null, IP)).isFalse();
    }

    @Test
    @DisplayName("un IP lipsă nu aruncă")
    void ipLipsaNuArunca() {
        protectie.inregistreazaEsec(CONT, null);
        assertThat(protectie.esteBlocat(CONT, null)).isFalse();
    }
}
