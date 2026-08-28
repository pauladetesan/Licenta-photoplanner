package ro.utcn.photoplanner.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ro.utcn.photoplanner.service.ProtectieLogin;

import java.io.IOException;

/**
 * Oprește încercările de autentificare venite de la cineva care a greșit deja de prea multe ori.
 * <p>
 * Verificarea se face <b>înainte</b> de a ajunge la compararea parolei, nu după: bcrypt e
 * intenționat lent, așa că cineva care încearcă parole la nesfârșit ar consuma procesorul
 * serverului chiar și când toate încercările eșuează.
 */
@Component
public class FiltruProtectieLogin extends OncePerRequestFilter {

    private final ProtectieLogin protectieLogin;

    public FiltruProtectieLogin(ProtectieLogin protectieLogin) {
        this.protectieLogin = protectieLogin;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest cerere) {
        return !("POST".equalsIgnoreCase(cerere.getMethod())
                && "/login".equals(cerere.getServletPath()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest cerere, HttpServletResponse raspuns,
                                     FilterChain lant) throws ServletException, IOException {

        String email = cerere.getParameter("username");
        String ip = cerere.getRemoteAddr();

        if (protectieLogin.esteBlocat(email, ip)) {
            /*
             * Mesajul nu spune dacă adresa există sau nu — numărăm și încercările pe adrese
             * neînregistrate, tocmai ca răspunsul să nu devină o cale de a afla ce conturi sunt.
             */
            raspuns.sendRedirect(cerere.getContextPath() + "/login?blocat");
            return;
        }

        lant.doFilter(cerere, raspuns);
    }
}
