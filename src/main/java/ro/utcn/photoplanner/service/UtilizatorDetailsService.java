package ro.utcn.photoplanner.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ro.utcn.photoplanner.model.Utilizator;
import ro.utcn.photoplanner.repository.UtilizatorRepository;

@Service
public class UtilizatorDetailsService implements UserDetailsService {

    private final UtilizatorRepository utilizatorRepository;

    public UtilizatorDetailsService(UtilizatorRepository utilizatorRepository) {
        this.utilizatorRepository = utilizatorRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Utilizator utilizator = utilizatorRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("Cont inexistent: " + email));

        // Contul-substitut nu aparține nimănui; nu trebuie să se poată intra pe el.
        if (utilizator.isContSistem()) {
            throw new UsernameNotFoundException("Cont inexistent: " + email);
        }

        return User.withUsername(utilizator.getEmail())
                .password(utilizator.getParola())
                .roles("UTILIZATOR")
                .build();
    }
}
