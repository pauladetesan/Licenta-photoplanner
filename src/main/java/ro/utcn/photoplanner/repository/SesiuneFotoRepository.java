package ro.utcn.photoplanner.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ro.utcn.photoplanner.model.Locatie;
import ro.utcn.photoplanner.model.SesiuneFoto;
import ro.utcn.photoplanner.model.Utilizator;

import java.time.LocalDate;
import java.util.List;

public interface SesiuneFotoRepository extends JpaRepository<SesiuneFoto, Long> {

    /** Sesiunile de azi înainte, cele mai apropiate primele. Locația vine în aceeași interogare. */
    @EntityGraph(attributePaths = "locatie")
    List<SesiuneFoto> findByUtilizatorAndDataGreaterThanEqualOrderByDataAsc(
            Utilizator utilizator, LocalDate de_la);

    /** Sesiunile trecute, cele mai recente primele. */
    @EntityGraph(attributePaths = "locatie")
    List<SesiuneFoto> findByUtilizatorAndDataLessThanOrderByDataDesc(
            Utilizator utilizator, LocalDate inainte_de);

    /** Sesiunile legate de o locație — se șterg odată cu ea. */
    List<SesiuneFoto> findByLocatie(Locatie locatie);

    /** Toate sesiunile cuiva — se șterg odată cu contul. */
    List<SesiuneFoto> findByUtilizator(Utilizator utilizator);

    long countByUtilizator(Utilizator utilizator);
}
