package ro.utcn.photoplanner.config;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;
import ro.utcn.photoplanner.service.ProtectieLogin;

/** Ține numărătoarea de autentificări eșuate la zi, ascultând evenimentele Spring Security. */
@Component
public class AscultatorAutentificare {

    private final ProtectieLogin protectieLogin;

    public AscultatorAutentificare(ProtectieLogin protectieLogin) {
        this.protectieLogin = protectieLogin;
    }

    @EventListener
    public void laEsec(AbstractAuthenticationFailureEvent eveniment) {
        Authentication autentificare = eveniment.getAuthentication();
        protectieLogin.inregistreazaEsec(numeUtilizator(autentificare), adresaIp(autentificare));
    }

    @EventListener
    public void laSucces(AuthenticationSuccessEvent eveniment) {
        Authentication autentificare = eveniment.getAuthentication();
        protectieLogin.inregistreazaSucces(numeUtilizator(autentificare), adresaIp(autentificare));
    }

    private static String numeUtilizator(Authentication autentificare) {
        return autentificare != null ? autentificare.getName() : null;
    }

    private static String adresaIp(Authentication autentificare) {
        if (autentificare != null
                && autentificare.getDetails() instanceof WebAuthenticationDetails detalii) {
            return detalii.getRemoteAddress();
        }
        return null;
    }
}
