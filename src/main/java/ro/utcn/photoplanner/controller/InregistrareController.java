package ro.utcn.photoplanner.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ro.utcn.photoplanner.service.UtilizatorService;

@Controller
public class InregistrareController {

    private final UtilizatorService utilizatorService;

    public InregistrareController(UtilizatorService utilizatorService) {
        this.utilizatorService = utilizatorService;
    }

    @GetMapping("/inregistrare")
    public String afiseazaFormular() {
        return "inregistrare";
    }

    @PostMapping("/inregistrare")
    public String proceseazaFormular(@RequestParam String email,
                                     @RequestParam String parola,
                                     @RequestParam String numeAfisat,
                                     Model model) {
        try {
            utilizatorService.inregistreaza(email, parola, numeAfisat);
            return "redirect:/?cont=creat";
        } catch (IllegalArgumentException e) {
            model.addAttribute("eroare", e.getMessage());
            return "inregistrare";
        }
    }
}