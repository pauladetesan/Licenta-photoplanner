package ro.utcn.photoplanner.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ro.utcn.photoplanner.model.Fotografie;
import ro.utcn.photoplanner.model.Locatie;
import ro.utcn.photoplanner.model.Utilizator;
import ro.utcn.photoplanner.repository.UtilizatorRepository;
import ro.utcn.photoplanner.service.FotografieService;
import ro.utcn.photoplanner.service.LocatieService;

import java.time.Duration;

@Controller
public class FotografieController {

    private final FotografieService fotografieService;
    private final LocatieService locatieService;
    private final UtilizatorRepository utilizatorRepository;

    public FotografieController(FotografieService fotografieService, LocatieService locatieService,
                                 UtilizatorRepository utilizatorRepository) {
        this.fotografieService = fotografieService;
        this.locatieService = locatieService;
        this.utilizatorRepository = utilizatorRepository;
    }

    private Utilizator utilizatorCurent(Authentication authentication) {
        return utilizatorRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Utilizator autentificat inexistent."));
    }

    private Utilizator utilizatorCurentSauNull(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return utilizatorRepository.findByEmail(authentication.getName()).orElse(null);
    }

    /**
     * Ia fotografia doar dacă locația ei e vizibilă pentru cine cere.
     * Fără asta, cineva ar putea ghici id-uri și ar vedea pozele locațiilor private ale altora.
     */
    private Fotografie fotografieVizibila(Long id, Authentication authentication) {
        Fotografie fotografie = fotografieService.gaseste(id);
        locatieService.gasesteVizibila(fotografie.getLocatie().getId(),
                utilizatorCurentSauNull(authentication));
        return fotografie;
    }

    @GetMapping("/poze/{id}")
    public ResponseEntity<byte[]> imagine(@PathVariable Long id, Authentication authentication) {
        Fotografie fotografie = fotografieVizibila(id, authentication);
        return raspuns(fotografie.getTipMime(), fotografieService.continut(fotografie));
    }

    @GetMapping("/poze/{id}/mica")
    public ResponseEntity<byte[]> miniatura(@PathVariable Long id, Authentication authentication) {
        Fotografie fotografie = fotografieVizibila(id, authentication);
        return raspuns(MediaType.IMAGE_JPEG_VALUE, fotografieService.miniatura(fotografie));
    }

    private static ResponseEntity<byte[]> raspuns(String tipMime, byte[] continut) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(tipMime))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePrivate())
                .body(continut);
    }

    @PostMapping("/locatii/{id}/poze")
    public String incarca(@PathVariable Long id,
                           @RequestParam("poza") MultipartFile poza,
                           Authentication authentication,
                           RedirectAttributes atribute) {

        Utilizator utilizator = utilizatorCurent(authentication);
        Locatie locatie = locatieService.gasesteProprie(id, utilizator);

        try {
            fotografieService.adauga(locatie, poza);
        } catch (IllegalArgumentException e) {
            atribute.addFlashAttribute("eroarePoza", e.getMessage());
        }

        return "redirect:/locatii/" + id;
    }

    @PostMapping("/poze/{id}/sterge")
    public String sterge(@PathVariable Long id, Authentication authentication) {
        Locatie locatie = fotografieService.sterge(id, utilizatorCurent(authentication));
        return "redirect:/locatii/" + locatie.getId();
    }
}
