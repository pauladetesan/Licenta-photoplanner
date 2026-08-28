package ro.utcn.photoplanner.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import ro.utcn.photoplanner.model.Locatie;
import ro.utcn.photoplanner.model.Tema;
import ro.utcn.photoplanner.model.Utilizator;
import ro.utcn.photoplanner.model.Vizibilitate;
import ro.utcn.photoplanner.repository.UtilizatorRepository;
import ro.utcn.photoplanner.service.ComentariuService;
import ro.utcn.photoplanner.service.CriteriiCautare;
import ro.utcn.photoplanner.service.CoordonateFoto;
import ro.utcn.photoplanner.service.ExifService;
import ro.utcn.photoplanner.service.FavoritService;
import ro.utcn.photoplanner.service.FotografieService;
import ro.utcn.photoplanner.service.InfoSoare;
import ro.utcn.photoplanner.service.LocatieService;
import ro.utcn.photoplanner.service.LuminaService;
import ro.utcn.photoplanner.service.LunaService;
import ro.utcn.photoplanner.service.PonderiService;
import ro.utcn.photoplanner.service.ScorService;
import ro.utcn.photoplanner.service.SoareService;
import ro.utcn.photoplanner.service.VremeService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class LocatieController {

    private final LocatieService locatieService;
    private final SoareService soareService;
    private final LuminaService luminaService;
    private final LunaService lunaService;
    private final VremeService vremeService;
    private final ExifService exifService;
    private final ComentariuService comentariuService;
    private final FavoritService favoritService;
    private final FotografieService fotografieService;
    private final ScorService scorService;
    private final PonderiService ponderiService;
    private final UtilizatorRepository utilizatorRepository;

    public LocatieController(LocatieService locatieService, SoareService soareService,
                              LuminaService luminaService, LunaService lunaService,
                              VremeService vremeService,
                              ExifService exifService,
                              ComentariuService comentariuService, FavoritService favoritService,
                              FotografieService fotografieService,
                              ScorService scorService, PonderiService ponderiService,
                              UtilizatorRepository utilizatorRepository) {
        this.locatieService = locatieService;
        this.soareService = soareService;
        this.luminaService = luminaService;
        this.lunaService = lunaService;
        this.vremeService = vremeService;
        this.exifService = exifService;
        this.comentariuService = comentariuService;
        this.favoritService = favoritService;
        this.fotografieService = fotografieService;
        this.scorService = scorService;
        this.ponderiService = ponderiService;
        this.utilizatorRepository = utilizatorRepository;
    }

    private Utilizator utilizatorCurent(Authentication authentication) {
        return utilizatorRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Utilizator autentificat inexistent."));
    }

    /** Etichetă lizibilă pentru fusul locației la ziua aleasă, ex. „CET, UTC+01:00”. */
    private static String etichetaZona(ZoneId zona, LocalDate data) {
        ZonedDateTime laPranz = data.atTime(12, 0).atZone(zona);
        String prescurtare = laPranz.format(DateTimeFormatter.ofPattern("zzz", Locale.ENGLISH));
        String decalaj = laPranz.getOffset().getId();
        return prescurtare + ", UTC" + ("Z".equals(decalaj) ? "+00:00" : decalaj);
    }

    /** Împerechează fiecare locație cu orele de lumină de azi, calculate în fusul ei orar. */
    private Map<Locatie, InfoSoare> cuInfoSoare(List<Locatie> locatii) {
        Map<Locatie, InfoSoare> rezultat = new LinkedHashMap<>();
        LocalDate azi = LocalDate.now();

        for (Locatie locatie : locatii) {
            rezultat.put(locatie, soareService.calculeaza(
                    locatie.getLatitudine(), locatie.getLongitudine(), azi, locatie.getZona()));
        }
        return rezultat;
    }

    /** Id-ul fotografiei de copertă pentru fiecare locație, ca listele să poată afișa o miniatură. */
    private Map<Long, Long> coperti(Collection<Locatie> locatii) {
        return fotografieService.coperti(locatii);
    }

    /** La fel ca {@link #utilizatorCurent}, dar întoarce null pentru vizitatorii neautentificați. */
    private Utilizator utilizatorCurentSauNull(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return utilizatorRepository.findByEmail(authentication.getName()).orElse(null);
    }

    @GetMapping("/locatii")
    public String listaPublice(@RequestParam(defaultValue = "0") int pagina,
                                @RequestParam(required = false) String text,
                                @RequestParam(required = false) Set<Tema> teme,
                                @RequestParam(required = false) Double lat,
                                @RequestParam(required = false) Double lon,
                                @RequestParam(required = false) Double raza,
                                Model model) {

        CriteriiCautare criterii = new CriteriiCautare(
                text, teme != null ? teme : Set.of(), lat, lon, raza);

        Page<Locatie> paginaLocatii = locatieService.cauta(criterii, pagina);
        Map<Locatie, InfoSoare> locatii = cuInfoSoare(paginaLocatii.getContent());

        model.addAttribute("locatii", locatii);
        model.addAttribute("coperti", coperti(locatii.keySet()));
        model.addAttribute("pagina", paginaLocatii.getNumber());
        model.addAttribute("totalPagini", paginaLocatii.getTotalPages());
        model.addAttribute("totalLocatii", paginaLocatii.getTotalElements());

        // Filtrele curente, ca să le putem re-afișa în formular și păstra în linkurile de paginare.
        model.addAttribute("criterii", criterii);
        model.addAttribute("toateTemele", Tema.values());

        if (criterii.areDistanta()) {
            Map<Long, Long> distante = new LinkedHashMap<>();
            for (Locatie locatie : locatii.keySet()) {
                distante.put(locatie.getId(), Math.round(LocatieService.distantaKm(
                        criterii.lat(), criterii.lon(),
                        locatie.getLatitudine(), locatie.getLongitudine())));
            }
            model.addAttribute("distante", distante);
        }

        return "locatii";
    }

    @GetMapping("/locatii/mele")
    public String listaProprii(Model model, Authentication authentication) {
        Utilizator autor = utilizatorCurent(authentication);
        Map<Locatie, InfoSoare> locatii = cuInfoSoare(locatieService.listaProprii(autor));
        model.addAttribute("locatii", locatii);
        model.addAttribute("coperti", coperti(locatii.keySet()));
        return "locatiile-mele";
    }

    /**
     * Locațiile publice pentru harta de pe prima pagină.
     * <p>
     * Doar cele publice — cele private nu au ce căuta pe o pagină deschisă oricui.
     */
    @GetMapping("/api/locatii-harta")
    @ResponseBody
    public List<LocatiePeHarta> locatiiPentruHarta(Authentication authentication) {
        List<Locatie> publice = locatieService.publicePentruHarta();

        // Doar cine e autentificat primește ceva în plus, și numai locațiile lui private.
        Utilizator curent = utilizatorCurentSauNull(authentication);
        List<Locatie> private_ = curent != null
                ? locatieService.privatePentruHarta(curent)
                : List.of();

        List<Locatie> toate = new ArrayList<>(publice);
        toate.addAll(private_);

        Map<Long, Long> coperti = fotografieService.coperti(toate);
        Set<Long> idPrivate = private_.stream().map(Locatie::getId).collect(Collectors.toSet());

        return toate.stream()
                .map(l -> new LocatiePeHarta(
                        l.getId(), l.getNume(), l.getLatitudine(), l.getLongitudine(),
                        l.getTeme().stream().map(Enum::name).sorted().toList(),
                        coperti.get(l.getId()),
                        idPrivate.contains(l.getId())))
                .toList();
    }

    @GetMapping("/locatii/noua")
    public String formularAdaugare(Model model) {
        if (!model.containsAttribute("locatieForm")) {
            model.addAttribute("locatieForm", new LocatieForm());
        }
        model.addAttribute("teme", Tema.values());
        model.addAttribute("vizibilitati", Vizibilitate.values());
        return "locatie-noua";
    }

    @PostMapping("/locatii/extrage-din-poza")
    @ResponseBody
    public CoordonateFoto extrageDinPoza(@RequestParam("poza") MultipartFile poza) {
        return exifService.extrageCoordonate(poza);
    }

    @PostMapping("/locatii")
    public String adauga(@Valid @ModelAttribute("locatieForm") LocatieForm locatieForm,
                          BindingResult rezultat, Model model, Authentication authentication) {

        if (rezultat.hasErrors()) {
            return formularAdaugare(model);
        }

        Utilizator autor = utilizatorCurent(authentication);

        Locatie locatie = new Locatie();
        locatie.setNume(locatieForm.getNume());
        locatie.setDescriere(locatieForm.getDescriere());
        locatie.setLatitudine(locatieForm.getLatitudine());
        locatie.setLongitudine(locatieForm.getLongitudine());
        locatie.setOrientareScena(locatieForm.getOrientareScena());
        locatie.setVizibilitate(locatieForm.getVizibilitate());
        locatie.setTeme(locatieForm.getTeme());

        locatieService.adauga(locatie, autor);
        return "redirect:/locatii/mele";
    }

    @GetMapping("/locatii/{id}/editare")
    public String formularEditare(@PathVariable Long id, Model model, Authentication authentication) {
        Locatie locatie = locatieService.gasesteProprie(id, utilizatorCurent(authentication));

        model.addAttribute("locatie", locatie);
        if (!model.containsAttribute("locatieForm")) {
            model.addAttribute("locatieForm", LocatieForm.din(locatie));
        }
        model.addAttribute("teme", Tema.values());
        model.addAttribute("vizibilitati", Vizibilitate.values());
        return "locatie-editare";
    }

    @PostMapping("/locatii/{id}/editare")
    public String editeaza(@PathVariable Long id,
                            @Valid @ModelAttribute("locatieForm") LocatieForm locatieForm,
                            BindingResult rezultat, Model model, Authentication authentication) {

        if (rezultat.hasErrors()) {
            return formularEditare(id, model, authentication);
        }

        locatieService.actualizeaza(id, utilizatorCurent(authentication),
                locatieForm.getNume(), locatieForm.getDescriere(),
                locatieForm.getLatitudine(), locatieForm.getLongitudine(),
                locatieForm.getOrientareScena(), locatieForm.getVizibilitate(),
                locatieForm.getTeme());
        return "redirect:/locatii/mele";
    }

    @PostMapping("/locatii/{id}/sterge")
    public String sterge(@PathVariable Long id, Authentication authentication) {
        locatieService.sterge(id, utilizatorCurent(authentication));
        return "redirect:/locatii/mele";
    }

    @GetMapping("/locatii/{id}")
    public String detaliu(@PathVariable Long id,
                           @RequestParam(required = false)
                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                           @RequestParam(required = false) Tema tema,
                           Model model, Authentication authentication) {

        Utilizator curent = utilizatorCurentSauNull(authentication);
        Locatie locatie = locatieService.gasesteVizibila(id, curent);

        ZoneId zonaLocatiei = locatie.getZona();
        LocalDate dataAleasa = data != null ? data : LocalDate.now(zonaLocatiei);
        InfoSoare infoSoare = soareService.calculeaza(
                locatie.getLatitudine(), locatie.getLongitudine(), dataAleasa, zonaLocatiei);

        model.addAttribute("locatie", locatie);
        model.addAttribute("data", dataAleasa);
        model.addAttribute("esteAzi", dataAleasa.equals(LocalDate.now(zonaLocatiei)));
        model.addAttribute("infoSoare", infoSoare);
        model.addAttribute("zonaLocatiei", zonaLocatiei.getId());
        model.addAttribute("etichetaZona", etichetaZona(zonaLocatiei, dataAleasa));
        model.addAttribute("infoLuna", lunaService.calculeaza(
                locatie.getLatitudine(), locatie.getLongitudine(), dataAleasa, zonaLocatiei));
        model.addAttribute("infoVreme", vremeService.pentruOreleDeAur(
                locatie.getLatitudine(), locatie.getLongitudine(), dataAleasa, zonaLocatiei,
                infoSoare.mijlocDimineata(), infoSoare.mijlocSeara()));

        if (locatie.getOrientareScena() != null) {
            model.addAttribute("luminiOreDeAur", luminaService.calculeazaPentruOreleDeAur(
                    locatie.getLatitudine(), locatie.getLongitudine(),
                    locatie.getOrientareScena(), infoSoare));
        }

        model.addAttribute("fotografii", fotografieService.listaPentru(locatie));
        model.addAttribute("comentarii", comentariuService.listaPentru(locatie));
        model.addAttribute("utilizatorCurentId", curent != null ? curent.getId() : null);
        model.addAttribute("esteAutor", curent != null && curent.getId().equals(locatie.getAutor().getId()));
        model.addAttribute("esteFavorit", favoritService.esteFavorit(curent, locatie));
        model.addAttribute("numarFavorite", favoritService.numarFavorite(locatie));

        adaugaRecomandarea(model, locatie, tema, curent);
        return "locatie-detaliu";
    }

    /**
     * Cel mai bun moment din următoarele zile, pentru tema aleasă.
     * <p>
     * Tema implicită e una dintre cele ale locației: dacă locul e marcat pentru astro, întrebarea
     * firească e „când merg după stele aici”, nu „când fac portrete”. Vizitatorii nelogați văd
     * recomandarea calculată cu ponderile implicite — nu au cum să aibă ponderi proprii.
     */
    private void adaugaRecomandarea(Model model, Locatie locatie, Tema tema, Utilizator curent) {
        Tema aleasa = tema != null ? tema : temaImplicita(locatie);

        model.addAttribute("temaScor", aleasa);
        model.addAttribute("toateTemele", Tema.values());
        model.addAttribute("zileScor", ScorService.ZILE_IMPLICIT);
        model.addAttribute("recomandare", scorService.celMaiBun(
                locatie, aleasa, ponderiService.pentru(curent), ScorService.ZILE_IMPLICIT)
                .orElse(null));
    }

    /** Prima temă a locației, în ordine stabilă; peisajul, dacă locația nu are niciuna. */
    private static Tema temaImplicita(Locatie locatie) {
        return locatie.getTeme().stream()
                .min(Comparator.comparing(Enum::name))
                .orElse(Tema.PEISAJ);
    }

    /** Păstrează ziua aleasă la întoarcerea pe pagina locației, ca să nu sară înapoi la azi. */
    private String redirectSprePagina(Long id, LocalDate data) {
        return "redirect:/locatii/" + id + (data != null ? "?data=" + data : "");
    }

    @PostMapping("/locatii/{id}/comentarii")
    public String adaugaComentariu(@PathVariable Long id, @RequestParam String continut,
                                    @RequestParam(required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                                    Authentication authentication) {
        Utilizator autor = utilizatorCurent(authentication);
        Locatie locatie = locatieService.gasesteVizibila(id, autor);
        comentariuService.adauga(locatie, autor, continut);
        return redirectSprePagina(id, data);
    }

    @PostMapping("/comentarii/{id}/sterge")
    public String stergeComentariu(@PathVariable Long id,
                                    @RequestParam(required = false)
                                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                                    Authentication authentication) {

        Locatie locatie = comentariuService.sterge(id, utilizatorCurent(authentication));
        return redirectSprePagina(locatie.getId(), data);
    }

    @PostMapping("/locatii/{id}/favorit")
    public String comutaFavorit(@PathVariable Long id,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                                 Authentication authentication) {
        Utilizator utilizator = utilizatorCurent(authentication);
        Locatie locatie = locatieService.gasesteVizibila(id, utilizator);
        favoritService.comuta(utilizator, locatie);
        return redirectSprePagina(id, data);
    }

    @GetMapping("/favorite")
    public String favorite(Model model, Authentication authentication) {
        Utilizator utilizator = utilizatorCurent(authentication);
        Map<Locatie, InfoSoare> locatii = cuInfoSoare(favoritService.listaFavorite(utilizator));
        model.addAttribute("locatii", locatii);
        model.addAttribute("coperti", coperti(locatii.keySet()));
        return "favorite";
    }
}
