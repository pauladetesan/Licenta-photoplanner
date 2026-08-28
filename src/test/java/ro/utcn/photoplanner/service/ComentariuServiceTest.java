package ro.utcn.photoplanner.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ro.utcn.photoplanner.model.Comentariu;
import ro.utcn.photoplanner.model.Locatie;
import ro.utcn.photoplanner.model.Utilizator;
import ro.utcn.photoplanner.repository.ComentariuRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cine poate șterge un comentariu: cel care l-a scris și cel de la care e locația.
 * Restul lumii, nimeni.
 */
@ExtendWith(MockitoExtension.class)
class ComentariuServiceTest {

    @Mock private ComentariuRepository comentariuRepository;

    private ComentariuService serviciu;

    private Utilizator autorComentariu;
    private Utilizator autorLocatie;
    private Utilizator strain;
    private Comentariu comentariu;

    @BeforeEach
    void pregateste() {
        serviciu = new ComentariuService(comentariuRepository);

        autorComentariu = utilizator(1L);
        autorLocatie = utilizator(2L);
        strain = utilizator(3L);

        Locatie locatie = new Locatie();
        locatie.setId(10L);
        locatie.setAutor(autorLocatie);

        comentariu = new Comentariu();
        comentariu.setId(100L);
        comentariu.setAutor(autorComentariu);
        comentariu.setLocatie(locatie);
        comentariu.setContinut("ceva");
    }

    private static Utilizator utilizator(Long id) {
        Utilizator u = new Utilizator();
        u.setId(id);
        u.setEmail("u" + id + "@example.com");
        return u;
    }

    // --- cine poate ---

    @Test
    @DisplayName("cel care a scris comentariul îl poate șterge")
    void autorulComentariuluiPoate() {
        assertThat(serviciu.poateSterge(comentariu, autorComentariu)).isTrue();
    }

    /** Autorul locației trebuie să-și poată curăța pagina de spam sau jigniri. */
    @Test
    @DisplayName("autorul locației poate șterge comentariile de pe pagina lui")
    void autorulLocatieiPoate() {
        assertThat(serviciu.poateSterge(comentariu, autorLocatie)).isTrue();
    }

    @Test
    @DisplayName("altcineva nu poate")
    void strainulNuPoate() {
        assertThat(serviciu.poateSterge(comentariu, strain)).isFalse();
    }

    @Test
    @DisplayName("un vizitator nelogat nu poate")
    void nelogatulNuPoate() {
        assertThat(serviciu.poateSterge(comentariu, null)).isFalse();
    }

    // --- ștergerea propriu-zisă ---

    @Test
    @DisplayName("autorul comentariului îl șterge, iar serviciul întoarce locația")
    void autorulSterge() {
        when(comentariuRepository.findById(100L)).thenReturn(Optional.of(comentariu));

        Locatie locatie = serviciu.sterge(100L, autorComentariu);

        assertThat(locatie.getId()).isEqualTo(10L);
        verify(comentariuRepository).delete(comentariu);
    }

    @Test
    @DisplayName("autorul locației poate șterge comentariul altcuiva de pe pagina lui")
    void autorulLocatieiSterge() {
        when(comentariuRepository.findById(100L)).thenReturn(Optional.of(comentariu));

        serviciu.sterge(100L, autorLocatie);

        verify(comentariuRepository).delete(comentariu);
    }

    /** 403, nu 404: comentariul se vede oricum pe pagină, n-are rost să pretindem că nu există. */
    @Test
    @DisplayName("un străin primește 403 și comentariul rămâne")
    void strainulPrimesteForbidden() {
        when(comentariuRepository.findById(100L)).thenReturn(Optional.of(comentariu));

        Throwable t = catchThrowable(() -> serviciu.sterge(100L, strain));

        assertThat(t).isInstanceOf(ResponseStatusException.class);
        assertThat(((ResponseStatusException) t).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(comentariuRepository, never()).delete(any());
    }

    @Test
    @DisplayName("un comentariu inexistent dă 404")
    void comentariuInexistent() {
        when(comentariuRepository.findById(404L)).thenReturn(Optional.empty());

        Throwable t = catchThrowable(() -> serviciu.sterge(404L, autorComentariu));

        assertThat(((ResponseStatusException) t).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- adăugarea ---

    @Test
    @DisplayName("un comentariu gol e respins")
    void comentariulGolERespins() {
        Locatie locatie = new Locatie();

        assertThat(catchThrowable(() -> serviciu.adauga(locatie, autorComentariu, "   ")))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(catchThrowable(() -> serviciu.adauga(locatie, autorComentariu, null)))
                .isInstanceOf(ResponseStatusException.class);
        verify(comentariuRepository, never()).save(any());
    }
}
