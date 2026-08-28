package ro.utcn.photoplanner.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import ro.utcn.photoplanner.model.Fotografie;
import ro.utcn.photoplanner.model.Locatie;
import ro.utcn.photoplanner.model.Utilizator;
import ro.utcn.photoplanner.model.Vizibilitate;
import ro.utcn.photoplanner.repository.FotografieRepository;
import ro.utcn.photoplanner.repository.LocatieRepository;
import ro.utcn.photoplanner.repository.SesiuneFotoRepository;
import ro.utcn.photoplanner.repository.UtilizatorRepository;
import ro.utcn.photoplanner.service.FotografieService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cine are voie să vadă fotografiile.
 * <p>
 * Id-urile fotografiilor sunt numere consecutive, deci ușor de ghicit: singurul lucru care
 * ține pozele locațiilor private ascunse e verificarea din server. Testul rulează pe tot
 * contextul aplicației, cu lanțul real de securitate, nu pe servicii simulate — altfel nu
 * s-ar verifica tocmai partea care contează.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FotografieControllerTest {

    private static final String PROPRIETAR = "proprietar@example.com";
    private static final String STRAIN = "strain@example.com";

    @Autowired private MockMvc mockMvc;
    @Autowired private UtilizatorRepository utilizatorRepository;
    @Autowired private LocatieRepository locatieRepository;
    @Autowired private FotografieRepository fotografieRepository;
    @Autowired private SesiuneFotoRepository sesiuneRepository;
    @Autowired private FotografieService fotografieService;
    @Autowired private PasswordEncoder passwordEncoder;

    private Locatie locatiePublica;
    private Locatie locatiePrivata;
    private Fotografie pozaPublica;
    private Fotografie pozaPrivata;

    @BeforeEach
    void pregateste() {
        // Curățenie completă: fișierele de pe disc nu se anulează singure între teste.
        locatieRepository.findAll().forEach(fotografieService::stergeToate);
        sesiuneRepository.deleteAll();
        locatieRepository.deleteAll();
        utilizatorRepository.deleteAll();

        Utilizator proprietar = utilizator(PROPRIETAR, "Proprietarul");
        utilizator(STRAIN, "Străinul");

        locatiePublica = locatie(proprietar, Vizibilitate.PUBLICA, "Loc public");
        locatiePrivata = locatie(proprietar, Vizibilitate.PRIVATA, "Loc ascuns");

        pozaPublica = fotografieService.adauga(locatiePublica, imagine());
        pozaPrivata = fotografieService.adauga(locatiePrivata, imagine());
    }

    private Utilizator utilizator(String email, String nume) {
        Utilizator u = new Utilizator();
        u.setEmail(email);
        u.setNumeAfisat(nume);
        u.setParola(passwordEncoder.encode("parolaDeTest1"));
        return utilizatorRepository.save(u);
    }

    /** Locațiile se scriu direct, ca să nu pornim căutarea de fus orar la fiecare test. */
    private Locatie locatie(Utilizator autor, Vizibilitate vizibilitate, String nume) {
        Locatie l = new Locatie();
        l.setAutor(autor);
        l.setNume(nume);
        l.setLatitudine(46.77);
        l.setLongitudine(23.59);
        l.setVizibilitate(vizibilitate);
        l.setFusOrar("Europe/Bucharest");
        return locatieRepository.save(l);
    }

    private static MockMultipartFile imagine() {
        try (ByteArrayOutputStream iesire = new ByteArrayOutputStream()) {
            ImageIO.write(new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB), "jpg", iesire);
            return new MockMultipartFile("poza", "test.jpg", MediaType.IMAGE_JPEG_VALUE,
                    iesire.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Nu pot genera imaginea de test", e);
        }
    }

    // --- locație publică: pozele se văd de oricine ---

    @Test
    @WithAnonymousUser
    @DisplayName("poza unei locații publice se vede fără autentificare")
    void pozaPublicaSeVedeNelogat() throws Exception {
        mockMvc.perform(get("/poze/{id}", pozaPublica.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG));

        mockMvc.perform(get("/poze/{id}/mica", pozaPublica.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG));
    }

    @Test
    @WithMockUser(username = STRAIN)
    @DisplayName("poza unei locații publice se vede și de alt utilizator")
    void pozaPublicaSeVedeDeAltcineva() throws Exception {
        mockMvc.perform(get("/poze/{id}", pozaPublica.getId())).andExpect(status().isOk());
        mockMvc.perform(get("/poze/{id}/mica", pozaPublica.getId())).andExpect(status().isOk());
    }

    // --- locație privată: doar autorul ---

    @Test
    @WithMockUser(username = PROPRIETAR)
    @DisplayName("autorul își vede poza de la locația privată")
    void pozaPrivataSeVedeDeAutor() throws Exception {
        mockMvc.perform(get("/poze/{id}", pozaPrivata.getId())).andExpect(status().isOk());
        mockMvc.perform(get("/poze/{id}/mica", pozaPrivata.getId())).andExpect(status().isOk());
    }

    /**
     * 404, nu 403: un 403 ar confirma că fotografia există, ceea ce spune deja ceva despre
     * locația privată a altcuiva.
     */
    @Test
    @WithAnonymousUser
    @DisplayName("poza unei locații private dă 404 unui vizitator nelogat, nu 403")
    void pozaPrivataAscunsaDeNelogat() throws Exception {
        mockMvc.perform(get("/poze/{id}", pozaPrivata.getId())).andExpect(status().isNotFound());
        mockMvc.perform(get("/poze/{id}/mica", pozaPrivata.getId())).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = STRAIN)
    @DisplayName("poza unei locații private dă 404 altui utilizator, nu 403")
    void pozaPrivataAscunsaDeAltcineva() throws Exception {
        mockMvc.perform(get("/poze/{id}", pozaPrivata.getId())).andExpect(status().isNotFound());
        mockMvc.perform(get("/poze/{id}/mica", pozaPrivata.getId())).andExpect(status().isNotFound());
    }

    @Test
    @WithAnonymousUser
    @DisplayName("o fotografie inexistentă dă 404")
    void pozaInexistenta() throws Exception {
        mockMvc.perform(get("/poze/{id}", 999999L)).andExpect(status().isNotFound());
    }

    // --- ștergere ---

    @Test
    @WithMockUser(username = STRAIN)
    @DisplayName("altcineva nu poate șterge fotografia: 403, iar fișierul rămâne")
    void strainulNuStergePoza() throws Exception {
        mockMvc.perform(post("/poze/{id}/sterge", pozaPublica.getId()).with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(fotografieRepository.findById(pozaPublica.getId())).isPresent();
    }

    @Test
    @WithMockUser(username = PROPRIETAR)
    @DisplayName("autorul își poate șterge fotografia")
    void autorulStergePoza() throws Exception {
        mockMvc.perform(post("/poze/{id}/sterge", pozaPublica.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(fotografieRepository.findById(pozaPublica.getId())).isEmpty();
    }

    // --- încărcare ---

    @Test
    @WithMockUser(username = STRAIN)
    @DisplayName("nu poți încărca fotografii la locația altcuiva")
    void strainulNuIncarca() throws Exception {
        long inainte = fotografieRepository.countByLocatie(locatiePublica);

        mockMvc.perform(multipart("/locatii/{id}/poze", locatiePublica.getId())
                        .file(imagine()).with(csrf()))
                .andExpect(status().isForbidden());

        assertThat(fotografieRepository.countByLocatie(locatiePublica)).isEqualTo(inainte);
    }

    @Test
    @WithAnonymousUser
    @DisplayName("un vizitator nelogat nu poate încărca fotografii")
    void nelogatulNuIncarca() throws Exception {
        mockMvc.perform(multipart("/locatii/{id}/poze", locatiePublica.getId())
                        .file(imagine()).with(csrf()))
                .andExpect(status().is3xxRedirection());  // trimis la autentificare
    }

    @Test
    @WithMockUser(username = PROPRIETAR)
    @DisplayName("autorul poate încărca o fotografie la locația lui")
    void autorulIncarca() throws Exception {
        long inainte = fotografieRepository.countByLocatie(locatiePublica);

        mockMvc.perform(multipart("/locatii/{id}/poze", locatiePublica.getId())
                        .file(imagine()).with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(fotografieRepository.countByLocatie(locatiePublica)).isEqualTo(inainte + 1);
    }
}
