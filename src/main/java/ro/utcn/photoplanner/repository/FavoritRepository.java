package ro.utcn.photoplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.utcn.photoplanner.model.Favorit;
import ro.utcn.photoplanner.model.Locatie;
import ro.utcn.photoplanner.model.Utilizator;

import java.util.List;
import java.util.Optional;

public interface FavoritRepository extends JpaRepository<Favorit, Long> {

    Optional<Favorit> findByUtilizatorAndLocatie(Utilizator utilizator, Locatie locatie);

    boolean existsByUtilizatorAndLocatie(Utilizator utilizator, Locatie locatie);

    List<Favorit> findByUtilizatorOrderByDataAdaugareDesc(Utilizator utilizator);

    long countByLocatie(Locatie locatie);

    /** Favoritele altora pentru o locație — se șterg odată cu locația. */
    List<Favorit> findByLocatie(Locatie locatie);

    /** Tot ce a marcat cineva ca favorit — se șterge odată cu contul lui. */
    List<Favorit> findByUtilizator(Utilizator utilizator);
}
