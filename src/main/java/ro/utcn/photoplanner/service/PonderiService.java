package ro.utcn.photoplanner.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ro.utcn.photoplanner.model.FactorScor;
import ro.utcn.photoplanner.model.PonderiScor;
import ro.utcn.photoplanner.model.Utilizator;
import ro.utcn.photoplanner.repository.PonderiScorRepository;

import java.util.Map;

/**
 * Ponderile fiecărui utilizator: se creează la prima nevoie, cu valorile implicite.
 * <p>
 * Nu se creează la înregistrare, ci abia când sunt cerute: un rând gol pentru fiecare cont care
 * nu s-a uitat niciodată la scoruri n-ar folosi la nimic.
 */
@Service
public class PonderiService {

    private final PonderiScorRepository ponderiRepository;

    public PonderiService(PonderiScorRepository ponderiRepository) {
        this.ponderiRepository = ponderiRepository;
    }

    /** Ponderile utilizatorului, sau setul implicit dacă nu și-a schimbat nimic încă. */
    @Transactional
    public PonderiScor pentru(Utilizator utilizator) {
        if (utilizator == null) {
            return PonderiScor.implicite(null);
        }
        return ponderiRepository.findByUtilizator(utilizator)
                .orElseGet(() -> PonderiScor.implicite(utilizator));
    }

    /** Salvează ponderile alese. Valorile din afara intervalului sunt aduse înăuntru. */
    @Transactional
    public PonderiScor salveaza(Utilizator utilizator, Map<FactorScor, Integer> alese) {
        PonderiScor ponderi = ponderiRepository.findByUtilizator(utilizator)
                .orElseGet(() -> PonderiScor.implicite(utilizator));

        for (FactorScor factor : FactorScor.values()) {
            Integer valoare = alese.get(factor);
            if (valoare != null) {
                ponderi.setPondere(factor, valoare);
            }
        }
        return ponderiRepository.save(ponderi);
    }

    /** Readuce toate ponderile la valorile implicite. */
    @Transactional
    public PonderiScor reseteaza(Utilizator utilizator) {
        PonderiScor ponderi = ponderiRepository.findByUtilizator(utilizator)
                .orElseGet(() -> PonderiScor.implicite(utilizator));

        for (FactorScor factor : FactorScor.values()) {
            ponderi.setPondere(factor, factor.getPondereImplicita());
        }
        return ponderiRepository.save(ponderi);
    }
}
