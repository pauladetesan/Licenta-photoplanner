package ro.utcn.photoplanner.controller;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ro.utcn.photoplanner.model.Vizibilitate;

import static org.assertj.core.api.Assertions.assertThat;

/** Validarea formularului de locație — coordonatele imposibile nu trebuie să ajungă în baza de date. */
class LocatieFormTest {

    private static Validator validator;

    @BeforeAll
    static void pregateste() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    private static LocatieForm formularValid() {
        LocatieForm f = new LocatieForm();
        f.setNume("Piața Unirii");
        f.setLatitudine(46.7712);
        f.setLongitudine(23.5892);
        f.setVizibilitate(Vizibilitate.PUBLICA);
        return f;
    }

    private static boolean areEroarePe(LocatieForm f, String camp) {
        return validator.validate(f).stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals(camp));
    }

    @Test
    void formularulValidTrece() {
        assertThat(validator.validate(formularValid())).isEmpty();
    }

    @DisplayName("latitudinea trebuie să fie între -90 și 90")
    @ParameterizedTest(name = "lat={0}")
    @ValueSource(doubles = {999, 91, -91, 180})
    void latitudineImposibilaEsteRespinsa(double lat) {
        LocatieForm f = formularValid();
        f.setLatitudine(lat);
        assertThat(areEroarePe(f, "latitudine")).isTrue();
    }

    @DisplayName("latitudinile valide trec, inclusiv la limite")
    @ParameterizedTest(name = "lat={0}")
    @ValueSource(doubles = {-90, -45.5, 0, 46.7712, 90})
    void latitudineValidaTrece(double lat) {
        LocatieForm f = formularValid();
        f.setLatitudine(lat);
        assertThat(areEroarePe(f, "latitudine")).isFalse();
    }

    @DisplayName("longitudinea trebuie să fie între -180 și 180")
    @ParameterizedTest(name = "lon={0}")
    @ValueSource(doubles = {500, 181, -181})
    void longitudineImposibilaEsteRespinsa(double lon) {
        LocatieForm f = formularValid();
        f.setLongitudine(lon);
        assertThat(areEroarePe(f, "longitudine")).isTrue();
    }

    @DisplayName("longitudinile valide trec, inclusiv la limite")
    @ParameterizedTest(name = "lon={0}")
    @ValueSource(doubles = {-180, -73.9969, 0, 180})
    void longitudineValidaTrece(double lon) {
        LocatieForm f = formularValid();
        f.setLongitudine(lon);
        assertThat(areEroarePe(f, "longitudine")).isFalse();
    }

    @DisplayName("numele nu poate lipsi sau fi doar spații")
    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t"})
    void numeGolEsteRespins(String nume) {
        LocatieForm f = formularValid();
        f.setNume(nume);
        assertThat(areEroarePe(f, "nume")).isTrue();
    }

    @Test
    void coordonateleLipsaSuntRespinse() {
        LocatieForm f = formularValid();
        f.setLatitudine(null);
        f.setLongitudine(null);

        assertThat(areEroarePe(f, "latitudine")).isTrue();
        assertThat(areEroarePe(f, "longitudine")).isTrue();
    }

    @DisplayName("orientarea, dacă e dată, trebuie să fie între 0 și 359")
    @ParameterizedTest(name = "orientare={0}")
    @ValueSource(ints = {-1, 360, 720})
    void orientareImposibilaEsteRespinsa(int orientare) {
        LocatieForm f = formularValid();
        f.setOrientareScena(orientare);
        assertThat(areEroarePe(f, "orientareScena")).isTrue();
    }

    @Test
    void orientareaEsteOptionala() {
        LocatieForm f = formularValid();
        f.setOrientareScena(null);
        assertThat(areEroarePe(f, "orientareScena")).isFalse();
    }

    @Test
    void numelePreaLungEsteRespins() {
        LocatieForm f = formularValid();
        f.setNume("x".repeat(121));
        assertThat(areEroarePe(f, "nume")).isTrue();
    }
}
