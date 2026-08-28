package ro.utcn.photoplanner.service;

import org.springframework.stereotype.Service;
import ro.utcn.photoplanner.model.FactorScor;
import ro.utcn.photoplanner.model.Locatie;
import ro.utcn.photoplanner.model.MomentZi;
import ro.utcn.photoplanner.model.PonderiScor;
import ro.utcn.photoplanner.model.Tema;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Dă o notă fiecărui moment din următoarele zile și spune care e cel mai bun.
 * <p>
 * Scorul unui moment e:
 *
 * <pre>
 *   scor = potrivireMoment(temă, moment) × Σ(pondere · notă) / Σ(pondere)
 * </pre>
 *
 * Adică media ponderată a factorilor, înmulțită cu cât de potrivit e momentul zilei pentru temă.
 * Potrivirea e multiplicator, nu factor: astrofotografia la prânz trebuie să iasă zero oricât de
 * senin ar fi cerul, iar un factor cu pondere n-ar putea garanta asta.
 * <p>
 * Ce înseamnă „bine” pentru fiecare factor ține de temă ({@link PreferinteTema}), iar cât
 * cântărește fiecare ține de utilizator ({@link PonderiScor}).
 * <p>
 * Un factor pe care nu-l putem calcula — direcția luminii pentru o locație fără orientarea
 * scenei, vremea când prognoza nu răspunde — se scoate din sumă cu totul, ponderea lui cu tot.
 * Dacă i-am da nota zero, o locație căreia îi lipsește o informație ar părea mai proastă decât e.
 */
@Service
public class ScorService {

    /** Câte zile în față se caută, implicit. */
    public static final int ZILE_IMPLICIT = 7;

    private final SoareService soareService;
    private final LunaService lunaService;
    private final VremeService vremeService;
    private final LuminaService luminaService;

    public ScorService(SoareService soareService, LunaService lunaService,
                        VremeService vremeService, LuminaService luminaService) {
        this.soareService = soareService;
        this.lunaService = lunaService;
        this.vremeService = vremeService;
        this.luminaService = luminaService;
    }

    /**
     * Cât de aproape e o valoare de cea ideală, pe o scară de la 0 la 1.
     * <p>
     * E un clopot: nota scade cu atât mai repede cu cât toleranța e mai mică. Se folosește la
     * nori, la unghiul luminii și la temperatură — toate trei sunt „e bine pe la atât”, nu
     * „cu cât mai mult, cu atât mai bine”.
     */
    static double apropiere(double valoare, double optim, double toleranta) {
        double abatere = (valoare - optim) / toleranta;
        return Math.exp(-0.5 * abatere * abatere);
    }

    /** Ora exactă la care se referă momentul ales, în ziua și locația date. */
    public static ZonedDateTime oraMomentului(MomentZi moment, InfoSoare soare) {
        return switch (moment) {
            case RASARIT -> soare.rasarit();
            case ORA_AUR_DIMINEATA -> soare.mijlocDimineata();
            case ZI -> mijloculZilei(soare);
            case ORA_AUR_SEARA -> soare.mijlocSeara();
            case APUS -> soare.apus();
            // Cerul se întunecă de tot la ceva vreme după apus.
            case NOAPTE -> soare.apus() != null ? soare.apus().plusHours(2) : null;
        };
    }

    private static ZonedDateTime mijloculZilei(InfoSoare soare) {
        if (soare.rasarit() == null || soare.apus() == null) {
            return null;
        }
        long secunde = Duration.between(soare.rasarit(), soare.apus()).getSeconds();
        return soare.rasarit().plusSeconds(secunde / 2);
    }

    /**
     * Cel mai bun moment pentru o temă, în următoarele {@code zile} zile.
     *
     * @return momentul cu scorul cel mai mare, sau gol dacă niciunul nu e posibil (de pildă
     *         astro pentru o locație unde soarele nu apune în perioada asta)
     */
    public Optional<ScorMoment> celMaiBun(Locatie locatie, Tema tema, PonderiScor ponderi, int zile) {
        return toateMomentele(locatie, tema, ponderi, zile).stream()
                .max(Comparator.comparingDouble(ScorMoment::scor));
    }

    /** Toate momentele evaluate, ordonate descrescător după scor. */
    public List<ScorMoment> clasament(Locatie locatie, Tema tema, PonderiScor ponderi, int zile) {
        return toateMomentele(locatie, tema, ponderi, zile).stream()
                .sorted(Comparator.comparingDouble(ScorMoment::scor).reversed())
                .toList();
    }

    private List<ScorMoment> toateMomentele(Locatie locatie, Tema tema, PonderiScor ponderi,
                                             int zile) {
        ZoneId zona = locatie.getZona();
        LocalDate azi = LocalDate.now(zona);
        ZonedDateTime acum = ZonedDateTime.now(zona);

        List<ScorMoment> rezultate = new ArrayList<>();

        for (int i = 0; i < zile; i++) {
            LocalDate data = azi.plusDays(i);
            InfoSoare soare = soareService.calculeaza(
                    locatie.getLatitudine(), locatie.getLongitudine(), data, zona);
            InfoLuna luna = lunaService.calculeaza(
                    locatie.getLatitudine(), locatie.getLongitudine(), data, zona);

            for (MomentZi moment : MomentZi.values()) {
                double potrivire = PreferinteTema.potrivireMoment(tema, moment);
                if (potrivire <= 0) {
                    continue; // momentul nu are sens pentru tema asta
                }

                ZonedDateTime cand = oraMomentului(moment, soare);
                // Momentul poate lipsi (dincolo de cercul polar) sau poate fi deja trecut azi.
                if (cand == null || cand.isBefore(acum)) {
                    continue;
                }

                rezultate.add(evalueaza(locatie, tema, moment, cand, data, zona, soare, luna,
                        ponderi, potrivire));
            }
        }
        return rezultate;
    }

    private ScorMoment evalueaza(Locatie locatie, Tema tema, MomentZi moment, ZonedDateTime cand,
                                  LocalDate data, ZoneId zona, InfoSoare soare, InfoLuna luna,
                                  PonderiScor ponderi, double potrivire) {

        List<ContributieFactor> contributii = new ArrayList<>();

        VremeMoment vreme = vremeService.laUnMoment(
                locatie.getLatitudine(), locatie.getLongitudine(), data, zona, cand);

        if (vreme != null) {
            PreferinteTema.Optim optimNori = PreferinteTema.nori(tema, moment);
            adauga(contributii, ponderi, FactorScor.NORI,
                    apropiere(vreme.nori(), optimNori.valoare(), optimNori.toleranta()),
                    vreme.nori() + "% nori — " + vreme.eticheta());

            if (vreme.precipitatie() != null) {
                adauga(contributii, ponderi, FactorScor.PRECIPITATII,
                        1.0 - vreme.precipitatie() / 100.0,
                        vreme.precipitatie() + "% șanse de ploaie");
            }

            if (vreme.temperatura() != null) {
                PreferinteTema.Optim optimTemp = PreferinteTema.temperatura();
                adauga(contributii, ponderi, FactorScor.TEMPERATURA,
                        apropiere(vreme.temperatura(), optimTemp.valoare(), optimTemp.toleranta()),
                        Math.round(vreme.temperatura()) + "°C");
            }
        }

        // Direcția luminii are sens doar cât timp soarele e pe cer și doar dacă știm încotro
        // e îndreptată scena.
        if (locatie.getOrientareScena() != null && moment != MomentZi.NOAPTE) {
            InfoLumina lumina = luminaService.calculeaza(
                    locatie.getLatitudine(), locatie.getLongitudine(),
                    locatie.getOrientareScena(), moment.getEticheta(), cand);

            // Sub orizont nu mai e nimic de judecat despre direcție. Pragul e cel al lui
            // LuminaService, nu unul nou inventat aici.
            if (lumina.tip() != TipLumina.SUB_ORIZONT) {
                PreferinteTema.Optim optimUnghi = PreferinteTema.unghiLumina(tema);
                adauga(contributii, ponderi, FactorScor.LUMINA,
                        apropiere(Math.abs(lumina.unghiRelativ()),
                                optimUnghi.valoare(), optimUnghi.toleranta()),
                        lumina.tip().getEticheta());
            }
        }

        // Luna contează la fotografia de noapte; ziua, lumina ei nu se vede oricum.
        if (moment == MomentZi.NOAPTE) {
            adauga(contributii, ponderi, FactorScor.LUNA,
                    1.0 - luna.fractieIluminata(),
                    luna.procentIluminat() + "% din lună luminat");
        }

        return new ScorMoment(locatie, moment, cand,
                potrivire * mediePonderata(contributii), potrivire, contributii);
    }

    private static void adauga(List<ContributieFactor> contributii, PonderiScor ponderi,
                                FactorScor factor, double nota, String detaliu) {
        int pondere = ponderi.totulPeZero() ? factor.getPondereImplicita() : ponderi.pondere(factor);
        if (pondere <= 0) {
            return; // utilizatorul a spus că nu-l interesează factorul ăsta
        }
        contributii.add(new ContributieFactor(factor, Math.clamp(nota, 0, 1), pondere, detaliu));
    }

    /**
     * Media ponderată a factorilor care s-au putut calcula.
     * <p>
     * Împărțirea se face la suma ponderilor <em>prezente</em>, nu la totalul teoretic: altfel
     * o locație fără orientarea scenei ar fi pedepsită pentru o informație care lipsește, nu
     * pentru condiții proaste.
     */
    private static double mediePonderata(List<ContributieFactor> contributii) {
        double sumaAporturilor = 0;
        int sumaPonderilor = 0;

        for (ContributieFactor c : contributii) {
            sumaAporturilor += c.aport();
            sumaPonderilor += c.pondere();
        }

        // Fără niciun factor calculabil nu avem ce spune; 0 ar fi o afirmație pe care n-o susține
        // nimic, așa că rămâne o valoare neutră.
        return sumaPonderilor == 0 ? 0.5 : sumaAporturilor / sumaPonderilor;
    }
}
