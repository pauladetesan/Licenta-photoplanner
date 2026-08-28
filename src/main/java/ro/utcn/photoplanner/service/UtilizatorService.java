package ro.utcn.photoplanner.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ro.utcn.photoplanner.model.Utilizator;
import ro.utcn.photoplanner.repository.UtilizatorRepository;

@Service
public class UtilizatorService {

    private final UtilizatorRepository utilizatorRepository;
    private final PasswordEncoder passwordEncoder;

    public UtilizatorService(UtilizatorRepository utilizatorRepository,
                             PasswordEncoder passwordEncoder) {
        this.utilizatorRepository = utilizatorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Utilizator inregistreaza(String email, String parolaInClar, String numeAfisat) {

        String emailNormalizat = email.trim().toLowerCase();

        if (utilizatorRepository.existsByEmail(emailNormalizat)) {
            throw new IllegalArgumentException("Există deja un cont cu această adresă de email.");
        }

        if (parolaInClar == null || parolaInClar.length() < 8) {
            throw new IllegalArgumentException("Parola trebuie să aibă cel puțin 8 caractere.");
        }

        Utilizator utilizator = new Utilizator();
        utilizator.setEmail(emailNormalizat);
        utilizator.setParola(passwordEncoder.encode(parolaInClar));
        utilizator.setNumeAfisat(numeAfisat.trim());

        return utilizatorRepository.save(utilizator);
    }
}