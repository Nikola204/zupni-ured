package ba.sum.fsre.zupniured.service;

import ba.sum.fsre.zupniured.dto.*;
import ba.sum.fsre.zupniured.model.*;
import ba.sum.fsre.zupniured.repository.*;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class DokumentService {
    private static final Sort SORT = Sort.by(Sort.Direction.DESC, "datumZahtjeva", "datumKreiranja", "id");
    private final DokumentRepository dokumenti;
    private final ZupljaninRepository zupljani;
    private final KorisnikRepository korisnici;
    private final KrstenjeRepository krstenja;
    private final PrvaPricestRepository pricesti;
    private final KrizmaRepository krizme;
    private final VjencanjeRepository vjencanja;
    private final Validator validator;

    public DokumentService(DokumentRepository dokumenti, ZupljaninRepository zupljani,
                           KorisnikRepository korisnici, KrstenjeRepository krstenja,
                           PrvaPricestRepository pricesti, KrizmaRepository krizme,
                           VjencanjeRepository vjencanja, Validator validator) {
        this.dokumenti = dokumenti; this.zupljani = zupljani; this.korisnici = korisnici;
        this.krstenja = krstenja; this.pricesti = pricesti; this.krizme = krizme;
        this.vjencanja = vjencanja; this.validator = validator;
    }

    public List<Dokument> pronadjiSve(DokumentFilter filter) {
        provjeri(filter);
        return dokumenti.findAll((root, query, cb) -> {
            List<Predicate> uvjeti = new ArrayList<>();
            if (filter.getPretraga() != null && !filter.getPretraga().isBlank()) {
                String pojam = filter.getPretraga().strip().toLowerCase(Locale.ROOT);
                uvjeti.add(cb.or(
                    cb.gt(cb.locate(cb.lower(root.get("imePrimatelja")), pojam), 0),
                    cb.gt(cb.locate(cb.lower(root.get("prezimePrimatelja")), pojam), 0),
                    cb.gt(cb.locate(cb.lower(root.get("svrha")), pojam), 0)));
            }
            if (filter.getVrstaDokumenta() != null) uvjeti.add(cb.equal(root.get("vrstaDokumenta"), filter.getVrstaDokumenta()));
            if (filter.getStatus() != null) uvjeti.add(cb.equal(root.get("status"), filter.getStatus()));
            if (filter.getOd() != null) uvjeti.add(cb.greaterThanOrEqualTo(root.get("datumZahtjeva"), filter.getOd()));
            if (filter.getDoDatuma() != null) uvjeti.add(cb.lessThanOrEqualTo(root.get("datumZahtjeva"), filter.getDoDatuma()));
            return cb.and(uvjeti.toArray(Predicate[]::new));
        }, SORT);
    }

    public List<Dokument> zaZupljanina(Long id) { return dokumenti.findByZupljaninId(id, SORT); }

    public Dokument pronadjiPoId(Long id) {
        return dokumenti.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Dokument nije pronađen."));
    }

    public List<Zupljanin> zupljaniZaIzbor() { return zupljani.findAll(Sort.by("prezime", "ime", "id")); }
    public List<Krstenje> krstenjaZaIzbor() { return krstenja.findAll(Sort.by(Sort.Direction.DESC, "datumKrstenja", "id")); }
    public List<PrvaPricest> pricestiZaIzbor() { return pricesti.findAll(Sort.by(Sort.Direction.DESC, "datumPricesti", "id")); }
    public List<Krizma> krizmeZaIzbor() { return krizme.findAll(Sort.by(Sort.Direction.DESC, "datumKrizme", "id")); }
    public List<Vjencanje> vjencanjaZaIzbor() { return vjencanja.findAll(Sort.by(Sort.Direction.DESC, "datumVjencanja", "id")); }

    public DokumentForma formaZaUredjivanje(Long id) {
        Dokument d = pronadjiPoId(id);
        DokumentForma f = new DokumentForma();
        f.setImePrimatelja(d.getImePrimatelja());
        f.setPrezimePrimatelja(d.getPrezimePrimatelja());
        f.setVrstaDokumenta(d.getVrstaDokumenta());
        f.setStatus(d.getStatus());
        f.setDatumZahtjeva(d.getDatumZahtjeva());
        f.setDatumIzdavanja(d.getDatumIzdavanja());
        f.setSvrha(d.getSvrha());
        f.setNapomena(d.getNapomena());
        f.setZupljaninId(d.getZupljanin() == null ? null : d.getZupljanin().getId());
        f.setKrstenjeId(d.getKrstenje() == null ? null : d.getKrstenje().getId());
        f.setPrvaPricestId(d.getPrvaPricest() == null ? null : d.getPrvaPricest().getId());
        f.setKrizmaId(d.getKrizma() == null ? null : d.getKrizma().getId());
        f.setVjencanjeId(d.getVjencanje() == null ? null : d.getVjencanje().getId());
        return f;
    }

    @Transactional
    public Dokument dodaj(DokumentForma forma, String emailKorisnika) {
        Dokument dokument = new Dokument();
        pripremi(forma, dokument, emailKorisnika);
        return dokumenti.saveAndFlush(dokument);
    }

    @Transactional
    public void uredi(Long id, DokumentForma forma, String emailKorisnika) {
        Dokument dokument = pronadjiPoId(id);
        pripremi(forma, dokument, emailKorisnika);
        dokumenti.saveAndFlush(dokument);
    }

    @Transactional
    public void promijeniStatus(Long id, DokumentStatusForma forma, String emailKorisnika) {
        provjeri(forma);
        Dokument dokument = pronadjiPoId(id);
        // Ponovno provjeri i izvor prije izdavanja, čak i ako je izvor naknadno uređivan.
        DokumentForma sviPodaci = formaZaUredjivanje(id);
        sviPodaci.setStatus(forma.getStatus());
        sviPodaci.setDatumIzdavanja(forma.getDatumIzdavanja());
        pripremi(sviPodaci, dokument, emailKorisnika);
        dokumenti.saveAndFlush(dokument);
    }

    private void pripremi(DokumentForma f, Dokument d, String emailKorisnika) {
        provjeri(f);
        provjeriOdabirIzvora(f);
        Krstenje krstenje = f.getKrstenjeId() == null ? null : krstenja.findById(f.getKrstenjeId())
                .orElseThrow(() -> greska("krstenjeId", "Odabrano krštenje nije pronađeno."));
        PrvaPricest pricest = f.getPrvaPricestId() == null ? null : pricesti.findById(f.getPrvaPricestId())
                .orElseThrow(() -> greska("prvaPricestId", "Odabrana prva pričest nije pronađena."));
        Krizma krizma = f.getKrizmaId() == null ? null : krizme.findById(f.getKrizmaId())
                .orElseThrow(() -> greska("krizmaId", "Odabrana krizma nije pronađena."));
        Vjencanje vjencanje = f.getVjencanjeId() == null ? null : vjencanja.findById(f.getVjencanjeId())
                .orElseThrow(() -> greska("vjencanjeId", "Odabrano vjenčanje nije pronađeno."));
        Zupljanin osoba = f.getZupljaninId() == null ? null : zupljani.findById(f.getZupljaninId())
                .orElseThrow(() -> greska("zupljaninId", "Odabrani župljanin nije pronađen."));
        Zupljanin izvornaOsoba = krstenje != null ? krstenje.getZupljanin()
                : pricest != null ? pricest.getZupljanin() : krizma != null ? krizma.getZupljanin() : null;
        if (izvornaOsoba != null) {
            if (osoba != null && !osoba.getId().equals(izvornaOsoba.getId())) {
                throw greska("zupljaninId", "Odabrani župljanin ne odgovara osobi iz sakramentalnog zapisa.");
            }
            osoba = izvornaOsoba;
        }

        String ime = f.getImePrimatelja(), prezime = f.getPrezimePrimatelja();
        if (osoba != null) {
            // Kod običnog uređivanja čuvamo ranije spremljeno povijesno ime.
            boolean istaOsoba = d.getZupljanin() != null && d.getZupljanin().getId().equals(osoba.getId());
            ime = istaOsoba ? d.getImePrimatelja() : osoba.getIme();
            prezime = istaOsoba ? d.getPrezimePrimatelja() : osoba.getPrezime();
        }
        if (ime == null || ime.isBlank()) throw greska("imePrimatelja", "Ime primatelja je obavezno.");
        if (prezime == null || prezime.isBlank()) throw greska("prezimePrimatelja", "Prezime primatelja je obavezno.");

        LocalDate izdavanje = f.getDatumIzdavanja();
        Korisnik izdao = d.getIzdaoKorisnik();
        if (izdao != null) {
            // Prvo izdavanje ostaje sačuvano i nakon promjene statusa.
            if (izdavanje != null && !izdavanje.equals(d.getDatumIzdavanja())) {
                throw greska("datumIzdavanja", "Datum prvog izdavanja ostaje sačuvan. Za novo izdavanje unesite novi zahtjev.");
            }
            izdavanje = d.getDatumIzdavanja();
        }
        if (f.getStatus() == StatusDokumenta.IZDAN && izdavanje == null) {
            throw greska("datumIzdavanja", "Za izdani dokument unesite datum izdavanja.");
        }
        if (izdavanje != null && izdavanje.isBefore(f.getDatumZahtjeva())) {
            throw greska("datumIzdavanja", "Datum izdavanja ne smije biti prije datuma zahtjeva.");
        }
        if (f.getStatus() == StatusDokumenta.IZDAN && izdao == null) {
            izdao = korisnici.findByEmailIgnoreCase(emailKorisnika).filter(Korisnik::isAktivan)
                    .orElseThrow(() -> greska("status", "Prijavljeni djelatnik nije pronađen ili nije aktivan."));
        }

        d.setZupljanin(osoba);
        d.setImePrimatelja(ime); d.setPrezimePrimatelja(prezime);
        d.setKrstenje(krstenje); d.setPrvaPricest(pricest); d.setKrizma(krizma); d.setVjencanje(vjencanje);
        d.setVrstaDokumenta(f.getVrstaDokumenta()); d.setStatus(f.getStatus());
        d.setDatumZahtjeva(f.getDatumZahtjeva()); d.setDatumIzdavanja(izdavanje);
        d.setIzdaoKorisnik(izdao);
        d.setSvrha(f.getSvrha()); d.setNapomena(f.getNapomena());
    }

    private void provjeriOdabirIzvora(DokumentForma f) {
        provjeriIzvor(f.getVrstaDokumenta() == VrstaDokumenta.KRSNI_LIST, f.getKrstenjeId(), "krstenjeId", "krštenje");
        provjeriIzvor(f.getVrstaDokumenta() == VrstaDokumenta.POTVRDA_O_PRVOJ_PRICESTI, f.getPrvaPricestId(), "prvaPricestId", "prvu pričest");
        provjeriIzvor(f.getVrstaDokumenta() == VrstaDokumenta.POTVRDA_O_KRIZMI, f.getKrizmaId(), "krizmaId", "krizmu");
        provjeriIzvor(f.getVrstaDokumenta() == VrstaDokumenta.POTVRDA_O_VJENCANJU, f.getVjencanjeId(), "vjencanjeId", "vjenčanje");
    }

    private void provjeriIzvor(boolean potreban, Long id, String polje, String naziv) {
        if (potreban && id == null) throw greska(polje, "Za odabranu vrstu dokumenta odaberite " + naziv + ".");
        if (!potreban && id != null) throw greska(polje, "Odabrani izvor ne odgovara vrsti dokumenta. Uklonite odabir za " + naziv + ".");
    }

    private void provjeri(Object forma) {
        var greske = validator.validate(forma);
        if (!greske.isEmpty()) throw new ConstraintViolationException(greske);
    }
    private DokumentPodaciException greska(String polje, String poruka) {
        return new DokumentPodaciException(polje, poruka);
    }
}
