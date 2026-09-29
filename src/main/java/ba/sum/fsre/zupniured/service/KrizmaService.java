package ba.sum.fsre.zupniured.service;

import ba.sum.fsre.zupniured.dto.KrizmaForma;
import ba.sum.fsre.zupniured.model.Krizma;
import ba.sum.fsre.zupniured.model.Zupljanin;
import ba.sum.fsre.zupniured.repository.KrizmaRepository;
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
public class KrizmaService {

    private static final String DUPLIKAT_PORUKA = "Odabrani župljanin već ima evidentiranu krizmu.";

    private final KrizmaRepository krizmaRepository;
    private final ZupljaninRepository zupljaninRepository;
    private final Validator validator;

    public KrizmaService(KrizmaRepository krizmaRepository,
                           ZupljaninRepository zupljaninRepository, Validator validator) {
        this.krizmaRepository = krizmaRepository;
        this.zupljaninRepository = zupljaninRepository;
        this.validator = validator;
    }

    public List<Krizma> pronadjiSve(String pretraga) {
        Sort sort = Sort.by(Sort.Order.desc("datumKrizme"), Sort.Order.desc("id"));
        if (pretraga == null || pretraga.isBlank()) {
            return krizmaRepository.findAll(sort);
        }
        String pojam = pretraga.strip();
        Integer godina = null;
        try {
            godina = Integer.valueOf(pojam);
        } catch (NumberFormatException ignored) {
            // Tekstualni pojam pretražuje ime, prezime i broj matice.
        }
        return krizmaRepository.pretrazi(pojam, godina, sort);
    }

    public List<Zupljanin> zupljaniZaIzbor(Long krizmaId) {
        return krizmaRepository.pronadjiZupljaneZaIzbor(krizmaId, Sort.by("prezime", "ime", "id"));
    }

    public Krizma pronadjiPoId(Long id) {
        return krizmaRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Krizma nije pronađena."));
    }

    public KrizmaForma formaZaUredjivanje(Long id) {
        Krizma krizma = pronadjiPoId(id);
        KrizmaForma forma = new KrizmaForma();
        forma.setZupljaninId(krizma.getZupljanin().getId());
        forma.setDatumKrizme(krizma.getDatumKrizme());
        forma.setMjestoKrizme(krizma.getMjestoKrizme());
        forma.setImeKrizmanogKuma(krizma.getImeKrizmanogKuma());
        forma.setKrizmatelj(krizma.getKrizmatelj());
        forma.setBrojMatice(krizma.getBrojMatice());
        forma.setGodinaMatice(krizma.getGodinaMatice());
        forma.setNapomena(krizma.getNapomena());
        return forma;
    }

    @Transactional
    public Krizma dodaj(KrizmaForma podaci) {
        Zupljanin zupljanin = provjeriPodatke(podaci);
        provjeriJedinstvenost(zupljanin.getId(), null);
        Krizma krizma = new Krizma();
        prenesiPodatke(podaci, zupljanin, krizma);
        return spremi(krizma);
    }

    @Transactional
    public void uredi(Long id, KrizmaForma podaci) {
        Krizma krizma = pronadjiPoId(id);
        Zupljanin zupljanin = provjeriPodatke(podaci);
        provjeriJedinstvenost(zupljanin.getId(), id);
        prenesiPodatke(podaci, zupljanin, krizma);
        spremi(krizma);
    }

    private void provjeriJedinstvenost(Long zupljaninId, Long krizmaId) {
        boolean postoji = krizmaId == null
                ? krizmaRepository.existsByZupljaninId(zupljaninId)
                : krizmaRepository.existsByZupljaninIdAndIdNot(zupljaninId, krizmaId);
        if (postoji) {
            throw new KrizmaPodaciException(DUPLIKAT_PORUKA);
        }
    }

    private Krizma spremi(Krizma krizma) {
        try {
            return krizmaRepository.saveAndFlush(krizma);
        } catch (DataIntegrityViolationException greska) {
            // Baza štiti i od dva istodobna zahtjeva koja prođu servisnu provjeru.
            if (greska.getCause() instanceof org.hibernate.exception.ConstraintViolationException uzrok
                    && "uk_krizme_zupljanin".equals(uzrok.getConstraintName())) {
                throw new KrizmaPodaciException(DUPLIKAT_PORUKA);
            }
            throw greska;
        }
    }

    private Zupljanin provjeriPodatke(KrizmaForma podaci) {
        var greske = validator.validate(podaci);
        if (!greske.isEmpty()) {
            throw new ConstraintViolationException(greske);
        }
        return zupljaninRepository.findById(podaci.getZupljaninId())
                .orElseThrow(KrizmaPodaciException::new);
    }

    private void prenesiPodatke(KrizmaForma podaci, Zupljanin zupljanin, Krizma krizma) {
        krizma.setZupljanin(zupljanin);
        krizma.setDatumKrizme(podaci.getDatumKrizme());
        krizma.setMjestoKrizme(podaci.getMjestoKrizme());
        krizma.setImeKrizmanogKuma(podaci.getImeKrizmanogKuma());
        krizma.setKrizmatelj(podaci.getKrizmatelj());
        krizma.setBrojMatice(podaci.getBrojMatice());
        krizma.setGodinaMatice(podaci.getGodinaMatice());
        krizma.setNapomena(podaci.getNapomena());
    }
}
