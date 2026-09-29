package ba.sum.fsre.zupniured.controller;

import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ba.sum.fsre.zupniured.service.ObavijestService;
import ba.sum.fsre.zupniured.service.PregledService;

@Controller
public class HomeController {
    private final ObavijestService obavijestService;
    private final PregledService pregledService;

    public HomeController(ObavijestService obavijestService, PregledService pregledService) {
        this.obavijestService = obavijestService;
        this.pregledService = pregledService;
    }

    @GetMapping("/")
    public String home(Authentication authentication, Model model){
        if (authentication != null) model.addAttribute("statistike", pregledService.statistike());
        model.addAttribute("obavijesti", obavijestService.obavijestiZaPocetnu());
        model.addAttribute("prijavljen", authentication != null);
        model.addAttribute("administrator", authentication != null && authentication.getAuthorities()
                .stream().anyMatch(ovlast -> ovlast.getAuthority().equals("ROLE_ADMIN")));
        return "index";
    }
}
