package ro.utcn.photoplanner.model;

/**
 * Factorii din care se compune scorul unui moment.
 * <p>
 * Fiecare factor dă o notă între 0 și 1 pentru un moment anume, iar scorul final e media lor
 * ponderată. Ce înseamnă „bine” pentru un factor ține de temă — o zi acoperită e proastă pentru
 * peisaj și bună pentru portret — iar <em>cât</em> contează fiecare ține de utilizator. Astea sunt
 * două lucruri diferite și se reglează separat: temele stau în {@code PreferinteTema}, ponderile
 * în {@link PonderiScor}.
 */
public enum FactorScor {

    NORI("Nori", "Cât de acoperit e cerul. Nu „mai puțin e mai bine”: la o oră de aur, "
            + "norii împrăștiați sunt cei care colorează cerul, iar pentru portret lumina "
            + "difuză a unei zile acoperite e un avantaj.", 3),

    LUMINA("Direcția luminii", "Din ce parte cade soarele pe scenă, față de direcția în care "
            + "fotografiezi. Se poate calcula doar dacă locația are orientarea scenei "
            + "completată.", 2),

    LUNA("Luna", "Cât din discul lunar e luminat. Contează la fotografia de noapte, unde "
            + "o lună puternică spală stelele slabe.", 1),

    PRECIPITATII("Precipitații", "Probabilitatea de ploaie la ora respectivă.", 2),

    TEMPERATURA("Temperatură", "Cât de comod se stă afară. Cântărește puțin implicit — "
            + "urcă-l dacă nu vrei să pleci pe ger.", 1);

    private final String eticheta;
    private final String explicatie;
    private final int pondereImplicita;

    FactorScor(String eticheta, String explicatie, int pondereImplicita) {
        this.eticheta = eticheta;
        this.explicatie = explicatie;
        this.pondereImplicita = pondereImplicita;
    }

    public String getEticheta() { return eticheta; }

    public String getExplicatie() { return explicatie; }

    public int getPondereImplicita() { return pondereImplicita; }

    /** Ponderea maximă pe care o poate alege un utilizator. */
    public static final int PONDERE_MAXIMA = 5;
}
