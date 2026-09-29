package ba.sum.fsre.zupniured.controller;
import ba.sum.fsre.zupniured.model.*;
import ba.sum.fsre.zupniured.dto.MisnaNakanaFilter;
import ba.sum.fsre.zupniured.dto.MisnaNakanaStatusForma;
import ba.sum.fsre.zupniured.service.MisnaNakanaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
@RequestMapping("/misne-nakane")
public class MisnaNakanaController {
    private final MisnaNakanaService service;
    public MisnaNakanaController(MisnaNakanaService service) { this.service = service; }
    @ModelAttribute("statusi")
    public StatusMisneNakane[] statusi() { return StatusMisneNakane.values(); }
    @InitBinder("misnaNakana")
    public void polja(WebDataBinder binder) {
        binder.setAllowedFields("nakana", "datum", "vrijeme", "mjesto", "naruciteljIme",
                "naruciteljPrezime", "naruciteljTelefon", "naruciteljEmail", "svecenik",
                "status", "prilog", "napomena");
    }
    @InitBinder("promjenaStatusa")
    public void poljaStatusa(WebDataBinder binder) { binder.setAllowedFields("status"); }
    @InitBinder("filter")
    public void poljaFiltera(WebDataBinder binder) { binder.setAllowedFields("pretraga", "status", "od", "doDatuma"); }

    @GetMapping
    public String lista(@Valid @ModelAttribute("filter") MisnaNakanaFilter filter,
                        BindingResult rezultat, Model model) {
        model.addAttribute("nakane", rezultat.hasErrors() ? List.of() :
                service.pronadjiSve(filter.getPretraga(), filter.getStatus(), filter.getOd(), filter.getDoDatuma()));
        return "misne-nakane/lista";
    }
    @GetMapping("/novi")
    public String novi(Model model) {
        model.addAttribute("misnaNakana", new MisnaNakana());
        return "misne-nakane/forma";
    }
    @PostMapping("/novi")
    public String dodaj(@Valid @ModelAttribute("misnaNakana") MisnaNakana podaci, BindingResult rezultat) {
        if (rezultat.hasErrors()) return "misne-nakane/forma";
        return "redirect:/misne-nakane/" + service.dodaj(podaci).getId();
    }
    @GetMapping("/uredi/{id}")
    public String uredjivanje(@PathVariable Long id, Model model) {
        model.addAttribute("misnaNakana", service.pronadjiPoId(id));
        model.addAttribute("nakanaId", id);
        return "misne-nakane/uredi";
    }
    @PostMapping("/uredi/{id}")
    public String uredi(@PathVariable Long id, @Valid @ModelAttribute("misnaNakana") MisnaNakana podaci,
                        BindingResult rezultat, Model model) {
        service.pronadjiPoId(id);
        model.addAttribute("nakanaId", id);
        if (rezultat.hasErrors()) return "misne-nakane/uredi";
        service.uredi(id, podaci);
        return "redirect:/misne-nakane/" + id;
    }
    @GetMapping("/{id}")
    public String detalji(@PathVariable Long id, Model model) {
        MisnaNakana zapis = service.pronadjiPoId(id);
        model.addAttribute("misnaNakana", zapis);
        MisnaNakanaStatusForma forma = new MisnaNakanaStatusForma();
        forma.setStatus(zapis.getStatus());
        model.addAttribute("promjenaStatusa", forma);
        return "misne-nakane/detalji";
    }
    @PostMapping("/{id}/status")
    public String status(@PathVariable Long id,
                         @Valid @ModelAttribute("promjenaStatusa") MisnaNakanaStatusForma forma,
                         BindingResult rezultat, Model model) {
        model.addAttribute("misnaNakana", service.pronadjiPoId(id));
        if (rezultat.hasErrors()) return "misne-nakane/detalji";
        service.promijeniStatus(id, forma.getStatus());
        return "redirect:/misne-nakane/" + id;
    }
}
