package ba.sum.fsre.zupniured.controller;

import ba.sum.fsre.zupniured.dto.ObavijestForma;
import ba.sum.fsre.zupniured.dto.ObavijestFilter;
import ba.sum.fsre.zupniured.service.ObavijestService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Controller
public class ObavijestController {
    private final ObavijestService service;

    public ObavijestController(ObavijestService service) { this.service = service; }

    @InitBinder("obavijest")
    public void polja(WebDataBinder binder) {
        binder.setAllowedFields("naslov", "sadrzaj", "datumObjave", "datumIsteka", "aktivna", "istaknuta");
    }

    @GetMapping("/obavijesti")
    public String javnaLista(Model model) {
        model.addAttribute("obavijesti", service.javneObavijesti());
        return "obavijesti/lista";
    }

    @GetMapping("/obavijesti/{id}")
    public String javniDetalji(@PathVariable Long id, Model model) {
        model.addAttribute("obavijest", service.javnaPoId(id));
        return "obavijesti/detalji";
    }

    @GetMapping("/admin/obavijesti")
    public String lista(@RequestParam(defaultValue = "") String pretraga,
                        @RequestParam(defaultValue = "SVE") ObavijestFilter filter, Model model) {
        model.addAttribute("obavijesti", service.pronadjiSve(pretraga, filter));
        model.addAttribute("pretraga", pretraga);
        model.addAttribute("filter", filter);
        model.addAttribute("filtri", ObavijestFilter.values());
        return "obavijesti/admin/lista";
    }

    @GetMapping("/admin/obavijesti/novi")
    public String novi(Model model) {
        model.addAttribute("obavijest", new ObavijestForma());
        return "obavijesti/admin/forma";
    }

    @PostMapping("/admin/obavijesti/novi")
    public String dodaj(@Valid @ModelAttribute("obavijest") ObavijestForma forma,
                        BindingResult rezultat, Authentication authentication) {
        if (rezultat.hasErrors()) return "obavijesti/admin/forma";
        return "redirect:/admin/obavijesti/" + service.dodaj(forma, authentication.getName()).getId();
    }

    @GetMapping("/admin/obavijesti/uredi/{id}")
    public String uredjivanje(@PathVariable Long id, Model model) {
        model.addAttribute("obavijest", service.formaZaUredjivanje(id));
        model.addAttribute("obavijestId", id);
        return "obavijesti/admin/uredi";
    }

    @PostMapping("/admin/obavijesti/uredi/{id}")
    public String uredi(@PathVariable Long id, @Valid @ModelAttribute("obavijest") ObavijestForma forma,
                        BindingResult rezultat, Model model) {
        service.pronadjiPoId(id);
        model.addAttribute("obavijestId", id);
        if (rezultat.hasErrors()) return "obavijesti/admin/uredi";
        service.uredi(id, forma);
        return "redirect:/admin/obavijesti/" + id;
    }

    @GetMapping("/admin/obavijesti/{id}")
    public String detalji(@PathVariable Long id, Model model) {
        model.addAttribute("obavijest", service.pronadjiPoId(id));
        return "obavijesti/admin/detalji";
    }

    @PostMapping("/admin/obavijesti/deaktiviraj/{id}")
    public String deaktiviraj(@PathVariable Long id) {
        service.postaviAktivnost(id, false);
        return "redirect:/admin/obavijesti/" + id;
    }

    @PostMapping("/admin/obavijesti/aktiviraj/{id}")
    public String aktiviraj(@PathVariable Long id) {
        service.postaviAktivnost(id, true);
        return "redirect:/admin/obavijesti/" + id;
    }
}
