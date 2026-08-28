package ro.utcn.photoplanner.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "favorite", uniqueConstraints = @UniqueConstraint(columnNames = {"utilizator_id", "locatie_id"}))
public class Favorit {

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
    private LocalDateTime dataAdaugare = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Utilizator getUtilizator() { return utilizator; }
    public void setUtilizator(Utilizator utilizator) { this.utilizator = utilizator; }

    public Locatie getLocatie() { return locatie; }
    public void setLocatie(Locatie locatie) { this.locatie = locatie; }

    public LocalDateTime getDataAdaugare() { return dataAdaugare; }
    public void setDataAdaugare(LocalDateTime dataAdaugare) { this.dataAdaugare = dataAdaugare; }
}
