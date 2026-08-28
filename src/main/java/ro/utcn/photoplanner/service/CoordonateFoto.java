package ro.utcn.photoplanner.service;

/** Coordonate GPS extrase din metadatele EXIF ale unei fotografii. */
public record CoordonateFoto(
        boolean gasit,
        Double latitudine,
        Double longitudine,
        Integer orientareScena,
        String mesaj
) {
    public static CoordonateFoto negasit(String mesaj) {
        return new CoordonateFoto(false, null, null, null, mesaj);
    }

    public static CoordonateFoto gasit(double latitudine, double longitudine, Integer orientareScena) {
        return new CoordonateFoto(true, latitudine, longitudine, orientareScena, "Coordonate găsite în fotografie.");
    }
}
