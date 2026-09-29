package ba.sum.fsre.zupniured.service;

import ba.sum.fsre.zupniured.dto.KorisnikForma;
import ba.sum.fsre.zupniured.model.Korisnik;
import ba.sum.fsre.zupniured.model.Uloga;
import ba.sum.fsre.zupniured.repository.KorisnikRepository;
import jakarta.validation.Validator;
import jakarta.validation.groups.Default;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class KorisnikService {

    private final KorisnikRepository korisnikRepository;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;

    public KorisnikService(KorisnikRepository korisnikRepository,
                           PasswordEncoder passwordEncoder, Validator validator) {
        this.korisnikRepository = korisnikRepository;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
    }

    public List<Korisnik> pronadjiSve() {
        return korisnikRepository.findAll(Sort.by("prezime", "ime", "email"));
    }

    public KorisnikForma pripremiZaUredjivanje(Long id) {
        Korisnik korisnik = pronadjiPoId(id);
        KorisnikForma forma = new KorisnikForma();
        forma.setIme(korisnik.getIme());
        forma.setPrezime(korisnik.getPrezime());
        forma.setEmail(korisnik.getEmail());
        forma.setUloga(korisnik.getUloga());
        forma.setAktivan(korisnik.isAktivan());
        return forma;
    }

    @Transactional
    public Korisnik dodaj(KorisnikForma forma) {
        provjeriPodatke(forma, KorisnikForma.Novi.class);
        if (korisnikRepository.existsByEmailIgnoreCase(forma.getEmail())) {
            throw new KorisnikPodaciException("email", "Korisnik s tom email adresom već postoji.");
        }

        Korisnik korisnik = new Korisnik();
        prenesiPodatke(forma, korisnik);
        korisnik.setLozinka(passwordEncoder.encode(forma.getLozinka()));
        return korisnikRepository.saveAndFlush(korisnik);
    }

    @Transactional
    public void uredi(Long id, KorisnikForma forma) {
        provjeriPodatke(forma, Default.class);
        Korisnik korisnik = pronadjiPoId(id);
        if (korisnikRepository.existsByEmailIgnoreCaseAndIdNot(forma.getEmail(), id)) {
            throw new KorisnikPodaciException("email", "Korisnik s tom email adresom već postoji.");
        }

        prenesiPodatke(forma, korisnik);
        if (forma.getLozinka() != null && !forma.getLozinka().isEmpty()) {
            korisnik.setLozinka(passwordEncoder.encode(forma.getLozinka()));
        }
        korisnikRepository.saveAndFlush(korisnik);
    }

    @Transactional
    public void deaktiviraj(Long id) {
        Korisnik korisnik = pronadjiPoId(id);
        korisnik.setAktivan(false);
        korisnikRepository.save(korisnik);
    }

    @Transactional
    public void kreirajPocetnogAdministratora(KorisnikForma forma) {
        if (korisnikRepository.count() == 0) {
            forma.setUloga(Uloga.ADMIN);
            forma.setAktivan(true);
            dodaj(forma);
        }
    }

    private Korisnik pronadjiPoId(Long id) {
        return korisnikRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Korisnik nije pronađen."));
    }

    private void prenesiPodatke(KorisnikForma forma, Korisnik korisnik) {
        korisnik.setIme(forma.getIme());
        korisnik.setPrezime(forma.getPrezime());
        korisnik.setEmail(forma.getEmail());
        korisnik.setUloga(forma.getUloga());
        korisnik.setAktivan(forma.isAktivan());
    }

    private void provjeriPodatke(KorisnikForma forma, Class<?> grupa) {
        var greske = validator.validate(forma, grupa);
        if (!greske.isEmpty()) {
            var greska = greske.iterator().next();
            throw new KorisnikPodaciException(greska.getPropertyPath().toString(), greska.getMessage());
        }
        String lozinka = forma.getLozinka();
        if (lozinka != null && !lozinka.isEmpty()) {
            if (lozinka.isBlank()) {
                throw new KorisnikPodaciException("lozinka", "Lozinka ne smije sadržavati samo razmake.");
            }
            if (lozinka.getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new KorisnikPodaciException("lozinka",
                        "Lozinka može imati najviše 72 UTF-8 bajta; slova s kvačicama zauzimaju više bajtova.");
            }
        }
    }
}
