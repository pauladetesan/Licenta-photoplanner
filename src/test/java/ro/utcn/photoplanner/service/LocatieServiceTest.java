package ro.utcn.photoplanner.service;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ro.utcn.photoplanner.model.Locatie;
import ro.utcn.photoplanner.model.Utilizator;
import ro.utcn.photoplanner.model.Vizibilitate;
import ro.utcn.photoplanner.repository.LocatieRepository;
import ro.utcn.photoplanner.repository.SesiuneFotoRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.when;

/**
 * Regulile de acces la locații. Sunt reguli de securitate: dacă se strică, ori se pierde
 * accesul propriu, ori devin vizibile locațiile private ale altcuiva.
 * <p>
 * Testele sunt scrise plat, fără clase {@code @Nested}: cu ele, o rulare filtrată prin
 * {@code -Dtest=LocatieServiceTest} nu descoperă nimic și trece verde fără să execute nimic.
 */
@ExtendWith(MockitoExtension.class)
class LocatieServiceTest {

    @Mock private LocatieRepository locatieRepository;
    @Mock private FusOrarService fusOrarService;
    @Mock private FotografieService fotografieService;
    @Mock private SesiuneFotoRepository sesiuneRepository;

    private LocatieService serviciu;

    private Utilizator proprietar;
    private Utilizator strain;

    @BeforeEach
    void pregateste() {
        serviciu = new LocatieService(locatieRepository, fusOrarService, fotografieService,
                sesiuneRepository);
        proprietar = utilizator(1L);
        strain = utilizator(2L);
    }

    private static Utilizator utilizator(Long id) {
        Utilizator u = new Utilizator();
        u.setId(id);
        u.setEmail("utilizator" + id + "@example.com");
        u.setNumeAfisat("Utilizator " + id);
        return u;
    }

    private Locatie locatie(Vizibilitate vizibilitate) {
        Locatie l = new Locatie();
        l.setId(10L);
        l.setAutor(proprietar);
        l.setVizibilitate(vizibilitate);
        l.setNume("Undeva");
        l.setLatitudine(46.77);
        l.setLongitudine(23.59);
        return l;
    }

    private static void asteaptaStatus(Throwable t, HttpStatus status) {
        assertThat(t).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) t).getStatusCode()).isEqualTo(status);
    }

    // --- gasesteProprie: doar autorul poate modifica ---

    @Test
    @DisplayName("gasesteProprie: autorul primește locația")
    void gasesteProprieAutorulPrimesteLocatia() {
        Locatie a = locatie(Vizibilitate.PRIVATA);
        when(locatieRepository.findById(10L)).thenReturn(Optional.of(a));

        assertThat(serviciu.gasesteProprie(10L, proprietar)).isSameAs(a);
    }

    @Test
    @DisplayName("gasesteProprie: altcineva primește 403")
    void gasesteProprieAltcinevaPrimesteForbidden() {
        when(locatieRepository.findById(10L)).thenReturn(Optional.of(locatie(Vizibilitate.PUBLICA)));

        asteaptaStatus(catchThrowable(() -> serviciu.gasesteProprie(10L, strain)),
                HttpStatus.FORBIDDEN);
    }

    @Test
    @DisplayName("gasesteProprie: o locație inexistentă dă 404")
    void gasesteProprieInexistentaDaNotFound() {
        when(locatieRepository.findById(99L)).thenReturn(Optional.empty());

        asteaptaStatus(catchThrowable(() -> serviciu.gasesteProprie(99L, proprietar)),
                HttpStatus.NOT_FOUND);
    }

    // --- gasesteVizibila: publicele se văd, privatele doar de autor ---

    @Test
    @DisplayName("gasesteVizibila: o locație publică se vede de oricine, inclusiv nelogat")
    void vizibilaPublicaSeVedeDeOricine() {
        Locatie a = locatie(Vizibilitate.PUBLICA);
        when(locatieRepository.findById(10L)).thenReturn(Optional.of(a));

        assertThat(serviciu.gasesteVizibila(10L, strain)).isSameAs(a);
        assertThat(serviciu.gasesteVizibila(10L, null)).isSameAs(a);
    }

    @Test
    @DisplayName("gasesteVizibila: o locație privată se vede de autorul ei")
    void vizibilaPrivataSeVedeDeAutor() {
        Locatie a = locatie(Vizibilitate.PRIVATA);
        when(locatieRepository.findById(10L)).thenReturn(Optional.of(a));

        assertThat(serviciu.gasesteVizibila(10L, proprietar)).isSameAs(a);
    }

    /**
     * 404, nu 403: un 403 ar confirma că locația există, ceea ce e deja o scurgere de
     * informație despre locațiile private ale altcuiva.
     */
    @Test
    @DisplayName("gasesteVizibila: privata altcuiva dă 404, nu 403")
    void vizibilaPrivataAltcuivaDaNotFoundNuForbidden() {
        when(locatieRepository.findById(10L)).thenReturn(Optional.of(locatie(Vizibilitate.PRIVATA)));

        asteaptaStatus(catchThrowable(() -> serviciu.gasesteVizibila(10L, strain)),
                HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("gasesteVizibila: privata nu se vede de un vizitator neautentificat")
    void vizibilaPrivataNuSeVedeNelogat() {
        when(locatieRepository.findById(10L)).thenReturn(Optional.of(locatie(Vizibilitate.PRIVATA)));

        asteaptaStatus(catchThrowable(() -> serviciu.gasesteVizibila(10L, null)),
                HttpStatus.NOT_FOUND);
    }

    // --- sterge: curăță și ce atârnă de locație ---

    /** O sesiune planificată nu mai are sens fără locația ei, deci dispare odată cu ea. */
    @Test
    @DisplayName("ștergerea locației șterge pozele și sesiunile planificate acolo")
    void stergereaCuratăPozeleSiSesiunile() {
        Locatie a = locatie(Vizibilitate.PRIVATA);
        when(locatieRepository.findById(10L)).thenReturn(Optional.of(a));
        ro.utcn.photoplanner.model.SesiuneFoto sesiune = new ro.utcn.photoplanner.model.SesiuneFoto();
        when(sesiuneRepository.findByLocatie(a)).thenReturn(java.util.List.of(sesiune));

        serviciu.sterge(10L, proprietar);

        org.mockito.Mockito.verify(fotografieService).stergeToate(a);
        org.mockito.Mockito.verify(sesiuneRepository).deleteAll(java.util.List.of(sesiune));
        org.mockito.Mockito.verify(locatieRepository).delete(a);
    }

    // --- copiazaInPortofoliu: preluarea unui loc găsit de altcineva ---

    private Locatie locatieAltcuiva() {
        Locatie l = locatie(Vizibilitate.PUBLICA);
        l.setDescriere("faleza dinspre est");
        l.setOrientareScena(90);
        l.setFusOrar("Europe/Bucharest");
        l.setTeme(new java.util.HashSet<>(java.util.List.of(ro.utcn.photoplanner.model.Tema.PEISAJ)));
        return l;
    }

    /**
     * Căutarea copiei existente se face după locația găsită <em>întâi</em>, nu după cea din care
     * copiem — de aceea id-ul urmărit se dă explicit.
     */
    private Locatie copiaSalvata(Locatie original, Utilizator cine, Long idRadacina) {
        when(locatieRepository.findByAutorAndPreluataDin(cine, idRadacina))
                .thenReturn(Optional.empty());
        when(locatieRepository.save(org.mockito.ArgumentMatchers.any(Locatie.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        return serviciu.copiazaInPortofoliu(original, cine);
    }

    @Test
    @DisplayName("copia preia datele locului, dar aparține celui care o salvează")
    void copiaAreDateleSiNoulAutor() {
        Locatie original = locatieAltcuiva();
        Locatie copie = copiaSalvata(original, strain, 10L);

        assertThat(copie.getAutor()).isSameAs(strain);
        assertThat(copie.getNume()).isEqualTo(original.getNume());
        assertThat(copie.getLatitudine()).isEqualTo(original.getLatitudine());
        assertThat(copie.getOrientareScena()).isEqualTo(90);
        assertThat(copie.getFusOrar()).isEqualTo("Europe/Bucharest");
        assertThat(copie.getTeme()).containsExactly(ro.utcn.photoplanner.model.Tema.PEISAJ);
    }

    /**
     * Copia nu are voie să apară public: ar însemna să republici descoperirea altcuiva
     * ca fiind a ta, fără ca el să fi cerut asta.
     */
    @Test
    @DisplayName("copia este privată, nu republică locul altcuiva")
    void copiaEstePrivata() {
        assertThat(copiaSalvata(locatieAltcuiva(), strain, 10L).getVizibilitate())
                .isEqualTo(Vizibilitate.PRIVATA);
    }

    @Test
    @DisplayName("copia păstrează numele celui care a găsit locul")
    void copiaPastreazaAtribuirea() {
        Locatie copie = copiaSalvata(locatieAltcuiva(), strain, 10L);

        assertThat(copie.getPreluataDeLa()).isEqualTo(proprietar.getNumeAfisat());
        assertThat(copie.getPreluataDin()).isEqualTo(10L);
        assertThat(copie.estePreluata()).isTrue();
    }

    /** Copiind o copie, meritul rămâne la cel care a găsit locul întâi. */
    @Test
    @DisplayName("copia unei copii atribuie tot autorului dintâi")
    void copiaUneiCopiiPastreazaAutorulDintai() {
        Locatie copieIntermediara = locatieAltcuiva();
        copieIntermediara.setId(50L);
        copieIntermediara.setPreluataDeLa("Descoperitorul");
        copieIntermediara.setPreluataDin(10L);

        Utilizator alTreilea = utilizator(3L);
        Locatie copie = copiaSalvata(copieIntermediara, alTreilea, 10L);

        assertThat(copie.getPreluataDeLa()).isEqualTo("Descoperitorul");
        assertThat(copie.getPreluataDin()).isEqualTo(10L);
    }

    @Test
    @DisplayName("a doua planificare la același loc refolosește copia, nu face alta")
    void nuSeFaceADouaCopie() {
        Locatie original = locatieAltcuiva();
        Locatie existenta = new Locatie();
        existenta.setId(77L);
        when(locatieRepository.findByAutorAndPreluataDin(strain, 10L))
                .thenReturn(Optional.of(existenta));

        assertThat(serviciu.copiazaInPortofoliu(original, strain)).isSameAs(existenta);
        org.mockito.Mockito.verify(locatieRepository, org.mockito.Mockito.never())
                .save(org.mockito.ArgumentMatchers.any(Locatie.class));
    }

    @Test
    @DisplayName("locul tău nu se copiază în propriul portofoliu")
    void loculPropriuNuSeCopiaza() {
        Locatie aMea = locatie(Vizibilitate.PUBLICA);

        assertThat(serviciu.copiazaInPortofoliu(aMea, proprietar)).isSameAs(aMea);
        org.mockito.Mockito.verify(locatieRepository, org.mockito.Mockito.never())
                .save(org.mockito.ArgumentMatchers.any(Locatie.class));
    }

    // --- distantaKm: formula haversine ---

    @Test
    @DisplayName("distanta: același punct înseamnă zero")
    void distantaAcelasiPunctEsteZero() {
        assertThat(LocatieService.distantaKm(46.77, 23.59, 46.77, 23.59)).isZero();
    }

    @Test
    @DisplayName("distanta: Cluj–Paris este aproximativ 1601 km")
    void distantaClujParis() {
        assertThat(LocatieService.distantaKm(46.7712, 23.5892, 48.8584, 2.2945))
                .isCloseTo(1601.5, Offset.offset(5.0));
    }

    @Test
    @DisplayName("distanta: Cluj–New York este aproximativ 7340 km")
    void distantaClujNewYork() {
        assertThat(LocatieService.distantaKm(46.7712, 23.5892, 40.7061, -73.9969))
                .isCloseTo(7339.5, Offset.offset(15.0));
    }

    @Test
    @DisplayName("distanta: este simetrică")
    void distantaEsteSimetrica() {
        assertThat(LocatieService.distantaKm(46.77, 23.59, 44.43, 26.10))
                .isEqualTo(LocatieService.distantaKm(44.43, 26.10, 46.77, 23.59));
    }

    /** Trecerea peste meridianul 180 nu trebuie să dea o distanță uriașă. */
    @Test
    @DisplayName("distanta: trecerea peste antimeridian nu umflă distanța")
    void distantaPesteAntimeridian() {
        assertThat(LocatieService.distantaKm(0.0, 179.5, 0.0, -179.5)).isLessThan(120.0);
    }
}
