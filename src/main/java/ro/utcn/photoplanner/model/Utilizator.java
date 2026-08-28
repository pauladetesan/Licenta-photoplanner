package ro.utcn.photoplanner.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "utilizatori")
public class Utilizator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String parola;

    @Column(nullable = false, length = 100)
    private String numeAfisat;

    /**
     * Marchează contul-substitut sub care rămân locațiile publice ale celor care și-au șters
     * contul. Nu e al nimănui: nu se poate autentifica și nu i se poate reseta parola.
     */
    /*
     * `columnDefinition` cu valoare implicită nu e un moft: fără ea, Hibernate nu poate adăuga
     * o coloană NOT NULL într-un tabel care are deja rânduri, așa că pur și simplu nu o adaugă
     * — iar aplicația cade abia la prima interogare. E limita lui `ddl-auto=update`.
     */
    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean contSistem = false;

    @Column(nullable = false)
    private LocalDateTime dataInregistrare = LocalDateTime.now();

    // --- getteri și setteri ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getParola() { return parola; }
    public void setParola(String parola) { this.parola = parola; }

    public String getNumeAfisat() { return numeAfisat; }
    public void setNumeAfisat(String numeAfisat) { this.numeAfisat = numeAfisat; }

    public boolean isContSistem() { return contSistem; }
    public void setContSistem(boolean contSistem) { this.contSistem = contSistem; }

    public LocalDateTime getDataInregistrare() { return dataInregistrare; }
    public void setDataInregistrare(LocalDateTime d) { this.dataInregistrare = d; }
}
