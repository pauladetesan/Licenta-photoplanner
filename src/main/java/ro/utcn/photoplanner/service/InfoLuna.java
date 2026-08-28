package ro.utcn.photoplanner.service;

import java.time.ZonedDateTime;

/**
 * Datele despre lună pentru o zi și o locație — ce contează când planifici o sesiune astro.
 *
 * @param rasarit           răsăritul lunii, sau null dacă nu răsare în ziua respectivă
 * @param apus              apusul lunii, sau null dacă nu apune în ziua respectivă
 * @param mereuDeasupra     luna nu apune deloc în ziua respectivă
 * @param mereuSubOrizont   luna nu răsare deloc în ziua respectivă
 * @param fractieIluminata  cât din discul lunar e luminat, între 0 (lună nouă) și 1 (lună plină)
 * @param faza              faza cea mai apropiată
 * @param urmatoareaLunaNoua următoarea lună nouă — cerul cel mai întunecat
 * @param urmatoareaLunaPlina următoarea lună plină
 */
public record InfoLuna(
        ZonedDateTime rasarit,
        ZonedDateTime apus,
        boolean mereuDeasupra,
        boolean mereuSubOrizont,
        double fractieIluminata,
        FazaLuna faza,
        ZonedDateTime urmatoareaLunaNoua,
        ZonedDateTime urmatoareaLunaPlina
) {

    /** Procentul de disc luminat, rotunjit — pentru afișare. */
    public int procentIluminat() {
        return (int) Math.round(fractieIluminata * 100);
    }

    /**
     * Cât de prielnic e cerul pentru astrofotografie în noaptea asta.
     * Sub 25% luminat cerul e destul de întunecat pentru Calea Lactee.
     */
    public String verdictAstro() {
        int procent = procentIluminat();
        if (procent <= 10) {
            return "Cer foarte întunecat — condiții excelente pentru Calea Lactee.";
        }
        if (procent <= 25) {
            return "Cer destul de întunecat — bun pentru astrofotografie.";
        }
        if (procent <= 60) {
            return "Luna spală o parte din stelele slabe.";
        }
        return "Lună puternică — stelele slabe și Calea Lactee vor fi greu de prins.";
    }
}
