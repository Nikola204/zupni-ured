package ba.sum.fsre.zupniured;

import ba.sum.fsre.zupniured.dto.KrstenjeForma;
import ba.sum.fsre.zupniured.model.Krstenje;
import ba.sum.fsre.zupniured.model.Zupljanin;
import ba.sum.fsre.zupniured.repository.KrstenjeRepository;
import ba.sum.fsre.zupniured.repository.ZupljaninRepository;
import ba.sum.fsre.zupniured.service.KrstenjePodaciException;
import ba.sum.fsre.zupniured.service.KrstenjeService;
import ba.sum.fsre.zupniured.service.ZupljaninService;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.sql.SQLException;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "app.pocetni-admin.enabled=false")
@Transactional
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class KrstenjeServiceTests {

    private final KrstenjeService servis;
    private final ZupljaninService zupljaninService;
    private final KrstenjeRepository repository;
    private final ZupljaninRepository zupljaninRepository;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbcTemplate;

    KrstenjeServiceTests(KrstenjeService servis, ZupljaninService zupljaninService,
                        KrstenjeRepository repository, ZupljaninRepository zupljaninRepository,
                        EntityManager entityManager, JdbcTemplate jdbcTemplate) {
        this.servis = servis;
        this.zupljaninService = zupljaninService;
        this.repository = repository;
        this.zupljaninRepository = zupljaninRepository;
        this.entityManager = entityManager;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Test
    void spremaSvaPoljaVezuIDatumKreiranjaBezDupliciranjaZupljanina() {
        Zupljanin osoba = osoba("Ana", "Anić");
        long brojOsoba = zupljaninRepository.count();
        KrstenjeForma forma = forma(osoba);
        forma.setImeOca("Ivan");
        forma.setImeMajke("Marija");
        forma.setImeKuma("Marko");
        forma.setImeKume("Iva");
        forma.setKrstitelj("Fra Petar");
        forma.setBrojMatice(" K-12 ");
        forma.setGodinaMatice(2000);
        forma.setNapomena(" Napomena ");
        Krstenje spremljen = servis.dodaj(forma);
        osvjezi();

        Krstenje zapis = servis.pronadjiPoId(spremljen.getId());
        assertThat(zapis.getDatumKreiranja()).isNotNull();
        assertThat(zapis.getZupljanin().getId()).isEqualTo(osoba.getId());
        assertThat(servis.formaZaUredjivanje(zapis.getId())).usingRecursiveComparison().isEqualTo(forma);
        assertThat(zupljaninRepository.count()).isEqualTo(brojOsoba);
        assertThat(zapis.getBrojMatice()).isEqualTo("K-12");
        assertThat(zapis.getNapomena()).isEqualTo("Napomena");

        Zupljanin izmjeneOsobe = new Zupljanin();
        izmjeneOsobe.setIme("Novo ime");
        izmjeneOsobe.setPrezime("Novo prezime");
        zupljaninService.uredi(osoba.getId(), izmjeneOsobe);
        zupljaninService.deaktiviraj(osoba.getId());
        osvjezi();
        assertThat(servis.pronadjiPoId(zapis.getId()).getZupljanin().getIme()).isEqualTo("Novo ime");
        assertThat(repository.existsById(zapis.getId())).isTrue();
    }

    @Test
    void uredjivanjeMijenjaPodatkeIVezuAliCuvaIdIDatumKreiranja() {
        Krstenje spremljen = servis.dodaj(forma(osoba("Ana", "Anić")));
        osvjezi();
        var datum = servis.pronadjiPoId(spremljen.getId()).getDatumKreiranja();
        long brojZapisa = repository.count();
        KrstenjeForma izmjene = forma(osoba("Ivan", "Ivić"));
        izmjene.setDatumKrstenja(LocalDate.of(2001, 2, 3));
        izmjene.setMjestoKrstenja("Čitluk");
        izmjene.setImeOca("Otac");
        izmjene.setImeMajke("Majka");
        izmjene.setImeKuma("Kum");
        izmjene.setImeKume("Kuma");
        izmjene.setKrstitelj("Krstitelj");
        izmjene.setBrojMatice("B-25");
        izmjene.setGodinaMatice(2001);
        izmjene.setNapomena("Izmjena");
        servis.uredi(spremljen.getId(), izmjene);
        osvjezi();
        assertThat(servis.formaZaUredjivanje(spremljen.getId())).usingRecursiveComparison().isEqualTo(izmjene);
        assertThat(servis.pronadjiPoId(spremljen.getId()).getDatumKreiranja()).isEqualTo(datum);
        assertThat(repository.count()).isEqualTo(brojZapisa);
    }

    @Test
    void opcionalnaPoljaMoguOstatiPraznaIMoguSeOcistiti() {
        KrstenjeForma forma = forma(osoba("Ana", "Anić"));
        forma.setBrojMatice("12");
        forma.setGodinaMatice(2000);
        forma.setNapomena("Napomena");
        Krstenje zapis = servis.dodaj(forma);
        forma.setBrojMatice(" ");
        forma.setGodinaMatice(null);
        forma.setNapomena("");
        servis.uredi(zapis.getId(), forma);
        osvjezi();
        assertThat(servis.formaZaUredjivanje(zapis.getId())).usingRecursiveComparison().isEqualTo(forma);
    }

    @Test
    void pretrazujeImePrezimeIBrojMaticeNeovisnoOVeliciniSlova() {
        String pojam = "Test" + UUID.randomUUID();
        Krstenje ime = servis.dodaj(forma(osoba("Ana" + pojam, "Anić")));
        Krstenje prezime = servis.dodaj(forma(osoba("Ivan", "Ivić" + pojam)));
        KrstenjeForma forma = forma(osoba("Petar", "Petrić"));
        forma.setBrojMatice("M-" + pojam);
        Krstenje matica = servis.dodaj(forma);
        servis.dodaj(forma(osoba("Druga", "Osoba")));
        osvjezi();
        for (String pretraga : new String[]{pojam.toLowerCase(Locale.ROOT),
                "  " + pojam.toUpperCase(Locale.ROOT) + "  "}) {
            assertThat(servis.pronadjiSve(pretraga)).extracting(Krstenje::getId)
                    .containsExactly(matica.getId(), prezime.getId(), ime.getId());
        }
    }

    @Test
    void pretrazujeGodinuMatice() {
        KrstenjeForma forma = forma(osoba("Ana", "Anić"));
        forma.setGodinaMatice(1876);
        Krstenje zapis = servis.dodaj(forma);
        forma.setGodinaMatice(1877);
        forma.setZupljaninId(osoba("Ivan", "Ivić").getId());
        Krstenje drugi = servis.dodaj(forma);
        assertThat(servis.pronadjiSve("1876")).extracting(Krstenje::getId)
                .contains(zapis.getId()).doesNotContain(drugi.getId());
    }

    @Test
    void praznaPretragaVracaSveAPretragabezPodudaranjaPraznuListu() {
        Krstenje zapis = servis.dodaj(forma(osoba("Ana", "Anić")));
        for (String pojam : new String[]{null, "", "  "}) {
            assertThat(servis.pronadjiSve(pojam)).extracting(Krstenje::getId).contains(zapis.getId());
        }
        assertThat(servis.pronadjiSve(UUID.randomUUID().toString())).isEmpty();
        assertThat(servis.pronadjiSve("9999999999999999999999999999999999")).isEmpty();
    }

    @Test
    void posebniZnakoviPretrageNisuZamjenskiZnakovi() {
        String pojam = "Test" + UUID.randomUUID();
        KrstenjeForma forma = forma(osoba("Ana", "Anić"));
        forma.setBrojMatice(pojam + "%_");
        Krstenje tocni = servis.dodaj(forma);
        forma.setBrojMatice(pojam + "AB");
        forma.setZupljaninId(osoba("Ivan", "Ivić").getId());
        Krstenje drugi = servis.dodaj(forma);
        assertThat(servis.pronadjiSve(pojam + "%_")).extracting(Krstenje::getId)
                .containsExactly(tocni.getId()).doesNotContain(drugi.getId());
    }

    @Test
    void izborJeSortiranPoPrezimenuIImenuIUkljucujeNeaktivne() {
        String prezime = "Test" + UUID.randomUUID();
        Zupljanin zadnji = osoba("Ana", prezime + "B");
        Zupljanin drugi = osoba("Zoran", prezime + "A");
        Zupljanin prvi = osoba("Ana", prezime + "A");
        zupljaninService.deaktiviraj(prvi.getId());
        assertThat(servis.zupljaniZaIzbor().stream().filter(z -> z.getPrezime().startsWith(prezime)))
                .extracting(Zupljanin::getId).containsExactly(prvi.getId(), drugi.getId(), zadnji.getId());
    }

    @Test
    void servisOdbijaNeispravanUnos() {
        long broj = repository.count();
        assertThatThrownBy(() -> servis.dodaj(new KrstenjeForma()))
                .isInstanceOf(ConstraintViolationException.class);
        assertThat(repository.count()).isEqualTo(broj);
    }

    @Test
    void neispravnoUredjivanjeNeMijenjaZapis() {
        Krstenje zapis = servis.dodaj(forma(osoba("Ana", "Anić")));
        KrstenjeForma prije = servis.formaZaUredjivanje(zapis.getId());
        KrstenjeForma izmjene = servis.formaZaUredjivanje(zapis.getId());
        izmjene.setDatumKrstenja(LocalDate.now().plusDays(1));
        assertThatThrownBy(() -> servis.uredi(zapis.getId(), izmjene))
                .isInstanceOf(ConstraintViolationException.class);
        assertThat(servis.formaZaUredjivanje(zapis.getId())).usingRecursiveComparison().isEqualTo(prije);
    }

    @Test
    void nepostojeciZupljaninSeOdbijaPriDodavanjuIUredjivanju() {
        Krstenje zapis = servis.dodaj(forma(osoba("Ana", "Anić")));
        KrstenjeForma forma = servis.formaZaUredjivanje(zapis.getId());
        forma.setZupljaninId(0L);
        for (Runnable radnja : new Runnable[]{() -> servis.dodaj(forma), () -> servis.uredi(zapis.getId(), forma)}) {
            assertThatThrownBy(radnja::run).isInstanceOf(KrstenjePodaciException.class);
        }
    }

    @Test
    void nepostojeciZapisVraca404() {
        KrstenjeForma forma = forma(osoba("Ana", "Anić"));
        for (Runnable radnja : new Runnable[]{() -> servis.pronadjiPoId(0L),
                () -> servis.formaZaUredjivanje(0L), () -> servis.uredi(0L, forma)}) {
            assertThatThrownBy(radnja::run).isInstanceOfSatisfying(ResponseStatusException.class,
                    greska -> assertThat(greska.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    @Test
    void prviZapisUspijeADrugiZaIstogZupljaninaSeOdbija() {
        KrstenjeForma forma = forma(osoba("Ana", "Anić"));
        Krstenje prvi = servis.dodaj(forma);
        osvjezi();
        long broj = repository.count();

        assertThat(prvi.getId()).isNotNull();
        assertThatThrownBy(() -> servis.dodaj(forma))
                .isInstanceOf(KrstenjePodaciException.class)
                .hasMessage("Odabrani župljanin već ima evidentirano krštenje.");
        assertThat(repository.count()).isEqualTo(broj);
        assertThat(servis.formaZaUredjivanje(prvi.getId())).usingRecursiveComparison().isEqualTo(forma);
    }

    @Test
    void uredjivanjeIstogZapisaNijeKonflikt() {
        KrstenjeForma forma = forma(osoba("Ana", "Anić"));
        Krstenje zapis = servis.dodaj(forma);
        osvjezi();
        long broj = repository.count();
        forma.setMjestoKrstenja("Čitluk");

        servis.uredi(zapis.getId(), forma);
        osvjezi();

        assertThat(repository.count()).isEqualTo(broj);
        assertThat(servis.formaZaUredjivanje(zapis.getId())).usingRecursiveComparison().isEqualTo(forma);
    }

    @Test
    void uredjivanjeNeMozePreuzetiZupljaninaKojiVecImaKrstenje() {
        Krstenje prvi = servis.dodaj(forma(osoba("Ana", "Anić")));
        Krstenje drugi = servis.dodaj(forma(osoba("Ivan", "Ivić")));
        osvjezi();
        KrstenjeForma prije = servis.formaZaUredjivanje(prvi.getId());
        KrstenjeForma drugiPrije = servis.formaZaUredjivanje(drugi.getId());
        KrstenjeForma izmjene = servis.formaZaUredjivanje(prvi.getId());
        izmjene.setZupljaninId(drugiPrije.getZupljaninId());
        izmjene.setMjestoKrstenja("Novo mjesto");

        assertThatThrownBy(() -> servis.uredi(prvi.getId(), izmjene))
                .isInstanceOf(KrstenjePodaciException.class)
                .hasMessage("Odabrani župljanin već ima evidentirano krštenje.");
        osvjezi();
        assertThat(servis.formaZaUredjivanje(prvi.getId())).usingRecursiveComparison().isEqualTo(prije);
        assertThat(servis.formaZaUredjivanje(drugi.getId())).usingRecursiveComparison().isEqualTo(drugiPrije);
    }

    @Test
    void bazaImaJedinstvenoOgranicenjeSamoNadZupljaninom() {
        var stupci = jdbcTemplate.queryForList("""
                select kcu.column_name
                from information_schema.table_constraints tc
                join information_schema.key_column_usage kcu
                  on tc.constraint_catalog = kcu.constraint_catalog
                 and tc.constraint_schema = kcu.constraint_schema
                 and tc.constraint_name = kcu.constraint_name
                where tc.table_schema = current_schema()
                  and tc.table_name = 'krstenja'
                  and tc.constraint_type = 'UNIQUE'
                  and tc.constraint_name = 'uk_krstenja_zupljanin'
                """, String.class);
        assertThat(stupci).containsExactly("zupljanin_id");
    }

    @Test
    void bazaOdbijaDuplikatPriIzravnomUnosuBezServisaIJpaValidacije() {
        Zupljanin zupljanin = osoba("Ana", "Anić");
        servis.dodaj(forma(zupljanin));

        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into krstenja (zupljanin_id, datum_krstenja, mjesto_krstenja, datum_kreiranja)
                values (?, ?, ?, current_timestamp)
                """, zupljanin.getId(), LocalDate.of(2000, 1, 1), "Mostar"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause().isInstanceOfSatisfying(SQLException.class, greska -> {
                    assertThat(greska.getSQLState()).isEqualTo("23505");
                    assertThat(greska.getMessage()).contains("uk_krstenja_zupljanin");
                });
        // Nakon SQL greške PostgreSQL transakcija smije samo završiti rollbackom.
    }

    @Test
    void bazaOdbijaDuplikatPriIzravnomUredjivanju() {
        Krstenje prvi = servis.dodaj(forma(osoba("Ana", "Anić")));
        Krstenje drugi = servis.dodaj(forma(osoba("Ivan", "Ivić")));

        assertThatThrownBy(() -> jdbcTemplate.update(
                "update krstenja set zupljanin_id = ? where id = ?",
                prvi.getZupljanin().getId(), drugi.getId()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause().isInstanceOfSatisfying(SQLException.class, greska -> {
                    assertThat(greska.getSQLState()).isEqualTo("23505");
                    assertThat(greska.getMessage()).contains("uk_krstenja_zupljanin");
                });
    }

    private Zupljanin osoba(String ime, String prezime) {
        Zupljanin osoba = new Zupljanin();
        osoba.setIme(ime);
        osoba.setPrezime(prezime);
        osoba.setDatumRodjenja(LocalDate.of(1999, 1, 1));
        return zupljaninService.dodaj(osoba);
    }

    private KrstenjeForma forma(Zupljanin osoba) {
        KrstenjeForma forma = new KrstenjeForma();
        forma.setZupljaninId(osoba.getId());
        forma.setDatumKrstenja(LocalDate.of(2000, 5, 12));
        forma.setMjestoKrstenja(" Mostar ");
        return forma;
    }

    private void osvjezi() {
        entityManager.flush();
        entityManager.clear();
    }
}
