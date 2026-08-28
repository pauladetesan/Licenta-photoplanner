package ro.utcn.photoplanner.service;

import org.shredzone.commons.suncalc.MoonIllumination;
import org.shredzone.commons.suncalc.MoonPhase;
import org.shredzone.commons.suncalc.MoonTimes;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Calculează datele despre lună pentru o locație și o zi, tot cu commons-suncalc.
 * Contează mai ales pentru tema ASTRO: o lună plină spală stelele slabe.
 */
@Service
public class LunaService {

    public InfoLuna calculeaza(double latitudine, double longitudine, LocalDate data, ZoneId zona) {

        MoonTimes ore = MoonTimes.compute()
                .timezone(zona)
                .on(data)
                .at(latitudine, longitudine)
                .oneDay()
                .execute();

        MoonIllumination iluminare = MoonIllumination.compute()
                .timezone(zona)
                .on(data)
                .at(latitudine, longitudine)
                .execute();

        MoonPhase lunaNoua = MoonPhase.compute()
                .timezone(zona)
                .on(data)
                .phase(MoonPhase.Phase.NEW_MOON)
                .execute();

        MoonPhase lunaPlina = MoonPhase.compute()
                .timezone(zona)
                .on(data)
                .phase(MoonPhase.Phase.FULL_MOON)
                .execute();

        return new InfoLuna(
                ore.getRise(),
                ore.getSet(),
                ore.isAlwaysUp(),
                ore.isAlwaysDown(),
                iluminare.getFraction(),
                FazaLuna.din(iluminare.getClosestPhase()),
                lunaNoua.getTime(),
                lunaPlina.getTime()
        );
    }
}
