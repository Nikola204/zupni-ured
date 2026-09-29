package ba.sum.fsre.zupniured;

import ba.sum.fsre.zupniured.dto.*;
import ba.sum.fsre.zupniured.model.*;
import ba.sum.fsre.zupniured.repository.ZupljaninRepository;
import ba.sum.fsre.zupniured.service.*;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "app.pocetni-admin.enabled=false")
@Transactional
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class DokumentServiceTests {
    private final DokumentService service;
    private final KorisnikService korisnici;
    private final ZupljaninService osobe;
    private final ZupljaninRepository zupljani;
    private final ZupljaninProfilService profili;
    private final EntityManager em;
    DokumentServiceTests(DokumentService service, KorisnikService korisnici, ZupljaninService osobe,
                         ZupljaninRepository zupljani, ZupljaninProfilService profili, EntityManager em) {
        this.service = service; this.korisnici = korisnici; this.osobe = osobe;
        this.zupljani = zupljani; this.profili = profili; this.em = em;
    }

    @Test
    void rucniPrimateljNemaZupljaninaIZadaniStatusJeZaprimljen() {
        long prije = zupljani.count();
        var d = service.dodaj(forma(), null);
        em.clear();
        d = service.pronadjiPoId(d.getId());
        assertThat(d.getZupljanin()).isNull();
        assertThat(d.getImePrimatelja()).isEqualTo("Marko");
        assertThat(d.getPrezimePrimatelja()).isEqualTo("Markić");
        assertThat(d.getStatus()).isEqualTo(StatusDokumenta.ZAPRIMLJEN);
        assertThat(d.getIzdaoKorisnik()).isNull();
        assertThat(d.getDatumKreiranja()).isNotNull();
        assertThat(d.getDatumIzmjene()).isEqualTo(d.getDatumKreiranja());
        assertThat(zupljani.count()).isEqualTo(prije);
    }

    @Test
    void povezaniPrimateljKopiraImeICuvaPovijestCakIPriUredjivanju() {
        var z = osoba("Ana"); var f = forma();
        f.setZupljaninId(z.getId()); f.setImePrimatelja(null); f.setPrezimePrimatelja(null);
        var d = service.dodaj(f, null);
        assertThat(d.getImePrimatelja()).isEqualTo("Ana");
        assertThat(d.getPrezimePrimatelja()).isEqualTo("Test");
        z.setIme("Novo ime"); z.setPrezime("Novo prezime");
        em.flush(); em.clear();
        var prije = service.pronadjiPoId(d.getId());
        var kreiranje = prije.getDatumKreiranja();
        var izmjena = prije.getDatumIzmjene();
        f = service.formaZaUredjivanje(d.getId()); f.setNapomena("Izmjena napomene");
        service.uredi(d.getId(), f, null); em.clear();
        var poslije = service.pronadjiPoId(d.getId());
        assertThat(poslije.getImePrimatelja()).isEqualTo("Ana");
        assertThat(poslije.getPrezimePrimatelja()).isEqualTo("Test");
        assertThat(poslije.getDatumKreiranja()).isEqualTo(kreiranje);
        assertThat(poslije.getDatumIzmjene()).isAfter(izmjena);
    }

    @Test
    void uredjivanjePodrzavaPromjenuDodavanjeIUklanjanjeVeze() {
        var d = service.dodaj(forma(), null);
        var z = osoba("Ana"); var drugi = osoba("Iva");
        var f = service.formaZaUredjivanje(d.getId()); f.setZupljaninId(z.getId());
        service.uredi(d.getId(), f, null);
        assertThat(d.getImePrimatelja()).isEqualTo("Ana");
        f.setZupljaninId(drugi.getId()); service.uredi(d.getId(), f, null);
        assertThat(d.getImePrimatelja()).isEqualTo("Iva");
        f.setZupljaninId(null); f.setImePrimatelja("Ručni"); f.setPrezimePrimatelja("Primatelj");
        service.uredi(d.getId(), f, null);
        assertThat(d.getZupljanin()).isNull();
        assertThat(d.getImePrimatelja()).isEqualTo("Ručni");
    }

    @ParameterizedTest
    @EnumSource(value = VrstaDokumenta.class, names = {"KRSNI_LIST", "POTVRDA_O_PRVOJ_PRICESTI", "POTVRDA_O_KRIZMI", "POTVRDA_O_VJENCANJU"})
    void izvorJeObavezanIMoraPostojati(VrstaDokumenta vrsta) {
        var f = forma(); f.setVrstaDokumenta(vrsta);
        assertThatThrownBy(() -> service.dodaj(f, null)).isInstanceOf(DokumentPodaciException.class);
        postaviIzvor(f, vrsta, 0L);
        assertThatThrownBy(() -> service.dodaj(f, null)).isInstanceOf(DokumentPodaciException.class)
                .hasMessageContaining("nije prona");
    }

    @ParameterizedTest
    @EnumSource(value = VrstaDokumenta.class, names = {"KRSNI_LIST", "POTVRDA_O_PRVOJ_PRICESTI", "POTVRDA_O_KRIZMI"})
    void sakramentOdredjujeOsobuIOdbijaSukob(VrstaDokumenta vrsta) {
        var z = osoba("Ana"); var drugi = osoba("Iva");
        var f = forma(); f.setVrstaDokumenta(vrsta);
        postaviIzvor(f, vrsta, sakrament(vrsta, z));
        f.setZupljaninId(drugi.getId());
        assertThatThrownBy(() -> service.dodaj(f, null)).isInstanceOf(DokumentPodaciException.class)
                .hasMessageContaining("ne odgovara osobi");
        f.setZupljaninId(null); f.setImePrimatelja(null); f.setPrezimePrimatelja(null);
        var d = service.dodaj(f, null);
        assertThat(d.getZupljanin().getId()).isEqualTo(z.getId());
        assertThat(d.getImePrimatelja()).isEqualTo("Ana");
        f.setZupljaninId(drugi.getId());
        assertThatThrownBy(() -> service.uredi(d.getId(), f, null)).isInstanceOf(DokumentPodaciException.class);
        assertThat(d.getZupljanin().getId()).isEqualTo(z.getId());
    }

    @Test
    void potvrdaOVjencanjuZaOsobuIzvanZupljanaMozeSeIzdati() {
        var v = new Vjencanje();
        v.setImeMladozenje("Marko"); v.setPrezimeMladozenje("Markić");
        v.setImeNevjeste("Ana"); v.setPrezimeNevjeste("Anić");
        v.setDatumVjencanja(LocalDate.of(2020, 5, 12)); v.setMjestoVjencanja("Mostar");
        em.persist(v); em.flush();
        long prije = zupljani.count();
        var f = forma(); f.setVrstaDokumenta(VrstaDokumenta.POTVRDA_O_VJENCANJU);
        f.setVjencanjeId(v.getId()); f.setStatus(StatusDokumenta.IZDAN);
        f.setDatumIzdavanja(LocalDate.of(2020, 6, 2));
        var korisnik = korisnik();
        var d = service.dodaj(f, korisnik.getEmail());
        em.clear();
        d = service.pronadjiPoId(d.getId());
        assertThat(d.getVjencanje().getId()).isEqualTo(v.getId());
        assertThat(d.getZupljanin()).isNull();
        assertThat(d.getImePrimatelja()).isEqualTo("Marko");
        assertThat(d.getIzdaoKorisnik().getId()).isEqualTo(korisnik.getId());
        assertThat(zupljani.count()).isEqualTo(prije);
    }

    @ParameterizedTest
    @EnumSource(value = VrstaDokumenta.class, names = {"OSTALO", "POTVRDA_O_SLOBODNOM_STANJU"})
    void vrsteBezIzvoraSuValjaneAliNePrihvacajuProizvoljanIzvor(VrstaDokumenta vrsta) {
        var f = forma(); f.setVrstaDokumenta(vrsta);
        assertThat(service.dodaj(f, null).getVrstaDokumenta()).isEqualTo(vrsta);
        f.setKrstenjeId(sakrament(VrstaDokumenta.KRSNI_LIST, osoba("Ana")));
        assertThatThrownBy(() -> service.dodaj(f, null)).isInstanceOf(DokumentPodaciException.class)
                .hasMessageContaining("ne odgovara vrsti");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ime", "prezime", "vrsta", "status", "datum", "datumIzdavanja", "redoslijed", "duzina", "zupljanin"})
    void neispravniPodaciSeOdbijaju(String slucaj) {
        var f = forma();
        switch (slucaj) {
            case "ime" -> f.setImePrimatelja(" ");
            case "prezime" -> f.setPrezimePrimatelja(null);
            case "vrsta" -> f.setVrstaDokumenta(null);
            case "status" -> f.setStatus(null);
            case "datum" -> f.setDatumZahtjeva(null);
            case "datumIzdavanja" -> f.setStatus(StatusDokumenta.IZDAN);
            case "redoslijed" -> f.setDatumIzdavanja(f.getDatumZahtjeva().minusDays(1));
            case "duzina" -> f.setImePrimatelja("a".repeat(101));
            case "zupljanin" -> f.setZupljaninId(0L);
        }
        assertThatThrownBy(() -> service.dodaj(f, null))
                .isInstanceOfAny(DokumentPodaciException.class, ConstraintViolationException.class);
    }

    @ParameterizedTest
    @EnumSource(StatusDokumenta.class)
    void promjenaStatusa(StatusDokumenta status) {
        var d = service.dodaj(forma(), null);
        var f = new DokumentStatusForma(); f.setStatus(status);
        if (status == StatusDokumenta.IZDAN) f.setDatumIzdavanja(LocalDate.of(2020, 6, 1));
        var k = korisnik();
        service.promijeniStatus(d.getId(), f, k.getEmail()); em.clear();
        d = service.pronadjiPoId(d.getId());
        assertThat(d.getStatus()).isEqualTo(status);
        if (status == StatusDokumenta.IZDAN) {
            assertThat(d.getIzdaoKorisnik().getId()).isEqualTo(k.getId());
            assertThat(d.getDatumIzdavanja()).isEqualTo(LocalDate.of(2020, 6, 1));
        } else assertThat(d.getIzdaoKorisnik()).isNull();
    }

    @Test
    void prvoIzdavanjeOstajeSacuvanoPriUredjivanjuIPromjenamaStatusa() {
        var prvi = korisnik(); var drugi = korisnik();
        var f = forma(); f.setStatus(StatusDokumenta.IZDAN); f.setDatumIzdavanja(LocalDate.of(2020, 6, 2));
        var d = service.dodaj(f, prvi.getEmail());
        f.setNapomena("Nova napomena");
        service.uredi(d.getId(), f, drugi.getEmail());
        var promjena = new DokumentStatusForma(); promjena.setStatus(StatusDokumenta.ODBIJEN);
        service.promijeniStatus(d.getId(), promjena, drugi.getEmail());
        assertThat(d.getIzdaoKorisnik().getId()).isEqualTo(prvi.getId());
        assertThat(d.getDatumIzdavanja()).isEqualTo(LocalDate.of(2020, 6, 2));
        promjena.setStatus(StatusDokumenta.IZDAN);
        service.promijeniStatus(d.getId(), promjena, drugi.getEmail());
        assertThat(d.getIzdaoKorisnik().getId()).isEqualTo(prvi.getId());
        promjena.setDatumIzdavanja(LocalDate.of(2020, 6, 3));
        assertThatThrownBy(() -> service.promijeniStatus(d.getId(), promjena, drugi.getEmail()))
                .isInstanceOf(DokumentPodaciException.class);
    }

    @Test
    void izdavanjeBezAktivnogDjelatnikaSeOdbija() {
        var f = forma(); f.setStatus(StatusDokumenta.IZDAN); f.setDatumIzdavanja(LocalDate.of(2020, 6, 1));
        assertThatThrownBy(() -> service.dodaj(f, "nepostojeci@primjer.hr")).isInstanceOf(DokumentPodaciException.class);
        var k = korisnik(); k.setAktivan(false); em.flush();
        assertThatThrownBy(() -> service.dodaj(f, k.getEmail())).isInstanceOf(DokumentPodaciException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ime", "prezime", "svrha"})
    void pretragaJeNeosjetljivaNaVelicinuSlova(String polje) {
        String oznaka = "Test" + UUID.randomUUID();
        var f = forma();
        switch (polje) {
            case "ime" -> f.setImePrimatelja(oznaka);
            case "prezime" -> f.setPrezimePrimatelja(oznaka);
            case "svrha" -> f.setSvrha(oznaka);
        }
        var d = service.dodaj(f, null); service.dodaj(forma(), null);
        var filter = new DokumentFilter(); filter.setPretraga(" " + oznaka.toUpperCase(Locale.ROOT) + " ");
        assertThat(service.pronadjiSve(filter)).extracting(Dokument::getId).containsExactly(d.getId());
    }

    @Test
    void filtriIRedoslijedPoDatumima() {
        String oznaka = UUID.randomUUID().toString();
        var f = forma(); f.setSvrha(oznaka);
        var prvi = service.dodaj(f, null);
        f.setStatus(StatusDokumenta.U_OBRADI);
        var drugi = service.dodaj(f, null);
        f.setVrstaDokumenta(VrstaDokumenta.POTVRDA_O_SLOBODNOM_STANJU);
        f.setDatumZahtjeva(LocalDate.of(2019, 1, 1));
        var stariji = service.dodaj(f, null);
        var filter = new DokumentFilter(); filter.setPretraga(oznaka);
        assertThat(service.pronadjiSve(filter)).extracting(Dokument::getId).containsExactly(drugi.getId(), prvi.getId(), stariji.getId());
        filter.setStatus(StatusDokumenta.U_OBRADI);
        assertThat(service.pronadjiSve(filter)).hasSize(2);
        filter.setVrstaDokumenta(VrstaDokumenta.OSTALO);
        filter.setOd(LocalDate.of(2020, 6, 1)); filter.setDoDatuma(LocalDate.of(2020, 6, 1));
        assertThat(service.pronadjiSve(filter)).extracting(Dokument::getId).containsExactly(drugi.getId());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3})
    void profilDohvacaSamoStvarnuVezu(int broj) {
        var z = osoba("Marko");
        var nepovezan = forma(); nepovezan.setPrezimePrimatelja("Test");
        var rucni = service.dodaj(nepovezan, null);
        var f = forma(); f.setZupljaninId(z.getId());
        for (int i = 0; i < broj; i++) service.dodaj(f, null);
        var profil = profili.pronadjiPoId(z.getId());
        assertThat(profil.dokumenti()).hasSize(broj).extracting(Dokument::getId).doesNotContain(rucni.getId());
    }

    private DokumentForma forma() {
        var f = new DokumentForma(); f.setImePrimatelja("Marko"); f.setPrezimePrimatelja("Markić");
        f.setVrstaDokumenta(VrstaDokumenta.OSTALO); f.setDatumZahtjeva(LocalDate.of(2020, 6, 1));
        return f;
    }
    private Zupljanin osoba(String ime) {
        var z = new Zupljanin(); z.setIme(ime); z.setPrezime("Test"); return osobe.dodaj(z);
    }
    private Korisnik korisnik() {
        var f = new KorisnikForma(); f.setIme("Test"); f.setPrezime("Djelatnik");
        f.setEmail(UUID.randomUUID() + "@primjer.hr"); f.setLozinka("TestnaLozinka123!"); f.setUloga(Uloga.ADMIN);
        return korisnici.dodaj(f);
    }
    private Long sakrament(VrstaDokumenta vrsta, Zupljanin z) {
        if (vrsta == VrstaDokumenta.KRSNI_LIST) {
            var k = new Krstenje(); k.setZupljanin(z); k.setDatumKrstenja(LocalDate.of(2010, 1, 1));
            k.setMjestoKrstenja("Mostar"); em.persist(k); em.flush(); return k.getId();
        }
        if (vrsta == VrstaDokumenta.POTVRDA_O_PRVOJ_PRICESTI) {
            var k = new PrvaPricest(); k.setZupljanin(z); k.setDatumPricesti(LocalDate.of(2010, 1, 1));
            k.setMjestoPricesti("Mostar"); em.persist(k); em.flush(); return k.getId();
        }
        var k = new Krizma(); k.setZupljanin(z); k.setDatumKrizme(LocalDate.of(2010, 1, 1));
        k.setMjestoKrizme("Mostar"); em.persist(k); em.flush(); return k.getId();
    }
    private void postaviIzvor(DokumentForma f, VrstaDokumenta vrsta, Long id) {
        switch (vrsta) {
            case KRSNI_LIST -> f.setKrstenjeId(id);
            case POTVRDA_O_PRVOJ_PRICESTI -> f.setPrvaPricestId(id);
            case POTVRDA_O_KRIZMI -> f.setKrizmaId(id);
            case POTVRDA_O_VJENCANJU -> f.setVjencanjeId(id);
            default -> throw new IllegalArgumentException();
        }
    }
}
