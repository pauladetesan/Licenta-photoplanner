package ro.utcn.photoplanner.controller;

import org.hamcrest.Matchers;
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
import ro.utcn.photoplanner.model.Utilizator;
import ro.utcn.photoplanner.repository.UtilizatorRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Delogarea.
 * <p>
 * Paginile aveau un link {@code <a href="/logout">}, iar Spring Security nu ascultă decât pe
 * POST /logout cât timp protecția CSRF e pornită: apăsarea pe „Delogare” dădea 404 și lăsa
 * sesiunea deschisă. Nimeni nu putea ieși din cont. Testele de aici țin drumul deschis.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DelogareTest {

    private static final String EMAIL = "delogare@example.com";

    /** Paginile din care se poate ieși din cont — fiecare își desenează propriul rând de navigare. */
    private static final String[] PAGINI_CU_NAVIGARE = {
            "/", "/locatii", "/locatii/mele", "/favorite", "/sesiuni", "/cont"
    };

    @Autowired private MockMvc mockMvc;
    @Autowired private UtilizatorRepository utilizatorRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    /*
     * Paginile din spatele autentificării caută utilizatorul în baza de date după numele din
     * sesiune; un principal fals, fără rând în tabel, le-ar pica cu 500 înainte să apuce să
     * deseneze rândul de navigare pe care vrem să-l verificăm.
     */
    @BeforeEach
    void pregateste() {
        utilizatorRepository.findByEmail(EMAIL).ifPresent(utilizatorRepository::delete);

        Utilizator utilizator = new Utilizator();
        utilizator.setEmail(EMAIL);
        utilizator.setNumeAfisat("Cineva");
        utilizator.setParola(passwordEncoder.encode("parolaDeTest1"));
        utilizatorRepository.save(utilizator);
    }

    @AfterEach
    void curata() {
        utilizatorRepository.findByEmail(EMAIL).ifPresent(utilizatorRepository::delete);
    }

    @Test
    @DisplayName("POST /logout închide sesiunea și trimite la prima pagină")
    @WithMockUser(username = EMAIL)
    void postulDelogheaza() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(unauthenticated());
    }

    /**
     * Fără asta, greșeala de dinainte s-ar putea întoarce nevăzută: un GET pe /logout nu arată
     * ca o eroare, ci doar ca o pagină care nu face nimic.
     */
    @Test
    @DisplayName("GET /logout nu delogează pe nimeni — de aceea butonul trebuie să fie POST")
    @WithMockUser(username = EMAIL)
    void getulNuDelogheaza() throws Exception {
        mockMvc.perform(get("/logout"))
                .andExpect(authenticated());
    }

    @Test
    @DisplayName("Fiecare pagină arată un formular POST de delogare, nu un link")
    @WithMockUser(username = EMAIL)
    void toatePaginileAuFormularNuLink() throws Exception {
        for (String pagina : PAGINI_CU_NAVIGARE) {
            mockMvc.perform(get(pagina))
                    .andExpect(status().isOk())
                    .andExpect(content().string(Matchers.containsString("action=\"/logout\"")))
                    .andExpect(content().string(Matchers.not(
                            Matchers.containsString("href=\"/logout\""))));
        }
    }
}
