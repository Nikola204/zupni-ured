package ba.sum.fsre.zupniured.service;

import ba.sum.fsre.zupniured.dto.ObavijestForma;
import ba.sum.fsre.zupniured.dto.ObavijestFilter;
import ba.sum.fsre.zupniured.model.Obavijest;
import ba.sum.fsre.zupniured.repository.ObavijestRepository;
import ba.sum.fsre.zupniured.repository.KorisnikRepository;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class ObavijestService {
    private static final Sort ADMIN_SORT = Sort.by(Sort.Direction.DESC, "datumObjave", "datumKreiranja", "id");
    private static final Sort JAVNI_SORT = Sort.by(Sort.Direction.DESC, "istaknuta").and(ADMIN_SORT);
    private final ObavijestRepository repository;
    private final KorisnikRepository korisnici;
    private final Validator validator;

    public ObavijestService(ObavijestRepository repository, KorisnikRepository korisnici, Validator validator) {
        this.repository = repository;
        this.korisnici = korisnici;
        this.validator = validator;
    }

    public List<Obavijest> pronadjiSve(String pretraga, ObavijestFilter filter) {
        Specification<Obavijest> uvjet = (root, query, cb) -> cb.conjunction();
        if (pretraga != null && !pretraga.isBlank()) {
            String pojam = pretraga.strip().toLowerCase(Locale.ROOT);
            uvjet = uvjet.and((root, query, cb) -> cb.or(
                    cb.gt(cb.locate(cb.lower(root.get("naslov")), pojam), 0),
                    cb.gt(cb.locate(cb.lower(root.get("sadrzaj")), pojam), 0)));
        }
        if (filter == ObavijestFilter.AKTIVNE || filter == ObavijestFilter.NEAKTIVNE) {
            uvjet = uvjet.and((root, query, cb) -> cb.equal(root.get("aktivna"), filter == ObavijestFilter.AKTIVNE));
        } else if (filter == ObavijestFilter.ISTAKNUTE) {
            uvjet = uvjet.and((root, query, cb) -> cb.isTrue(root.get("istaknuta")));
        }
        return repository.findAll(uvjet, ADMIN_SORT);
    }

    private Specification<Obavijest> javnoDostupne() {
        LocalDate danas = LocalDate.now();
        return (root, query, cb) -> cb.and(
                cb.isTrue(root.get("aktivna")),
                cb.lessThanOrEqualTo(root.get("datumObjave"), danas),
                cb.or(cb.isNull(root.get("datumIsteka")), cb.greaterThanOrEqualTo(root.get("datumIsteka"), danas)));
    }

    public List<Obavijest> javneObavijesti() {
        return repository.findAll(javnoDostupne(), JAVNI_SORT);
    }

    public List<Obavijest> obavijestiZaPocetnu() {
        return repository.findAll(javnoDostupne(), PageRequest.of(0, 3, JAVNI_SORT)).getContent();
    }

    public Obavijest javnaPoId(Long id) {
        return repository.findOne(javnoDostupne().and((root, query, cb) -> cb.equal(root.get("id"), id)))
                .orElseThrow(this::nijePronadjena);
    }

    public Obavijest pronadjiPoId(Long id) {
        return repository.findById(id).orElseThrow(this::nijePronadjena);
    }

    private ResponseStatusException nijePronadjena() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Obavijest nije pronađena.");
    }

    public ObavijestForma formaZaUredjivanje(Long id) {
        Obavijest zapis = pronadjiPoId(id);
        ObavijestForma forma = new ObavijestForma();
        forma.setNaslov(zapis.getNaslov());
        forma.setSadrzaj(zapis.getSadrzaj());
        forma.setDatumObjave(zapis.getDatumObjave());
        forma.setDatumIsteka(zapis.getDatumIsteka());
        forma.setIstaknuta(zapis.isIstaknuta());
        forma.setAktivna(zapis.isAktivna());
        return forma;
    }

    @Transactional
    public Obavijest dodaj(ObavijestForma forma, String emailAutora) {
        provjeri(forma);
        var autor = korisnici.findByEmailIgnoreCase(emailAutora)
                .filter(k -> k.isAktivan())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Prijavljeni korisnik nije dostupan."));
        Obavijest zapis = new Obavijest();
        zapis.setAutor(autor);
        prenesi(forma, zapis);
        return repository.saveAndFlush(zapis);
    }

    @Transactional
    public void uredi(Long id, ObavijestForma forma) {
        provjeri(forma);
        Obavijest zapis = pronadjiPoId(id);
        prenesi(forma, zapis);
        repository.saveAndFlush(zapis);
    }

    @Transactional
    public void postaviAktivnost(Long id, boolean aktivna) {
        Obavijest zapis = pronadjiPoId(id);
        zapis.setAktivna(aktivna);
        repository.saveAndFlush(zapis);
    }

    private void provjeri(ObavijestForma forma) {
        var greske = validator.validate(forma);
        if (!greske.isEmpty()) throw new ConstraintViolationException(greske);
    }

    private void prenesi(ObavijestForma forma, Obavijest zapis) {
        zapis.setNaslov(forma.getNaslov());
        zapis.setSadrzaj(forma.getSadrzaj());
        zapis.setDatumObjave(forma.getDatumObjave());
        zapis.setDatumIsteka(forma.getDatumIsteka());
        zapis.setIstaknuta(forma.isIstaknuta());
        zapis.setAktivna(forma.isAktivna());
    }
}
