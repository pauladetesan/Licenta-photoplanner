package ro.utcn.photoplanner.model;

import jakarta.persistence.*;

import java.util.EnumMap;
import java.util.Map;

/**
 * Cât contează fiecare factor pentru un utilizator anume.
 * <p>
 * Ponderile spun <em>cât cântărește</em> un factor, nu <em>ce e bine</em> la el: că portretului îi
 * priește lumina difuză ține de temă și e la fel pentru toată lumea, dar cât de mult contează
 * cerul față de direcția luminii e o alegere personală.
 * <p>
 * Ponderile stau în tabel propriu, nu ca niște coloane noi în {@code utilizatori}: pe
 * {@code ddl-auto=update} o coloană nouă obligatorie într-un tabel cu rânduri nu se adaugă, pe
 * când un tabel nou se creează fără probleme. Așa se pot adăuga factori noi mai târziu fără să
 * fie nevoie de nicio migrare.
 */
@Entity
@Table(name = "ponderi_scor")
public class PonderiScor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "utilizator_id", unique = true)
    private Utilizator utilizator;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ponderi_scor_valori",
                     joinColumns = @JoinColumn(name = "ponderi_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "factor", length = 30)
    @Column(name = "pondere", nullable = false)
    private Map<FactorScor, Integer> valori = new EnumMap<>(FactorScor.class);

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Utilizator getUtilizator() { return utilizator; }
    public void setUtilizator(Utilizator utilizator) { this.utilizator = utilizator; }

    public Map<FactorScor, Integer> getValori() { return valori; }
    public void setValori(Map<FactorScor, Integer> valori) { this.valori = valori; }

    /**
     * Ponderea unui factor, cu revenire la valoarea implicită dacă lipsește.
     * <p>
     * Lipsa e normală, nu o eroare: un factor adăugat după ce utilizatorul și-a salvat ponderile
     * nu are cum să fie în harta lui.
     */
    public int pondere(FactorScor factor) {
        Integer valoare = valori.get(factor);
        return valoare != null ? valoare : factor.getPondereImplicita();
    }

    public void setPondere(FactorScor factor, int pondere) {
        valori.put(factor, Math.clamp(pondere, 0, FactorScor.PONDERE_MAXIMA));
    }

    /** Setul implicit, pentru cine nu și-a schimbat nimic. */
    public static PonderiScor implicite(Utilizator utilizator) {
        PonderiScor ponderi = new PonderiScor();
        ponderi.setUtilizator(utilizator);
        for (FactorScor factor : FactorScor.values()) {
            ponderi.setPondere(factor, factor.getPondereImplicita());
        }
        return ponderi;
    }

    /**
     * Dacă toate ponderile sunt zero, scorul n-ar mai avea din ce să se compună. Atunci se
     * folosesc valorile implicite, ca pagina să arate ceva în loc de zero peste tot.
     */
    @Transient
    public boolean totulPeZero() {
        for (FactorScor factor : FactorScor.values()) {
            if (pondere(factor) > 0) {
                return false;
            }
        }
        return true;
    }
}
