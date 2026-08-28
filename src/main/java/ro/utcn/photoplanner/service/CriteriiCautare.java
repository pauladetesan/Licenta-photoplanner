package ro.utcn.photoplanner.service;

import ro.utcn.photoplanner.model.Tema;

import java.util.Set;

/**
 * Filtrele după care se caută în locațiile publice. Oricare dintre ele poate lipsi.
 *
 * @param text  bucată de text căutată în nume și descriere
 * @param teme  temele acceptate — o locație trece dacă are măcar una dintre ele
 * @param lat   latitudinea punctului față de care se caută
 * @param lon   longitudinea punctului
 * @param raza  raza de căutare, în kilometri
 */
public record CriteriiCautare(String text, Set<Tema> teme, Double lat, Double lon, Double raza) {

    /** Placeholder trimis când nu filtrăm după teme: un `in ()` gol ar da SQL invalid. */
    private static final Set<Tema> ORICE_TEMA = Set.of(Tema.PORTRET);

    public static CriteriiCautare goale() {
        return new CriteriiCautare(null, Set.of(), null, null, null);
    }

    public boolean areText() {
        return text != null && !text.isBlank();
    }

    public boolean areTeme() {
        return teme != null && !teme.isEmpty();
    }

    /** Căutarea după distanță are sens doar cu punct și rază valide. */
    public boolean areDistanta() {
        return lat != null && lon != null && raza != null && raza > 0
                && lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
    }

    /** Textul pregătit pentru `like`, sau null dacă nu se filtrează după text. */
    public String tiparText() {
        return areText() ? "%" + text.trim().toLowerCase() + "%" : null;
    }

    public Set<Tema> temeSauPlaceholder() {
        return areTeme() ? teme : ORICE_TEMA;
    }

    public double latSauZero() { return areDistanta() ? lat : 0.0; }
    public double lonSauZero() { return areDistanta() ? lon : 0.0; }
    public double razaSauZero() { return areDistanta() ? raza : 0.0; }

    public boolean areVreunFiltru() {
        return areText() || areTeme() || areDistanta();
    }
}
