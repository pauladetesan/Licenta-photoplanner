package ro.utcn.photoplanner.service;

/**
 * Vremea la un moment anume.
 *
 * @param nori         gradul de acoperire cu nori, în procente
 * @param precipitatie probabilitatea de precipitații, în procente (poate lipsi)
 * @param temperatura  temperatura în grade Celsius (poate lipsi)
 */
public record VremeMoment(int nori, Integer precipitatie, Double temperatura) {

    /**
     * Ce înseamnă acoperirea cu nori pentru fotografie.
     * <p>
     * Cerul complet senin nu e neapărat ideal: fără nori, apusul e curat, dar fără culoare.
     * Norii împrăștiați sunt cei care prind lumina și dau cerul colorat.
     */
    public String verdict() {
        if (nori <= 15) {
            return "Cer senin — lumină curată și previzibilă, dar apus fără culoare dramatică.";
        }
        if (nori <= 60) {
            return "Nori împrăștiați — cele mai bune șanse de cer colorat la răsărit sau apus.";
        }
        if (nori <= 85) {
            return "Înnorat — lumina va fi difuză, bună pentru portrete, slabă pentru peisaj.";
        }
        return "Acoperit — probabil fără oră de aur vizibilă.";
    }

    /**
     * Verdictul potrivit momentului ales.
     * <p>
     * Contează cu adevărat: la o oră de aur, cerul complet senin e o pierdere (fără nori nu are
     * ce colora), dar la o sesiune astro e exact ce vrei. Un singur text pentru ambele ar fi
     * derutant tocmai când e mai important.
     */
    public String verdictPentru(ro.utcn.photoplanner.model.MomentZi moment) {
        return switch (moment) {
            case NOAPTE -> verdictNoapte();
            case ZI -> verdictZi();
            default -> verdict();
        };
    }

    /** Pentru astro contează doar cât cer liber rămâne. */
    private String verdictNoapte() {
        if (nori <= 15) {
            return "Cer senin — condiții foarte bune pentru stele.";
        }
        if (nori <= 40) {
            return "Nori împrăștiați — se pot prinde ferestre de cer liber.";
        }
        if (nori <= 75) {
            return "Mult cer acoperit — stelele se vor vedea greu.";
        }
        return "Acoperit — fără stele în noaptea asta.";
    }

    /** Ziua, norii înmoaie lumina în loc să o coloreze. */
    private String verdictZi() {
        if (nori <= 15) {
            return "Soare puternic — contraste dure și umbre tăioase la prânz.";
        }
        if (nori <= 60) {
            return "Soare cu intermitențe — lumina se schimbă des.";
        }
        return "Lumină difuză, uniformă — bună pentru portrete și detalii.";
    }

    /** O etichetă scurtă pentru afișare lângă procent. */
    public String eticheta() {
        if (nori <= 15) return "senin";
        if (nori <= 60) return "parțial noros";
        if (nori <= 85) return "înnorat";
        return "acoperit";
    }

    public boolean plouaProbabil() {
        return precipitatie != null && precipitatie >= 50;
    }
}
