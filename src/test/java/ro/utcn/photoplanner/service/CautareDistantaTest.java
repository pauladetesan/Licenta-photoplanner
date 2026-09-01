package ro.utcn.photoplanner.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.security.crypto.password.PasswordEncoder;
import ro.utcn.photoplanner.model.Locatie;
import ro.utcn.photoplanner.model.Utilizator;
import ro.utcn.photoplanner.model.Vizibilitate;
import ro.utcn.photoplanner.repository.LocatieRepository;
import ro.utcn.photoplanner.repository.UtilizatorRepository;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Filtrul „pe o rază de X km” din interogare.
 * <p>
 * Distanța se calculează în două locuri: în interogare, ca să se poată filtra înainte de
 * paginare, și în {@link LocatieService#distantaKm} pentru numărul afișat în listă. Interogarea
 * folosea legea cosinusurilor, iar Java haversine — două formule diferite pentru aceeași
 * mărime, și nimic nu verifica dacă sunt de acord. Acum amândouă sunt haversine, iar testele de
 * aici țin lucrurile așa.
 */
@SpringBootTest
class CautareDistantaTest {

    /** Punctul de referință: centrul Clujului. */
    private static final double LAT = 46.7712;
    private static final double LON = 23.5892;

    private static final String EMAIL = "distanta@example.com";

    @Autowired private LocatieService locatieService;
    @Autowired private LocatieRepository locatieRepository;
    @Autowired private UtilizatorRepository utilizatorRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Utilizator autor;

    @BeforeEach
    void pregateste() {
        curata();

        autor = new Utilizator();
        autor.setEmail(EMAIL);
        autor.setNumeAfisat("Test Distanță");
        autor.setParola(passwordEncoder.encode("parolaDeTest1"));
        autor = utilizatorRepository.save(autor);
    }

    @AfterEach
    void curata() {
        utilizatorRepository.findByEmail(EMAIL).ifPresent(u -> {
            locatieRepository.deleteAll(locatieRepository.findByAutorOrderByDataAdaugareDesc(u));
            utilizatorRepository.delete(u);
        });
    }

    /** Salvează o locație publică la coordonatele date, fără să treacă prin căutarea fusului orar. */
    private Locatie locatie(String nume, double lat, double lon) {
        Locatie locatie = new Locatie();
        locatie.setNume(nume);
        locatie.setLatitudine(lat);
        locatie.setLongitudine(lon);
        locatie.setVizibilitate(Vizibilitate.PUBLICA);
        locatie.setFusOrar("Europe/Bucharest");
        locatie.setAutor(autor);
        return locatieRepository.save(locatie);
    }

    private List<String> numeGasite(double raza) {
        Page<Locatie> pagina = locatieService.cauta(
                new CriteriiCautare(null, Set.of(), LAT, LON, raza), 0);
        return pagina.getContent().stream().map(Locatie::getNume).toList();
    }

    /**
     * Invariantul care contează: orice locație întoarsă de interogare trebuie să fie, după
     * formula din Java, chiar în raza cerută. Dacă cele două formule s-ar despărți, aici s-ar
     * vedea — o locație ar fi afișată cu „38 km” sub un filtru de 35 km.
     */
    private void toateSuntInRaza(double raza) {
        Page<Locatie> pagina = locatieService.cauta(
                new CriteriiCautare(null, Set.of(), LAT, LON, raza), 0);

        assertThat(pagina.getContent()).allSatisfy(l -> assertThat(
                LocatieService.distantaKm(LAT, LON, l.getLatitudine(), l.getLongitudine()))
                .isLessThanOrEqualTo(raza + 0.001));
    }

    @Test
    @DisplayName("Raza de 50 km ia locul apropiat și îl lasă afară pe cel de la 100 km")
    void razaDeCincizeciDeKilometri() {
        locatie("La zece km", 46.8610, 23.5892);      // ~10 km spre nord
        locatie("La o suta de km", 47.6700, 23.5892); // ~100 km spre nord

        assertThat(numeGasite(50.0)).contains("La zece km").doesNotContain("La o suta de km");
        toateSuntInRaza(50.0);
    }

    /**
     * Distanțele mici, unde se presupune că legea cosinusurilor ar ceda.
     * <p>
     * Testul <em>nu</em> face diferența între cele două formule — s-a verificat, trece la fel și
     * cu legea cosinusurilor, fiindcă în {@code double} eroarea ei la 50 de metri e de ordinul
     * centimetrilor. Rămâne aici fiindcă acoperă oricum filtrul la o scară la care nu era
     * încercat deloc.
     */
    @Test
    @DisplayName("La sub o sută de metri filtrul încă separă corect")
    void distanteFoarteMici() {
        locatie("La cincizeci de metri", LAT + 0.0005, LON);  // ~56 m
        locatie("La doua sute de metri", LAT + 0.0020, LON);  // ~222 m

        List<String> gasite = numeGasite(0.1); // 100 m

        assertThat(gasite).contains("La cincizeci de metri");
        assertThat(gasite).doesNotContain("La doua sute de metri");
        toateSuntInRaza(0.1);
    }

    @Test
    @DisplayName("Locația exact în punctul căutat intră la orice rază pozitivă")
    void punctulExact() {
        locatie("Chiar aici", LAT, LON);

        assertThat(numeGasite(0.001)).contains("Chiar aici");
    }

    @Test
    @DisplayName("Interogarea și formula din Java dau același verdict la limita razei")
    void aceeasiFormulaInAmbeleLocuri() {
        Locatie tinta = locatie("La limita", 46.8610, 23.5892);

        double distanta = LocatieService.distantaKm(
                LAT, LON, tinta.getLatitudine(), tinta.getLongitudine());

        // Puțin peste distanța calculată în Java: interogarea trebuie s-o includă.
        assertThat(numeGasite(distanta + 0.01)).contains("La limita");

        // Puțin sub: interogarea trebuie s-o excludă. Ar pica dacă cele două formule ar diferi
        // cu mai mult de 10 metri.
        assertThat(numeGasite(distanta - 0.01)).doesNotContain("La limita");
    }
}
