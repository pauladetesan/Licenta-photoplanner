package ro.utcn.photoplanner.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import ro.utcn.photoplanner.model.Locatie;
import ro.utcn.photoplanner.model.Tema;
import ro.utcn.photoplanner.model.Utilizator;
import ro.utcn.photoplanner.model.Vizibilitate;
import ro.utcn.photoplanner.repository.LocatieRepository;
import ro.utcn.photoplanner.repository.SesiuneFotoRepository;

import java.util.List;
import java.util.Set;

@Service
public class LocatieService {

    private final LocatieRepository locatieRepository;
    private final FusOrarService fusOrarService;
    private final FotografieService fotografieService;
    // Repository, nu serviciul: SesiuneFotoService depinde de noi, iar invers ar
    // închide un ciclu de dependențe.
    private final SesiuneFotoRepository sesiuneRepository;

    public LocatieService(LocatieRepository locatieRepository, FusOrarService fusOrarService,
                           FotografieService fotografieService,
                           SesiuneFotoRepository sesiuneRepository) {
        this.locatieRepository = locatieRepository;
        this.fusOrarService = fusOrarService;
        this.fotografieService = fotografieService;
        this.sesiuneRepository = sesiuneRepository;
    }

    public Locatie adauga(Locatie locatie, Utilizator autor) {
        locatie.setAutor(autor);
        locatie.setFusOrar(fusOrarService.pentruCoordonate(
                locatie.getLatitudine(), locatie.getLongitudine()));
        return locatieRepository.save(locatie);
    }

    /** Câte locații publice se afișează pe o pagină. */
    public static final int MARIME_PAGINA = 12;

    /** Câte locații trimitem cel mult către harta de pe prima pagină. */
    public static final int MAXIM_PE_HARTA = 500;

    public Page<Locatie> listaPublice(int pagina) {
        return cauta(CriteriiCautare.goale(), pagina);
    }

    /**
     * Toate locațiile publice, pentru harta de ansamblu. Are o limită, ca pagina să nu devină
     * imposibil de încărcat dacă baza crește mult.
     */
    public List<Locatie> publicePentruHarta() {
        return locatieRepository.findByVizibilitateOrderByDataAdaugareDesc(
                Vizibilitate.PUBLICA, PageRequest.of(0, MAXIM_PE_HARTA)).getContent();
    }

    /**
     * Copiază în portofoliul cuiva o locație găsită de altcineva.
     * <p>
     * Copia e a lui: rămâne chiar dacă originalul e șters, și de asta o sesiune planificată pe
     * copie nu mai depinde de deciziile altui utilizator. Trei lucruri sunt intenționate:
     * <ul>
     *   <li>copia e <b>privată</b> — a republica automat descoperirea altcuiva ca fiind a ta ar
     *       fi, în cel mai bun caz, nepoliticos;</li>
     *   <li>păstrează numele celui care a găsit locul, ca meritul să rămână unde trebuie;</li>
     *   <li><b>nu</b> copiază fotografiile — sunt munca lui, nu a ta.</li>
     * </ul>
     * Dacă utilizatorul are deja o copie a aceleiași locații, o primește pe aceea.
     */
    public Locatie copiazaInPortofoliu(Locatie original, Utilizator nouAutor) {
        if (original.getAutor().getId().equals(nouAutor.getId())) {
            return original; // e deja a lui, nu are ce copia
        }

        // Dacă originalul e el însuși o copie, atribuim tot autorului dintâi.
        Long idOriginal = original.getPreluataDin() != null
                ? original.getPreluataDin() : original.getId();
        String gasitaDe = original.getPreluataDeLa() != null
                ? original.getPreluataDeLa() : original.getAutor().getNumeAfisat();

        return locatieRepository.findByAutorAndPreluataDin(nouAutor, idOriginal)
                .orElseGet(() -> {
                    Locatie copie = new Locatie();
                    copie.setAutor(nouAutor);
                    copie.setNume(original.getNume());
                    copie.setDescriere(original.getDescriere());
                    copie.setLatitudine(original.getLatitudine());
                    copie.setLongitudine(original.getLongitudine());
                    copie.setOrientareScena(original.getOrientareScena());
                    copie.setTeme(new java.util.HashSet<>(original.getTeme()));
                    copie.setFusOrar(original.getFusOrar());
                    copie.setVizibilitate(Vizibilitate.PRIVATA);
                    copie.setPreluataDeLa(gasitaDe);
                    copie.setPreluataDin(idOriginal);
                    return locatieRepository.save(copie);
                });
    }

    /** Locațiile private ale utilizatorului dat — niciodată ale altcuiva. */
    public List<Locatie> privatePentruHarta(Utilizator autor) {
        return locatieRepository.findByAutorAndVizibilitate(autor, Vizibilitate.PRIVATA);
    }

    public Page<Locatie> cauta(CriteriiCautare criterii, int pagina) {
        return locatieRepository.cauta(
                criterii.tiparText(),
                criterii.areTeme(), criterii.temeSauPlaceholder(),
                criterii.areDistanta(), criterii.latSauZero(), criterii.lonSauZero(),
                criterii.razaSauZero(),
                PageRequest.of(Math.max(0, pagina), MARIME_PAGINA));
    }

    /** Distanța pe suprafața Pământului între două puncte, în kilometri (haversine). */
    public static double distantaKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(dLon / 2), 2);
        return 6371.0 * 2 * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }

    public List<Locatie> listaProprii(Utilizator autor) {
        return locatieRepository.findByAutorOrderByDataAdaugareDesc(autor);
    }

    /** Găsește o locație și verifică faptul că îi aparține autorului dat. */
    public Locatie gasesteProprie(Long id, Utilizator autor) {
        Locatie locatie = locatieRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Locația nu există."));

        if (!locatie.getAutor().getId().equals(autor.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Nu poți modifica o locație care nu îți aparține.");
        }

        return locatie;
    }

    public Locatie actualizeaza(Long id, Utilizator autor, String nume, String descriere, Double latitudine,
                                 Double longitudine, Integer orientareScena, Vizibilitate vizibilitate,
                                 Set<Tema> teme) {
        Locatie locatie = gasesteProprie(id, autor);

        // Fusul orar se re-deduce doar dacă s-au mutat coordonatele — căutarea nu e gratuită.
        boolean coordonateSchimbate = !latitudine.equals(locatie.getLatitudine())
                || !longitudine.equals(locatie.getLongitudine());
        if (coordonateSchimbate || locatie.getFusOrar() == null) {
            locatie.setFusOrar(fusOrarService.pentruCoordonate(latitudine, longitudine));
        }

        locatie.setNume(nume);
        locatie.setDescriere(descriere);
        locatie.setLatitudine(latitudine);
        locatie.setLongitudine(longitudine);
        locatie.setOrientareScena(orientareScena);
        locatie.setVizibilitate(vizibilitate);
        locatie.setTeme(teme != null ? teme : Set.of());

        return locatieRepository.save(locatie);
    }

    public void sterge(Long id, Utilizator autor) {
        Locatie locatie = gasesteProprie(id, autor);
        // Întâi pozele: altfel rămân fișiere orfane pe disc.
        fotografieService.stergeToate(locatie);
        // Apoi sesiunile planificate acolo — fără locație nu mai înseamnă nimic.
        sesiuneRepository.deleteAll(sesiuneRepository.findByLocatie(locatie));
        locatieRepository.delete(locatie);
    }

    /**
     * Găsește o locație vizibilă pentru vizitatorul dat: publică pentru oricine, sau privată doar
     * pentru autorul ei. Întoarce 404 și pentru locații private ale altcuiva, ca să nu dezvăluie
     * existența lor.
     */
    public Locatie gasesteVizibila(Long id, Utilizator vizitator) {
        Locatie locatie = locatieRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Locația nu există."));

        boolean esteAutor = vizitator != null && locatie.getAutor().getId().equals(vizitator.getId());
        if (locatie.getVizibilitate() == Vizibilitate.PRIVATA && !esteAutor) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Locația nu există.");
        }

        return locatie;
    }
}
