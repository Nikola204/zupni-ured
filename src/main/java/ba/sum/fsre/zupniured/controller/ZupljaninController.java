package ba.sum.fsre.zupniured.controller;

import ba.sum.fsre.zupniured.model.Zupljanin;
import ba.sum.fsre.zupniured.service.ZupljaninService;
import ba.sum.fsre.zupniured.service.ZupljaninProfilService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/zupljani")
public class ZupljaninController {

    private final ZupljaninService zupljaninService;

    private final ZupljaninProfilService profilService;

    public ZupljaninController(ZupljaninService zupljaninService, ZupljaninProfilService profilService) {
        this.zupljaninService = zupljaninService;
        this.profilService = profilService;
    }

    @GetMapping("/{id}")
    public String prikaziDetalje(@PathVariable Long id, Model model) {
        model.addAttribute("profil", profilService.pronadjiPoId(id));
        return "zupljani/detalji";
    }

    @InitBinder("zupljanin")
    public void postaviPoljaObrasca(WebDataBinder binder) {
        binder.setAllowedFields("ime", "prezime", "datumRodjenja", "mjestoRodjenja",
                "adresa", "telefon", "email", "napomena");
    }

    @GetMapping
    public String prikaziZupljane(@RequestParam(defaultValue = "") String pretraga, Model model) {
        model.addAttribute("zupljani", zupljaninService.pronadjiSve(pretraga));
        model.addAttribute("pretraga", pretraga);
        return "zupljani/lista";
    }

    @GetMapping("/novi")
    public String prikaziNovi(Model model) {
        model.addAttribute("zupljanin", new Zupljanin());
        return "zupljani/forma";
    }

    @PostMapping("/novi")
    public String dodaj(@Valid @ModelAttribute("zupljanin") Zupljanin zupljanin,
                        BindingResult rezultat) {
        if (rezultat.hasErrors()) {
            return "zupljani/forma";
        }
        zupljaninService.dodaj(zupljanin);
        return "redirect:/zupljani";
    }

    @GetMapping("/uredi/{id}")
    public String prikaziUredjivanje(@PathVariable Long id, Model model) {
        model.addAttribute("zupljanin", zupljaninService.pronadjiPoId(id));
        model.addAttribute("zupljaninId", id);
        return "zupljani/uredi";
    }

    @PostMapping("/uredi/{id}")
    public String uredi(@PathVariable Long id,
                        @Valid @ModelAttribute("zupljanin") Zupljanin zupljanin,
                        BindingResult rezultat, Model model) {
        model.addAttribute("zupljaninId", id);
        if (rezultat.hasErrors()) {
            return "zupljani/uredi";
        }
        zupljaninService.uredi(id, zupljanin);
        return "redirect:/zupljani";
    }

    @PostMapping("/deaktiviraj/{id}")
    public String deaktiviraj(@PathVariable Long id) {
        zupljaninService.deaktiviraj(id);
        return "redirect:/zupljani";
    }

    @PostMapping("/reaktiviraj/{id}")
    public String reaktiviraj(@PathVariable Long id) {
        zupljaninService.reaktiviraj(id);
        return "redirect:/zupljani";
    }
}
