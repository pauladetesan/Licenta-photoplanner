package ro.utcn.photoplanner.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.utcn.photoplanner.model.Locatie;
import ro.utcn.photoplanner.model.Utilizator;
import ro.utcn.photoplanner.model.Vizibilitate;
import ro.utcn.photoplanner.repository.ComentariuRepository;
import ro.utcn.photoplanner.repository.FavoritRepository;
import ro.utcn.photoplanner.repository.LocatieRepository;
import ro.utcn.photoplanner.repository.SesiuneFotoRepository;
import ro.utcn.photoplanner.repository.TokenResetareRepository;
import ro.utcn.photoplanner.repository.UtilizatorRepository;

import java.util.List;

/**
 * Ștergerea definitivă a unui cont și a tot ce ține de el.
 * <p>
 * Ordinea contează: fiecare rând care trimite spre altul trebuie șters înaintea lui, altfel
 * baza refuză operația. De aceea se merge dinspre lucrurile care depind de altceva spre cele
 * de care depind celelalte, iar contul e ultimul.
 * <p>
 * Se șterg și datele <em>altora</em> care atârnă de locațiile celui care pleacă — comentariile
 * lăsate acolo, favoritele, sesiunile planificate. Fără locație nu mai au la ce se referi.
 */
@Service
public class StergereContService {

    private static final Logger log = LoggerFactory.getLogger(StergereContService.class);

    private final UtilizatorRepository utilizatorRepository;
    private final LocatieRepository locatieRepository;
    private final ComentariuRepository comentariuRepository;
    private final FavoritRepository favoritRepository;
    private final SesiuneFotoRepository sesiuneRepository;
    private final TokenResetareRepository tokenRepository;
    private final FotografieService fotografieService;
    private final ContAnonimService contAnonimService;

    public StergereContService(UtilizatorRepository utilizatorRepository,
                                LocatieRepository locatieRepository,
                                ComentariuRepository comentariuRepository,
                                FavoritRepository favoritRepository,
                                SesiuneFotoRepository sesiuneRepository,
                                TokenResetareRepository tokenRepository,
                                FotografieService fotografieService,
                                ContAnonimService contAnonimService) {
        this.utilizatorRepository = utilizatorRepository;
        this.locatieRepository = locatieRepository;
        this.comentariuRepository = comentariuRepository;
        this.favoritRepository = favoritRepository;
        this.sesiuneRepository = sesiuneRepository;
        this.tokenRepository = tokenRepository;
        this.fotografieService = fotografieService;
        this.contAnonimService = contAnonimService;
    }

    /** Ce anume dispare — se arată utilizatorului înainte să confirme. */
    public record Rezumat(long locatii, long publice, long fotografii, long comentarii,
                           long favorite, long sesiuni) {}

    public Rezumat rezumat(Utilizator utilizator) {
        List<Locatie> locatii = locatieRepository.findByAutorOrderByDataAdaugareDesc(utilizator);
        long fotografii = locatii.stream()
                .mapToLong(l -> fotografieService.listaPentru(l).size())
                .sum();

        long publice = locatii.stream()
                .filter(l -> l.getVizibilitate() == Vizibilitate.PUBLICA)
                .count();

        return new Rezumat(
                locatii.size(),
                publice,
                fotografii,
                comentariuRepository.findByAutor(utilizator).size(),
                favoritRepository.findByUtilizator(utilizator).size(),
                sesiuneRepository.countByUtilizator(utilizator));
    }

    /**
     * @param pastreazaPublice dacă locațiile publice rămân pe site sub un cont anonim, în loc
     *                         să dispară. Coordonatele unui loc nu sunt un dat personal, spre
     *                         deosebire de nume, email și fotografii — care se șterg oricum.
     */
    @Transactional
    public void sterge(Utilizator utilizator, boolean pastreazaPublice) {
        Long id = utilizator.getId();

        // 1. Ce a lăsat utilizatorul pe la alții — nu ține nimic altceva de ele.
        tokenRepository.deleteAll(tokenRepository.findByUtilizator(utilizator));
        favoritRepository.deleteAll(favoritRepository.findByUtilizator(utilizator));
        comentariuRepository.deleteAll(comentariuRepository.findByAutor(utilizator));
        sesiuneRepository.deleteAll(sesiuneRepository.findByUtilizator(utilizator));

        // 2. Locațiile lui.
        for (Locatie locatie : locatieRepository.findByAutorOrderByDataAdaugareDesc(utilizator)) {

            if (pastreazaPublice && locatie.getVizibilitate() == Vizibilitate.PUBLICA) {
                /*
                 * Locul rămâne, dar fără nimic care să trimită înapoi la persoană: fotografiile
                 * sunt munca lui și pleacă odată cu el, iar autorul devine contul anonim.
                 * Comentariile și favoritele altora rămân — locația la care se referă e încă acolo.
                 */
                fotografieService.stergeToate(locatie);
                locatie.setAutor(contAnonimService.obtine());
                locatieRepository.save(locatie);
                continue;
            }

            // Altfel dispare cu tot ce au strâns alții pe ea.
            fotografieService.stergeToate(locatie);                                  // și fișierele de pe disc
            comentariuRepository.deleteAll(comentariuRepository.findByLocatie(locatie));
            favoritRepository.deleteAll(favoritRepository.findByLocatie(locatie));
            sesiuneRepository.deleteAll(sesiuneRepository.findByLocatie(locatie));
            locatieRepository.delete(locatie);
        }

        // 3. Contul însuși, la final.
        utilizatorRepository.delete(utilizator);

        /*
         * Copiile pe care alții le-au salvat în portofoliul lor rămân: sunt rândurile lor, cu
         * datele locului. Păstrează și numele sub care a fost găsit — o mențiune scrisă atunci,
         * nu o legătură către contul acesta.
         */
        log.info("Contul {} a fost șters definitiv, împreună cu datele lui.", id);
    }
}
