package ro.utcn.photoplanner.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ro.utcn.photoplanner.model.Utilizator;
import ro.utcn.photoplanner.repository.UtilizatorRepository;

@Service
public class UtilizatorService {

    /** Lungimea minimă a parolei. */
    private static final int LUNGIME_MINIMA_PAROLA = 8;

    private final UtilizatorRepository utilizatorRepository;
    private final PasswordEncoder passwordEncoder;

    public UtilizatorService(UtilizatorRepository utilizatorRepository,
                             PasswordEncoder passwordEncoder) {
        this.utilizatorRepository = utilizatorRepository;
        this.passwordEncoder = passwordEncoder;
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
}
