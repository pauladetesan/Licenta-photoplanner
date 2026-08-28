package ro.utcn.photoplanner.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "comentarii")
public class Comentariu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String continut;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id")
    private Utilizator autor;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "locatie_id")
    private Locatie locatie;

    @Column(nullable = false)
    private LocalDateTime dataAdaugare = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContinut() { return continut; }
    public void setContinut(String continut) { this.continut = continut; }

    public Utilizator getAutor() { return autor; }
    public void setAutor(Utilizator autor) { this.autor = autor; }

    public Locatie getLocatie() { return locatie; }
    public void setLocatie(Locatie locatie) { this.locatie = locatie; }

    public LocalDateTime getDataAdaugare() { return dataAdaugare; }
    public void setDataAdaugare(LocalDateTime dataAdaugare) { this.dataAdaugare = dataAdaugare; }
}
