package ro.utcn.photoplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.utcn.photoplanner.model.Fotografie;
import ro.utcn.photoplanner.model.Locatie;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FotografieRepository extends JpaRepository<Fotografie, Long> {

    List<Fotografie> findByLocatieOrderByDataAdaugareAsc(Locatie locatie);

    /** Fotografiile mai multor locații deodată — ca listele să nu ceară coperta locație cu locație. */
    List<Fotografie> findByLocatieInOrderByDataAdaugareAsc(Collection<Locatie> locatii);

    /** Prima fotografie a locației — folosită drept copertă în liste. */
    Optional<Fotografie> findFirstByLocatieOrderByDataAdaugareAsc(Locatie locatie);

    long countByLocatie(Locatie locatie);
}
