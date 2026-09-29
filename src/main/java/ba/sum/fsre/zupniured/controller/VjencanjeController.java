package ba.sum.fsre.zupniured.controller;

import ba.sum.fsre.zupniured.dto.VjencanjeForma;
import ba.sum.fsre.zupniured.service.VjencanjePodaciException;
import ba.sum.fsre.zupniured.service.VjencanjeService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/vjencanja")
public class VjencanjeController {

    private final VjencanjeService vjencanjeService;

    public VjencanjeController(VjencanjeService vjencanjeService) {
        this.vjencanjeService = vjencanjeService;
    }

    @InitBinder("vjencanje")
    public void postaviPoljaObrasca(WebDataBinder binder) {
        binder.setAllowedFields("mladozenjaZupljaninId", "nevjestaZupljaninId",
                "imeMladozenje", "prezimeMladozenje", "imeNevjeste", "prezimeNevjeste", "datumVjencanja", "mjestoVjencanja", "svjedokMladozenje", "svjedokNevjeste", "sluziteljVjencanja", "brojMatice", "godinaMatice", "napomena");
    }

    @GetMapping
    public String prikaziSve(@RequestParam(defaultValue = "") String pretraga, Model model) {
        model.addAttribute("vjencanja", vjencanjeService.pronadjiSve(pretraga));
        model.addAttribute("pretraga", pretraga);
        return "vjencanja/lista";
    }

    @GetMapping("/novi")
    public String prikaziNovi(Model model) {
        model.addAttribute("vjencanje", new VjencanjeForma());
        return pripremiFormu(model, "vjencanja/forma");
    }

    @PostMapping("/novi")
    public String dodaj(@Valid @ModelAttribute("vjencanje") VjencanjeForma forma,
                        BindingResult rezultat, Model model) {
        if (!rezultat.hasErrors()) {
            try {
                var spremljen = vjencanjeService.dodaj(forma);
                return "redirect:/vjencanja/" + spremljen.getId();
            } catch (VjencanjePodaciException greska) {
                rezultat.rejectValue(greska.getPolje(), "vjencanje.zupljanin", greska.getMessage());
            }
        }
        return pripremiFormu(model, "vjencanja/forma");
    }

    @GetMapping("/uredi/{id}")
    public String prikaziUredjivanje(@PathVariable Long id, Model model) {
        model.addAttribute("vjencanje", vjencanjeService.formaZaUredjivanje(id));
        model.addAttribute("vjencanjeId", id);
        return pripremiFormu(model, "vjencanja/uredi");
    }

    @PostMapping("/uredi/{id}")
    public String uredi(@PathVariable Long id, @Valid @ModelAttribute("vjencanje") VjencanjeForma forma,
                        BindingResult rezultat, Model model) {
        vjencanjeService.pronadjiPoId(id);
        model.addAttribute("vjencanjeId", id);
        if (!rezultat.hasErrors()) {
            try {
                vjencanjeService.uredi(id, forma);
                return "redirect:/vjencanja/" + id;
            } catch (VjencanjePodaciException greska) {
                rezultat.rejectValue(greska.getPolje(), "vjencanje.zupljanin", greska.getMessage());
            }
        }
        return pripremiFormu(model, "vjencanja/uredi");
    }

    @GetMapping("/{id}")
    public String prikaziDetalje(@PathVariable Long id, Model model) {
        model.addAttribute("vjencanje", vjencanjeService.pronadjiPoId(id));
        return "vjencanja/detalji";
    }

    private String pripremiFormu(Model model, String pogled) {
        model.addAttribute("zupljani", vjencanjeService.zupljaniZaIzbor());
        return pogled;
    }
}
