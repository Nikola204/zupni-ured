package ba.sum.fsre.zupniured.controller;

import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ba.sum.fsre.zupniured.service.ObavijestService;
import ba.sum.fsre.zupniured.service.PregledService;
import org.springframework.beans.factory.annotation.Value;
import java.util.Map;

@Controller
public class HomeController {
    private final ObavijestService obavijestService;
    private final PregledService pregledService;
    private final Map<String, String> kontaktZupe;

    public HomeController(ObavijestService obavijestService, PregledService pregledService,
                          @Value("${app.zupa.telefon:}") String telefon,
                          @Value("${app.zupa.mobitel:}") String mobitel,
                          @Value("${app.zupa.email:}") String email,
                          @Value("${app.zupa.adresa:}") String adresa,
                          @Value("${app.zupa.radno-vrijeme:}") String radnoVrijeme) {
        this.obavijestService = obavijestService;
        this.pregledService = pregledService;
        this.kontaktZupe = Map.of("telefon", telefon, "mobitel", mobitel, "email", email,
                "adresa", adresa, "radnoVrijeme", radnoVrijeme);
    }

    @GetMapping("/")
    public String home(Authentication authentication, Model model){
        if (authentication != null) model.addAttribute("statistike", pregledService.statistike());
        model.addAttribute("obavijesti", obavijestService.obavijestiZaPocetnu());
        model.addAttribute("kontaktZupe", kontaktZupe);
        model.addAttribute("prijavljen", authentication != null);
        model.addAttribute("administrator", authentication != null && authentication.getAuthorities()
                .stream().anyMatch(ovlast -> ovlast.getAuthority().equals("ROLE_ADMIN")));
        return "index";
    }
}
