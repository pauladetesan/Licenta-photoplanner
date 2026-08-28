package ro.utcn.photoplanner.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.utcn.photoplanner.model.Utilizator;
import ro.utcn.photoplanner.repository.UtilizatorRepository;

@Service
public class UtilizatorService {

    private static final Logger log = LoggerFactory.getLogger(UtilizatorService.class);

    /** Lungimea minimă a parolei. */
    private static final int LUNGIME_MINIMA_PAROLA = 8;

    /** Peste 72 de octeți bcrypt ignoră restul, așa că nu are rost să promitem mai mult. */
    private static final int LUNGIME_MAXIMA_PAROLA = 72;

    private final UtilizatorRepository utilizatorRepository;
    private final PasswordEncoder passwordEncoder;
    private final ResetareParolaService resetareParolaService;

    public UtilizatorService(UtilizatorRepository utilizatorRepository,
                             PasswordEncoder passwordEncoder,
                             ResetareParolaService resetareParolaService) {
        this.utilizatorRepository = utilizatorRepository;
        this.passwordEncoder = passwordEncoder;
        this.resetareParolaService = resetareParolaService;
    }

    /** Normalizarea sub care se compară și se salvează adresele. */
    private static String normalizeaza(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    /**
     * Spune dacă adresa e deja folosită, ca formularul să poată arăta mesajul lângă câmp
     * în loc să aștepte o excepție.
     */
    public boolean emailEsteLuat(String email) {
        return utilizatorRepository.existsByEmail(normalizeaza(email));
    }

    /*
     * Verificările de aici se suprapun cu cele din InregistrareForm, și e intenționat:
     * formularul apără pagina, iar metoda asta apără baza de date de orice alt apelant.
     * Un cont fără nume sau cu o adresă care nu poate primi email nu are cum să fie reparat
     * mai târziu — linkul de resetare a parolei nu are unde să ajungă.
     */
    public Utilizator inregistreaza(String email, String parolaInClar, String numeAfisat) {

        String emailNormalizat = normalizeaza(email);

        if (emailNormalizat.isEmpty()) {
            throw new IllegalArgumentException("Adresa de email este obligatorie.");
        }

        if (utilizatorRepository.existsByEmail(emailNormalizat)) {
            throw new IllegalArgumentException("Există deja un cont cu această adresă de email.");
        }

        if (numeAfisat == null || numeAfisat.isBlank()) {
            throw new IllegalArgumentException("Numele afișat este obligatoriu.");
        }

        if (parolaInClar == null || parolaInClar.length() < LUNGIME_MINIMA_PAROLA) {
            throw new IllegalArgumentException(
                    "Parola trebuie să aibă cel puțin " + LUNGIME_MINIMA_PAROLA + " caractere.");
        }

        Utilizator utilizator = new Utilizator();
        utilizator.setEmail(emailNormalizat);
        utilizator.setParola(passwordEncoder.encode(parolaInClar));
        utilizator.setNumeAfisat(numeAfisat.trim());

        return utilizatorRepository.save(utilizator);
    }

    /**
     * Schimbă parola unui cont deja autentificat.
     * <p>
     * Se cere și parola curentă, nu doar sesiunea deschisă: cineva care găsește laptopul
     * descuiat nu trebuie să poată încuia contul pe din afară schimbând parola. E aceeași
     * regulă ca la ștergerea contului.
     *
     * @throws IllegalArgumentException dacă parola curentă nu se potrivește sau cea nouă nu e bună
     */
    @Transactional
    public void schimbaParola(Utilizator utilizator, String parolaCurenta, String parolaNoua) {

        if (parolaCurenta == null || !passwordEncoder.matches(parolaCurenta, utilizator.getParola())) {
            throw new IllegalArgumentException("Parola curentă nu este corectă.");
        }

        if (parolaNoua == null || parolaNoua.length() < LUNGIME_MINIMA_PAROLA
                || parolaNoua.length() > LUNGIME_MAXIMA_PAROLA) {
            throw new IllegalArgumentException("Parola nouă trebuie să aibă între "
                    + LUNGIME_MINIMA_PAROLA + " și " + LUNGIME_MAXIMA_PAROLA + " caractere.");
        }

        if (parolaNoua.equals(parolaCurenta)) {
            throw new IllegalArgumentException("Parola nouă este aceeași cu cea veche.");
        }

        utilizator.setParola(passwordEncoder.encode(parolaNoua));
        utilizatorRepository.save(utilizator);

        // Un link de resetare cerut înainte nu mai are voie să meargă după schimbare.
        resetareParolaService.anuleazaCererileInAsteptare(utilizator);

        log.info("Parola contului {} a fost schimbată din pagina contului.", utilizator.getId());
    }
}
