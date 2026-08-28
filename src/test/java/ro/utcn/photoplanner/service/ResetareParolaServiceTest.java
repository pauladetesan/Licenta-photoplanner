package ro.utcn.photoplanner.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import ro.utcn.photoplanner.model.TokenResetare;
import ro.utcn.photoplanner.model.Utilizator;
import ro.utcn.photoplanner.repository.TokenResetareRepository;
import ro.utcn.photoplanner.repository.UtilizatorRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Resetarea parolei. Aici se verifică exact proprietățile care fac diferența între o resetare
 * sigură și una prin care se poate intra pe conturi străine.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ResetareParolaServiceTest {

    @Mock private UtilizatorRepository utilizatorRepository;
    @Mock private TokenResetareRepository tokenRepository;
    @Mock private ExpeditorLinkResetare expeditor;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private ResetareParolaService serviciu;
    private Utilizator utilizator;

    @BeforeEach
    void pregateste() {
        serviciu = new ResetareParolaService(utilizatorRepository, tokenRepository, encoder,
                expeditor, 60, 5, "http://localhost:8080");

        utilizator = new Utilizator();
        utilizator.setId(1L);
        utilizator.setEmail("cineva@example.com");
        utilizator.setNumeAfisat("Cineva");
        utilizator.setParola(encoder.encode("parolaVeche1"));
    }

    /** Prinde tokenul din linkul trimis, ca să putem verifica fluxul complet. */
    private String tokenulTrimis() {
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(expeditor).trimite(any(), link.capture());
        return link.getValue().substring(link.getValue().indexOf("token=") + 6);
    }

    @Test
    @DisplayName("o adresă necunoscută nu trimite nimic și nu dă nicio eroare")
    void adresaNecunoscutaNuDivulgaNimic() {
        when(utilizatorRepository.findByEmail("nimeni@example.com")).thenReturn(Optional.empty());

        serviciu.cereResetare("nimeni@example.com");

        verify(expeditor, never()).trimite(any(), anyString());
        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("adresa e normalizată, ca la înregistrare")
    void adresaEsteNormalizata() {
        when(utilizatorRepository.findByEmail("cineva@example.com")).thenReturn(Optional.of(utilizator));

        serviciu.cereResetare("  CINEVA@Example.COM  ");

        verify(expeditor).trimite(any(), anyString());
    }

    @Test
    @DisplayName("în baza de date se salvează amprenta, niciodată tokenul din link")
    void seSalveazaDoarAmprenta() {
        when(utilizatorRepository.findByEmail(anyString())).thenReturn(Optional.of(utilizator));

        serviciu.cereResetare("cineva@example.com");

        ArgumentCaptor<TokenResetare> salvat = ArgumentCaptor.forClass(TokenResetare.class);
        verify(tokenRepository).save(salvat.capture());

        String token = tokenulTrimis();
        assertThat(salvat.getValue().getAmprenta())
                .isNotEqualTo(token)
                .hasSize(64)                                    // SHA-256 în hexazecimal
                .isEqualTo(ResetareParolaService.amprenta(token));
    }

    @Test
    @DisplayName("tokenurile sunt lungi și diferite de fiecare dată")
    void tokenurileSuntUniceSiLungi() {
        when(utilizatorRepository.findByEmail(anyString())).thenReturn(Optional.of(utilizator));

        ArgumentCaptor<String> linkuri = ArgumentCaptor.forClass(String.class);
        for (int i = 0; i < 5; i++) {
            serviciu.cereResetare("cineva@example.com");
        }
        verify(expeditor, org.mockito.Mockito.times(5)).trimite(any(), linkuri.capture());

        assertThat(linkuri.getAllValues()).doesNotHaveDuplicates();
        linkuri.getAllValues().forEach(l ->
                assertThat(l.substring(l.indexOf("token=") + 6)).hasSizeGreaterThanOrEqualTo(40));
    }

    @Test
    @DisplayName("prea multe cereri într-o oră sunt ignorate")
    void cereriPreaDeseSuntOprite() {
        when(utilizatorRepository.findByEmail(anyString())).thenReturn(Optional.of(utilizator));
        when(tokenRepository.countByUtilizatorAndDataCreareAfter(any(), any())).thenReturn(5L);

        serviciu.cereResetare("cineva@example.com");

        verify(expeditor, never()).trimite(any(), anyString());
        verify(tokenRepository, never()).save(any());
    }

    /**
     * Dacă trimiterea eșuează doar pentru conturile care există, o eroare vizibilă ar deveni
     * chiar semnalul pe care încercăm să nu-l dăm: „contul acesta există”.
     */
    @Test
    @DisplayName("o eroare la trimitere nu se vede în afară — altfel ar divulga ce conturi există")
    void eroareaLaTrimitereNuIese() {
        when(utilizatorRepository.findByEmail(anyString())).thenReturn(Optional.of(utilizator));
        org.mockito.Mockito.doThrow(new RuntimeException("SMTP picat"))
                .when(expeditor).trimite(any(), anyString());

        // Exact ca pentru o adresă inexistentă: fără excepție, fără indiciu.
        serviciu.cereResetare("cineva@example.com");

        verify(tokenRepository).save(any());
    }

    @Test
    @DisplayName("un token expirat nu mai este valabil")
    void tokenExpiratNuEValabil() {
        TokenResetare expirat = token(LocalDateTime.now().minusMinutes(1), null);
        when(tokenRepository.findByAmprenta(anyString())).thenReturn(Optional.of(expirat));

        assertThat(serviciu.gasesteValabil("orice")).isEmpty();
    }

    @Test
    @DisplayName("un token deja folosit nu mai este valabil")
    void tokenFolositNuEValabil() {
        TokenResetare folosit = token(LocalDateTime.now().plusHours(1), LocalDateTime.now());
        when(tokenRepository.findByAmprenta(anyString())).thenReturn(Optional.of(folosit));

        assertThat(serviciu.gasesteValabil("orice")).isEmpty();
    }

    @Test
    @DisplayName("un token inexistent nu este valabil")
    void tokenInexistentNuEValabil() {
        when(tokenRepository.findByAmprenta(anyString())).thenReturn(Optional.empty());

        assertThat(serviciu.gasesteValabil("inventat")).isEmpty();
        assertThat(serviciu.gasesteValabil(null)).isEmpty();
        assertThat(serviciu.gasesteValabil("  ")).isEmpty();
    }

    @Test
    @DisplayName("resetarea chiar schimbă parola, iar cea veche nu mai merge")
    void resetareaSchimbaParola() {
        TokenResetare valabil = token(LocalDateTime.now().plusHours(1), null);
        when(tokenRepository.findByAmprenta(anyString())).thenReturn(Optional.of(valabil));
        when(tokenRepository.findByUtilizatorAndDataFolosiriiIsNull(utilizator))
                .thenReturn(List.of(valabil));

        serviciu.reseteaza("token-valabil", "parolaNoua123");

        assertThat(encoder.matches("parolaNoua123", utilizator.getParola())).isTrue();
        assertThat(encoder.matches("parolaVeche1", utilizator.getParola())).isFalse();
        verify(utilizatorRepository).save(utilizator);
    }

    @Test
    @DisplayName("după folosire, tokenul și celelalte cereri în așteptare sunt anulate")
    void tokenurileSuntAnulateDupaFolosire() {
        TokenResetare folosit = token(LocalDateTime.now().plusHours(1), null);
        TokenResetare altul = token(LocalDateTime.now().plusHours(1), null);
        when(tokenRepository.findByAmprenta(anyString())).thenReturn(Optional.of(folosit));
        when(tokenRepository.findByUtilizatorAndDataFolosiriiIsNull(utilizator))
                .thenReturn(List.of(folosit, altul));

        serviciu.reseteaza("token-valabil", "parolaNoua123");

        assertThat(folosit.getDataFolosirii()).isNotNull();
        assertThat(altul.getDataFolosirii()).isNotNull();
        assertThat(folosit.esteValabil()).isFalse();
        assertThat(altul.esteValabil()).isFalse();
    }

    @Test
    @DisplayName("o parolă prea scurtă e respinsă și nu schimbă nimic")
    void parolaPreaScurtaEsteRespinsa() {
        TokenResetare valabil = token(LocalDateTime.now().plusHours(1), null);
        when(tokenRepository.findByAmprenta(anyString())).thenReturn(Optional.of(valabil));
        String inainte = utilizator.getParola();

        assertThatThrownBy(() -> serviciu.reseteaza("token-valabil", "scurt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("8");

        assertThat(utilizator.getParola()).isEqualTo(inainte);
        verify(utilizatorRepository, never()).save(any());
    }

    @Test
    @DisplayName("un token invalid nu poate schimba parola")
    void tokenInvalidNuSchimbaParola() {
        when(tokenRepository.findByAmprenta(anyString())).thenReturn(Optional.empty());
        String inainte = utilizator.getParola();

        assertThatThrownBy(() -> serviciu.reseteaza("inventat", "parolaNoua123"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(utilizator.getParola()).isEqualTo(inainte);
        verify(utilizatorRepository, never()).save(any());
    }

    @Test
    @DisplayName("amprenta e stabilă pentru același token și diferită pentru altele")
    void amprentaEsteStabila() {
        assertThat(ResetareParolaService.amprenta("abc"))
                .isEqualTo(ResetareParolaService.amprenta("abc"))
                .isNotEqualTo(ResetareParolaService.amprenta("abd"));
    }

    private TokenResetare token(LocalDateTime expira, LocalDateTime folosit) {
        TokenResetare t = new TokenResetare();
        t.setUtilizator(utilizator);
        t.setAmprenta("x".repeat(64));
        t.setExpira(expira);
        t.setDataFolosirii(folosit);
        return t;
    }
}
