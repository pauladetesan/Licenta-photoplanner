package ro.utcn.photoplanner.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.utcn.photoplanner.model.Utilizator;
import ro.utcn.photoplanner.repository.UtilizatorRepository;

import java.util.UUID;

/**
 * Contul-substitut sub care rămân locațiile publice ale celor care și-au șters contul.
 * <p>
 * Ideea e că un drum spre o cascadă nu e un dat cu caracter personal — numele, adresa de email
 * și fotografiile sunt. Așa, cine pleacă dispare cu adevărat, dar locurile pe care le-a găsit
 * rămân de folos celorlalți.
 */
@Service
public class ContAnonimService {

    /** Domeniu care nu există și nu poate primi email — nimeni nu poate revendica adresa asta. */
    static final String EMAIL = "utilizator-sters@photoplanner.invalid";
    static final String NUME = "Utilizator șters";

    private final UtilizatorRepository utilizatorRepository;
    private final PasswordEncoder passwordEncoder;

    public ContAnonimService(UtilizatorRepository utilizatorRepository,
                              PasswordEncoder passwordEncoder) {
        this.utilizatorRepository = utilizatorRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Îl găsește, sau îl creează la prima nevoie. */
    @Transactional
    public Utilizator obtine() {
        return utilizatorRepository.findByContSistemTrue().orElseGet(this::creeaza);
    }

    private Utilizator creeaza() {
        Utilizator anonim = new Utilizator();
        anonim.setEmail(EMAIL);
        anonim.setNumeAfisat(NUME);
        anonim.setContSistem(true);
        /*
         * Parola e un UUID aleatoriu pe care nu-l află nimeni, nici măcar noi — contul nu e
         * făcut ca să fie folosit. Steagul contSistem oprește oricum și autentificarea, și
         * resetarea parolei; parola aleatorie e doar a doua încuietoare.
         */
        anonim.setParola(passwordEncoder.encode(UUID.randomUUID().toString()));
        return utilizatorRepository.save(anonim);
    }
}
