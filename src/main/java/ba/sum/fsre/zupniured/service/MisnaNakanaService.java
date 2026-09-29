package ba.sum.fsre.zupniured.service;
import ba.sum.fsre.zupniured.model.*;
import ba.sum.fsre.zupniured.repository.MisnaNakanaRepository;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolationException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class MisnaNakanaService {
    private final MisnaNakanaRepository repository;
    private final Validator validator;
    public MisnaNakanaService(MisnaNakanaRepository repository, Validator validator) {
        this.repository = repository;
        this.validator = validator;
    }
    public List<MisnaNakana> pronadjiSve(String pretraga, StatusMisneNakane status,
                                       LocalDate od, LocalDate doDatuma) {
        return repository.findAll((root, query, cb) -> {
            List<Predicate> uvjeti = new ArrayList<>();
            if (pretraga != null && !pretraga.isBlank()) {
                String pojam = pretraga.strip().toLowerCase(Locale.ROOT);
                uvjeti.add(cb.or(
                    cb.gt(cb.locate(cb.lower(root.get("nakana")), pojam), 0),
                    cb.gt(cb.locate(cb.lower(root.get("naruciteljIme")), pojam), 0),
                    cb.gt(cb.locate(cb.lower(root.get("naruciteljPrezime")), pojam), 0),
                    cb.gt(cb.locate(cb.lower(root.get("svecenik")), pojam), 0)));
            }
            if (status != null) uvjeti.add(cb.equal(root.get("status"), status));
            if (od != null) uvjeti.add(cb.greaterThanOrEqualTo(root.get("datum"), od));
            if (doDatuma != null) uvjeti.add(cb.lessThanOrEqualTo(root.get("datum"), doDatuma));
            return cb.and(uvjeti.toArray(Predicate[]::new));
        }, Sort.by("datum", "vrijeme", "id"));
    }
    public MisnaNakana pronadjiPoId(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Misna nakana nije pronađena."));
    }
    @Transactional
    public MisnaNakana dodaj(MisnaNakana podaci) {
        provjeri(podaci);
        MisnaNakana zapis = new MisnaNakana();
        prenesi(podaci, zapis);
        return repository.saveAndFlush(zapis);
    }
    @Transactional
    public void uredi(Long id, MisnaNakana podaci) {
        provjeri(podaci);
        MisnaNakana zapis = pronadjiPoId(id);
        prenesi(podaci, zapis);
        repository.saveAndFlush(zapis);
    }
    @Transactional
    public void promijeniStatus(Long id, StatusMisneNakane status) {
        if (status == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status je obavezan.");
        MisnaNakana zapis = pronadjiPoId(id);
        zapis.setStatus(status);
        repository.saveAndFlush(zapis);
    }
    private void provjeri(MisnaNakana podaci) {
        var greske = validator.validate(podaci);
        if (!greske.isEmpty()) throw new ConstraintViolationException(greske);
    }
    private void prenesi(MisnaNakana izvor, MisnaNakana cilj) {
        cilj.setNakana(izvor.getNakana());
        cilj.setDatum(izvor.getDatum());
        cilj.setVrijeme(izvor.getVrijeme());
        cilj.setMjesto(izvor.getMjesto());
        cilj.setNaruciteljIme(izvor.getNaruciteljIme());
        cilj.setNaruciteljPrezime(izvor.getNaruciteljPrezime());
        cilj.setNaruciteljTelefon(izvor.getNaruciteljTelefon());
        cilj.setNaruciteljEmail(izvor.getNaruciteljEmail());
        cilj.setSvecenik(izvor.getSvecenik());
        cilj.setStatus(izvor.getStatus());
        cilj.setPrilog(izvor.getPrilog());
        cilj.setNapomena(izvor.getNapomena());
    }
}
