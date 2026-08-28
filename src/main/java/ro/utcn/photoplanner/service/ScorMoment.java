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

    /** Scorul cu două zecimale și virgulă, cum se scrie în românește: „0,84”. */
    public String scorFormatat() {
        return String.format(java.util.Locale.of("ro"), "%.2f", scor);
    }

    /** Scorul ca procent întreg, pentru bare și etichete. */
    public int procent() {
        return (int) Math.round(scor * 100);
    }

    /**
     * O apreciere în cuvinte, ca să nu rămână doar un număr gol.
     * <p>
     * Pragurile sunt alese ca scorul să însemne ceva la prima citire: peste 0,75 chiar merită
     * drumul, sub 0,35 mai bine aștepți altă zi.
     */
    public String verdict() {
        if (scor >= 0.75) return "Foarte bun";
        if (scor >= 0.55) return "Bun";
        if (scor >= 0.35) return "Acceptabil";
        return "Slab";
    }

    /** Contribuțiile ordonate descrescător după cât au cântărit — prima explică cel mai mult. */
    public List<ContributieFactor> contributiiDupaAport() {
        return contributii.stream()
                .sorted((a, b) -> Double.compare(b.aport(), a.aport()))
                .toList();
    }
}
