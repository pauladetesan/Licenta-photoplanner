package ro.utcn.photoplanner.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import ro.utcn.photoplanner.model.FactorScor;
import ro.utcn.photoplanner.model.Locatie;
import ro.utcn.photoplanner.model.MomentZi;
import ro.utcn.photoplanner.model.PonderiScor;
import ro.utcn.photoplanner.model.Tema;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Scorul unui moment.
 * <p>
 * Nu are nevoie de Spring: {@link ScorService} nu atinge baza de date, iar prognoza vine printr-un
 * {@link VremeService} înlocuit aici cu unul care întoarce mereu aceeași vreme. Așa se poate
 * compara efectul unei singure schimbări — de pildă doar acoperirea cu nori — fără să depindem
 * de ce zi e azi sau de ce răspunde Open-Meteo.
 */
class ScorServiceTest {

    private static final double LAT = 46.7712;
    private static final double LON = 23.6236;
    private static final ZoneId ZONA = ZoneId.of("Europe/Bucharest");

    /** Prognoză fixă, ca să putem varia un singur lucru odată. */
    private static class VremeFixa extends VremeService {
        private final VremeMoment vreme;

        VremeFixa(VremeMoment vreme) {
            super(false, "http://exemplu.invalid");
            this.vreme = vreme;
        }

        @Override
        public VremeMoment laUnMoment(double latitudine, double longitudine, LocalDate data,
                                       ZoneId zona, ZonedDateTime moment) {
            return vreme;
        }
    }

    private static ScorService serviciu(VremeMoment vreme) {
        return new ScorService(new SoareService(), new LunaService(),
                new VremeFixa(vreme), new LuminaService());
    }

    /** Vremea cu acoperirea dată, fără ploaie, la 18 grade — ca să varieze doar norii. */
    private static VremeMoment nori(int procent) {
        return new VremeMoment(procent, 0, 18.0);
    }

    private static Locatie locatie(Integer orientareScena) {
        Locatie locatie = new Locatie();
        locatie.setNume("Cheile Turzii");
        locatie.setLatitudine(LAT);
        locatie.setLongitudine(LON);
        locatie.setFusOrar(ZONA.getId());
        locatie.setOrientareScena(orientareScena);
        return locatie;
    }

    private static PonderiScor ponderiImplicite() {
        return PonderiScor.implicite(null);
    }

    /** Nota dată norilor pentru primul moment de tipul cerut. */
    private static double notaNori(Tema tema, MomentZi moment, int procentNori) {
        List<ScorMoment> clasament = serviciu(nori(procentNori))
                .clasament(locatie(180), tema, ponderiImplicite(), 7);

        ScorMoment ales = clasament.stream()
                .filter(s -> s.moment() == moment)
                .findFirst()
                .orElseThrow(() -> new AssertionError("niciun moment de tipul " + moment));

        return ales.contributii().stream()
                .filter(c -> c.factor() == FactorScor.NORI)
                .findFirst()
                .orElseThrow(() -> new AssertionError("norii nu au contat"))
                .nota();
    }

    // --- forma curbelor ---

    @Test
    @DisplayName("Apropierea e maximă fix la valoarea ideală și scade în ambele părți")
    void apropiereaAreVarfulLaOptim() {
        assertThat(ScorService.apropiere(40, 40, 25)).isEqualTo(1.0);
        assertThat(ScorService.apropiere(20, 40, 25)).isEqualTo(ScorService.apropiere(60, 40, 25));
        assertThat(ScorService.apropiere(40, 40, 25))
                .isGreaterThan(ScorService.apropiere(70, 40, 25));
        assertThat(ScorService.apropiere(0, 40, 25)).isBetween(0.0, 1.0);
    }

    /**
     * Miezul modelului. Dacă scorul ar fi „mai puțin, mai bine”, cerul gol ar câștiga mereu —
     * iar asta ar fi greșit exact la momentul care contează cel mai mult.
     */
    @Test
    @DisplayName("La ora de aur, norii împrăștiați bat și cerul senin, și cel acoperit")
    void noriiImprastiatiInvingCerulGol() {
        double senin = notaNori(Tema.PEISAJ, MomentZi.ORA_AUR_SEARA, 0);
        double imprastiati = notaNori(Tema.PEISAJ, MomentZi.ORA_AUR_SEARA, 40);
        double acoperit = notaNori(Tema.PEISAJ, MomentZi.ORA_AUR_SEARA, 100);

        assertThat(imprastiati).isGreaterThan(senin);
        assertThat(imprastiati).isGreaterThan(acoperit);
    }

    @Test
    @DisplayName("Pentru portret în timpul zilei, cerul acoperit e un avantaj, nu o pedeapsă")
    void portretulPrefereLuminaDifuza() {
        double senin = notaNori(Tema.PORTRET, MomentZi.ZI, 10);
        double acoperit = notaNori(Tema.PORTRET, MomentZi.ZI, 90);

        assertThat(acoperit).isGreaterThan(senin);
    }

    @Test
    @DisplayName("Pentru astro, orice nor strică — acolo chiar e „mai puțin, mai bine”")
    void astroVreaCerSenin() {
        double senin = notaNori(Tema.ASTRO, MomentZi.NOAPTE, 0);
        double innorat = notaNori(Tema.ASTRO, MomentZi.NOAPTE, 60);

        assertThat(senin).isGreaterThan(innorat);
        assertThat(senin).isEqualTo(1.0);
    }

    // --- potrivirea momentului cu tema ---

    @Test
    @DisplayName("Astrofotografia nu primește niciodată un moment de zi, oricât de senin ar fi")
    void astroDoarNoaptea() {
        List<ScorMoment> clasament = serviciu(nori(0))
                .clasament(locatie(180), Tema.ASTRO, ponderiImplicite(), 7);

        assertThat(clasament).isNotEmpty();
        assertThat(clasament).allSatisfy(s -> assertThat(s.moment()).isEqualTo(MomentZi.NOAPTE));
    }

    @ParameterizedTest
    @EnumSource(Tema.class)
    @DisplayName("Scorul rămâne între 0 și 1 pentru orice temă")
    void scorulRamaneInInterval(Tema tema) {
        List<ScorMoment> clasament = serviciu(nori(45))
                .clasament(locatie(180), tema, ponderiImplicite(), 7);

        assertThat(clasament).isNotEmpty();
        assertThat(clasament).allSatisfy(s -> assertThat(s.scor()).isBetween(0.0, 1.0));
    }

    @Test
    @DisplayName("Clasamentul e ordonat, iar cel mai bun moment e primul din el")
    void celMaiBunEPrimul() {
        ScorService serviciu = serviciu(nori(40));
        List<ScorMoment> clasament = serviciu.clasament(locatie(180), Tema.PEISAJ, ponderiImplicite(), 7);
        Optional<ScorMoment> best = serviciu.celMaiBun(locatie(180), Tema.PEISAJ, ponderiImplicite(), 7);

        assertThat(clasament).isSortedAccordingTo((a, b) -> Double.compare(b.scor(), a.scor()));
        assertThat(best).isPresent();
        assertThat(best.orElseThrow().scor()).isEqualTo(clasament.getFirst().scor());
    }

    @Test
    @DisplayName("Nu se propune niciodată un moment care a trecut deja")
    void doarMomenteViitoare() {
        ZonedDateTime acum = ZonedDateTime.now(ZONA);

        assertThat(serviciu(nori(40)).clasament(locatie(180), Tema.PEISAJ, ponderiImplicite(), 7))
                .allSatisfy(s -> assertThat(s.cand()).isAfter(acum));
    }

    // --- factori care lipsesc ---

    @Test
    @DisplayName("Fără orientarea scenei, direcția luminii nu intră în calcul")
    void faraOrientareNuSeJudecaLumina() {
        List<ScorMoment> fara = serviciu(nori(40))
                .clasament(locatie(null), Tema.PEISAJ, ponderiImplicite(), 7);
        List<ScorMoment> cu = serviciu(nori(40))
                .clasament(locatie(180), Tema.PEISAJ, ponderiImplicite(), 7);

        assertThat(fara).allSatisfy(s -> assertThat(s.contributii())
                .noneMatch(c -> c.factor() == FactorScor.LUMINA));
        assertThat(cu).anySatisfy(s -> assertThat(s.contributii())
                .anyMatch(c -> c.factor() == FactorScor.LUMINA));
    }

    /**
     * Un factor care lipsește se scoate din sumă cu pondere cu tot. Dacă i-am da nota zero, o
     * locație fără orientarea scenei ar părea mai proastă decât e — pedepsită pentru o informație
     * care lipsește, nu pentru condiții slabe.
     */
    @Test
    @DisplayName("Lipsa unui factor nu trage scorul în jos")
    void lipsaUnuiFactorNuPedepseste() {
        double faraOrientare = serviciu(nori(40))
                .celMaiBun(locatie(null), Tema.PEISAJ, ponderiImplicite(), 7)
                .orElseThrow().scor();

        assertThat(faraOrientare).isGreaterThan(0.4);
    }

    @Test
    @DisplayName("Când prognoza lipsește cu totul, scorul nu devine zero")
    void faraPrognozaScorulRamaneRezonabil() {
        ScorService fara = serviciu(null);

        ScorMoment best = fara.celMaiBun(locatie(180), Tema.PEISAJ, ponderiImplicite(), 7)
                .orElseThrow();

        assertThat(best.scor()).isGreaterThan(0.0);
        assertThat(best.contributii()).noneMatch(c -> c.factor() == FactorScor.NORI);
    }

    // --- ponderile utilizatorului ---

    @Test
    @DisplayName("Un factor cu ponderea zero dispare din calcul")
    void pondereaZeroScoateFactorul() {
        PonderiScor ponderi = PonderiScor.implicite(null);
        ponderi.setPondere(FactorScor.NORI, 0);

        assertThat(serviciu(nori(40)).clasament(locatie(180), Tema.PEISAJ, ponderi, 7))
                .allSatisfy(s -> assertThat(s.contributii())
                        .noneMatch(c -> c.factor() == FactorScor.NORI));
    }

    @Test
    @DisplayName("Ponderile schimbă ordinea, nu doar numerele")
    void ponderileSchimbaClasamentul() {
        // Cineva căruia îi pasă numai de direcția luminii, față de cineva căruia îi pasă
        // numai de cer: n-au de ce să primească același răspuns.
        PonderiScor doarLumina = PonderiScor.implicite(null);
        PonderiScor doarNori = PonderiScor.implicite(null);
        for (FactorScor factor : FactorScor.values()) {
            doarLumina.setPondere(factor, factor == FactorScor.LUMINA ? 5 : 0);
            doarNori.setPondere(factor, factor == FactorScor.NORI ? 5 : 0);
        }

        ScorService serviciu = serviciu(nori(40));
        Locatie locatie = locatie(90);

        List<ScorMoment> dupaLumina = serviciu.clasament(locatie, Tema.PEISAJ, doarLumina, 7);
        List<ScorMoment> dupaNori = serviciu.clasament(locatie, Tema.PEISAJ, doarNori, 7);

        assertThat(dupaLumina.getFirst().scor()).isNotEqualTo(dupaNori.getFirst().scor());
    }

    @Test
    @DisplayName("Cu toate ponderile pe zero se revine la valorile implicite")
    void totulPeZeroRevineLaImplicite() {
        PonderiScor zero = PonderiScor.implicite(null);
        for (FactorScor factor : FactorScor.values()) {
            zero.setPondere(factor, 0);
        }
        assertThat(zero.totulPeZero()).isTrue();

        ScorMoment best = serviciu(nori(40))
                .celMaiBun(locatie(180), Tema.PEISAJ, zero, 7).orElseThrow();

        assertThat(best.scor()).isGreaterThan(0.0);
        assertThat(best.contributii()).isNotEmpty();
    }

    // --- afișare ---

    private static ScorMoment cuScorul(double scor) {
        return new ScorMoment(locatie(180), MomentZi.ORA_AUR_SEARA,
                ZonedDateTime.now(ZONA), scor, 1.0, List.of());
    }

    @Test
    @DisplayName("Scorul se scrie cu virgulă, ca în românește")
    void scorulSeScrieCuVirgula() {
        assertThat(cuScorul(0.8412).scorFormatat()).isEqualTo("0,84");
        assertThat(cuScorul(0.8412).procent()).isEqualTo(84);
        assertThat(cuScorul(1.0).scorFormatat()).isEqualTo("1,00");
    }

    /**
     * Pragurile sunt pentru câștigătorul unui interval, nu pentru un moment oarecare. Prima
     * variantă (0,75 / 0,55 / 0,35) dădea „Foarte bun” pentru toate cele cinci teme pe date
     * reale — o etichetă pe care o primește toată lumea nu spune nimic.
     */
    /**
     * Pe date reale au apărut, în același tabel, „0,85 (Bun)” și „0,85 (Foarte bun)”: numărul
     * se rotunjea la afișare, dar verdictul se lua după valoarea brută. Eticheta trebuie să se
     * potrivească întotdeauna cu cifra de lângă ea.
     */
    @ParameterizedTest
    @CsvSource({
            "0.8497, 0|85, Foarte bun",
            "0.8502, 0|85, Foarte bun",
            "0.8449, 0|84, Bun",
            "0.6999, 0|70, Bun",
            "0.6949, 0|69, Acceptabil",
            "0.4999, 0|50, Acceptabil",
            "0.4949, 0|49, Slab"
    })
    @DisplayName("Verdictul se potrivește mereu cu numărul afișat, nu cu cel brut")
    void verdictulSePotriveleCuNumarulAfisat(double scor, String afisatCuBara, String asteptat) {
        String afisat = afisatCuBara.replace('|', ',');

        assertThat(cuScorul(scor).scorFormatat()).isEqualTo(afisat);
        assertThat(cuScorul(scor).verdict()).isEqualTo(asteptat);
    }

    @ParameterizedTest
    @CsvSource({
            "0.95, Foarte bun",
            "0.85, Foarte bun",
            "0.84, Bun",
            "0.70, Bun",
            "0.69, Acceptabil",
            "0.50, Acceptabil",
            "0.49, Slab",
            "0.10, Slab"
    })
    @DisplayName("Verdictul urmează pragurile calibrate pentru cel mai bun moment")
    void verdictulUrmeazaPragurile(double scor, String asteptat) {
        assertThat(cuScorul(scor).verdict()).isEqualTo(asteptat);
    }
}
