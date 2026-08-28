package ro.utcn.photoplanner.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * O sesiune foto planificată: unde, în ce zi și în ce moment al zilei.
 * <p>
 * Sesiunile sunt personale — se văd doar de cel care le-a făcut, chiar dacă locația e publică.
 */
@Entity
@Table(name = "sesiuni_foto")
public class SesiuneFoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizator_id")
    private Utilizator utilizator;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "locatie_id")
    private Locatie locatie;

    @Column(nullable = false)
    private LocalDate data;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MomentZi moment = MomentZi.ORA_AUR_SEARA;

    @Column(length = 1000)
    private String notite;

    @Column(nullable = false)
    private LocalDateTime dataCreare = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Utilizator getUtilizator() { return utilizator; }
    public void setUtilizator(Utilizator utilizator) { this.utilizator = utilizator; }

    public Locatie getLocatie() { return locatie; }
    public void setLocatie(Locatie locatie) { this.locatie = locatie; }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }

    public MomentZi getMoment() { return moment; }
    public void setMoment(MomentZi moment) { this.moment = moment; }

    public String getNotite() { return notite; }
    public void setNotite(String notite) { this.notite = notite; }

    public LocalDateTime getDataCreare() { return dataCreare; }
    public void setDataCreare(LocalDateTime dataCreare) { this.dataCreare = dataCreare; }
}
