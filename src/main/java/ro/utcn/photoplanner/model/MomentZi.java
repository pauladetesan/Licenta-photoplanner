package ro.utcn.photoplanner.model;

/** Partea din zi pentru care se planifică o sesiune foto. */
public enum MomentZi {

    RASARIT("Răsărit"),
    ORA_AUR_DIMINEATA("Oră de aur — dimineață"),
    ZI("În timpul zilei"),
    ORA_AUR_SEARA("Oră de aur — seară"),
    APUS("Apus"),
    NOAPTE("Noapte — astro");

    private final String eticheta;

    MomentZi(String eticheta) {
        this.eticheta = eticheta;
    }

    public String getEticheta() {
        return eticheta;
    }
}
