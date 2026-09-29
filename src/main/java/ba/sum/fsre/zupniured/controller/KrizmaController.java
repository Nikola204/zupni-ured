package ba.sum.fsre.zupniured.controller;

import ba.sum.fsre.zupniured.dto.KrizmaForma;
import ba.sum.fsre.zupniured.service.KrizmaPodaciException;
import ba.sum.fsre.zupniured.service.KrizmaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/krizma")
public class KrizmaController {

    private final KrizmaService krizmaService;

    public KrizmaController(KrizmaService krizmaService) {
        this.krizmaService = krizmaService;
    }

    @InitBinder("krizma")
    public void postaviPoljaObrasca(WebDataBinder binder) {
        binder.setAllowedFields("zupljaninId", "datumKrizme", "mjestoKrizme",
                "imeKrizmanogKuma", "krizmatelj", "brojMatice", "godinaMatice", "napomena");
    }

    @GetMapping
    public String prikaziSve(@RequestParam(defaultValue = "") String pretraga, Model model) {
        model.addAttribute("krizme", krizmaService.pronadjiSve(pretraga));
        model.addAttribute("pretraga", pretraga);
        return "krizma/lista";
    }

    @GetMapping("/novi")
    public String prikaziNovi(Model model) {
        model.addAttribute("krizma", new KrizmaForma());
        return pripremiFormu(model, "krizma/forma", null);
    }

    @PostMapping("/novi")
    public String dodaj(@Valid @ModelAttribute("krizma") KrizmaForma forma,
                        BindingResult rezultat, Model model) {
        if (!rezultat.hasErrors()) {
            try {
                var spremljen = krizmaService.dodaj(forma);
                return "redirect:/krizma/" + spremljen.getId();
            } catch (KrizmaPodaciException greska) {
                rezultat.rejectValue("zupljaninId", "krizma.zupljanin", greska.getMessage());
            }
        }
        return pripremiFormu(model, "krizma/forma", null);
    }

    @GetMapping("/uredi/{id}")
    public String prikaziUredjivanje(@PathVariable Long id, Model model) {
        model.addAttribute("krizma", krizmaService.formaZaUredjivanje(id));
        model.addAttribute("krizmaId", id);
        return pripremiFormu(model, "krizma/uredi", id);
    }

    @PostMapping("/uredi/{id}")
    public String uredi(@PathVariable Long id, @Valid @ModelAttribute("krizma") KrizmaForma forma,
                        BindingResult rezultat, Model model) {
        krizmaService.pronadjiPoId(id);
        model.addAttribute("krizmaId", id);
        if (!rezultat.hasErrors()) {
            try {
                krizmaService.uredi(id, forma);
                return "redirect:/krizma/" + id;
            } catch (KrizmaPodaciException greska) {
                rezultat.rejectValue("zupljaninId", "krizma.zupljanin", greska.getMessage());
            }
        }
        return pripremiFormu(model, "krizma/uredi", id);
    }

    @GetMapping("/{id}")
    public String prikaziDetalje(@PathVariable Long id, Model model) {
        model.addAttribute("krizma", krizmaService.pronadjiPoId(id));
        return "krizma/detalji";
    }

    private String pripremiFormu(Model model, String pogled, Long krizmaId) {
        model.addAttribute("zupljani", krizmaService.zupljaniZaIzbor(krizmaId));
        return pogled;
    }
}
