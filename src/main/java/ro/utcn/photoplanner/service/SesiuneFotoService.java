package ro.utcn.photoplanner.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ro.utcn.photoplanner.model.Locatie;
import ro.utcn.photoplanner.model.MomentZi;
import ro.utcn.photoplanner.model.SesiuneFoto;
import ro.utcn.photoplanner.model.Utilizator;
import ro.utcn.photoplanner.repository.SesiuneFotoRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Sesiunile foto planificate.
 * <p>
 * Două reguli stau la baza tuturor metodelor: se poate planifica doar la o locație pe care o
 * vezi (publică sau privată, dar a ta), și îți vezi doar propriile sesiuni.
 */
@Service
public class SesiuneFotoService {

    private final SesiuneFotoRepository sesiuneRepository;
    private final LocatieService locatieService;

    public SesiuneFotoService(SesiuneFotoRepository sesiuneRepository, LocatieService locatieService) {
        this.sesiuneRepository = sesiuneRepository;
        this.locatieService = locatieService;
    }

    public List<SesiuneFoto> viitoare(Utilizator utilizator) {
        return sesiuneRepository.findByUtilizatorAndDataGreaterThanEqualOrderByDataAsc(
                utilizator, LocalDate.now());
    }

    public List<SesiuneFoto> trecute(Utilizator utilizator) {
        return sesiuneRepository.findByUtilizatorAndDataLessThanOrderByDataDesc(
                utilizator, LocalDate.now());
    }

    public long numar(Utilizator utilizator) {
        return sesiuneRepository.countByUtilizator(utilizator);
    }

    /** Găsește o sesiune și verifică faptul că îi aparține utilizatorului dat. */
    public SesiuneFoto gasesteProprie(Long id, Utilizator utilizator) {
        SesiuneFoto sesiune = sesiuneRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sesiunea nu există."));

        // 404, nu 403: nu confirmăm nici măcar existența sesiunilor altcuiva.
        if (!sesiune.getUtilizator().getId().equals(utilizator.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Sesiunea nu există.");
        }

        return sesiune;
    }

    /**
     * @param inPortofoliu dacă locul e al altcuiva, salvează întâi o copie în portofoliul
     *                     utilizatorului și leagă sesiunea de ea. Așa sesiunea nu mai dispare
     *                     dacă autorul original își șterge locația.
     */
    @Transactional
    public SesiuneFoto planifica(Long idLocatie, Utilizator utilizator, LocalDate data,
                                  MomentZi moment, String notite, boolean inPortofoliu) {
        // Trecerea prin gasesteVizibila oprește planificarea la locațiile private ale altcuiva.
        Locatie locatie = locatieService.gasesteVizibila(idLocatie, utilizator);

        if (inPortofoliu) {
            locatie = locatieService.copiazaInPortofoliu(locatie, utilizator);
        }

        SesiuneFoto sesiune = new SesiuneFoto();
        sesiune.setUtilizator(utilizator);
        sesiune.setLocatie(locatie);
        sesiune.setData(data);
        sesiune.setMoment(moment);
        sesiune.setNotite(notite);

        return sesiuneRepository.save(sesiune);
    }

    @Transactional
    public SesiuneFoto actualizeaza(Long id, Utilizator utilizator, LocalDate data,
                                     MomentZi moment, String notite) {
        SesiuneFoto sesiune = gasesteProprie(id, utilizator);
        sesiune.setData(data);
        sesiune.setMoment(moment);
        sesiune.setNotite(notite);
        return sesiuneRepository.save(sesiune);
    }

    @Transactional
    public void sterge(Long id, Utilizator utilizator) {
        sesiuneRepository.delete(gasesteProprie(id, utilizator));
    }
}
