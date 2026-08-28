package ro.utcn.photoplanner.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "locatii")
public class Locatie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nume;

    @Column(length = 1000)
    private String descriere;

    @Column(nullable = false)
    private Double latitudine;

    @Column(nullable = false)
    private Double longitudine;

    /** Azimutul scenei, în grade (0 = nord, 90 = est, 180 = sud, 270 = vest). */
    private Integer orientareScena;

    /**
     * Numele celui care a găsit locația, dacă am preluat-o din portofoliul altcuiva.
     * Se ține ca text, nu ca legătură: atribuirea trebuie să rămână și dacă acel cont dispare.
     */
    @Column(length = 100)
    private String preluataDeLa;

    /** Id-ul locației originale — ca să nu facem o a doua copie a aceluiași loc. */
    private Long preluataDin;

    /**
     * Fusul orar al locației (ex. „Europe/Paris”), dedus din coordonate la salvare.
     * Orele de răsărit/apus se afișează în acest fus, nu în cel al serverului.
     */
    @Column(length = 60)
    private String fusOrar;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Vizibilitate vizibilitate = Vizibilitate.PRIVATA;

    /**
     * {@code @BatchSize} face diferența la liste: fără el Hibernate cere temele cu câte un
     * select pentru fiecare locație afișată.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "locatie_teme", joinColumns = @JoinColumn(name = "locatie_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "tema")
    @org.hibernate.annotations.BatchSize(size = 50)
    private Set<Tema> teme = new HashSet<>();

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizator_id")
    private Utilizator autor;

    @Column(nullable = false)
    private LocalDateTime dataAdaugare = LocalDateTime.now();

    // --- getteri și setteri ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNume() { return nume; }
    public void setNume(String nume) { this.nume = nume; }

    public String getDescriere() { return descriere; }
    public void setDescriere(String descriere) { this.descriere = descriere; }

    public Double getLatitudine() { return latitudine; }
    public void setLatitudine(Double latitudine) { this.latitudine = latitudine; }

    public Double getLongitudine() { return longitudine; }
    public void setLongitudine(Double longitudine) { this.longitudine = longitudine; }

    public Integer getOrientareScena() { return orientareScena; }
    public void setOrientareScena(Integer o) { this.orientareScena = o; }

    public String getPreluataDeLa() { return preluataDeLa; }
    public void setPreluataDeLa(String preluataDeLa) { this.preluataDeLa = preluataDeLa; }

    public Long getPreluataDin() { return preluataDin; }
    public void setPreluataDin(Long preluataDin) { this.preluataDin = preluataDin; }

    @Transient
    public boolean estePreluata() { return preluataDeLa != null; }

    public String getFusOrar() { return fusOrar; }
    public void setFusOrar(String fusOrar) { this.fusOrar = fusOrar; }

    /** Fusul orar al locației, cu revenire la cel al serverului dacă încă nu a fost dedus. */
    @Transient
    public java.time.ZoneId getZona() {
        if (fusOrar == null || fusOrar.isBlank()) {
            return java.time.ZoneId.systemDefault();
        }
        try {
            return java.time.ZoneId.of(fusOrar);
        } catch (java.time.DateTimeException e) {
            return java.time.ZoneId.systemDefault();
        }
    }

    public Vizibilitate getVizibilitate() { return vizibilitate; }
    public void setVizibilitate(Vizibilitate v) { this.vizibilitate = v; }

    public Set<Tema> getTeme() { return teme; }
    public void setTeme(Set<Tema> teme) { this.teme = teme; }

    public Utilizator getAutor() { return autor; }
    public void setAutor(Utilizator autor) { this.autor = autor; }

    public LocalDateTime getDataAdaugare() { return dataAdaugare; }
    public void setDataAdaugare(LocalDateTime d) { this.dataAdaugare = d; }
}