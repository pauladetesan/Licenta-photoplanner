package ro.utcn.photoplanner.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import ro.utcn.photoplanner.model.Utilizator;

/**
 * Trimite linkul de resetare prin email.
 * <p>
 * Se activează doar cu {@code photoplanner.resetare.expeditor=email}; altfel rămâne
 * {@link ExpeditorInLog}. Are nevoie și de datele serverului SMTP în {@code spring.mail.*}
 * — fără {@code spring.mail.host}, Spring nu creează deloc {@link JavaMailSender} și
 * aplicația pornește cu o eroare care spune exact asta.
 */
@Service
@ConditionalOnProperty(name = "photoplanner.resetare.expeditor", havingValue = "email")
public class ExpeditorEmail implements ExpeditorLinkResetare {

    private static final Logger log = LoggerFactory.getLogger(ExpeditorEmail.class);

    private final JavaMailSender mailSender;
    private final String adresaExpeditor;
    private final int minuteValabilitate;

    public ExpeditorEmail(JavaMailSender mailSender,
                           @Value("${photoplanner.resetare.de-la:no-reply@photoplanner.local}") String adresaExpeditor,
                           @Value("${photoplanner.resetare.minute:60}") int minuteValabilitate) {
        this.mailSender = mailSender;
        this.adresaExpeditor = adresaExpeditor;
        this.minuteValabilitate = minuteValabilitate;
        log.info("Linkurile de resetare se trimit prin email, de la {}", adresaExpeditor);
    }

    @Override
    public void trimite(Utilizator utilizator, String link) {
        SimpleMailMessage mesaj = new SimpleMailMessage();
        mesaj.setFrom(adresaExpeditor);
        mesaj.setTo(utilizator.getEmail());
        mesaj.setSubject("Resetarea parolei — PhotoPlanner");
        mesaj.setText(compune(utilizator.getNumeAfisat(), link));

        mailSender.send(mesaj);
        log.info("Am trimis linkul de resetare către contul {}.", utilizator.getId());
    }

    private String compune(String nume, String link) {
        return """
                Salut, %s!

                Ai cerut resetarea parolei pentru contul tău de PhotoPlanner.
                Deschide linkul de mai jos ca să alegi o parolă nouă:

                %s

                Linkul este valabil %d de minute și poate fi folosit o singură dată.

                Dacă nu tu ai cerut resetarea, poți ignora acest mesaj — parola rămâne neschimbată.

                — PhotoPlanner
                """.formatted(nume, link, minuteValabilitate);
    }
}
