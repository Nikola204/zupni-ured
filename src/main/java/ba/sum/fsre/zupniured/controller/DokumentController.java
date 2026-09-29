package ba.sum.fsre.zupniured.controller;

import ba.sum.fsre.zupniured.dto.*;
import ba.sum.fsre.zupniured.model.*;
import ba.sum.fsre.zupniured.service.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
@RequestMapping("/dokumenti")
public class DokumentController {
    private final DokumentService service;
    private final DokumentPdfService pdfService;
    public DokumentController(DokumentService service, DokumentPdfService pdfService) {
        this.service = service;
        this.pdfService = pdfService;
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        var pdf = pdfService.generiraj(id);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(pdf.nazivDatoteke()).build().toString())
                .cacheControl(CacheControl.noStore())
                .body(pdf.sadrzaj());
    }

    @ModelAttribute("vrste")
    public VrstaDokumenta[] vrste() { return VrstaDokumenta.values(); }
    @ModelAttribute("statusi")
    public StatusDokumenta[] statusi() { return StatusDokumenta.values(); }

    @InitBinder("dokument")
    public void polja(WebDataBinder binder) {
        binder.setAllowedFields("zupljaninId", "imePrimatelja", "prezimePrimatelja", "vrstaDokumenta",
                "status", "datumZahtjeva", "datumIzdavanja", "svrha", "napomena",
                "krstenjeId", "prvaPricestId", "krizmaId", "vjencanjeId");
    }
    @InitBinder("dokumentStatus")
    public void statusPolja(WebDataBinder binder) { binder.setAllowedFields("status", "datumIzdavanja"); }
    @InitBinder("dokumentFilter")
    public void filterPolja(WebDataBinder binder) {
        binder.setAllowedFields("pretraga", "vrstaDokumenta", "status", "od", "doDatuma");
    }

    @GetMapping
    public String lista(@Valid @ModelAttribute("dokumentFilter") DokumentFilter filter, BindingResult rezultat, Model model) {
        model.addAttribute("dokumenti", rezultat.hasErrors() ? List.of() : service.pronadjiSve(filter));
        return "dokumenti/lista";
    }

    @GetMapping("/novi")
    public String novi(Model model) {
        model.addAttribute("dokument", new DokumentForma());
        return pripremiFormu(model, "dokumenti/forma");
    }

    @PostMapping("/novi")
    public String dodaj(@Valid @ModelAttribute("dokument") DokumentForma forma, BindingResult rezultat,
                        Authentication authentication, Model model) {
        if (!rezultat.hasErrors()) {
            try {
                return "redirect:/dokumenti/" + service.dodaj(forma, authentication.getName()).getId();
            } catch (DokumentPodaciException greska) {
                rezultat.rejectValue(greska.getPolje(), "dokument.podaci", greska.getMessage());
            }
        }
        return pripremiFormu(model, "dokumenti/forma");
    }

    @GetMapping("/uredi/{id}")
    public String uredjivanje(@PathVariable Long id, Model model) {
        model.addAttribute("dokument", service.formaZaUredjivanje(id));
        model.addAttribute("dokumentId", id);
        return pripremiFormu(model, "dokumenti/uredi");
    }

    @PostMapping("/uredi/{id}")
    public String uredi(@PathVariable Long id, @Valid @ModelAttribute("dokument") DokumentForma forma,
                        BindingResult rezultat, Authentication authentication, Model model) {
        service.pronadjiPoId(id);
        model.addAttribute("dokumentId", id);
        if (!rezultat.hasErrors()) {
            try {
                service.uredi(id, forma, authentication.getName());
                return "redirect:/dokumenti/" + id;
            } catch (DokumentPodaciException greska) {
                rezultat.rejectValue(greska.getPolje(), "dokument.podaci", greska.getMessage());
            }
        }
        return pripremiFormu(model, "dokumenti/uredi");
    }

    @GetMapping("/{id}")
    public String detalji(@PathVariable Long id, Model model) {
        Dokument dokument = service.pronadjiPoId(id);
        model.addAttribute("dokument", dokument);
        DokumentStatusForma forma = new DokumentStatusForma();
        forma.setStatus(dokument.getStatus());
        forma.setDatumIzdavanja(dokument.getDatumIzdavanja());
        model.addAttribute("dokumentStatus", forma);
        return "dokumenti/detalji";
    }

    @PostMapping("/{id}/status")
    public String status(@PathVariable Long id,
                         @Valid @ModelAttribute("dokumentStatus") DokumentStatusForma forma,
                         BindingResult rezultat, Authentication authentication, Model model) {
        model.addAttribute("dokument", service.pronadjiPoId(id));
        if (!rezultat.hasErrors()) {
            try {
                service.promijeniStatus(id, forma, authentication.getName());
                return "redirect:/dokumenti/" + id;
            } catch (DokumentPodaciException greska) {
                rezultat.reject("dokument.status", greska.getMessage());
            }
        }
        return "dokumenti/detalji";
    }

    private String pripremiFormu(Model model, String pogled) {
        model.addAttribute("zupljani", service.zupljaniZaIzbor());
        model.addAttribute("krstenja", service.krstenjaZaIzbor());
        model.addAttribute("pricesti", service.pricestiZaIzbor());
        model.addAttribute("krizme", service.krizmeZaIzbor());
        model.addAttribute("vjencanja", service.vjencanjaZaIzbor());
        return pogled;
    }
}
