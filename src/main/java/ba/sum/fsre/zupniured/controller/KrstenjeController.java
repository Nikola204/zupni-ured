package ba.sum.fsre.zupniured.controller;

import ba.sum.fsre.zupniured.dto.KrstenjeForma;
import ba.sum.fsre.zupniured.service.KrstenjePodaciException;
import ba.sum.fsre.zupniured.service.KrstenjeService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/krstenja")
public class KrstenjeController {

    private final KrstenjeService krstenjeService;

    public KrstenjeController(KrstenjeService krstenjeService) {
        this.krstenjeService = krstenjeService;
    }

    @InitBinder("krstenje")
    public void postaviPoljaObrasca(WebDataBinder binder) {
        binder.setAllowedFields("zupljaninId", "datumKrstenja", "mjestoKrstenja",
                "imeOca", "imeMajke", "imeKuma", "imeKume", "krstitelj",
                "brojMatice", "godinaMatice", "napomena");
    }

    @GetMapping
    public String prikaziSve(@RequestParam(defaultValue = "") String pretraga, Model model) {
        model.addAttribute("krstenja", krstenjeService.pronadjiSve(pretraga));
        model.addAttribute("pretraga", pretraga);
        return "krstenja/lista";
    }

    @GetMapping("/novi")
    public String prikaziNovi(Model model) {
        model.addAttribute("krstenje", new KrstenjeForma());
        return pripremiFormu(model, "krstenja/forma");
    }

    @PostMapping("/novi")
    public String dodaj(@Valid @ModelAttribute("krstenje") KrstenjeForma forma,
                        BindingResult rezultat, Model model) {
        if (!rezultat.hasErrors()) {
            try {
                var spremljen = krstenjeService.dodaj(forma);
                return "redirect:/krstenja/" + spremljen.getId();
            } catch (KrstenjePodaciException greska) {
                rezultat.rejectValue("zupljaninId", "krstenje.zupljanin", greska.getMessage());
            }
        }
        return pripremiFormu(model, "krstenja/forma");
    }

    @GetMapping("/uredi/{id}")
    public String prikaziUredjivanje(@PathVariable Long id, Model model) {
        model.addAttribute("krstenje", krstenjeService.formaZaUredjivanje(id));
        model.addAttribute("krstenjeId", id);
        return pripremiFormu(model, "krstenja/uredi");
    }

    @PostMapping("/uredi/{id}")
    public String uredi(@PathVariable Long id, @Valid @ModelAttribute("krstenje") KrstenjeForma forma,
                        BindingResult rezultat, Model model) {
        krstenjeService.pronadjiPoId(id);
        model.addAttribute("krstenjeId", id);
        if (!rezultat.hasErrors()) {
            try {
                krstenjeService.uredi(id, forma);
                return "redirect:/krstenja/" + id;
            } catch (KrstenjePodaciException greska) {
                rezultat.rejectValue("zupljaninId", "krstenje.zupljanin", greska.getMessage());
            }
        }
        return pripremiFormu(model, "krstenja/uredi");
    }

    @GetMapping("/{id}")
    public String prikaziDetalje(@PathVariable Long id, Model model) {
        model.addAttribute("krstenje", krstenjeService.pronadjiPoId(id));
        return "krstenja/detalji";
    }

    private String pripremiFormu(Model model, String pogled) {
        model.addAttribute("zupljani", krstenjeService.zupljaniZaIzbor());
        return pogled;
    }
}
