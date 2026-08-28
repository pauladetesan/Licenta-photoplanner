package ro.utcn.photoplanner.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import us.dustinj.timezonemap.TimeZone;
import us.dustinj.timezonemap.TimeZoneMap;

import java.time.ZoneId;

/**
 * Deduce fusul orar al unei locații din coordonatele ei.
 * <p>
 * Încărcăm harta doar pentru o zonă mică din jurul punctului: e mult mai ieftin decât harta
 * întregii lumi, iar căutarea se face rar — doar când se salvează o locație.
 */
@Service
public class FusOrarService {

    private static final Logger log = LoggerFactory.getLogger(FusOrarService.class);

    /** Cât de mare e pătratul (în grade) încărcat în jurul punctului căutat. */
    private static final double MARJA_GRADE = 0.05;

    /**
     * Întoarce identificatorul fusului orar (ex. „Europe/Paris”), sau fusul serverului dacă
     * nu poate fi determinat.
     */
    public String pentruCoordonate(double latitudine, double longitudine) {
        try {
            TimeZoneMap harta = TimeZoneMap.forRegion(
                    latitudine - MARJA_GRADE, longitudine - MARJA_GRADE,
                    latitudine + MARJA_GRADE, longitudine + MARJA_GRADE);

            TimeZone fus = harta.getOverlappingTimeZone(latitudine, longitudine);
            if (fus != null) {
                return fus.getZoneId();
            }
            log.warn("Niciun fus orar găsit pentru {}, {}", latitudine, longitudine);
        } catch (RuntimeException e) {
            log.warn("Căutarea fusului orar a eșuat pentru {}, {}: {}",
                    latitudine, longitudine, e.toString());
        }
        return ZoneId.systemDefault().getId();
    }
}
