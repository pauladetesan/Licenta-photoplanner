package ro.utcn.photoplanner.service;

import org.shredzone.commons.suncalc.SunPosition;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Compară poziția soarelui cu orientarea scenei ca să spună din ce direcție cade lumina.
 * <p>
 * {@code orientareScena} este direcția în care privește aparatul foto (aceeași convenție ca eticheta
 * EXIF {@code GPSImgDirection}, din care o completăm automat): 0 = nord, 90 = est, 180 = sud, 270 = vest.
 * Deci un unghi relativ de 0° înseamnă că soarele e fix în fața aparatului — adică în spatele subiectului.
 */
@Service
public class LuminaService {

    /** Sub acest unghi de înălțime considerăm că soarele nu mai luminează util scena. */
    private static final double INALTIME_MINIMA_GRADE = 0.0;

    public InfoLumina calculeaza(double latitudine, double longitudine, int orientareScena,
                                  String eticheta, ZonedDateTime moment) {

        SunPosition pozitie = SunPosition.compute()
                .on(moment)
                .at(latitudine, longitudine)
                .execute();

        double azimut = pozitie.getAzimuth();
        double inaltime = pozitie.getAltitude();
        int unghiRelativ = (int) Math.round(normalizeaza(azimut - orientareScena));

        TipLumina tip = inaltime <= INALTIME_MINIMA_GRADE
                ? TipLumina.SUB_ORIZONT
                : clasifica(unghiRelativ);

        return new InfoLumina(eticheta, moment, azimut, inaltime, unghiRelativ, tip);
    }

    /**
     * Calculează lumina la mijlocul fiecărei ore de aur — momentele care contează când alegi
     * între o sesiune de dimineață și una de seară.
     */
    public List<InfoLumina> calculeazaPentruOreleDeAur(double latitudine, double longitudine,
                                                        int orientareScena, InfoSoare infoSoare) {
        List<InfoLumina> rezultat = new ArrayList<>();

        ZonedDateTime dimineata = infoSoare.mijlocDimineata();
        if (dimineata != null) {
            rezultat.add(calculeaza(latitudine, longitudine, orientareScena, "Oră de aur — dimineață", dimineata));
        }

        ZonedDateTime seara = infoSoare.mijlocSeara();
        if (seara != null) {
            rezultat.add(calculeaza(latitudine, longitudine, orientareScena, "Oră de aur — seară", seara));
        }

        return rezultat;
    }

    /** Aduce un unghi în intervalul [-180, 180). Vizibil pentru teste. */
    static double normalizeaza(double grade) {
        double rezultat = (grade + 180.0) % 360.0;
        if (rezultat < 0) {
            rezultat += 360.0;
        }
        return rezultat - 180.0;
    }

    /** Vizibil pentru teste. */
    static TipLumina clasifica(int unghiRelativ) {
        int absolut = Math.abs(unghiRelativ);

        if (absolut < 45) {
            return TipLumina.CONTRALUMINA;
        }
        if (absolut >= 135) {
            return TipLumina.FRONTALA;
        }
        return unghiRelativ > 0 ? TipLumina.LATERALA_DREAPTA : TipLumina.LATERALA_STANGA;
    }
}
