package ro.utcn.photoplanner.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ro.utcn.photoplanner.model.Locatie;
import ro.utcn.photoplanner.model.MomentZi;
import ro.utcn.photoplanner.model.SesiuneFoto;
import ro.utcn.photoplanner.model.Utilizator;
import ro.utcn.photoplanner.repository.UtilizatorRepository;
import ro.utcn.photoplanner.service.InfoSoare;
import ro.utcn.photoplanner.service.LocatieService;
import ro.utcn.photoplanner.service.SesiuneFotoService;
import ro.utcn.photoplanner.service.SoareService;
import ro.utcn.photoplanner.service.VremeService;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Controller
public class SesiuneFotoController {

    private final SesiuneFotoService sesiuneService;
    private final LocatieService locatieService;
    private final SoareService soareService;
    private final VremeService vremeService;
    private final UtilizatorRepository utilizatorRepository;

    public SesiuneFotoController(SesiuneFotoService sesiuneService, LocatieService locatieService,
                                  SoareService soareService, VremeService vremeService,
                                  UtilizatorRepository utilizatorRepository) {
        this.sesiuneService = sesiuneService;
        this.locatieService = locatieService;
        this.soareService = soareService;
        this.vremeService = vremeService;
        this.utilizatorRepository = utilizatorRepository;
    }

    private Utilizator utilizatorCurent(Authentication authentication) {
        return utilizatorRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Utilizator autentificat inexistent."));
    }

    /** Ora din zi la care se referă momentul ales, calculată pentru locația și ziua sesiunii. */
    static ZonedDateTime oraMomentului(MomentZi moment, InfoSoare soare) {
        return switch (moment) {
            case RASARIT -> soare.rasarit();
            case ORA_AUR_DIMINEATA -> soare.mijlocDimineata();
            case ZI -> mijloculZilei(soare);
            case ORA_AUR_SEARA -> soare.mijlocSeara();
            case APUS -> soare.apus();
            // Cerul se întunecă de tot la ceva vreme după apus.
            case NOAPTE -> soare.apus() != null ? soare.apus().plusHours(2) : null;
        };
    }

    private static ZonedDateTime mijloculZilei(InfoSoare soare) {
        if (soare.rasarit() == null || soare.apus() == null) {
            return null;
        }
        long secunde = Duration.between(soare.rasarit(), soare.apus()).getSeconds();
        return soare.rasarit().plusSeconds(secunde / 2);
    }

    /** Împachetează sesiunile cu ora momentului și prognoza, ca lista să le poată afișa. */
    private List<SesiunePlanificata> cuDetalii(List<SesiuneFoto> sesiuni) {
        return sesiuni.stream().map(s -> {
            Locatie locatie = s.getLocatie();
            ZoneId zona = locatie.getZona();
            InfoSoare soare = soareService.calculeaza(
                    locatie.getLatitudine(), locatie.getLongitudine(), s.getData(), zona);

            ZonedDateTime moment = oraMomentului(s.getMoment(), soare);
            return new SesiunePlanificata(s, moment, vremeService.laUnMoment(
                    locatie.getLatitudine(), locatie.getLongitudine(), s.getData(), zona, moment));
        }).toList();
    }

    @GetMapping("/sesiuni")
    public String lista(Model model, Authentication authentication) {
        Utilizator utilizator = utilizatorCurent(authentication);

        model.addAttribute("viitoare", cuDetalii(sesiuneService.viitoare(utilizator)));
        model.addAttribute("trecute", cuDetalii(sesiuneService.trecute(utilizator)));
        return "sesiuni";
    }

    @GetMapping("/sesiuni/noua")
    public String formularNoua(@RequestParam("locatie") Long idLocatie, Model model,
                                Authentication authentication) {
        Utilizator utilizator = utilizatorCurent(authentication);

        Locatie locatie = locatieService.gasesteVizibila(idLocatie, utilizator);

        model.addAttribute("locatie", locatie);
        model.addAttribute("momente", MomentZi.values());
        model.addAttribute("dataImplicita", LocalDate.now());
        // Copierea în portofoliu are sens doar pentru locurile găsite de altcineva.
        model.addAttribute("aAltcuiva", !locatie.getAutor().getId().equals(utilizator.getId()));
        return "sesiune-noua";
    }

    @PostMapping("/sesiuni")
    public String creeaza(@RequestParam("locatie") Long idLocatie,
                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                           @RequestParam MomentZi moment,
                           @RequestParam(required = false) String notite,
                           @RequestParam(defaultValue = "false") boolean inPortofoliu,
                           Authentication authentication) {

        sesiuneService.planifica(idLocatie, utilizatorCurent(authentication), data, moment,
                notite, inPortofoliu);
        return "redirect:/sesiuni";
    }

    @GetMapping("/sesiuni/{id}/editare")
    public String formularEditare(@PathVariable Long id, Model model, Authentication authentication) {
        SesiuneFoto sesiune = sesiuneService.gasesteProprie(id, utilizatorCurent(authentication));

        model.addAttribute("sesiune", sesiune);
        model.addAttribute("locatie", sesiune.getLocatie());
        model.addAttribute("momente", MomentZi.values());
        return "sesiune-editare";
    }

    @PostMapping("/sesiuni/{id}/editare")
    public String editeaza(@PathVariable Long id,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                            @RequestParam MomentZi moment,
                            @RequestParam(required = false) String notite,
                            Authentication authentication) {

        sesiuneService.actualizeaza(id, utilizatorCurent(authentication), data, moment, notite);
        return "redirect:/sesiuni";
    }

    @PostMapping("/sesiuni/{id}/sterge")
    public String sterge(@PathVariable Long id, Authentication authentication) {
        sesiuneService.sterge(id, utilizatorCurent(authentication));
        return "redirect:/sesiuni";
    }
}
