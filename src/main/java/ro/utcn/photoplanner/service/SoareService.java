package ro.utcn.photoplanner.service;

import org.shredzone.commons.suncalc.SunTimes;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

/** Calculează răsăritul, apusul și orele de aur pentru o locație, folosind commons-suncalc. */
@Service
public class SoareService {

    /**
     * @param zona fusul orar în care se întorc orele — cel al locației, nu al serverului,
     *             ca o locație din străinătate să arate ora de acolo.
     */
    public InfoSoare calculeaza(double latitudine, double longitudine, LocalDate data, ZoneId zona) {
        SunTimes vizual = calcul(latitudine, longitudine, data, zona, null);
        SunTimes oraDeAur = calcul(latitudine, longitudine, data, zona, SunTimes.Twilight.GOLDEN_HOUR);
        SunTimes oraAlbastra = calcul(latitudine, longitudine, data, zona, SunTimes.Twilight.BLUE_HOUR);

        return new InfoSoare(
                vizual.getRise(),
                vizual.getSet(),
                oraAlbastra.getRise(),
                oraDeAur.getRise(),
                oraDeAur.getSet(),
                oraAlbastra.getSet()
        );
    }

    private static SunTimes calcul(double latitudine, double longitudine, LocalDate data,
                                    ZoneId zona, SunTimes.Twilight crepuscul) {
        SunTimes.Parameters parametri = SunTimes.compute()
                .timezone(zona)
                .on(data)
                .at(latitudine, longitudine)
                .oneDay();

        if (crepuscul != null) {
            parametri = parametri.twilight(crepuscul);
        }

        return parametri.execute();
    }
}
