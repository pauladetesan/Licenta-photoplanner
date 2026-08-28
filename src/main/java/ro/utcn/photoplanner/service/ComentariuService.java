package ro.utcn.photoplanner.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ro.utcn.photoplanner.model.Comentariu;
import ro.utcn.photoplanner.model.Locatie;
import ro.utcn.photoplanner.model.Utilizator;
import ro.utcn.photoplanner.repository.ComentariuRepository;

import java.util.List;

@Service
public class ComentariuService {

    private final ComentariuRepository comentariuRepository;

    public ComentariuService(ComentariuRepository comentariuRepository) {
        this.comentariuRepository = comentariuRepository;
    }

    public Comentariu adauga(Locatie locatie, Utilizator autor, String continut) {
        if (continut == null || continut.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comentariul nu poate fi gol.");
        }

        Comentariu comentariu = new Comentariu();
        comentariu.setLocatie(locatie);
        comentariu.setAutor(autor);
        comentariu.setContinut(continut.trim());
        return comentariuRepository.save(comentariu);
    }

    public List<Comentariu> listaPentru(Locatie locatie) {
        return comentariuRepository.findByLocatieOrderByDataAdaugareAsc(locatie);
    }

    /**
     * Un comentariu poate fi șters de două persoane, din motive diferite:
     * <ul>
     *   <li><b>autorul lui</b> — să-și poată retrage ce a scris;</li>
     *   <li><b>autorul locației</b> — să poată curăța pagina lui de spam sau jigniri.</li>
     * </ul>
     */
    public boolean poateSterge(Comentariu comentariu, Utilizator utilizator) {
        if (utilizator == null) {
            return false;
        }
        return utilizator.getId().equals(comentariu.getAutor().getId())
                || utilizator.getId().equals(comentariu.getLocatie().getAutor().getId());
    }

    /**
     * Șterge un comentariu și întoarce locația pe care se afla, ca apelantul să știe unde
     * să se întoarcă.
     */
    @Transactional
    public Locatie sterge(Long id, Utilizator utilizator) {
        Comentariu comentariu = comentariuRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Comentariul nu există."));

        if (!poateSterge(comentariu, utilizator)) {
            // 403, nu 404: comentariul e oricum vizibil pe pagină, nu ascundem că există.
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Poți șterge doar comentariile tale sau pe cele de la locațiile tale.");
        }

        Locatie locatie = comentariu.getLocatie();
        comentariuRepository.delete(comentariu);
        return locatie;
    }
}
