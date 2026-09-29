package ba.sum.fsre.zupniured.service;

import ba.sum.fsre.zupniured.dto.PrvaPricestForma;
import ba.sum.fsre.zupniured.model.PrvaPricest;
import ba.sum.fsre.zupniured.model.Zupljanin;
import ba.sum.fsre.zupniured.repository.PrvaPricestRepository;
import ba.sum.fsre.zupniured.repository.ZupljaninRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class PrvaPricestService {

    private static final String DUPLIKAT_PORUKA = "Odabrani župljanin već ima evidentiranu prvu pričest.";

    private final PrvaPricestRepository prvaPricestRepository;
    private final ZupljaninRepository zupljaninRepository;
    private final Validator validator;

    public PrvaPricestService(PrvaPricestRepository prvaPricestRepository,
                           ZupljaninRepository zupljaninRepository, Validator validator) {
        this.prvaPricestRepository = prvaPricestRepository;
        this.zupljaninRepository = zupljaninRepository;
        this.validator = validator;
    }

    public List<PrvaPricest> pronadjiSve(String pretraga) {
        Sort sort = Sort.by(Sort.Order.desc("datumPricesti"), Sort.Order.desc("id"));
        if (pretraga == null || pretraga.isBlank()) {
            return prvaPricestRepository.findAll(sort);
        }
        String pojam = pretraga.strip();
        Integer godina = null;
        try {
            godina = Integer.valueOf(pojam);
        } catch (NumberFormatException ignored) {
            // Tekstualni pojam pretražuje ime, prezime i broj matice.
        }
        return prvaPricestRepository.pretrazi(pojam, godina, sort);
    }

    public List<Zupljanin> zupljaniZaIzbor(Long pricestId) {
        return prvaPricestRepository.pronadjiZupljaneZaIzbor(pricestId, Sort.by("prezime", "ime", "id"));
    }

    public PrvaPricest pronadjiPoId(Long id) {
        return prvaPricestRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Prva pričest nije pronađena."));
    }

    public PrvaPricestForma formaZaUredjivanje(Long id) {
        PrvaPricest prvaPricest = pronadjiPoId(id);
        PrvaPricestForma forma = new PrvaPricestForma();
        forma.setZupljaninId(prvaPricest.getZupljanin().getId());
        forma.setDatumPricesti(prvaPricest.getDatumPricesti());
        forma.setMjestoPricesti(prvaPricest.getMjestoPricesti());
        forma.setBrojMatice(prvaPricest.getBrojMatice());
        forma.setGodinaMatice(prvaPricest.getGodinaMatice());
        forma.setNapomena(prvaPricest.getNapomena());
        return forma;
    }

    @Transactional
    public PrvaPricest dodaj(PrvaPricestForma podaci) {
        Zupljanin zupljanin = provjeriPodatke(podaci);
        provjeriJedinstvenost(zupljanin.getId(), null);
        PrvaPricest prvaPricest = new PrvaPricest();
        prenesiPodatke(podaci, zupljanin, prvaPricest);
        return spremi(prvaPricest);
    }

    @Transactional
    public void uredi(Long id, PrvaPricestForma podaci) {
        PrvaPricest prvaPricest = pronadjiPoId(id);
        Zupljanin zupljanin = provjeriPodatke(podaci);
        provjeriJedinstvenost(zupljanin.getId(), id);
        prenesiPodatke(podaci, zupljanin, prvaPricest);
        spremi(prvaPricest);
    }

    private void provjeriJedinstvenost(Long zupljaninId, Long prvaPricestId) {
        boolean postoji = prvaPricestId == null
                ? prvaPricestRepository.existsByZupljaninId(zupljaninId)
                : prvaPricestRepository.existsByZupljaninIdAndIdNot(zupljaninId, prvaPricestId);
        if (postoji) {
            throw new PrvaPricestPodaciException(DUPLIKAT_PORUKA);
        }
    }

    private PrvaPricest spremi(PrvaPricest prvaPricest) {
        try {
            return prvaPricestRepository.saveAndFlush(prvaPricest);
        } catch (DataIntegrityViolationException greska) {
            // Baza štiti i od dva istodobna zahtjeva koja prođu servisnu provjeru.
            if (greska.getCause() instanceof org.hibernate.exception.ConstraintViolationException uzrok
                    && "uk_prve_pricesti_zupljanin".equals(uzrok.getConstraintName())) {
                throw new PrvaPricestPodaciException(DUPLIKAT_PORUKA);
            }
            throw greska;
        }
    }

    private Zupljanin provjeriPodatke(PrvaPricestForma podaci) {
        var greske = validator.validate(podaci);
        if (!greske.isEmpty()) {
            throw new ConstraintViolationException(greske);
        }
        return zupljaninRepository.findById(podaci.getZupljaninId())
                .orElseThrow(PrvaPricestPodaciException::new);
    }

    private void prenesiPodatke(PrvaPricestForma podaci, Zupljanin zupljanin, PrvaPricest prvaPricest) {
        prvaPricest.setZupljanin(zupljanin);
        prvaPricest.setDatumPricesti(podaci.getDatumPricesti());
        prvaPricest.setMjestoPricesti(podaci.getMjestoPricesti());
        prvaPricest.setBrojMatice(podaci.getBrojMatice());
        prvaPricest.setGodinaMatice(podaci.getGodinaMatice());
        prvaPricest.setNapomena(podaci.getNapomena());
    }
}
