package ro.utcn.photoplanner.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import ro.utcn.photoplanner.model.Locatie;
import ro.utcn.photoplanner.repository.LocatieRepository;
import ro.utcn.photoplanner.service.FusOrarService;

import java.util.List;

/**
 * Completează fusul orar pentru locațiile salvate înainte ca acest câmp să existe.
 * Rulează o singură dată, la pornire, și nu face nimic dacă toate locațiile îl au deja.
 */
@Component
public class CompletareFusOrar implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CompletareFusOrar.class);

    private final LocatieRepository locatieRepository;
    private final FusOrarService fusOrarService;

    public CompletareFusOrar(LocatieRepository locatieRepository, FusOrarService fusOrarService) {
        this.locatieRepository = locatieRepository;
        this.fusOrarService = fusOrarService;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Locatie> faraFusOrar = locatieRepository.findByFusOrarIsNull();
        if (faraFusOrar.isEmpty()) {
            return;
        }

        log.info("Completez fusul orar pentru {} locații...", faraFusOrar.size());
        for (Locatie locatie : faraFusOrar) {
            String fus = fusOrarService.pentruCoordonate(locatie.getLatitudine(), locatie.getLongitudine());
            locatie.setFusOrar(fus);
            log.info("  {} -> {}", locatie.getNume(), fus);
        }
        locatieRepository.saveAll(faraFusOrar);
    }
}
