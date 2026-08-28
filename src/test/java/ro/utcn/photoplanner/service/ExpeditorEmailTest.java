package ro.utcn.photoplanner.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import ro.utcn.photoplanner.model.Utilizator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExpeditorEmailTest {

    @Mock private JavaMailSender mailSender;

    private ExpeditorEmail expeditor;
    private Utilizator utilizator;

    private static final String LINK = "http://localhost:8080/reseteaza-parola?token=abc123";

    @BeforeEach
    void pregateste() {
        expeditor = new ExpeditorEmail(mailSender, "no-reply@photoplanner.test", 60);

        utilizator = new Utilizator();
        utilizator.setId(7L);
        utilizator.setEmail("cineva@example.com");
        utilizator.setNumeAfisat("Cineva");
    }

    private SimpleMailMessage mesajTrimis() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("mesajul pleacă spre adresa contului, de la adresa configurată")
    void adreseleSuntCorecte() {
        expeditor.trimite(utilizator, LINK);

        SimpleMailMessage mesaj = mesajTrimis();
        assertThat(mesaj.getTo()).containsExactly("cineva@example.com");
        assertThat(mesaj.getFrom()).isEqualTo("no-reply@photoplanner.test");
        assertThat(mesaj.getSubject()).contains("Resetarea parolei");
    }

    @Test
    @DisplayName("linkul apare în text exact așa cum a fost primit")
    void linkulEsteInText() {
        expeditor.trimite(utilizator, LINK);

        assertThat(mesajTrimis().getText()).contains(LINK);
    }

    @Test
    @DisplayName("textul spune pe numele destinatarului cât e valabil linkul")
    void textulEsteCompletat() {
        expeditor.trimite(utilizator, LINK);

        String text = mesajTrimis().getText();
        assertThat(text)
                .contains("Cineva")
                .contains("60")
                .contains("o singură dată")
                .doesNotContain("%s")
                .doesNotContain("%d");
    }

    /**
     * Trimiterea nu are voie să înghită eroarea aici: serviciul de resetare o prinde el, tocmai
     * ca răspunsul către utilizator să rămână identic. Dacă ar fi înghițită și aici, o
     * configurare greșită de SMTP ar trece complet neobservată.
     */
    @Test
    @DisplayName("o eroare de trimitere se propagă, ca serviciul să o poată consemna")
    void eroareaDeTrimiterePleacaMaiDeparte() {
        doThrow(new MailSendException("SMTP picat")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> expeditor.trimite(utilizator, LINK))
                .isInstanceOf(MailSendException.class);
    }
}
