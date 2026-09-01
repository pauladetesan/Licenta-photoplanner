package ro.utcn.photoplanner.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ro.utcn.photoplanner.model.Locatie;
import ro.utcn.photoplanner.model.Tema;
import ro.utcn.photoplanner.model.Utilizator;
import ro.utcn.photoplanner.model.Vizibilitate;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LocatieRepository extends JpaRepository<Locatie, Long> {

    /**
     * Distanța haversine, în kilometri, între punctul căutat ({@code :lat}, {@code :lon}) și
     * locația {@code l}.
     * <p>
     * Aceeași formulă ca în {@code LocatieService.distantaKm}, care calculează distanța afișată
     * în listă. Interogarea folosea înainte legea cosinusurilor.
     * <p>
     * Motivul schimbării e consecvența, nu precizia. Se spune adesea că legea cosinusurilor se
     * strică sub un kilometru, fiindcă {@code acos} e prost condiționat lângă 1 — dar asta e
     * valabil în simplă precizie. În {@code double}, la 50 de metri eroarea ei e de ordinul
     * centimetrilor, deci practic tot una cu haversine; s-a și verificat: testele din
     * {@code CautareDistantaTest} trec la fel cu ambele formule.
     * <p>
     * Ce se câștigă e că filtrul și numărul afișat pornesc acum din aceeași formulă, deci nu
     * pot ajunge niciodată să se contrazică — și că documentația de mai jos, care spunea de la
     * bun început „haversine”, a devenit adevărată.
     * <p>
     * E o constantă tocmai ca să nu fie scrisă de două ori, în interogare și în cea de numărare:
     * o formulă copiată e o formulă care ajunge să difere.
     * <p>
     * La locul folosirii, spațiul dinaintea ei se pune explicit cu {@code + " " +}: un text block
     * taie spațiile de la capătul fiecărei linii, deci „{@code or }” devine „{@code or}” și s-ar
     * lipi de formulă, dând „{@code or2 * 6371.0}”.
     */
    String DISTANTA_HAVERSINE_KM = """
            2 * 6371.0 * asin(sqrt(least(1.0,
                 sin((radians(l.latitudine) - radians(:lat)) / 2)
               * sin((radians(l.latitudine) - radians(:lat)) / 2)
               + cos(radians(:lat)) * cos(radians(l.latitudine))
               * sin((radians(l.longitudine) - radians(:lon)) / 2)
               * sin((radians(l.longitudine) - radians(:lon)) / 2))))""";

    /**
     * {@code @EntityGraph} aduce autorul în aceeași interogare — listele îi afișează numele,
     * iar fără asta ar urma câte un select pentru fiecare locație.
     */
    @EntityGraph(attributePaths = "autor")
    Page<Locatie> findByVizibilitateOrderByDataAdaugareDesc(Vizibilitate vizibilitate, Pageable pagina);

    @EntityGraph(attributePaths = "autor")
    List<Locatie> findByAutorOrderByDataAdaugareDesc(Utilizator autor);

    /**
     * Caută printre locațiile publice după text, teme și distanță — fiecare filtru poate fi oprit
     * prin steagul lui, ca să folosim o singură interogare în loc de mai multe variante.
     * <p>
     * Distanța e calculată cu formula haversine direct în interogare: dacă am filtra-o în Java,
     * după paginare, paginile ar ieși incomplete și numărătoarea greșită.
     */
    @EntityGraph(attributePaths = "autor")
    @Query(value = """
            select l from Locatie l
            where l.vizibilitate = ro.utcn.photoplanner.model.Vizibilitate.PUBLICA
              and (:tipar is null
                   or lower(l.nume) like :tipar
                   or lower(coalesce(l.descriere, '')) like :tipar)
              and (:filtreazaTeme = false
                   or exists (select t from Locatie alta join alta.teme t
                              where alta = l and t in :teme))
              and (:filtreazaDistanta = false
                   or """ + " " + DISTANTA_HAVERSINE_KM + """
                        <= :raza)
            order by l.dataAdaugare desc
            """,
            countQuery = """
            select count(l) from Locatie l
            where l.vizibilitate = ro.utcn.photoplanner.model.Vizibilitate.PUBLICA
              and (:tipar is null
                   or lower(l.nume) like :tipar
                   or lower(coalesce(l.descriere, '')) like :tipar)
              and (:filtreazaTeme = false
                   or exists (select t from Locatie alta join alta.teme t
                              where alta = l and t in :teme))
              and (:filtreazaDistanta = false
                   or """ + " " + DISTANTA_HAVERSINE_KM + """
                        <= :raza)
            """)
    Page<Locatie> cauta(@Param("tipar") String tipar,
                         @Param("filtreazaTeme") boolean filtreazaTeme,
                         @Param("teme") Collection<Tema> teme,
                         @Param("filtreazaDistanta") boolean filtreazaDistanta,
                         @Param("lat") double lat,
                         @Param("lon") double lon,
                         @Param("raza") double raza,
                         Pageable pagina);

    /** Locațiile private ale unui utilizator — doar ale lui, pentru harta de pe prima pagină. */
    List<Locatie> findByAutorAndVizibilitate(Utilizator autor, Vizibilitate vizibilitate);

    /** Copia pe care utilizatorul o are deja după o anumită locație, dacă există. */
    Optional<Locatie> findByAutorAndPreluataDin(Utilizator autor, Long preluataDin);

    List<Locatie> findByFusOrarIsNull();
}
