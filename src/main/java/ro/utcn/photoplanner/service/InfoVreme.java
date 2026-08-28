package ro.utcn.photoplanner.service;

/**
 * Prognoza pentru cele două ore de aur ale unei zile.
 *
 * @param disponibil dacă avem sau nu date; când e false, {@code mesaj} spune de ce
 */
public record InfoVreme(boolean disponibil, String mesaj,
                         VremeMoment dimineata, VremeMoment seara) {

    public static InfoVreme indisponibil(String mesaj) {
        return new InfoVreme(false, mesaj, null, null);
    }

    public static InfoVreme cu(VremeMoment dimineata, VremeMoment seara) {
        return new InfoVreme(true, null, dimineata, seara);
    }
}
