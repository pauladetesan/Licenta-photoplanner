package ro.utcn.photoplanner.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Orele soarelui. Testul care contează cel mai mult e cel cu fusul orar: la un moment dat
 * locațiile din străinătate afișau ora serverului, nu ora locului.
 */
class SoareServiceTest {

    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
    private static final ZoneId BUCURESTI = ZoneId.of("Europe/Bucharest");

    private final SoareService serviciu = new SoareService();

    // Turnul Eiffel
    private static final double PARIS_LAT = 48.8584;
    private static final double PARIS_LON = 2.2945;

    @Test
    @DisplayName("orele vin în fusul locației, nu în cel al serverului")
    void orelePrimescFusulLocatiei() {
        InfoSoare info = serviciu.calculeaza(
                PARIS_LAT, PARIS_LON, LocalDate.of(2026, 12, 21), PARIS);

        assertThat(info.rasarit().getZone()).isEqualTo(PARIS);
        // Iarna Parisul e pe CET = UTC+1.
        assertThat(info.rasarit().getOffset().getTotalSeconds()).isEqualTo(3600);
        // Răsăritul la Paris pe 21 decembrie e în jur de 08:41 ora locală.
        assertThat(info.rasarit().getHour()).isEqualTo(8);
        assertThat(info.rasarit().getMinute()).isBetween(35, 47);
    }

    @Test
    @DisplayName("aceeași zi și loc, alt fus: același moment, alt ceas")
    void acelasiMomentAltCeas() {
        LocalDate zi = LocalDate.of(2026, 12, 21);

        InfoSoare laParis = serviciu.calculeaza(PARIS_LAT, PARIS_LON, zi, PARIS);
        InfoSoare laBucuresti = serviciu.calculeaza(PARIS_LAT, PARIS_LON, zi, BUCURESTI);

        // Bucureștiul e cu o oră înaintea Parisului iarna, deci același răsărit se citește cu 1h mai târziu.
        assertThat(laBucuresti.rasarit().getHour() - laParis.rasarit().getHour()).isEqualTo(1);
    }

    @Test
    @DisplayName("ora de vară e luată în calcul")
    void tineContDeOraDeVara() {
        InfoSoare iarna = serviciu.calculeaza(PARIS_LAT, PARIS_LON, LocalDate.of(2026, 12, 21), PARIS);
        InfoSoare vara = serviciu.calculeaza(PARIS_LAT, PARIS_LON, LocalDate.of(2026, 6, 21), PARIS);

        assertThat(iarna.rasarit().getOffset().getTotalSeconds()).isEqualTo(3600);   // CET
        assertThat(vara.rasarit().getOffset().getTotalSeconds()).isEqualTo(7200);    // CEST
    }

    @Test
    @DisplayName("ziua e mult mai lungă la solstițiul de vară decât la cel de iarnă")
    void zileleSolstitiilorDifera() {
        InfoSoare iarna = serviciu.calculeaza(PARIS_LAT, PARIS_LON, LocalDate.of(2026, 12, 21), PARIS);
        InfoSoare vara = serviciu.calculeaza(PARIS_LAT, PARIS_LON, LocalDate.of(2026, 6, 21), PARIS);

        long oreIarna = Duration.between(iarna.rasarit(), iarna.apus()).toHours();
        long oreVara = Duration.between(vara.rasarit(), vara.apus()).toHours();

        assertThat(oreIarna).isBetween(7L, 9L);
        assertThat(oreVara).isBetween(15L, 17L);
        assertThat(oreVara).isGreaterThan(oreIarna);
    }

    @Test
    @DisplayName("ora de aur de dimineață încadrează răsăritul")
    void oraDeAurDimineataInconjoaraRasaritul() {
        InfoSoare info = serviciu.calculeaza(
                46.7712, 23.5892, LocalDate.of(2026, 8, 27), BUCURESTI);

        assertThat(info.oraDeAurDimineataStart()).isBefore(info.rasarit());
        assertThat(info.oraDeAurDimineataStop()).isAfter(info.rasarit());
        assertThat(info.oraDeAurSearaStart()).isBefore(info.apus());
        assertThat(info.oraDeAurSearaStop()).isAfter(info.apus());
    }
}
