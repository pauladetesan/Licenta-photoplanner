package ro.utcn.photoplanner.service;

import org.shredzone.commons.suncalc.MoonPhase;

/** Fazele lunii, cu denumiri și simboluri pentru afișare. */
public enum FazaLuna {

    LUNA_NOUA("Lună nouă", "🌑"),
    SEMILUNA_CRESCATOARE("Semilună crescătoare", "🌒"),
    PRIMUL_PATRAR("Primul pătrar", "🌓"),
    GIBOASA_CRESCATOARE("Lună crescătoare", "🌔"),
    LUNA_PLINA("Lună plină", "🌕"),
    GIBOASA_DESCRESCATOARE("Lună descrescătoare", "🌖"),
    ULTIMUL_PATRAR("Ultimul pătrar", "🌗"),
    SEMILUNA_DESCRESCATOARE("Semilună descrescătoare", "🌘");

    private final String eticheta;
    private final String simbol;

    FazaLuna(String eticheta, String simbol) {
        this.eticheta = eticheta;
        this.simbol = simbol;
    }

    public String getEticheta() { return eticheta; }
    public String getSimbol() { return simbol; }

    /** Traduce faza din commons-suncalc în varianta noastră. */
    public static FazaLuna din(MoonPhase.Phase faza) {
        return switch (faza) {
            case NEW_MOON -> LUNA_NOUA;
            case WAXING_CRESCENT -> SEMILUNA_CRESCATOARE;
            case FIRST_QUARTER -> PRIMUL_PATRAR;
            case WAXING_GIBBOUS -> GIBOASA_CRESCATOARE;
            case FULL_MOON -> LUNA_PLINA;
            case WANING_GIBBOUS -> GIBOASA_DESCRESCATOARE;
            case LAST_QUARTER -> ULTIMUL_PATRAR;
            case WANING_CRESCENT -> SEMILUNA_DESCRESCATOARE;
        };
    }
}
