package ro.utcn.photoplanner.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import ro.utcn.photoplanner.model.Utilizator;
import ro.utcn.photoplanner.repository.UtilizatorRepository;
import ro.utcn.photoplanner.service.UtilizatorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Crearea contului.
 * <p>
 * Câmpurile veneau direct ca {@code @RequestParam}, fără verificare pe server: atributele
 * {@code type="email"} și {@code required} din pagină opresc un browser, dar nu și o cerere
 * trimisă de-a dreptul. Se creau astfel conturi cu adresa „nu-e-email” sau cu numele gol —
 * conturi care nu mai pot fi recuperate niciodată, fiindcă linkul de resetare a parolei nu
 * are unde să ajungă.
 */
@SpringBootTest
@AutoConfigureMockMvc
class InregistrareControllerTest {

    private static final String EMAIL = "nou@example.com";
    private static final String OCUPAT = "ocupat@example.com";
    private static final String PAROLA = "parolaDeTest1";

    @Autowired private MockMvc mockMvc;
    @Autowired private UtilizatorRepository utilizatorRepository;
    @Autowired private UtilizatorService utilizatorService;

    /*
     * Numărul de conturi de la care pornim. Baza e comună întregii rulări, iar alte clase de
     * test își lasă acolo utilizatorii lor — de aceea verificăm diferența, nu totalul.
     */
    private long conturiLaInceput;

    @BeforeEach
    void pregateste() {
        curata();
        conturiLaInceput = utilizatorRepository.count();
    }

    @AfterEach
    void curata() {
        utilizatorRepository.findByEmail(EMAIL).ifPresent(utilizatorRepository::delete);
        utilizatorRepository.findByEmail(OCUPAT).ifPresent(utilizatorRepository::delete);
    }

    /** Câte conturi s-au adăugat de la începutul testului. */
    private long conturiNoi() {
        return utilizatorRepository.count() - conturiLaInceput;
    }

    /** Trimite formularul așa cum l-ar trimite pagina, cu valorile date. */
    private org.springframework.test.web.servlet.ResultActions trimite(
            String email, String parola, String nume) throws Exception {
        return mockMvc.perform(post("/inregistrare").with(csrf())
                .param("email", email)
                .param("parola", parola)
                .param("numeAfisat", nume));
    }

    @Test
    @DisplayName("Un formular corect creează contul")
    void formularulCorectCreeazaContul() throws Exception {
        trimite(EMAIL, PAROLA, "Nume Corect")
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?cont=creat"));

        assertThat(utilizatorRepository.findByEmail(EMAIL)).isPresent();
    }

    @ParameterizedTest
    @ValueSource(strings = {"nu-e-email", "fara@domeniu", "@example.com", "spatiu in@mijloc.ro", "doua@@a.ro", ""})
    @DisplayName("O adresă care nu e adresă nu creează cont")
    void adreseleInvalideSuntRespinse(String email) throws Exception {
        trimite(email, PAROLA, "Nume Corect")
                .andExpect(status().isOk())
                .andExpect(view().name("inregistrare"))
                .andExpect(model().attributeHasFieldErrors("inregistrareForm", "email"));

        assertThat(conturiNoi()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "   "})
    @DisplayName("Numele afișat nu poate fi gol")
    void numeleGolEsteRespins(String nume) throws Exception {
        trimite(EMAIL, PAROLA, nume)
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("inregistrareForm", "numeAfisat"));

        assertThat(utilizatorRepository.findByEmail(EMAIL)).isEmpty();
    }

    @Test
    @DisplayName("Parola prea scurtă este respinsă și pe server, nu doar în browser")
    void parolaScurtaEsteRespinsa() throws Exception {
        trimite(EMAIL, "scurta", "Nume Corect")
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("inregistrareForm", "parola"));

        assertThat(utilizatorRepository.findByEmail(EMAIL)).isEmpty();
    }

    @Test
    @DisplayName("Adresa deja folosită dă eroare pe câmpul ei, nu pe formular")
    void adresaOcupataDaEroarePeCamp() throws Exception {
        utilizatorService.inregistreaza(OCUPAT, PAROLA, "Primul");

        trimite(OCUPAT, PAROLA, "Al doilea")
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("inregistrareForm", "email"));

        assertThat(conturiNoi()).isOne();
    }

    @Test
    @DisplayName("Adresa se compară fără să conteze majusculele")
    void adresaSeNormalizeaza() throws Exception {
        utilizatorService.inregistreaza(OCUPAT, PAROLA, "Primul");

        trimite("Ocupat@Example.COM", PAROLA, "Al doilea")
                .andExpect(model().attributeHasFieldErrors("inregistrareForm", "email"));

        assertThat(conturiNoi()).isOne();
    }

    /*
     * Serviciul e ultima poartă înaintea bazei de date. Formularul apără pagina, dar orice alt
     * apelator — un import, o comandă de administrare scrisă mai târziu — trece direct pe aici.
     */
    @Test
    @DisplayName("Serviciul refuză singur numele gol și adresa lipsă")
    void serviciulSeAparaSingur() {
        assertThatThrownBy(() -> utilizatorService.inregistreaza(EMAIL, PAROLA, "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Numele afișat");

        assertThatThrownBy(() -> utilizatorService.inregistreaza("  ", PAROLA, "Nume"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");

        assertThat(conturiNoi()).isZero();
    }

    @Test
    @DisplayName("Numele afișat se salvează fără spațiile de la capete")
    void numeleSeCurataDeSpatii() throws Exception {
        trimite(EMAIL, PAROLA, "  Nume Cu Spatii  ")
                .andExpect(status().is3xxRedirection());

        Utilizator salvat = utilizatorRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(salvat.getNumeAfisat()).isEqualTo("Nume Cu Spatii");
    }
}
