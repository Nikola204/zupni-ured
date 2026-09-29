package ba.sum.fsre.zupniured.service;

import ba.sum.fsre.zupniured.dto.VjencanjeForma;
import ba.sum.fsre.zupniured.model.Vjencanje;
import ba.sum.fsre.zupniured.model.Zupljanin;
import ba.sum.fsre.zupniured.repository.VjencanjeRepository;
import ba.sum.fsre.zupniured.repository.ZupljaninRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class VjencanjeService {
    private final VjencanjeRepository repository;
    private final ZupljaninRepository zupljaninRepository;
    private final Validator validator;

    public VjencanjeService(VjencanjeRepository repository, ZupljaninRepository zupljaninRepository,
                           Validator validator) {
        this.repository = repository;
        this.zupljaninRepository = zupljaninRepository;
        this.validator = validator;
    }

    public List<Vjencanje> pronadjiSve(String pretraga) {
        Sort sort = Sort.by(Sort.Order.desc("datumVjencanja"), Sort.Order.desc("id"));
        if (pretraga == null || pretraga.isBlank()) return repository.findAll(sort);
        String pojam = pretraga.strip();
        Integer godina = null;
        try {
            godina = Integer.valueOf(pojam);
        } catch (NumberFormatException ignored) {
            // Tekst se pretražuje u povijesnim imenima i broju matice.
        }
        return repository.pretrazi(pojam, godina, sort);
    }

    public List<Zupljanin> zupljaniZaIzbor() {
        return zupljaninRepository.findAll(Sort.by("prezime", "ime", "id"));
    }

    public Vjencanje pronadjiPoId(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Vjenčanje nije pronađeno."));
    }

    public VjencanjeForma formaZaUredjivanje(Long id) {
        Vjencanje zapis = pronadjiPoId(id);
        VjencanjeForma forma = new VjencanjeForma();
        forma.setMladozenjaZupljaninId(zapis.getMladozenjaZupljanin() == null ? null : zapis.getMladozenjaZupljanin().getId());
        forma.setNevjestaZupljaninId(zapis.getNevjestaZupljanin() == null ? null : zapis.getNevjestaZupljanin().getId());
        forma.setImeMladozenje(zapis.getImeMladozenje());
        forma.setPrezimeMladozenje(zapis.getPrezimeMladozenje());
        forma.setImeNevjeste(zapis.getImeNevjeste());
        forma.setPrezimeNevjeste(zapis.getPrezimeNevjeste());
        forma.setDatumVjencanja(zapis.getDatumVjencanja());
        forma.setMjestoVjencanja(zapis.getMjestoVjencanja());
        forma.setSvjedokMladozenje(zapis.getSvjedokMladozenje());
        forma.setSvjedokNevjeste(zapis.getSvjedokNevjeste());
        forma.setSluziteljVjencanja(zapis.getSluziteljVjencanja());
        forma.setBrojMatice(zapis.getBrojMatice());
        forma.setGodinaMatice(zapis.getGodinaMatice());
        forma.setNapomena(zapis.getNapomena());
        return forma;
    }

    @Transactional
    public Vjencanje dodaj(VjencanjeForma forma) {
        Vjencanje zapis = new Vjencanje();
        prenesiPodatke(forma, zapis);
        return repository.saveAndFlush(zapis);
    }

    @Transactional
    public void uredi(Long id, VjencanjeForma forma) {
        Vjencanje zapis = pronadjiPoId(id);
        prenesiPodatke(forma, zapis);
        repository.saveAndFlush(zapis);
    }

    private Zupljanin pronadjiOsobu(Long id, String polje, String naziv) {
        if (id == null) return null;
        return zupljaninRepository.findById(id).orElseThrow(() ->
                new VjencanjePodaciException(polje, naziv + " nije pronađen u evidenciji župljana."));
    }

    private void prenesiPodatke(VjencanjeForma forma, Vjencanje zapis) {
        var greske = validator.validate(forma);
        if (!greske.isEmpty()) throw new ConstraintViolationException(greske);
        // Obje veze provjeravamo prije promjene postojećeg zapisa.
        Zupljanin mladozenja = pronadjiOsobu(forma.getMladozenjaZupljaninId(), "mladozenjaZupljaninId", "Odabrani mladoženja");
        Zupljanin nevjesta = pronadjiOsobu(forma.getNevjestaZupljaninId(), "nevjestaZupljaninId", "Odabrani zapis nevjeste");

        if (mladozenja == null) {
            zapis.setImeMladozenje(forma.getImeMladozenje());
            zapis.setPrezimeMladozenje(forma.getPrezimeMladozenje());
        } else if (!istaOsoba(zapis.getMladozenjaZupljanin(), mladozenja)) {
            zapis.setImeMladozenje(mladozenja.getIme());
            zapis.setPrezimeMladozenje(mladozenja.getPrezime());
        }
        if (nevjesta == null) {
            zapis.setImeNevjeste(forma.getImeNevjeste());
            zapis.setPrezimeNevjeste(forma.getPrezimeNevjeste());
        } else if (!istaOsoba(zapis.getNevjestaZupljanin(), nevjesta)) {
            zapis.setImeNevjeste(nevjesta.getIme());
            zapis.setPrezimeNevjeste(nevjesta.getPrezime());
        }
        // Uz istu vezu čuvamo povijesna imena, čak i ako su u evidenciji župljana promijenjena.
        zapis.setMladozenjaZupljanin(mladozenja);
        zapis.setNevjestaZupljanin(nevjesta);
        zapis.setDatumVjencanja(forma.getDatumVjencanja());
        zapis.setMjestoVjencanja(forma.getMjestoVjencanja());
        zapis.setSvjedokMladozenje(forma.getSvjedokMladozenje());
        zapis.setSvjedokNevjeste(forma.getSvjedokNevjeste());
        zapis.setSluziteljVjencanja(forma.getSluziteljVjencanja());
        zapis.setBrojMatice(forma.getBrojMatice());
        zapis.setGodinaMatice(forma.getGodinaMatice());
        zapis.setNapomena(forma.getNapomena());
    }

    private boolean istaOsoba(Zupljanin prethodna, Zupljanin odabrana) {
        return prethodna != null && prethodna.getId().equals(odabrana.getId());
    }
}
