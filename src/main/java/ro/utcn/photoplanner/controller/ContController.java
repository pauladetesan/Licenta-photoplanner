package ro.utcn.photoplanner.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ro.utcn.photoplanner.model.Utilizator;
import ro.utcn.photoplanner.repository.UtilizatorRepository;
import ro.utcn.photoplanner.service.StergereContService;

@Controller
public class ContController {

    private final UtilizatorRepository utilizatorRepository;
    private final StergereContService stergereContService;
    private final PasswordEncoder passwordEncoder;

    public ContController(UtilizatorRepository utilizatorRepository,
                           StergereContService stergereContService,
                           PasswordEncoder passwordEncoder) {
        this.utilizatorRepository = utilizatorRepository;
        this.stergereContService = stergereContService;
        this.passwordEncoder = passwordEncoder;
    }

    private Utilizator utilizatorCurent(Authentication authentication) {
        return utilizatorRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Utilizator autentificat inexistent."));
    }

    @GetMapping("/cont")
    public String cont(Model model, Authentication authentication) {
        Utilizator utilizator = utilizatorCurent(authentication);

        model.addAttribute("utilizator", utilizator);
        model.addAttribute("rezumat", stergereContService.rezumat(utilizator));
        return "cont";
    }

    /**
     * Ștergerea cere parola curentă.
     * <p>
     * Doar sesiunea deschisă nu e de ajuns: cineva care găsește laptopul descuiat ar putea
     * șterge contul dintr-un clic. Parola confirmă că cel care apasă e chiar proprietarul.
     */
    @PostMapping("/cont/sterge")
    public String sterge(@RequestParam String parola,
                          @RequestParam(defaultValue = "false") boolean pastreazaPublice,
                          Model model,
                          Authentication authentication, HttpServletRequest cerere) {

        Utilizator utilizator = utilizatorCurent(authentication);

        if (!passwordEncoder.matches(parola, utilizator.getParola())) {
            model.addAttribute("utilizator", utilizator);
            model.addAttribute("rezumat", stergereContService.rezumat(utilizator));
            model.addAttribute("eroare", "Parola nu este corectă. Contul nu a fost șters.");
            return "cont";
        }

        stergereContService.sterge(utilizator, pastreazaPublice);

        // Sesiunea trebuie să dispară odată cu contul, altfel ar rămâne una autentificată
        // pentru un utilizator care nu mai există.
        HttpSession sesiune = cerere.getSession(false);
        if (sesiune != null) {
            sesiune.invalidate();
        }
        SecurityContextHolder.clearContext();

        return "redirect:/?cont=sters";
    }
}
