package ro.utcn.photoplanner.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import ro.utcn.photoplanner.model.Fotografie;
import ro.utcn.photoplanner.model.Locatie;
import ro.utcn.photoplanner.model.Utilizator;
import ro.utcn.photoplanner.repository.FotografieRepository;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class FotografieService {

    /** Câte fotografii poate avea o locație. */
    private static final int MAXIM_PE_LOCATIE = 12;

    private final FotografieRepository fotografieRepository;
    private final StocareFotografii stocare;
    private final ImagineService imagineService;

    public FotografieService(FotografieRepository fotografieRepository,
                              StocareFotografii stocare, ImagineService imagineService) {
        this.fotografieRepository = fotografieRepository;
        this.stocare = stocare;
        this.imagineService = imagineService;
    }

    public List<Fotografie> listaPentru(Locatie locatie) {
        return fotografieRepository.findByLocatieOrderByDataAdaugareAsc(locatie);
    }

    public Optional<Fotografie> coperta(Locatie locatie) {
        return fotografieRepository.findFirstByLocatieOrderByDataAdaugareAsc(locatie);
    }

    /**
     * Coperta fiecărei locații din listă, adusă într-o singură interogare.
     *
     * @return id-ul locației → id-ul primei ei fotografii; locațiile fără poze lipsesc din hartă
     */
    public Map<Long, Long> coperti(Collection<Locatie> locatii) {
        if (locatii.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> rezultat = new HashMap<>();
        // Rezultatele vin ordonate crescător după dată, deci prima văzută pentru o locație e coperta.
        for (Fotografie fotografie : fotografieRepository.findByLocatieInOrderByDataAdaugareAsc(locatii)) {
            rezultat.putIfAbsent(fotografie.getLocatie().getId(), fotografie.getId());
        }
        return rezultat;
    }

    /**
     * Salvează o fotografie pentru locația dată.
     * <p>
     * Conținutul e decodat efectiv ca imagine înainte de salvare, ca să nu putem fi păcăliți
     * de un fișier oarecare redenumit în „.jpg”.
     */
    @Transactional
    public Fotografie adauga(Locatie locatie, MultipartFile fisier) {
        if (fisier == null || fisier.isEmpty()) {
            throw new IllegalArgumentException("Nu ai ales nicio fotografie.");
        }

        if (fotografieRepository.countByLocatie(locatie) >= MAXIM_PE_LOCATIE) {
            throw new IllegalArgumentException(
                    "O locație poate avea cel mult " + MAXIM_PE_LOCATIE + " fotografii.");
        }

        byte[] continut;
        try {
            continut = fisier.getBytes();
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Fișierul nu a putut fi citit.");
        }

        BufferedImage imagine = imagineService.decodeaza(continut);
        if (imagine == null) {
            throw new IllegalArgumentException("Fișierul nu este o imagine pe care o putem citi.");
        }

        String tipMime = fisier.getContentType() != null && fisier.getContentType().startsWith("image/")
                ? fisier.getContentType()
                : "application/octet-stream";

        String bazaNume = UUID.randomUUID().toString();
        String numeFisier = bazaNume + extensie(tipMime);
        String numeMiniatura = bazaNume + "_mic.jpg";

        stocare.scrie(numeFisier, continut);
        stocare.scrie(numeMiniatura, imagineService.miniatura(imagine));

        Fotografie fotografie = new Fotografie();
        fotografie.setLocatie(locatie);
        fotografie.setNumeFisier(numeFisier);
        fotografie.setNumeMiniatura(numeMiniatura);
        fotografie.setNumeOriginal(fisier.getOriginalFilename());
        fotografie.setTipMime(tipMime);
        fotografie.setDimensiune(continut.length);

        return fotografieRepository.save(fotografie);
    }

    private static String extensie(String tipMime) {
        return switch (tipMime) {
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    public Fotografie gaseste(Long id) {
        return fotografieRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fotografia nu există."));
    }

    public byte[] continut(Fotografie fotografie) {
        return stocare.citeste(fotografie.getNumeFisier());
    }

    public byte[] miniatura(Fotografie fotografie) {
        // Miniaturile mai vechi ar putea lipsi de pe disc; atunci servim originalul.
        if (stocare.exista(fotografie.getNumeMiniatura())) {
            return stocare.citeste(fotografie.getNumeMiniatura());
        }
        return continut(fotografie);
    }

    /** Șterge o fotografie, dacă locația îi aparține utilizatorului dat. */
    @Transactional
    public Locatie sterge(Long idFotografie, Utilizator utilizator) {
        Fotografie fotografie = gaseste(idFotografie);
        Locatie locatie = fotografie.getLocatie();

        if (!locatie.getAutor().getId().equals(utilizator.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Nu poți șterge fotografii de la o locație care nu îți aparține.");
        }

        fotografieRepository.delete(fotografie);
        stergeFisiere(fotografie);
        return locatie;
    }

    /** Șterge toate fotografiile unei locații — folosit când se șterge locația însăși. */
    @Transactional
    public void stergeToate(Locatie locatie) {
        List<Fotografie> fotografii = listaPentru(locatie);
        fotografieRepository.deleteAll(fotografii);
        fotografii.forEach(this::stergeFisiere);
    }

    private void stergeFisiere(Fotografie fotografie) {
        stocare.sterge(fotografie.getNumeFisier());
        stocare.sterge(fotografie.getNumeMiniatura());
    }
}
