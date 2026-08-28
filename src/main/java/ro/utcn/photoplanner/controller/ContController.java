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
import ro.utcn.photoplanner.service.UtilizatorService;

@Controller
public class ContController {

    private final UtilizatorRepository utilizatorRepository;
    private final StergereContService stergereContService;
    private final UtilizatorService utilizatorService;
    private final PasswordEncoder passwordEncoder;

    public ContController(UtilizatorRepository utilizatorRepository,
                           StergereContService stergereContService,
                           UtilizatorService utilizatorService,
                           PasswordEncoder passwordEncoder) {
        this.utilizatorRepository = utilizatorRepository;
        this.stergereContService = stergereContService;
        this.utilizatorService = utilizatorService;
        this.passwordEncoder = passwordEncoder;
    }

    private Utilizator utilizatorCurent(Authentication authentication) {
        return utilizatorRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Utilizator autentificat inexistent."));
    }

    @GetMapping("/cont")
    public String cont(Model model, Authentication authentication) {
        return paginaContului(utilizatorCurent(authentication), model);
    }

    /** Pagina contului, cu tot ce are nevoie ca să se deseneze. */
    private String paginaContului(Utilizator utilizator, Model model) {
        model.addAttribute("utilizator", utilizator);
        model.addAttribute("rezumat", stergereContService.rezumat(utilizator));
        return "cont";
    }

    /**
     * Schimbarea parolei din contul propriu.
     * <p>
     * Până acum singura cale spre o parolă nouă era „am uitat parola”, adică un link pe email —
     * ceea ce, cât timp expeditorul implicit scrie linkul în log, însemna că nu exista nicio
     * cale practică.
     */
    @PostMapping("/cont/parola")
    public String schimbaParola(@RequestParam String parolaCurenta,
                                 @RequestParam String parolaNoua,
                                 @RequestParam String parolaRepetata,
                                 Model model, Authentication authentication) {

        Utilizator utilizator = utilizatorCurent(authentication);

        if (!parolaNoua.equals(parolaRepetata)) {
            model.addAttribute("eroareParola", "Cele două parole noi nu coincid.");
            return paginaContului(utilizator, model);
        }

        try {
            utilizatorService.schimbaParola(utilizator, parolaCurenta, parolaNoua);
        } catch (IllegalArgumentException e) {
            model.addAttribute("eroareParola", e.getMessage());
            return paginaContului(utilizator, model);
        }

        /*
         * Sesiunea rămâne deschisă: parola s-a schimbat din contul propriu, cu parola veche la
         * mână, deci cel care stă în fața ecranului e chiar proprietarul. Nu are rost să-l dăm
         * afară din propria pagină.
         */
        return "redirect:/cont?parola=schimbata";
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
            model.addAttribute("eroare", "Parola nu este corectă. Contul nu a fost șters.");
            return paginaContului(utilizator, model);
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
