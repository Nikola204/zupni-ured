package ba.sum.fsre.zupniured.service;

import ba.sum.fsre.zupniured.model.Zupljanin;
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
public class ZupljaninService {

    private final ZupljaninRepository zupljaninRepository;
    private final Validator validator;

    public ZupljaninService(ZupljaninRepository zupljaninRepository, Validator validator) {
        this.zupljaninRepository = zupljaninRepository;
        this.validator = validator;
    }

    public List<Zupljanin> pronadjiSve(String pretraga) {
        Sort sort = Sort.by("prezime", "ime", "id");
        if (pretraga == null || pretraga.isBlank()) {
            return zupljaninRepository.findAll(sort);
        }
        String pojam = pretraga.strip();
        return zupljaninRepository.findByImeContainingIgnoreCaseOrPrezimeContainingIgnoreCase(
                pojam, pojam, sort);
    }

    public Zupljanin pronadjiPoId(Long id) {
        return zupljaninRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Župljanin nije pronađen."));
    }

    @Transactional
    public Zupljanin dodaj(Zupljanin podaci) {
        provjeriPodatke(podaci);
        Zupljanin zupljanin = new Zupljanin();
        prenesiPodatke(podaci, zupljanin);
        return zupljaninRepository.saveAndFlush(zupljanin);
    }

    @Transactional
    public void uredi(Long id, Zupljanin podaci) {
        provjeriPodatke(podaci);
        Zupljanin zupljanin = pronadjiPoId(id);
        prenesiPodatke(podaci, zupljanin);
        zupljaninRepository.saveAndFlush(zupljanin);
    }

    @Transactional
    public void deaktiviraj(Long id) {
        pronadjiPoId(id).setAktivan(false);
    }

    @Transactional
    public void reaktiviraj(Long id) {
        pronadjiPoId(id).setAktivan(true);
    }

    private void prenesiPodatke(Zupljanin podaci, Zupljanin zupljanin) {
        // ID, status i datum kreiranja nisu dio običnog uređivanja podataka.
        zupljanin.setIme(podaci.getIme());
        zupljanin.setPrezime(podaci.getPrezime());
        zupljanin.setDatumRodjenja(podaci.getDatumRodjenja());
        zupljanin.setMjestoRodjenja(podaci.getMjestoRodjenja());
        zupljanin.setAdresa(podaci.getAdresa());
        zupljanin.setTelefon(podaci.getTelefon());
        zupljanin.setEmail(podaci.getEmail());
        zupljanin.setNapomena(podaci.getNapomena());
    }

    private void provjeriPodatke(Zupljanin podaci) {
        var greske = validator.validate(podaci);
        if (!greske.isEmpty()) {
            throw new ConstraintViolationException(greske);
        }
    }
}
