package ro.utcn.photoplanner.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String acasa(Model model) {
        model.addAttribute("titlu", "PhotoPlanner");
        model.addAttribute("mesaj", "Planificarea sesiunilor foto în funcție de lumină");
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
