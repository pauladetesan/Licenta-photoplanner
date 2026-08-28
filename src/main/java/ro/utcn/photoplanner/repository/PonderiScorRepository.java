package ro.utcn.photoplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.utcn.photoplanner.model.PonderiScor;
import ro.utcn.photoplanner.model.Utilizator;

import java.util.Optional;

public interface PonderiScorRepository extends JpaRepository<PonderiScor, Long> {

    Optional<PonderiScor> findByUtilizator(Utilizator utilizator);

    void deleteByUtilizator(Utilizator utilizator);
}
