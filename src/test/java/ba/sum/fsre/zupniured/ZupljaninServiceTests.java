package ba.sum.fsre.zupniured;

import ba.sum.fsre.zupniured.model.Zupljanin;
import ba.sum.fsre.zupniured.repository.ZupljaninRepository;
import ba.sum.fsre.zupniured.service.ZupljaninService;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "app.pocetni-admin.enabled=false")
@Transactional
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ZupljaninServiceTests {

    private final ZupljaninService servis;
    private final ZupljaninRepository repository;
    private final EntityManager entityManager;

    ZupljaninServiceTests(ZupljaninService servis, ZupljaninRepository repository, EntityManager entityManager) {
        this.servis = servis;
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Test
    void dodavanjeSpremaSvaPoljaIAutomatskeVrijednosti() {
        Zupljanin podaci = podaci(" Ana ", " Anić ");
        podaci.setDatumRodjenja(LocalDate.of(1990, 5, 12));
        podaci.setMjestoRodjenja("Mostar");
        podaci.setAdresa("Ulica 1");
        podaci.setTelefon("+387 63 123 456");
        podaci.setEmail("ana@primjer.hr");
        podaci.setNapomena("Primjer napomene");
        podaci.setAktivan(false);
        Zupljanin spremljen = servis.dodaj(podaci);
        osvjezi();
        Zupljanin izBaze = servis.pronadjiPoId(spremljen.getId());

        assertThat(izBaze.getIme()).isEqualTo("Ana");
        assertThat(izBaze.getPrezime()).isEqualTo("Anić");
        assertThat(izBaze.getDatumRodjenja()).isEqualTo(LocalDate.of(1990, 5, 12));
        assertThat(izBaze.getMjestoRodjenja()).isEqualTo("Mostar");
        assertThat(izBaze.getAdresa()).isEqualTo("Ulica 1");
        assertThat(izBaze.getTelefon()).isEqualTo("+387 63 123 456");
        assertThat(izBaze.getEmail()).isEqualTo("ana@primjer.hr");
        assertThat(izBaze.getNapomena()).isEqualTo("Primjer napomene");
        assertThat(izBaze.isAktivan()).isTrue();
        assertThat(izBaze.getDatumKreiranja()).isNotNull();
    }

    @Test
    void uredjivanjeCuvaIdDatumKreiranjaIStatus() {
        Zupljanin spremljen = servis.dodaj(podaci("Ana", "Anić"));
        servis.deaktiviraj(spremljen.getId());
        osvjezi();
        var datumKreiranja = servis.pronadjiPoId(spremljen.getId()).getDatumKreiranja();

        Zupljanin izmjene = podaci("Ivan", "Ivić");
        izmjene.setDatumRodjenja(LocalDate.of(1985, 1, 2));
        izmjene.setMjestoRodjenja("Čitluk");
        izmjene.setAdresa("Nova adresa");
        izmjene.setTelefon("123456");
        izmjene.setEmail("ivan@primjer.hr");
        izmjene.setNapomena("Nova napomena");
        servis.uredi(spremljen.getId(), izmjene);
        osvjezi();

        Zupljanin uredjen = servis.pronadjiPoId(spremljen.getId());
        assertThat(uredjen.getId()).isEqualTo(spremljen.getId());
        assertThat(uredjen.getDatumKreiranja()).isEqualTo(datumKreiranja);
        assertThat(uredjen.isAktivan()).isFalse();
        assertThat(uredjen).usingRecursiveComparison()
                .ignoringFields("id", "aktivan", "datumKreiranja").isEqualTo(izmjene);
    }

    @Test
    void deaktivacijaIReaktivacijaNeBrisuZapis() {
        Zupljanin spremljen = servis.dodaj(podaci("Ana", "Anić"));
        long brojZapisa = repository.count();
        servis.deaktiviraj(spremljen.getId());
        osvjezi();
        assertThat(servis.pronadjiPoId(spremljen.getId()).isAktivan()).isFalse();
        servis.reaktiviraj(spremljen.getId());
        osvjezi();
        assertThat(servis.pronadjiPoId(spremljen.getId()).isAktivan()).isTrue();
        assertThat(repository.count()).isEqualTo(brojZapisa);
    }

    @Test
    void praznaPretragaVracaAktivneINeaktivne() {
        Zupljanin aktivan = servis.dodaj(podaci("Ana", "Anić"));
        Zupljanin neaktivan = servis.dodaj(podaci("Ivan", "Ivić"));
        servis.deaktiviraj(neaktivan.getId());
        for (String pojam : new String[]{null, "", "   "}) {
            assertThat(servis.pronadjiSve(pojam)).extracting(Zupljanin::getId)
                    .contains(aktivan.getId(), neaktivan.getId());
        }
    }

    @Test
    void pretragaPronalaziDioImenaIliPrezimenaNeovisnoOVeliciniSlova() {
        String pojam = "Pretraga" + UUID.randomUUID();
        Zupljanin ime = servis.dodaj(podaci("Ana" + pojam, "Anić"));
        Zupljanin prezime = servis.dodaj(podaci("Ivan", "Ivić" + pojam));
        servis.deaktiviraj(prezime.getId());
        servis.dodaj(podaci("Neodgovarajuće", "Prezime"));

        assertThat(servis.pronadjiSve("  " + pojam.toUpperCase(Locale.ROOT) + "  "))
                .extracting(Zupljanin::getId).containsExactly(ime.getId(), prezime.getId());
        assertThat(servis.pronadjiSve(pojam.toLowerCase(Locale.ROOT)))
                .extracting(Zupljanin::getId).containsExactly(ime.getId(), prezime.getId());
    }

    @Test
    void pretragaBezPodudaranjaVracaPraznuListu() {
        assertThat(servis.pronadjiSve(UUID.randomUUID().toString())).isEmpty();
    }

    @Test
    void neispravniPodaciSeNeSpremaju() {
        long brojZapisa = repository.count();
        assertThatThrownBy(() -> servis.dodaj(new Zupljanin()))
                .isInstanceOf(ConstraintViolationException.class);
        assertThat(repository.count()).isEqualTo(brojZapisa);
    }

    @Test
    void nepostojeciZapisVraca404() {
        Zupljanin izmjene = podaci("Ana", "Anić");
        for (Runnable radnja : new Runnable[]{
                () -> servis.pronadjiPoId(0L), () -> servis.uredi(0L, izmjene),
                () -> servis.deaktiviraj(0L), () -> servis.reaktiviraj(0L)}) {
            assertThatThrownBy(radnja::run).isInstanceOfSatisfying(ResponseStatusException.class,
                    greska -> assertThat(greska.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    private Zupljanin podaci(String ime, String prezime) {
        Zupljanin zupljanin = new Zupljanin();
        zupljanin.setIme(ime);
        zupljanin.setPrezime(prezime);
        return zupljanin;
    }

    private void osvjezi() {
        entityManager.flush();
        entityManager.clear();
    }
}
