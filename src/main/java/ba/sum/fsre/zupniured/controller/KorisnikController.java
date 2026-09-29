package ba.sum.fsre.zupniured.controller;

import ba.sum.fsre.zupniured.dto.KorisnikForma;
import ba.sum.fsre.zupniured.model.Uloga;
import ba.sum.fsre.zupniured.service.KorisnikPodaciException;
import ba.sum.fsre.zupniured.service.KorisnikService;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/korisnici")
public class KorisnikController {

    private final KorisnikService korisnikService;

    public KorisnikController(KorisnikService korisnikService) {
        this.korisnikService = korisnikService;
    }

    @ModelAttribute("uloge")
    public Uloga[] uloge() {
        return Uloga.values();
    }

    @GetMapping
    public String prikaziKorisnike(Model model) {
        model.addAttribute("korisnici", korisnikService.pronadjiSve());
        return "korisnici/lista";
    }

    @GetMapping("/novi")
    public String prikaziNovi(Model model) {
        model.addAttribute("korisnik", new KorisnikForma());
        return "korisnici/forma";
    }

    @PostMapping("/novi")
    public String dodaj(@Validated(KorisnikForma.Novi.class) @ModelAttribute("korisnik") KorisnikForma forma,
                        BindingResult rezultat) {
        if (rezultat.hasErrors()) {
            return "korisnici/forma";
        }
        try {
            korisnikService.dodaj(forma);
        } catch (KorisnikPodaciException e) {
            rezultat.rejectValue(e.getPolje(), "korisnik.neispravno", e.getMessage());
            return "korisnici/forma";
        } catch (DataIntegrityViolationException e) {
            rezultat.reject("korisnik.spremanje",
                    "Spremanje nije uspjelo. Email možda već postoji; provjerite podatke.");
            return "korisnici/forma";
        }
        return "redirect:/korisnici";
    }

    @GetMapping("/uredi/{id}")
    public String prikaziUredjivanje(@PathVariable Long id, Model model) {
        model.addAttribute("korisnik", korisnikService.pripremiZaUredjivanje(id));
        model.addAttribute("korisnikId", id);
        return "korisnici/uredi";
    }

    @PostMapping("/uredi/{id}")
    public String uredi(@PathVariable Long id,
                        @Valid @ModelAttribute("korisnik") KorisnikForma forma,
                        BindingResult rezultat, Model model) {
        model.addAttribute("korisnikId", id);
        if (rezultat.hasErrors()) {
            return "korisnici/uredi";
        }
        try {
            korisnikService.uredi(id, forma);
        } catch (KorisnikPodaciException e) {
            rezultat.rejectValue(e.getPolje(), "korisnik.neispravno", e.getMessage());
            return "korisnici/uredi";
        } catch (DataIntegrityViolationException e) {
            rezultat.reject("korisnik.spremanje",
                    "Spremanje nije uspjelo. Email možda već postoji; provjerite podatke.");
            return "korisnici/uredi";
        }
        return "redirect:/korisnici";
    }

    @PostMapping("/deaktiviraj/{id}")
    public String deaktiviraj(@PathVariable Long id) {
        korisnikService.deaktiviraj(id);
        return "redirect:/korisnici";
    }
}
