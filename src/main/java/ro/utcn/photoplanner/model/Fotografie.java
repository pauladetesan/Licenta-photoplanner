package ro.utcn.photoplanner.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * O fotografie atașată unei locații.
 * <p>
 * Conținutul stă pe disc, nu în baza de date; aici păstrăm doar numele generat al fișierului
 * și câteva date despre el. Numele de pe disc e generat de noi (UUID), niciodată preluat din
 * ce a încărcat utilizatorul.
 */
@Entity
@Table(name = "fotografii")
public class Fotografie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Numele fișierului pe disc, generat de aplicație. */
    @Column(nullable = false, length = 80, unique = true)
    private String numeFisier;

    /** Numele miniaturii pe disc. */
    @Column(nullable = false, length = 80)
    private String numeMiniatura;

    /** Numele cu care a venit fișierul de la utilizator — doar pentru afișare. */
    @Column(length = 255)
    private String numeOriginal;

    @Column(nullable = false, length = 60)
    private String tipMime;

    @Column(nullable = false)
    private long dimensiune;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "locatie_id")
    private Locatie locatie;

    @Column(nullable = false)
    private LocalDateTime dataAdaugare = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeFisier() { return numeFisier; }
    public void setNumeFisier(String numeFisier) { this.numeFisier = numeFisier; }

    public String getNumeMiniatura() { return numeMiniatura; }
    public void setNumeMiniatura(String numeMiniatura) { this.numeMiniatura = numeMiniatura; }

    public String getNumeOriginal() { return numeOriginal; }
    public void setNumeOriginal(String numeOriginal) { this.numeOriginal = numeOriginal; }

    public String getTipMime() { return tipMime; }
    public void setTipMime(String tipMime) { this.tipMime = tipMime; }

    public long getDimensiune() { return dimensiune; }
    public void setDimensiune(long dimensiune) { this.dimensiune = dimensiune; }

    public Locatie getLocatie() { return locatie; }
    public void setLocatie(Locatie locatie) { this.locatie = locatie; }

    public LocalDateTime getDataAdaugare() { return dataAdaugare; }
    public void setDataAdaugare(LocalDateTime dataAdaugare) { this.dataAdaugare = dataAdaugare; }
}
