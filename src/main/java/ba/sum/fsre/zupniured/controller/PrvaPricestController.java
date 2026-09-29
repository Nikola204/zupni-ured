package ba.sum.fsre.zupniured.controller;

import ba.sum.fsre.zupniured.dto.PrvaPricestForma;
import ba.sum.fsre.zupniured.service.PrvaPricestPodaciException;
import ba.sum.fsre.zupniured.service.PrvaPricestService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/prva-pricest")
public class PrvaPricestController {

    private final PrvaPricestService prvaPricestService;

    public PrvaPricestController(PrvaPricestService prvaPricestService) {
        this.prvaPricestService = prvaPricestService;
    }

    @InitBinder("prvaPricest")
    public void postaviPoljaObrasca(WebDataBinder binder) {
        binder.setAllowedFields("zupljaninId", "datumPricesti", "mjestoPricesti",
                "brojMatice", "godinaMatice", "napomena");
    }

    @GetMapping
    public String prikaziSve(@RequestParam(defaultValue = "") String pretraga, Model model) {
        model.addAttribute("pricesti", prvaPricestService.pronadjiSve(pretraga));
        model.addAttribute("pretraga", pretraga);
        return "prva-pricest/lista";
    }

    @GetMapping("/novi")
    public String prikaziNovi(Model model) {
        model.addAttribute("prvaPricest", new PrvaPricestForma());
        return pripremiFormu(model, "prva-pricest/forma", null);
    }

    @PostMapping("/novi")
    public String dodaj(@Valid @ModelAttribute("prvaPricest") PrvaPricestForma forma,
                        BindingResult rezultat, Model model) {
        if (!rezultat.hasErrors()) {
            try {
                var spremljen = prvaPricestService.dodaj(forma);
                return "redirect:/prva-pricest/" + spremljen.getId();
            } catch (PrvaPricestPodaciException greska) {
                rezultat.rejectValue("zupljaninId", "prvaPricest.zupljanin", greska.getMessage());
            }
        }
        return pripremiFormu(model, "prva-pricest/forma", null);
    }

    @GetMapping("/uredi/{id}")
    public String prikaziUredjivanje(@PathVariable Long id, Model model) {
        model.addAttribute("prvaPricest", prvaPricestService.formaZaUredjivanje(id));
        model.addAttribute("prvaPricestId", id);
        return pripremiFormu(model, "prva-pricest/uredi", id);
    }

    @PostMapping("/uredi/{id}")
    public String uredi(@PathVariable Long id, @Valid @ModelAttribute("prvaPricest") PrvaPricestForma forma,
                        BindingResult rezultat, Model model) {
        prvaPricestService.pronadjiPoId(id);
        model.addAttribute("prvaPricestId", id);
        if (!rezultat.hasErrors()) {
            try {
                prvaPricestService.uredi(id, forma);
                return "redirect:/prva-pricest/" + id;
            } catch (PrvaPricestPodaciException greska) {
                rezultat.rejectValue("zupljaninId", "prvaPricest.zupljanin", greska.getMessage());
            }
        }
        return pripremiFormu(model, "prva-pricest/uredi", id);
    }

    @GetMapping("/{id}")
    public String prikaziDetalje(@PathVariable Long id, Model model) {
        model.addAttribute("prvaPricest", prvaPricestService.pronadjiPoId(id));
        return "prva-pricest/detalji";
    }

    private String pripremiFormu(Model model, String pogled, Long pricestId) {
        model.addAttribute("zupljani", prvaPricestService.zupljaniZaIzbor(pricestId));
        return pogled;
    }
}
