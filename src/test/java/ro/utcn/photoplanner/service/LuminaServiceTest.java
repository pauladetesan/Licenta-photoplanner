package ro.utcn.photoplanner.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Interpretarea unghiului dintre soare și direcția de fotografiere.
 * <p>
 * {@code orientareScena} e direcția în care privește aparatul, deci 0° relativ înseamnă soarele
 * fix în fața obiectivului — adică în spatele subiectului.
 */
class LuminaServiceTest {

    @DisplayName("normalizeaza aduce orice unghi în [-180, 180)")
    @ParameterizedTest(name = "{0}° -> {1}°")
    @CsvSource({
            "0,      0",
            "90,    90",
            "-90,  -90",
            "180, -180",
            "270,  -90",
            "360,    0",
            "450,   90",
            "-270,  90"
    })
    void normalizeazaUnghiuri(double intrare, double asteptat) {
        assertThat(LuminaService.normalizeaza(intrare)).isCloseTo(asteptat, within(0.0001));
    }

    /**
     * Cazul care se strică cel mai ușor: aparatul spre 350°, soarele la 10°.
     * Diferența brută e -340°, dar unghiul real dintre ele e doar 20°.
     */
    @Test
    void diferentaPesteNordEsteMica() {
        assertThat(LuminaService.normalizeaza(10 - 350)).isCloseTo(20.0, within(0.0001));
        assertThat(LuminaService.clasifica((int) Math.round(LuminaService.normalizeaza(10 - 350))))
                .isEqualTo(TipLumina.CONTRALUMINA);
    }

    @DisplayName("clasifica pune unghiul în categoria corectă de lumină")
    @ParameterizedTest(name = "{0}° -> {1}")
    @CsvSource({
            // soarele în fața aparatului = în spatele subiectului
            "0,    CONTRALUMINA",
            "30,   CONTRALUMINA",
            "-30,  CONTRALUMINA",
            "44,   CONTRALUMINA",
            // lateral
            "45,   LATERALA_DREAPTA",
            "90,   LATERALA_DREAPTA",
            "134,  LATERALA_DREAPTA",
            "-45,  LATERALA_STANGA",
            "-90,  LATERALA_STANGA",
            "-134, LATERALA_STANGA",
            // soarele în spatele fotografului, luminând subiectul
            "135,  FRONTALA",
            "180,  FRONTALA",
            "-180, FRONTALA",
            "-135, FRONTALA"
    })
    void clasificaUnghiuri(int unghi, TipLumina asteptat) {
        assertThat(LuminaService.clasifica(unghi)).isEqualTo(asteptat);
    }

    /**
     * Verificare de bun-simț cu busola: privind spre sud (180°), soarele de la est (90°)
     * vine din stânga, iar cel de la vest (270°) din dreapta.
     */
    @Test
    void privindSpreSudSoareleDeLaEstVineDinStanga() {
        int estRelativ = (int) Math.round(LuminaService.normalizeaza(90 - 180));
        int vestRelativ = (int) Math.round(LuminaService.normalizeaza(270 - 180));

        assertThat(estRelativ).isEqualTo(-90);
        assertThat(vestRelativ).isEqualTo(90);
        assertThat(LuminaService.clasifica(estRelativ)).isEqualTo(TipLumina.LATERALA_STANGA);
        assertThat(LuminaService.clasifica(vestRelativ)).isEqualTo(TipLumina.LATERALA_DREAPTA);
    }

    @Test
    void fiecareTipAreEtichetaSiExplicatie() {
        for (TipLumina tip : TipLumina.values()) {
            assertThat(tip.getEticheta()).isNotBlank();
            assertThat(tip.getExplicatie()).isNotBlank();
        }
    }
}
