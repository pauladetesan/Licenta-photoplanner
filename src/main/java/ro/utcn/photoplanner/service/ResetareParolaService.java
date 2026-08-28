package ro.utcn.photoplanner.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.utcn.photoplanner.model.TokenResetare;
import ro.utcn.photoplanner.model.Utilizator;
import ro.utcn.photoplanner.repository.TokenResetareRepository;
import ro.utcn.photoplanner.repository.UtilizatorRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Resetarea parolei prin link cu token.
 * <p>
 * Regulile de securitate respectate aici:
 * <ul>
 *   <li>tokenul e generat cu {@link SecureRandom}, 32 de octeți — imposibil de ghicit;</li>
 *   <li>în baza de date se ține doar amprenta lui, deci o scurgere a bazei nu dă linkuri valide;</li>
 *   <li>expiră, se folosește o singură dată, iar folosirea lui le anulează pe celelalte;</li>
 *   <li>cererea de resetare răspunde la fel indiferent dacă adresa există sau nu, ca să nu se
 *       poată afla ce conturi există;</li>
 *   <li>numărul de cereri per cont într-un interval e limitat.</li>
 * </ul>
 */
@Service
public class ResetareParolaService {

    private static final Logger log = LoggerFactory.getLogger(ResetareParolaService.class);

    private static final int OCTETI_TOKEN = 32;
    private static final int LUNGIME_MINIMA_PAROLA = 8;

    private final UtilizatorRepository utilizatorRepository;
    private final TokenResetareRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final ExpeditorLinkResetare expeditor;
    private final SecureRandom aleator = new SecureRandom();

    private final int minuteValabilitate;
    private final int cereriMaximePeOra;
    private final String adresaDeBaza;

    public ResetareParolaService(UtilizatorRepository utilizatorRepository,
                                  TokenResetareRepository tokenRepository,
                                  PasswordEncoder passwordEncoder,
                                  ExpeditorLinkResetare expeditor,
                                  @Value("${photoplanner.resetare.minute:60}") int minuteValabilitate,
                                  @Value("${photoplanner.resetare.cereri-pe-ora:5}") int cereriMaximePeOra,
                                  @Value("${photoplanner.resetare.adresa-de-baza:http://localhost:8080}") String adresaDeBaza) {
        this.utilizatorRepository = utilizatorRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.expeditor = expeditor;
        this.minuteValabilitate = minuteValabilitate;
        this.cereriMaximePeOra = cereriMaximePeOra;
        this.adresaDeBaza = adresaDeBaza;
    }

    /**
     * Pornește resetarea pentru adresa dată.
     * <p>
     * Nu întoarce nimic și nu aruncă dacă adresa nu există: altfel formularul ar deveni o cale
     * prin care se poate afla ce conturi sunt înregistrate.
     */
    @Transactional
    public void cereResetare(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        Optional<Utilizator> poate = utilizatorRepository.findByEmail(email.trim().toLowerCase());
        if (poate.isEmpty()) {
            log.info("Cerere de resetare pentru o adresă neînregistrată — nu se trimite nimic.");
            return;
        }

        Utilizator utilizator = poate.get();

        // Nici resetarea nu are ce căuta pe contul-substitut: ar fi o cale de a-l revendica.
        if (utilizator.isContSistem()) {
            return;
        }

        long recente = tokenRepository.countByUtilizatorAndDataCreareAfter(
                utilizator, LocalDateTime.now().minusHours(1));
        if (recente >= cereriMaximePeOra) {
            log.warn("Prea multe cereri de resetare pentru contul {} — cererea a fost ignorată.",
                    utilizator.getId());
            return;
        }

        String token = genereazaToken();

        TokenResetare inregistrare = new TokenResetare();
        inregistrare.setUtilizator(utilizator);
        inregistrare.setAmprenta(amprenta(token));
        inregistrare.setExpira(LocalDateTime.now().plusMinutes(minuteValabilitate));
        tokenRepository.save(inregistrare);

        try {
            expeditor.trimite(utilizator, adresaDeBaza + "/reseteaza-parola?token=" + token);
        } catch (RuntimeException e) {
            /*
             * Dacă trimiterea eșuează, tot nu avem voie să lăsăm cererea să se termine altfel
             * decât pentru o adresă inexistentă: o eroare vizibilă doar la adresele reale ar
             * spune atacatorului exact ce conturi există. Eroarea rămâne în log, pentru noi.
             */
            log.error("Nu am putut trimite linkul de resetare pentru contul {}: {}",
                    utilizator.getId(), e.toString());
        }
    }

    /** Găsește tokenul valabil corespunzător, sau gol dacă nu există, a expirat sau a fost folosit. */
    public Optional<TokenResetare> gasesteValabil(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return tokenRepository.findByAmprenta(amprenta(token))
                .filter(TokenResetare::esteValabil);
    }

    /**
     * Schimbă parola pe baza tokenului.
     *
     * @throws IllegalArgumentException dacă tokenul nu mai e valabil sau parola e prea scurtă
     */
    @Transactional
    public void reseteaza(String token, String parolaNoua) {
        TokenResetare inregistrare = gasesteValabil(token).orElseThrow(
                () -> new IllegalArgumentException(
                        "Linkul de resetare nu mai este valabil. Cere unul nou."));

        if (parolaNoua == null || parolaNoua.length() < LUNGIME_MINIMA_PAROLA) {
            throw new IllegalArgumentException(
                    "Parola trebuie să aibă cel puțin " + LUNGIME_MINIMA_PAROLA + " caractere.");
        }

        Utilizator utilizator = inregistrare.getUtilizator();
        utilizator.setParola(passwordEncoder.encode(parolaNoua));
        utilizatorRepository.save(utilizator);

        anuleazaCererileInAsteptare(utilizator);

        log.info("Parola contului {} a fost schimbată prin resetare.", utilizator.getId());
    }

    /**
     * Face inutilizabile toate linkurile de resetare încă nefolosite ale unui cont.
     * <p>
     * Se cheamă ori de câte ori parola se schimbă, indiferent pe ce cale: un link cerut înainte
     * de schimbare nu mai are voie să funcționeze după ea. Altfel cineva care a apucat să ceară
     * o resetare ar putea intra peste noua parolă.
     */
    @Transactional
    public void anuleazaCererileInAsteptare(Utilizator utilizator) {
        LocalDateTime acum = LocalDateTime.now();
        List<TokenResetare> nefolosite =
                tokenRepository.findByUtilizatorAndDataFolosiriiIsNull(utilizator);
        nefolosite.forEach(t -> t.setDataFolosirii(acum));
        tokenRepository.saveAll(nefolosite);
    }

    private String genereazaToken() {
        byte[] octeti = new byte[OCTETI_TOKEN];
        aleator.nextBytes(octeti);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(octeti);
    }

    /** SHA-256 e potrivit aici: tokenul are deja 256 de biți de entropie, nu e o parolă de ghicit. */
    static String amprenta(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 ar trebui să existe în orice JVM", e);
        }
    }
}
