package ro.utcn.photoplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.utcn.photoplanner.model.Comentariu;
import ro.utcn.photoplanner.model.Locatie;
import ro.utcn.photoplanner.model.Utilizator;

import java.util.List;

public interface ComentariuRepository extends JpaRepository<Comentariu, Long> {

    List<Comentariu> findByLocatieOrderByDataAdaugareAsc(Locatie locatie);

    /** Comentariile scrise de cineva, oriunde — se șterg odată cu contul lui. */
    List<Comentariu> findByAutor(Utilizator autor);

    /** Comentariile altora pe o locație — se șterg odată cu locația. */
    List<Comentariu> findByLocatie(Locatie locatie);
}
