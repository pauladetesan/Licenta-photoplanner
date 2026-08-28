package ro.utcn.photoplanner.service;

import org.springframework.stereotype.Service;
import ro.utcn.photoplanner.model.Favorit;
import ro.utcn.photoplanner.model.Locatie;
import ro.utcn.photoplanner.model.Utilizator;
import ro.utcn.photoplanner.repository.FavoritRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoritService {

    private final FavoritRepository favoritRepository;

    public FavoritService(FavoritRepository favoritRepository) {
        this.favoritRepository = favoritRepository;
    }

    /** Comută starea de favorit pentru o locație; întoarce true dacă a devenit favorită. */
    public boolean comuta(Utilizator utilizator, Locatie locatie) {
        return favoritRepository.findByUtilizatorAndLocatie(utilizator, locatie)
                .map(favorit -> {
                    favoritRepository.delete(favorit);
                    return false;
                })
                .orElseGet(() -> {
                    Favorit favorit = new Favorit();
                    favorit.setUtilizator(utilizator);
                    favorit.setLocatie(locatie);
                    favoritRepository.save(favorit);
                    return true;
                });
    }

    public boolean esteFavorit(Utilizator utilizator, Locatie locatie) {
        return utilizator != null && favoritRepository.existsByUtilizatorAndLocatie(utilizator, locatie);
    }

    public long numarFavorite(Locatie locatie) {
        return favoritRepository.countByLocatie(locatie);
    }

    public List<Locatie> listaFavorite(Utilizator utilizator) {
        return favoritRepository.findByUtilizatorOrderByDataAdaugareDesc(utilizator).stream()
                .map(Favorit::getLocatie)
                .collect(Collectors.toList());
    }
}
