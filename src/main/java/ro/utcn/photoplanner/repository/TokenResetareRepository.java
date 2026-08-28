package ro.utcn.photoplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ro.utcn.photoplanner.model.TokenResetare;
import ro.utcn.photoplanner.model.Utilizator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TokenResetareRepository extends JpaRepository<TokenResetare, Long> {

    Optional<TokenResetare> findByAmprenta(String amprenta);

    /** Tokenurile încă nefolosite ale unui utilizator — se invalidează când unul e consumat. */
    List<TokenResetare> findByUtilizatorAndDataFolosiriiIsNull(Utilizator utilizator);

    /** Câte cereri s-au făcut recent pentru un cont — ca să nu se poată inunda cutia poștală. */
    long countByUtilizatorAndDataCreareAfter(Utilizator utilizator, LocalDateTime dupa);

    /** Toate tokenurile unui cont — se șterg odată cu el. */
    List<TokenResetare> findByUtilizator(Utilizator utilizator);

    /** Curățenie: tokenurile expirate demult nu mai folosesc nimănui. */
    void deleteByExpiraBefore(LocalDateTime moment);
}
