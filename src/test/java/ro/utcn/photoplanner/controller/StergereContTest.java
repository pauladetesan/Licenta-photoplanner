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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import ro.utcn.photoplanner.model.Comentariu;
import ro.utcn.photoplanner.model.Favorit;
import ro.utcn.photoplanner.model.Locatie;
import ro.utcn.photoplanner.model.MomentZi;
import ro.utcn.photoplanner.model.SesiuneFoto;
import ro.utcn.photoplanner.model.Utilizator;
import ro.utcn.photoplanner.model.Vizibilitate;
import ro.utcn.photoplanner.repository.ComentariuRepository;
import ro.utcn.photoplanner.repository.FavoritRepository;
import ro.utcn.photoplanner.repository.FotografieRepository;
import ro.utcn.photoplanner.repository.LocatieRepository;
import ro.utcn.photoplanner.repository.SesiuneFotoRepository;
import ro.utcn.photoplanner.repository.TokenResetareRepository;
import ro.utcn.photoplanner.repository.UtilizatorRepository;
import ro.utcn.photoplanner.service.FotografieService;
import ro.utcn.photoplanner.service.StergereContService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Ștergerea contului. E o operație fără drum de întoarcere, așa că se verifică două lucruri:
 * că șterge tot ce trebuie, și că nu se poate declanșa fără parolă.
 */
@SpringBootTest
@AutoConfigureMockMvc
class StergereContTest {

    private static final String PLEACA = "pleaca@example.com";
    private static final String RAMANE = "ramane@example.com";
    private static final String PAROLA = "parolaDeTest1";

    @Autowired private MockMvc mockMvc;
    @Autowired private UtilizatorRepository utilizatorRepository;
    @Autowired private LocatieRepository locatieRepository;
    @Autowired private ComentariuRepository comentariuRepository;
    @Autowired private FavoritRepository favoritRepository;
    @Autowired private SesiuneFotoRepository sesiuneRepository;
    @Autowired private TokenResetareRepository tokenRepository;
    @Autowired private FotografieRepository fotografieRepository;
    @Autowired private FotografieService fotografieService;
    @Autowired private StergereContService stergereContService;
    @Autowired private PasswordEncoder passwordEncoder;

    private Utilizator pleaca;
    private Utilizator ramane;
    private Locatie locatiaLui;
    private Locatie locatiaCeluilalt;

    @BeforeEach
    void pregateste() {
        locatieRepository.findAll().forEach(fotografieService::stergeToate);
        sesiuneRepository.deleteAll();
        favoritRepository.deleteAll();
        comentariuRepository.deleteAll();
        tokenRepository.deleteAll();
        locatieRepository.deleteAll();
        utilizatorRepository.deleteAll();

        pleaca = utilizator(PLEACA, "Cel care pleacă");
        ramane = utilizator(RAMANE, "Cel care rămâne");

        locatiaLui = locatie(pleaca, "Locul lui");
        locatiaCeluilalt = locatie(ramane, "Locul celuilalt");

        fotografieService.adauga(locatiaLui, imagine());

        // Ce a lăsat el pe la alții
        comentariu(pleaca, locatiaCeluilalt, "comentariul lui pe locul altuia");
        favorit(pleaca, locatiaCeluilalt);
        sesiune(pleaca, locatiaCeluilalt);

        // Ce au lăsat alții pe locația lui
        comentariu(ramane, locatiaLui, "comentariul altuia pe locul lui");
        favorit(ramane, locatiaLui);
        sesiune(ramane, locatiaLui);
    }

    private Utilizator utilizator(String email, String nume) {
        Utilizator u = new Utilizator();
        u.setEmail(email);
        u.setNumeAfisat(nume);
        u.setParola(passwordEncoder.encode(PAROLA));
        return utilizatorRepository.save(u);
    }

    private Locatie locatie(Utilizator autor, String nume) {
        Locatie l = new Locatie();
        l.setAutor(autor);
        l.setNume(nume);
        l.setLatitudine(46.77);
        l.setLongitudine(23.59);
        l.setVizibilitate(Vizibilitate.PUBLICA);
        l.setFusOrar("Europe/Bucharest");
        return locatieRepository.save(l);
    }

    private void comentariu(Utilizator autor, Locatie locatie, String text) {
        Comentariu c = new Comentariu();
        c.setAutor(autor);
        c.setLocatie(locatie);
        c.setContinut(text);
        comentariuRepository.save(c);
    }

    private void favorit(Utilizator utilizator, Locatie locatie) {
        Favorit f = new Favorit();
        f.setUtilizator(utilizator);
        f.setLocatie(locatie);
        favoritRepository.save(f);
    }

    private void sesiune(Utilizator utilizator, Locatie locatie) {
        SesiuneFoto s = new SesiuneFoto();
        s.setUtilizator(utilizator);
        s.setLocatie(locatie);
        s.setData(LocalDate.now().plusDays(3));
        s.setMoment(MomentZi.ORA_AUR_SEARA);
        sesiuneRepository.save(s);
    }

    private static MockMultipartFile imagine() {
        try (ByteArrayOutputStream iesire = new ByteArrayOutputStream()) {
            ImageIO.write(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB), "jpg", iesire);
            return new MockMultipartFile("poza", "t.jpg", MediaType.IMAGE_JPEG_VALUE, iesire.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // --- ce dispare ---

    @Test
    @DisplayName("ștergerea contului duce cu ea locațiile, pozele, comentariile, favoritele și sesiunile lui")
    void stergereaDuceTotulCuEa() {
        stergereContService.sterge(pleaca, false);

        assertThat(utilizatorRepository.findByEmail(PLEACA)).isEmpty();
        assertThat(locatieRepository.findById(locatiaLui.getId())).isEmpty();
        assertThat(fotografieRepository.count()).isZero();
        assertThat(comentariuRepository.findByAutor(pleaca)).isEmpty();
        assertThat(favoritRepository.findByUtilizator(pleaca)).isEmpty();
        assertThat(sesiuneRepository.countByUtilizator(pleaca)).isZero();
    }

    /**
     * Comentariile, favoritele și sesiunile altora atârnau de locația lui: fără ea nu mai au
     * la ce se referi, deci pleacă și ele.
     */
    @Test
    @DisplayName("ce au lăsat alții pe locația lui dispare odată cu locația")
    void ceAuLasatAltiiPeLocatiaLuiDispare() {
        stergereContService.sterge(pleaca, false);

        assertThat(comentariuRepository.findByAutor(ramane)).isEmpty();
        assertThat(favoritRepository.findByUtilizator(ramane)).isEmpty();
        assertThat(sesiuneRepository.countByUtilizator(ramane)).isZero();
    }

    @Test
    @DisplayName("celălalt cont și locația lui rămân neatinse")
    void celalaltContRamane() {
        stergereContService.sterge(pleaca, false);

        assertThat(utilizatorRepository.findByEmail(RAMANE)).isPresent();
        assertThat(locatieRepository.findById(locatiaCeluilalt.getId())).isPresent();
    }

    @Test
    @DisplayName("rezumatul spune corect ce urmează să dispară")
    void rezumatulEsteCorect() {
        StergereContService.Rezumat r = stergereContService.rezumat(pleaca);

        assertThat(r.locatii()).isEqualTo(1);
        assertThat(r.publice()).isEqualTo(1);
        assertThat(r.fotografii()).isEqualTo(1);
        assertThat(r.comentarii()).isEqualTo(1);
        assertThat(r.favorite()).isEqualTo(1);
        assertThat(r.sesiuni()).isEqualTo(1);
    }

    // --- varianta cu păstrarea locațiilor publice ---

    @Test
    @DisplayName("cu păstrare: locația publică rămâne, dar trece la contul anonim")
    void locatiaPublicaRamaneSubContAnonim() {
        stergereContService.sterge(pleaca, true);

        Locatie ramasa = locatieRepository.findById(locatiaLui.getId()).orElseThrow();
        // `autor` e LAZY: în afara unei sesiuni Hibernate se citește doar id-ul din proxy,
        // așa că restul câmpurilor se verifică pe entitatea încărcată separat.
        Utilizator anonim = utilizatorRepository.findByContSistemTrue().orElseThrow();

        assertThat(ramasa.getAutor().getId()).isEqualTo(anonim.getId());
        assertThat(ramasa.getAutor().getId()).isNotEqualTo(pleaca.getId());
        assertThat(anonim.isContSistem()).isTrue();
        assertThat(anonim.getNumeAfisat()).isEqualTo("Utilizator șters");
        assertThat(utilizatorRepository.findByEmail(PLEACA)).isEmpty();
    }

    /** Fotografiile sunt munca personală a autorului, nu date despre un loc: pleacă oricum. */
    @Test
    @DisplayName("cu păstrare: fotografiile dispar chiar dacă locația rămâne")
    void fotografiileDisparSiCuPastrare() {
        stergereContService.sterge(pleaca, true);

        assertThat(locatieRepository.findById(locatiaLui.getId())).isPresent();
        assertThat(fotografieRepository.count()).isZero();
    }

    @Test
    @DisplayName("cu păstrare: comentariile și favoritele altora de pe locație rămân")
    void ceAuLasatAltiiRamaneCandLocatiaRamane() {
        stergereContService.sterge(pleaca, true);

        assertThat(comentariuRepository.findByAutor(ramane)).hasSize(1);
        assertThat(favoritRepository.findByUtilizator(ramane)).hasSize(1);
        assertThat(sesiuneRepository.countByUtilizator(ramane)).isEqualTo(1);
    }

    @Test
    @DisplayName("cu păstrare: locațiile private tot dispar")
    void locatiilePrivateDisparSiCuPastrare() {
        Locatie privata = new Locatie();
        privata.setAutor(pleaca);
        privata.setNume("Ascunsa");
        privata.setLatitudine(46.0);
        privata.setLongitudine(23.0);
        privata.setVizibilitate(Vizibilitate.PRIVATA);
        privata.setFusOrar("Europe/Bucharest");
        privata = locatieRepository.save(privata);

        stergereContService.sterge(pleaca, true);

        assertThat(locatieRepository.findById(privata.getId())).isEmpty();
    }

    /** Contul-substitut nu e al nimănui: nu trebuie să se poată intra pe el. */
    @Test
    @DisplayName("contul anonim nu se poate autentifica")
    void contulAnonimNuSePoateAutentifica() {
        stergereContService.sterge(pleaca, true);

        Utilizator anonim = utilizatorRepository.findByContSistemTrue().orElseThrow();
        assertThat(anonim.isContSistem()).isTrue();

        org.springframework.security.core.userdetails.UserDetailsService serviciu =
                new ro.utcn.photoplanner.service.UtilizatorDetailsService(utilizatorRepository);

        assertThat(org.assertj.core.api.Assertions.catchThrowable(
                () -> serviciu.loadUserByUsername(anonim.getEmail())))
                .isInstanceOf(org.springframework.security.core.userdetails.UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("un singur cont anonim, oricâte conturi s-ar șterge")
    void unSingurContAnonim() {
        stergereContService.sterge(pleaca, true);
        stergereContService.sterge(ramane, true);

        assertThat(utilizatorRepository.findAll().stream()
                .filter(Utilizator::isContSistem).count()).isEqualTo(1);
    }

    // --- poarta de parolă ---

    @Test
    @WithMockUser(username = PLEACA)
    @DisplayName("fără parola corectă contul nu se șterge")
    void faraParolaCorectaNuSeSterge() throws Exception {
        mockMvc.perform(post("/cont/sterge").param("parola", "parola-gresita")
                        .param("pastreazaPublice", "false").with(csrf()))
                .andExpect(status().isOk());   // formularul se re-afișează cu eroare

        assertThat(utilizatorRepository.findByEmail(PLEACA)).isPresent();
        assertThat(locatieRepository.findById(locatiaLui.getId())).isPresent();
    }

    @Test
    @WithMockUser(username = PLEACA)
    @DisplayName("cu parola corectă contul se șterge")
    void cuParolaCorectaSeSterge() throws Exception {
        mockMvc.perform(post("/cont/sterge").param("parola", PAROLA)
                        .param("pastreazaPublice", "false").with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(utilizatorRepository.findByEmail(PLEACA)).isEmpty();
    }

    @Test
    @DisplayName("un vizitator nelogat nu poate șterge conturi")
    void nelogatulNuPoateSterge() throws Exception {
        mockMvc.perform(post("/cont/sterge").param("parola", PAROLA)
                        .param("pastreazaPublice", "false").with(csrf()))
                .andExpect(status().is3xxRedirection());   // trimis la autentificare

        assertThat(utilizatorRepository.findByEmail(PLEACA)).isPresent();
    }

    /** Fiecare își șterge doar contul lui: pagina lucrează pe cel autentificat, nu pe un id din cerere. */
    @Test
    @WithMockUser(username = RAMANE)
    @DisplayName("ștergerea acționează asupra contului autentificat, nu al altuia")
    void stergereaEstePeContulPropriu() throws Exception {
        mockMvc.perform(post("/cont/sterge").param("parola", PAROLA)
                        .param("pastreazaPublice", "false").with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(utilizatorRepository.findByEmail(RAMANE)).isEmpty();
        assertThat(utilizatorRepository.findByEmail(PLEACA)).isPresent();
    }
}
