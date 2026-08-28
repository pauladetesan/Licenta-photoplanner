package ro.utcn.photoplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.utcn.photoplanner.model.Utilizator;
import java.util.Optional;

public interface UtilizatorRepository extends JpaRepository<Utilizator, Long> {

    Optional<Utilizator> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Contul-substitut pentru locațiile rămase după ștergerea unui cont. */
    java.util.Optional<Utilizator> findByContSistemTrue();
}