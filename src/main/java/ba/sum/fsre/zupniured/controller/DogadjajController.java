package ba.sum.fsre.zupniured.controller;

import ba.sum.fsre.zupniured.model.Dogadjaj;
import ba.sum.fsre.zupniured.service.DogadjajService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;

@Controller
@RequestMapping("/dogadjaji")
public class DogadjajController {

    private final DogadjajService dogadjajService;

    public DogadjajController(DogadjajService dogadjajService) {
        this.dogadjajService = dogadjajService;
    }

    @GetMapping
    public String prikaziDogadjaje(Model model) {

        model.addAttribute(
                "dogadjaji",
                dogadjajService.pronadjiSve()
        );

        return "dogadjaji/lista";
    }

    @GetMapping("/novi")
    public String prikaziFormuZaNoviDogadjaj(Model model) {

        model.addAttribute(
                "dogadjaj",
                new Dogadjaj()
        );

        return "dogadjaji/forma";
    }

    @PostMapping("/novi")
    public String spremiDogadjaj(
            @Valid @ModelAttribute("dogadjaj") Dogadjaj dogadjaj,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return "dogadjaji/forma";
        }

        dogadjajService.spremi(dogadjaj);

        return "redirect:/dogadjaji";
    }

    @GetMapping("/uredi/{id}")
    public String prikaziFormuZaUredjivanje(@PathVariable Long id,
                                            Model model) {

        Dogadjaj dogadjaj = dogadjajService.pronadjiPoId(id);

        model.addAttribute("dogadjaj", dogadjaj);

        return "dogadjaji/uredi";
    }

    @PostMapping("/uredi/{id}")
    public String urediDogadjaj(
            @PathVariable Long id,
            @Valid @ModelAttribute("dogadjaj") Dogadjaj dogadjaj,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            dogadjaj.setId(id);
            return "dogadjaji/uredi";
        }

        dogadjaj.setId(id);

        dogadjajService.spremi(dogadjaj);

        return "redirect:/dogadjaji";
    }

    @PostMapping("/obrisi/{id}")
    public String obrisiDogadjaj(@PathVariable Long id) {

        dogadjajService.obrisi(id);

        return "redirect:/dogadjaji";
    }
}