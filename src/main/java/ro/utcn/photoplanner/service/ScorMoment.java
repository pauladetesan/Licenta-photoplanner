package ro.utcn.photoplanner.service;

import ro.utcn.photoplanner.model.Locatie;
import ro.utcn.photoplanner.model.MomentZi;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * Un moment evaluat: unde, când și cât de bun.
 *
 * @param locatie      locația evaluată
 * @param moment       partea din zi
 * @param cand         ora exactă a momentului, în fusul locației
 * @param scor         nota finală, între 0 și 1
 * @param potrivire    cât de potrivit e momentul zilei pentru temă (multiplicatorul)
 * @param contributii  ce a contat și cât, pentru explicație
 */
public record ScorMoment(Locatie locatie, MomentZi moment, ZonedDateTime cand,
                          double scor, double potrivire, List<ContributieFactor> contributii) {

    /**
     * Scorul rotunjit la cele două zecimale care ajung pe ecran.
     * <p>
     * Tot ce se arată utilizatorului pornește de aici — și numărul, și verdictul. Altfel un scor
     * de 0,8497 s-ar afișa „0,85”, dar ar fi judecat ca 0,8497 și ar primi „Bun”, în timp ce un
     * 0,8502 afișat tot „0,85” ar primi „Foarte bun”: același număr pe ecran, două etichete
     * diferite. S-a și întâmplat, pe date reale.
     */
    private double scorAfisat() {
        return Math.round(scor * 100) / 100.0;
    }

    /** Scorul cu două zecimale și virgulă, cum se scrie în românește: „0,84”. */
    public String scorFormatat() {
        return String.format(java.util.Locale.of("ro"), "%.2f", scorAfisat());
    }

    /** Scorul ca procent întreg, pentru bare și etichete. */
    public int procent() {
        return (int) Math.round(scor * 100);
    }

    /**
     * O apreciere în cuvinte, ca să nu rămână doar un număr gol.
     * <p>
     * Pragurile sunt calibrate pentru <em>câștigătorul</em> unui interval, nu pentru un moment
     * oarecare. Prima variantă (0,75 / 0,55 / 0,35) fusese gândită pentru un moment singur și
     * s-a dovedit inutilă la prima rulare pe date reale: cel mai bun moment din șapte zile e
     * ales dintre vreo treizeci de candidați, așa că iese aproape întotdeauna sus, iar toate
     * cele cinci teme au primit „Foarte bun”. O etichetă pe care o primește toată lumea nu
     * spune nimic.
     * <p>
     * Cu pragurile de acum, „Foarte bun” înseamnă o fereastră chiar ieșită din comun, nu doar
     * cea mai bună dintre cele disponibile.
     */
    public String verdict() {
        // Se judecă numărul afișat, nu cel brut: eticheta trebuie să se potrivească mereu cu
        // cifra de lângă ea.
        double afisat = scorAfisat();
        if (afisat >= 0.85) return "Foarte bun";
        if (afisat >= 0.70) return "Bun";
        if (afisat >= 0.50) return "Acceptabil";
        return "Slab";
    }

    /** Contribuțiile ordonate descrescător după cât au cântărit — prima explică cel mai mult. */
    public List<ContributieFactor> contributiiDupaAport() {
        return contributii.stream()
                .sorted((a, b) -> Double.compare(b.aport(), a.aport()))
                .toList();
    }
}
