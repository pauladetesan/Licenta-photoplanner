package ro.utcn.photoplanner.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import ro.utcn.photoplanner.model.TokenResetare;
import ro.utcn.photoplanner.model.Utilizator;
import ro.utcn.photoplanner.repository.TokenResetareRepository;
import ro.utcn.photoplanner.repository.UtilizatorRepository;
import ro.utcn.photoplanner.service.ResetareParolaService;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Schimbarea parolei din pagina contului.
 * <p>
 * Până acum singura cale spre o parolă nouă era linkul de pe email, adică — cât timp expeditorul
 * implicit doar scrie linkul în log — nicio cale.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SchimbareParolaTest {

    private static final String EMAIL = "schimba@example.com";
    private static final String VECHE = "parolaVeche1";
    private static final String NOUA = "parolaNoua123";

    @Autowired private MockMvc mockMvc;
    @Autowired private UtilizatorRepository utilizatorRepository;
    @Autowired private TokenResetareRepository tokenRepository;
    @Autowired private ResetareParolaService resetareParolaService;
    @Autowired private PasswordEncoder passwordEncoder;

    private Utilizator utilizator;

    @BeforeEach
    void pregateste() {
        curata();

        utilizator = new Utilizator();
        utilizator.setEmail(EMAIL);
        utilizator.setNumeAfisat("Cine Schimbă");
        utilizator.setParola(passwordEncoder.encode(VECHE));
        utilizator = utilizatorRepository.save(utilizator);
    }

    @AfterEach
    void curata() {
        utilizatorRepository.findByEmail(EMAIL).ifPresent(u -> {
            tokenRepository.deleteAll(tokenRepository.findByUtilizator(u));
            utilizatorRepository.delete(u);
        });
    }

    /** Trimite formularul de schimbare a parolei. */
    private org.springframework.test.web.servlet.ResultActions trimite(
            String curenta, String noua, String repetata) throws Exception {
        return mockMvc.perform(post("/cont/parola").with(csrf())
                .param("parolaCurenta", curenta)
                .param("parolaNoua", noua)
                .param("parolaRepetata", repetata));
    }

    /** Recitește contul din baza de date, ca să vedem ce s-a salvat de fapt. */
    private Utilizator reincarcat() {
        return utilizatorRepository.findByEmail(EMAIL).orElseThrow();
    }

    @Test
    @DisplayName("Cu parola curentă corectă, parola se schimbă")
    @WithMockUser(username = EMAIL)
    void parolaSeSchimba() throws Exception {
        trimite(VECHE, NOUA, NOUA)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cont?parola=schimbata"));

        assertThat(passwordEncoder.matches(NOUA, reincarcat().getParola())).isTrue();
        assertThat(passwordEncoder.matches(VECHE, reincarcat().getParola())).isFalse();
    }

    @Test
    @DisplayName("Fără parola curentă corectă nu se schimbă nimic")
    @WithMockUser(username = EMAIL)
    void parolaCurentaGresitaNuSchimbaNimic() throws Exception {
        trimite("altceva1234", NOUA, NOUA)
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("eroareParola"));

        assertThat(passwordEncoder.matches(VECHE, reincarcat().getParola())).isTrue();
    }

    @Test
    @DisplayName("Cele două parole noi trebuie să coincidă")
    @WithMockUser(username = EMAIL)
    void parolelNoiTrebuieSaCoincida() throws Exception {
        trimite(VECHE, NOUA, "parolaAltaTotal9")
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("eroareParola"));

        assertThat(passwordEncoder.matches(VECHE, reincarcat().getParola())).isTrue();
    }

    @Test
    @DisplayName("Parola nouă prea scurtă e respinsă și pe server")
    @WithMockUser(username = EMAIL)
    void parolaNouaScurtaEsteRespinsa() throws Exception {
        trimite(VECHE, "scurta", "scurta")
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("eroareParola"));

        assertThat(passwordEncoder.matches(VECHE, reincarcat().getParola())).isTrue();
    }

    @Test
    @DisplayName("Parola nouă nu poate fi aceeași cu cea veche")
    @WithMockUser(username = EMAIL)
    void parolaNouaNuPoateFiCeaVeche() throws Exception {
        trimite(VECHE, VECHE, VECHE)
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("eroareParola"));
    }

    /**
     * Cineva care a apucat să ceară un link de resetare nu trebuie să poată intra peste parola
     * nouă folosindu-l după schimbare.
     */
    @Test
    @DisplayName("Schimbarea parolei anulează linkurile de resetare în așteptare")
    @WithMockUser(username = EMAIL)
    void schimbareaAnuleazaLinkurileDeResetare() throws Exception {
        resetareParolaService.cereResetare(EMAIL);

        TokenResetare inainte = tokenRepository.findByUtilizator(utilizator).getFirst();
        assertThat(inainte.esteValabil()).isTrue();

        trimite(VECHE, NOUA, NOUA).andExpect(status().is3xxRedirection());

        TokenResetare dupa = tokenRepository.findById(inainte.getId()).orElseThrow();
        assertThat(dupa.esteValabil()).isFalse();
        assertThat(dupa.getDataFolosirii()).isNotNull().isBefore(LocalDateTime.now().plusMinutes(1));
    }

    @Test
    @DisplayName("Schimbarea parolei cere autentificare")
    void anonimNuPoateSchimbaParola() throws Exception {
        trimite(VECHE, NOUA, NOUA).andExpect(status().is3xxRedirection());

        assertThat(passwordEncoder.matches(VECHE, reincarcat().getParola())).isTrue();
    }
}
