package ro.utcn.photoplanner.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Citirea și scrierea fișierelor cu fotografii pe disc. */
@Service
public class StocareFotografii {

    private static final Logger log = LoggerFactory.getLogger(StocareFotografii.class);

    private final Path director;

    public StocareFotografii(@Value("${photoplanner.poze.director:./data/poze}") String cale) {
        this.director = Paths.get(cale).toAbsolutePath().normalize();
        try {
            Files.createDirectories(director);
        } catch (IOException e) {
            throw new UncheckedIOException("Nu pot crea directorul pentru poze: " + director, e);
        }
        log.info("Fotografiile se salvează în {}", director);
    }

    /**
     * Rezolvă numele unui fișier în directorul de poze, refuzând orice nume care ar scăpa
     * din el (de exemplu „../../ceva”).
     */
    private Path rezolva(String numeFisier) {
        Path cale = director.resolve(numeFisier).normalize();
        if (!cale.startsWith(director)) {
            throw new IllegalArgumentException("Nume de fișier nepermis: " + numeFisier);
        }
        return cale;
    }

    public void scrie(String numeFisier, byte[] continut) {
        try {
            Files.write(rezolva(numeFisier), continut);
        } catch (IOException e) {
            throw new UncheckedIOException("Nu pot scrie fișierul " + numeFisier, e);
        }
    }

    public byte[] citeste(String numeFisier) {
        try {
            return Files.readAllBytes(rezolva(numeFisier));
        } catch (IOException e) {
            throw new UncheckedIOException("Nu pot citi fișierul " + numeFisier, e);
        }
    }

    public boolean exista(String numeFisier) {
        return Files.exists(rezolva(numeFisier));
    }

    /** Șterge fișierul, fără să arunce dacă lipsește — ștergerea trebuie să fie idempotentă. */
    public void sterge(String numeFisier) {
        try {
            Files.deleteIfExists(rezolva(numeFisier));
        } catch (IOException e) {
            log.warn("Nu am putut șterge fișierul {}: {}", numeFisier, e.toString());
        }
    }
}
