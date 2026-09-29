package ba.sum.fsre.zupniured.service;

import ba.sum.fsre.zupniured.dto.KrstenjeForma;
import ba.sum.fsre.zupniured.model.Krstenje;
import ba.sum.fsre.zupniured.model.Zupljanin;
import ba.sum.fsre.zupniured.repository.KrstenjeRepository;
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
public class KrstenjeService {

    private static final String DUPLIKAT_PORUKA = "Odabrani župljanin već ima evidentirano krštenje.";

    private final KrstenjeRepository krstenjeRepository;
    private final ZupljaninRepository zupljaninRepository;
    private final Validator validator;

    public KrstenjeService(KrstenjeRepository krstenjeRepository,
                           ZupljaninRepository zupljaninRepository, Validator validator) {
        this.krstenjeRepository = krstenjeRepository;
        this.zupljaninRepository = zupljaninRepository;
        this.validator = validator;
    }

    public List<Krstenje> pronadjiSve(String pretraga) {
        Sort sort = Sort.by(Sort.Order.desc("datumKrstenja"), Sort.Order.desc("id"));
        if (pretraga == null || pretraga.isBlank()) {
            return krstenjeRepository.findAll(sort);
        }
        String pojam = pretraga.strip();
        Integer godina = null;
        try {
            godina = Integer.valueOf(pojam);
        } catch (NumberFormatException ignored) {
            // Tekstualni pojam pretražuje ime, prezime i broj matice.
        }
        return krstenjeRepository.pretrazi(pojam, godina, sort);
    }

    public List<Zupljanin> zupljaniZaIzbor() {
        return zupljaninRepository.findAll(Sort.by("prezime", "ime", "id"));
    }

    public Krstenje pronadjiPoId(Long id) {
        return krstenjeRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Krštenje nije pronađeno."));
    }

    public KrstenjeForma formaZaUredjivanje(Long id) {
        Krstenje krstenje = pronadjiPoId(id);
        KrstenjeForma forma = new KrstenjeForma();
        forma.setZupljaninId(krstenje.getZupljanin().getId());
        forma.setDatumKrstenja(krstenje.getDatumKrstenja());
        forma.setMjestoKrstenja(krstenje.getMjestoKrstenja());
        forma.setImeOca(krstenje.getImeOca());
        forma.setImeMajke(krstenje.getImeMajke());
        forma.setImeKuma(krstenje.getImeKuma());
        forma.setImeKume(krstenje.getImeKume());
        forma.setKrstitelj(krstenje.getKrstitelj());
        forma.setBrojMatice(krstenje.getBrojMatice());
        forma.setGodinaMatice(krstenje.getGodinaMatice());
        forma.setNapomena(krstenje.getNapomena());
        return forma;
    }

    @Transactional
    public Krstenje dodaj(KrstenjeForma podaci) {
        Zupljanin zupljanin = provjeriPodatke(podaci);
        provjeriJedinstvenost(zupljanin.getId(), null);
        Krstenje krstenje = new Krstenje();
        prenesiPodatke(podaci, zupljanin, krstenje);
        return spremi(krstenje);
    }

    @Transactional
    public void uredi(Long id, KrstenjeForma podaci) {
        Krstenje krstenje = pronadjiPoId(id);
        Zupljanin zupljanin = provjeriPodatke(podaci);
        provjeriJedinstvenost(zupljanin.getId(), id);
        prenesiPodatke(podaci, zupljanin, krstenje);
        spremi(krstenje);
    }

    private void provjeriJedinstvenost(Long zupljaninId, Long krstenjeId) {
        boolean postoji = krstenjeId == null
                ? krstenjeRepository.existsByZupljaninId(zupljaninId)
                : krstenjeRepository.existsByZupljaninIdAndIdNot(zupljaninId, krstenjeId);
        if (postoji) {
            throw new KrstenjePodaciException(DUPLIKAT_PORUKA);
        }
    }

    private Krstenje spremi(Krstenje krstenje) {
        try {
            return krstenjeRepository.saveAndFlush(krstenje);
        } catch (DataIntegrityViolationException greska) {
            // Baza štiti i od dva istodobna zahtjeva koja prođu servisnu provjeru.
            if (greska.getCause() instanceof org.hibernate.exception.ConstraintViolationException uzrok
                    && "uk_krstenja_zupljanin".equals(uzrok.getConstraintName())) {
                throw new KrstenjePodaciException(DUPLIKAT_PORUKA);
            }
            throw greska;
        }
    }

    private Zupljanin provjeriPodatke(KrstenjeForma podaci) {
        var greske = validator.validate(podaci);
        if (!greske.isEmpty()) {
            throw new ConstraintViolationException(greske);
        }
        return zupljaninRepository.findById(podaci.getZupljaninId())
                .orElseThrow(KrstenjePodaciException::new);
    }

    private void prenesiPodatke(KrstenjeForma podaci, Zupljanin zupljanin, Krstenje krstenje) {
        krstenje.setZupljanin(zupljanin);
        krstenje.setDatumKrstenja(podaci.getDatumKrstenja());
        krstenje.setMjestoKrstenja(podaci.getMjestoKrstenja());
        krstenje.setImeOca(podaci.getImeOca());
        krstenje.setImeMajke(podaci.getImeMajke());
        krstenje.setImeKuma(podaci.getImeKuma());
        krstenje.setImeKume(podaci.getImeKume());
        krstenje.setKrstitelj(podaci.getKrstitelj());
        krstenje.setBrojMatice(podaci.getBrojMatice());
        krstenje.setGodinaMatice(podaci.getGodinaMatice());
        krstenje.setNapomena(podaci.getNapomena());
    }
}
