package ro.utcn.photoplanner.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ro.utcn.photoplanner.model.Fotografie;
import ro.utcn.photoplanner.model.Locatie;
import ro.utcn.photoplanner.model.Utilizator;
import ro.utcn.photoplanner.repository.FotografieRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FotografieServiceTest {

    @Mock private FotografieRepository fotografieRepository;
    @Mock private StocareFotografii stocare;
    @Mock private ImagineService imagineService;

    private FotografieService serviciu;

    private Utilizator proprietar;
    private Utilizator strain;
    private Locatie locatie;
    private Fotografie fotografie;

    @BeforeEach
    void pregateste() {
        serviciu = new FotografieService(fotografieRepository, stocare, imagineService);

        proprietar = new Utilizator();
        proprietar.setId(1L);
        strain = new Utilizator();
        strain.setId(2L);

        locatie = new Locatie();
        locatie.setId(10L);
        locatie.setAutor(proprietar);

        fotografie = new Fotografie();
        fotografie.setId(100L);
        fotografie.setLocatie(locatie);
        fotografie.setNumeFisier("abc.jpg");
        fotografie.setNumeMiniatura("abc_mic.jpg");
    }

    @Test
    @DisplayName("proprietarul își poate șterge fotografia, iar fișierele dispar de pe disc")
    void proprietarulPoateSterge() {
        when(fotografieRepository.findById(100L)).thenReturn(Optional.of(fotografie));

        Locatie rezultat = serviciu.sterge(100L, proprietar);

        assertThat(rezultat).isSameAs(locatie);
        verify(fotografieRepository).delete(fotografie);
        verify(stocare).sterge("abc.jpg");
        verify(stocare).sterge("abc_mic.jpg");
    }

    @Test
    @DisplayName("altcineva nu poate șterge, și nu se atinge nimic de pe disc")
    void strainulNuPoateSterge() {
        when(fotografieRepository.findById(100L)).thenReturn(Optional.of(fotografie));

        Throwable t = catchThrowable(() -> serviciu.sterge(100L, strain));

        assertThat(t).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) t).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(fotografieRepository, never()).delete(any());
        verify(stocare, never()).sterge(any());
    }

    @Test
    @DisplayName("o fotografie inexistentă dă 404")
    void fotografieInexistenta() {
        when(fotografieRepository.findById(404L)).thenReturn(Optional.empty());

        Throwable t = catchThrowable(() -> serviciu.gaseste(404L));

        assertThat(t).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) t).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("ștergerea locației curăță și fișierele fotografiilor ei")
    void stergereaLocatieiCuratăFisierele() {
        when(fotografieRepository.findByLocatieOrderByDataAdaugareAsc(locatie))
                .thenReturn(List.of(fotografie));

        serviciu.stergeToate(locatie);

        verify(fotografieRepository).deleteAll(List.of(fotografie));
        verify(stocare).sterge("abc.jpg");
        verify(stocare).sterge("abc_mic.jpg");
    }

    @Test
    @DisplayName("coperti alege prima fotografie a fiecărei locații, într-o singură interogare")
    void copertiIaPrimaFotografie() {
        Locatie alta = new Locatie();
        alta.setId(20L);

        Fotografie primaLa10 = fotografie;
        Fotografie aDouaLa10 = new Fotografie();
        aDouaLa10.setId(101L);
        aDouaLa10.setLocatie(locatie);
        Fotografie primaLa20 = new Fotografie();
        primaLa20.setId(200L);
        primaLa20.setLocatie(alta);

        // Repository-ul le întoarce ordonate crescător după dată.
        when(fotografieRepository.findByLocatieInOrderByDataAdaugareAsc(List.of(locatie, alta)))
                .thenReturn(List.of(primaLa10, aDouaLa10, primaLa20));

        assertThat(serviciu.coperti(List.of(locatie, alta)))
                .containsEntry(10L, 100L)
                .containsEntry(20L, 200L)
                .hasSize(2);
    }

    @Test
    @DisplayName("fără locații nu se interoghează deloc baza")
    void listaGoalaNuInterogheaza() {
        assertThat(serviciu.coperti(List.of())).isEmpty();
        verify(fotografieRepository, never()).findByLocatieInOrderByDataAdaugareAsc(any());
    }
}
