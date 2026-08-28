package ro.utcn.photoplanner.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import ro.utcn.photoplanner.service.UtilizatorService;

@Controller
public class InregistrareController {

    private final UtilizatorService utilizatorService;

    public InregistrareController(UtilizatorService utilizatorService) {
        this.utilizatorService = utilizatorService;
    }

    @GetMapping("/inregistrare")
    public String afiseazaFormular(Model model) {
        if (!model.containsAttribute("inregistrareForm")) {
            model.addAttribute("inregistrareForm", new InregistrareForm());
        }
        return "inregistrare";
    }

    @PostMapping("/inregistrare")
    public String proceseazaFormular(@Valid @ModelAttribute("inregistrareForm") InregistrareForm form,
                                     BindingResult rezultat, Model model) {

        // Adresa ocupată e o problemă a unui câmp anume, nu a formularului întreg: mesajul
        // trebuie să apară lângă căsuța pe care utilizatorul are de corectat-o.
        if (!rezultat.hasFieldErrors("email") && utilizatorService.emailEsteLuat(form.getEmail())) {
            rezultat.rejectValue("email", "ocupat",
                    "Există deja un cont cu această adresă de email.");
        }

        if (rezultat.hasErrors()) {
            return afiseazaFormular(model);
        }

        utilizatorService.inregistreaza(form.getEmail(), form.getParola(), form.getNumeAfisat());
        return "redirect:/?cont=creat";
    }
}
