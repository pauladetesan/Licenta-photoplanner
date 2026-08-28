package ro.utcn.photoplanner.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ro.utcn.photoplanner.model.Locatie;
import ro.utcn.photoplanner.model.MomentZi;
import ro.utcn.photoplanner.model.SesiuneFoto;
import ro.utcn.photoplanner.model.Utilizator;
import ro.utcn.photoplanner.model.Vizibilitate;
import ro.utcn.photoplanner.repository.SesiuneFotoRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Regulile de acces la sesiunile planificate: sunt personale, chiar dacă locația e publică. */
@ExtendWith(MockitoExtension.class)
class SesiuneFotoServiceTest {

    @Mock private SesiuneFotoRepository sesiuneRepository;
    @Mock private LocatieService locatieService;

    private SesiuneFotoService serviciu;

    private Utilizator proprietar;
    private Utilizator strain;
    private Locatie locatie;

    @BeforeEach
    void pregateste() {
        serviciu = new SesiuneFotoService(sesiuneRepository, locatieService);

        proprietar = utilizator(1L);
        strain = utilizator(2L);

        locatie = new Locatie();
        locatie.setId(10L);
        locatie.setAutor(proprietar);
        locatie.setVizibilitate(Vizibilitate.PUBLICA);
        locatie.setNume("Undeva");
    }

    private static Utilizator utilizator(Long id) {
        Utilizator u = new Utilizator();
        u.setId(id);
        u.setEmail("u" + id + "@example.com");
        return u;
    }

    private SesiuneFoto sesiuneA(Utilizator alCui) {
        SesiuneFoto s = new SesiuneFoto();
        s.setId(100L);
        s.setUtilizator(alCui);
        s.setLocatie(locatie);
        s.setData(LocalDate.of(2026, 9, 11));
        s.setMoment(MomentZi.NOAPTE);
        return s;
    }

    @Test
    @DisplayName("planificarea trece prin verificarea de vizibilitate a locației")
    void planificareaVerificaVizibilitatea() {
        when(locatieService.gasesteVizibila(10L, proprietar)).thenReturn(locatie);

        serviciu.planifica(10L, proprietar, LocalDate.of(2026, 9, 11), MomentZi.NOAPTE, "note", false);

        verify(locatieService).gasesteVizibila(10L, proprietar);

        ArgumentCaptor<SesiuneFoto> salvata = ArgumentCaptor.forClass(SesiuneFoto.class);
        verify(sesiuneRepository).save(salvata.capture());
        assertThat(salvata.getValue().getUtilizator()).isSameAs(proprietar);
        assertThat(salvata.getValue().getLocatie()).isSameAs(locatie);
        assertThat(salvata.getValue().getMoment()).isEqualTo(MomentZi.NOAPTE);
    }

    /** Dacă locația nu se vede, nici sesiunea nu are cum să fie creată. */
    @Test
    @DisplayName("nu se poate planifica la o locație privată a altcuiva")
    void nuSePoatePlanificaLaLocatieAscunsa() {
        when(locatieService.gasesteVizibila(10L, strain))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Locația nu există."));

        Throwable t = catchThrowable(() ->
                serviciu.planifica(10L, strain, LocalDate.now(), MomentZi.APUS, null, false));

        assertThat(t).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) t).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(sesiuneRepository, never()).save(any());
    }

    @Test
    @DisplayName("proprietarul își găsește sesiunea")
    void proprietarulIsiGasesteSesiunea() {
        SesiuneFoto a = sesiuneA(proprietar);
        when(sesiuneRepository.findById(100L)).thenReturn(Optional.of(a));

        assertThat(serviciu.gasesteProprie(100L, proprietar)).isSameAs(a);
    }

    /** 404, nu 403 — nu confirmăm nici măcar că sesiunea altcuiva există. */
    @Test
    @DisplayName("sesiunea altcuiva dă 404, nu 403")
    void sesiuneaAltcuivaDaNotFound() {
        when(sesiuneRepository.findById(100L)).thenReturn(Optional.of(sesiuneA(proprietar)));

        Throwable t = catchThrowable(() -> serviciu.gasesteProprie(100L, strain));

        assertThat(t).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) t).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("nu se poate șterge sesiunea altcuiva")
    void nuSeStergeSesiuneaAltcuiva() {
        when(sesiuneRepository.findById(100L)).thenReturn(Optional.of(sesiuneA(proprietar)));

        catchThrowable(() -> serviciu.sterge(100L, strain));

        verify(sesiuneRepository, never()).delete(any());
    }

    @Test
    @DisplayName("nu se poate edita sesiunea altcuiva")
    void nuSeEditeazaSesiuneaAltcuiva() {
        SesiuneFoto a = sesiuneA(proprietar);
        when(sesiuneRepository.findById(100L)).thenReturn(Optional.of(a));
        LocalDate inainte = a.getData();

        catchThrowable(() -> serviciu.actualizeaza(
                100L, strain, LocalDate.of(2030, 1, 1), MomentZi.ZI, "furat"));

        assertThat(a.getData()).isEqualTo(inainte);
        verify(sesiuneRepository, never()).save(any());
    }

    @Test
    @DisplayName("proprietarul își poate edita sesiunea")
    void proprietarulIsiEditeazaSesiunea() {
        SesiuneFoto a = sesiuneA(proprietar);
        when(sesiuneRepository.findById(100L)).thenReturn(Optional.of(a));

        serviciu.actualizeaza(100L, proprietar, LocalDate.of(2026, 10, 1), MomentZi.RASARIT, "devreme");

        assertThat(a.getData()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(a.getMoment()).isEqualTo(MomentZi.RASARIT);
        assertThat(a.getNotite()).isEqualTo("devreme");
        verify(sesiuneRepository).save(a);
    }

    @Test
    @DisplayName("o sesiune inexistentă dă 404")
    void sesiuneInexistenta() {
        when(sesiuneRepository.findById(404L)).thenReturn(Optional.empty());

        Throwable t = catchThrowable(() -> serviciu.gasesteProprie(404L, proprietar));

        assertThat(((ResponseStatusException) t).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
