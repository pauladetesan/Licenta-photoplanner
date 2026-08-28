package ro.utcn.photoplanner.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Un token de resetare a parolei.
 * <p>
 * În baza de date se păstrează doar amprenta (SHA-256) a tokenului, nu tokenul însuși: dacă
 * cineva ajunge la conținutul tabelei, nu poate reconstrui linkurile trimise pe email.
 * Tokenul în clar există o singură dată, în linkul trimis utilizatorului.
 */
@Entity
@Table(name = "tokenuri_resetare",
       indexes = @Index(name = "idx_token_amprenta", columnList = "amprenta"))
public class TokenResetare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String amprenta;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizator_id")
    private Utilizator utilizator;

    @Column(nullable = false)
    private LocalDateTime expira;

    /** Momentul folosirii; null cât timp tokenul e încă valabil. Un token se folosește o singură dată. */
    private LocalDateTime dataFolosirii;

    @Column(nullable = false)
    private LocalDateTime dataCreare = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAmprenta() { return amprenta; }
    public void setAmprenta(String amprenta) { this.amprenta = amprenta; }

    public Utilizator getUtilizator() { return utilizator; }
    public void setUtilizator(Utilizator utilizator) { this.utilizator = utilizator; }

    public LocalDateTime getExpira() { return expira; }
    public void setExpira(LocalDateTime expira) { this.expira = expira; }

    public LocalDateTime getDataFolosirii() { return dataFolosirii; }
    public void setDataFolosirii(LocalDateTime dataFolosirii) { this.dataFolosirii = dataFolosirii; }

    public LocalDateTime getDataCreare() { return dataCreare; }
    public void setDataCreare(LocalDateTime dataCreare) { this.dataCreare = dataCreare; }

    @Transient
    public boolean esteValabil() {
        return dataFolosirii == null && expira.isAfter(LocalDateTime.now());
    }
}
