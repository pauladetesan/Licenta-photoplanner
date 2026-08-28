package ro.utcn.photoplanner.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ro.utcn.photoplanner.service.ResetareParolaService;

@Controller
public class ResetareParolaController {

    private final ResetareParolaService resetareService;

    public ResetareParolaController(ResetareParolaService resetareService) {
        this.resetareService = resetareService;
    }

    @GetMapping("/parola-uitata")
    public String formularCerere() {
        return "parola-uitata";
    }

    /**
     * Răspunsul e același indiferent dacă adresa există sau nu — altfel pagina ar spune
     * atacatorului ce conturi sunt înregistrate.
     */
    @PostMapping("/parola-uitata")
    public String proceseazaCerere(@RequestParam String email, Model model) {
        resetareService.cereResetare(email);
        model.addAttribute("trimis", true);
        return "parola-uitata";
    }

    @GetMapping("/reseteaza-parola")
    public String formularResetare(@RequestParam(required = false) String token, Model model) {
        if (resetareService.gasesteValabil(token).isEmpty()) {
            model.addAttribute("tokenInvalid", true);
            return "reseteaza-parola";
        }
        model.addAttribute("token", token);
        return "reseteaza-parola";
    }

    @PostMapping("/reseteaza-parola")
    public String proceseazaResetare(@RequestParam String token,
                                      @RequestParam String parola,
                                      @RequestParam String parolaRepetata,
                                      Model model) {

        if (!parola.equals(parolaRepetata)) {
            model.addAttribute("token", token);
            model.addAttribute("eroare", "Cele două parole nu coincid.");
            return "reseteaza-parola";
        }

        try {
            resetareService.reseteaza(token, parola);
        } catch (IllegalArgumentException e) {
            model.addAttribute("token", token);
            model.addAttribute("eroare", e.getMessage());
            return "reseteaza-parola";
        }

        return "redirect:/login?resetat";
    }
}
