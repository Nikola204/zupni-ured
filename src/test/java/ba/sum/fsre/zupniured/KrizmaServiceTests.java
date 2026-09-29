package ba.sum.fsre.zupniured;

import ba.sum.fsre.zupniured.dto.KrizmaForma;
import ba.sum.fsre.zupniured.model.Krizma;
import ba.sum.fsre.zupniured.model.Krstenje;
import ba.sum.fsre.zupniured.model.PrvaPricest;
import ba.sum.fsre.zupniured.model.Zupljanin;
import ba.sum.fsre.zupniured.repository.KrizmaRepository;
import ba.sum.fsre.zupniured.repository.ZupljaninRepository;
import ba.sum.fsre.zupniured.service.KrizmaPodaciException;
import ba.sum.fsre.zupniured.service.KrizmaService;
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
class KrizmaServiceTests {

    private final KrizmaService servis;
    private final ZupljaninService zupljaninService;
    private final KrizmaRepository repository;
    private final ZupljaninRepository zupljaninRepository;
    private final EntityManager entityManager;
    private final JdbcTemplate jdbcTemplate;

    KrizmaServiceTests(KrizmaService servis, ZupljaninService zupljaninService,
                        KrizmaRepository repository, ZupljaninRepository zupljaninRepository,
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
        KrizmaForma forma = forma(osoba);
        forma.setBrojMatice(" K-12 ");
        forma.setGodinaMatice(2000);
        forma.setNapomena(" Napomena ");
        forma.setImeKrizmanogKuma(" Ivan Ivić ");
        forma.setKrizmatelj(" Biskup Petar ");
        Krizma spremljen = servis.dodaj(forma);
        osvjezi();

        Krizma zapis = servis.pronadjiPoId(spremljen.getId());
        assertThat(zapis.getDatumKreiranja()).isNotNull();
        assertThat(zapis.getZupljanin().getId()).isEqualTo(osoba.getId());
        assertThat(servis.formaZaUredjivanje(zapis.getId())).usingRecursiveComparison().isEqualTo(forma);
        assertThat(zupljaninRepository.count()).isEqualTo(brojOsoba);
        assertThat(zapis.getBrojMatice()).isEqualTo("K-12");
        assertThat(zapis.getNapomena()).isEqualTo("Napomena");
        assertThat(zapis.getImeKrizmanogKuma()).isEqualTo("Ivan Ivić");
        assertThat(zapis.getKrizmatelj()).isEqualTo("Biskup Petar");

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
        Krizma spremljen = servis.dodaj(forma(osoba("Ana", "Anić")));
        osvjezi();
        var datum = servis.pronadjiPoId(spremljen.getId()).getDatumKreiranja();
        long brojZapisa = repository.count();
        KrizmaForma izmjene = forma(osoba("Ivan", "Ivić"));
        izmjene.setDatumKrizme(LocalDate.of(2001, 2, 3));
        izmjene.setMjestoKrizme("Čitluk");
        izmjene.setBrojMatice("B-25");
        izmjene.setGodinaMatice(2001);
        izmjene.setNapomena("Izmjena");
        izmjene.setImeKrizmanogKuma("Marko Marić");
        izmjene.setKrizmatelj("Biskup Ivan");
        servis.uredi(spremljen.getId(), izmjene);
        osvjezi();
        assertThat(servis.formaZaUredjivanje(spremljen.getId())).usingRecursiveComparison().isEqualTo(izmjene);
        assertThat(servis.pronadjiPoId(spremljen.getId()).getDatumKreiranja()).isEqualTo(datum);
        assertThat(repository.count()).isEqualTo(brojZapisa);
    }

    @Test
    void opcionalnaPoljaMoguOstatiPraznaIMoguSeOcistiti() {
        KrizmaForma forma = forma(osoba("Ana", "Anić"));
        forma.setBrojMatice("12");
        forma.setGodinaMatice(2000);
        forma.setNapomena("Napomena");
        forma.setImeKrizmanogKuma("Ivan Ivić");
        forma.setKrizmatelj("Biskup Petar");
        Krizma zapis = servis.dodaj(forma);
        forma.setBrojMatice(" ");
        forma.setGodinaMatice(null);
        forma.setNapomena("");
        forma.setImeKrizmanogKuma(" ");
        forma.setKrizmatelj("");
        servis.uredi(zapis.getId(), forma);
        osvjezi();
        assertThat(servis.formaZaUredjivanje(zapis.getId())).usingRecursiveComparison().isEqualTo(forma);
    }

    @Test
    void pretrazujeImePrezimeIBrojMaticeNeovisnoOVeliciniSlova() {
        String pojam = "Test" + UUID.randomUUID();
        Krizma ime = servis.dodaj(forma(osoba("Ana" + pojam, "Anić")));
        Krizma prezime = servis.dodaj(forma(osoba("Ivan", "Ivić" + pojam)));
        KrizmaForma forma = forma(osoba("Petar", "Petrić"));
        forma.setBrojMatice("M-" + pojam);
        Krizma matica = servis.dodaj(forma);
        servis.dodaj(forma(osoba("Druga", "Osoba")));
        osvjezi();
        for (String pretraga : new String[]{pojam.toLowerCase(Locale.ROOT),
                "  " + pojam.toUpperCase(Locale.ROOT) + "  "}) {
            assertThat(servis.pronadjiSve(pretraga)).extracting(Krizma::getId)
                    .containsExactly(matica.getId(), prezime.getId(), ime.getId());
        }
    }

    @Test
    void pretrazujeGodinuMatice() {
        KrizmaForma forma = forma(osoba("Ana", "Anić"));
        forma.setGodinaMatice(1876);
        Krizma zapis = servis.dodaj(forma);
        forma.setGodinaMatice(1877);
        forma.setZupljaninId(osoba("Ivan", "Ivić").getId());
        Krizma drugi = servis.dodaj(forma);
        assertThat(servis.pronadjiSve("1876")).extracting(Krizma::getId)
                .contains(zapis.getId()).doesNotContain(drugi.getId());
    }

    @Test
    void praznaPretragaVracaSveAPretragabezPodudaranjaPraznuListu() {
        Krizma zapis = servis.dodaj(forma(osoba("Ana", "Anić")));
        for (String pojam : new String[]{null, "", "  "}) {
            assertThat(servis.pronadjiSve(pojam)).extracting(Krizma::getId).contains(zapis.getId());
        }
        assertThat(servis.pronadjiSve(UUID.randomUUID().toString())).isEmpty();
        assertThat(servis.pronadjiSve("9999999999999999999999999999999999")).isEmpty();
    }

    @Test
    void posebniZnakoviPretrageNisuZamjenskiZnakovi() {
        String pojam = "Test" + UUID.randomUUID();
        KrizmaForma forma = forma(osoba("Ana", "Anić"));
        forma.setBrojMatice(pojam + "%_");
        Krizma tocni = servis.dodaj(forma);
        forma.setBrojMatice(pojam + "AB");
        forma.setZupljaninId(osoba("Ivan", "Ivić").getId());
        Krizma drugi = servis.dodaj(forma);
        assertThat(servis.pronadjiSve(pojam + "%_")).extracting(Krizma::getId)
                .containsExactly(tocni.getId()).doesNotContain(drugi.getId());
    }

    @Test
    void izborJeSortiranPoPrezimenuIImenuIUkljucujeNeaktivne() {
        String prezime = "Test" + UUID.randomUUID();
        Zupljanin zadnji = osoba("Ana", prezime + "B");
        Zupljanin drugi = osoba("Zoran", prezime + "A");
        Zupljanin prvi = osoba("Ana", prezime + "A");
        zupljaninService.deaktiviraj(prvi.getId());
        assertThat(servis.zupljaniZaIzbor(null).stream().filter(z -> z.getPrezime().startsWith(prezime)))
                .extracting(Zupljanin::getId).containsExactly(prvi.getId(), drugi.getId(), zadnji.getId());
    }

    @Test
    void servisOdbijaNeispravanUnos() {
        long broj = repository.count();
        assertThatThrownBy(() -> servis.dodaj(new KrizmaForma()))
                .isInstanceOf(ConstraintViolationException.class);
        assertThat(repository.count()).isEqualTo(broj);
    }

    @Test
    void neispravnoUredjivanjeNeMijenjaZapis() {
        Krizma zapis = servis.dodaj(forma(osoba("Ana", "Anić")));
        KrizmaForma prije = servis.formaZaUredjivanje(zapis.getId());
        KrizmaForma izmjene = servis.formaZaUredjivanje(zapis.getId());
        izmjene.setDatumKrizme(LocalDate.now().plusDays(1));
        assertThatThrownBy(() -> servis.uredi(zapis.getId(), izmjene))
                .isInstanceOf(ConstraintViolationException.class);
        assertThat(servis.formaZaUredjivanje(zapis.getId())).usingRecursiveComparison().isEqualTo(prije);
    }

    @Test
    void nepostojeciZupljaninSeOdbijaPriDodavanjuIUredjivanju() {
        Krizma zapis = servis.dodaj(forma(osoba("Ana", "Anić")));
        KrizmaForma forma = servis.formaZaUredjivanje(zapis.getId());
        forma.setZupljaninId(0L);
        for (Runnable radnja : new Runnable[]{() -> servis.dodaj(forma), () -> servis.uredi(zapis.getId(), forma)}) {
            assertThatThrownBy(radnja::run).isInstanceOf(KrizmaPodaciException.class);
        }
    }

    @Test
    void nepostojeciZapisVraca404() {
        KrizmaForma forma = forma(osoba("Ana", "Anić"));
        for (Runnable radnja : new Runnable[]{() -> servis.pronadjiPoId(0L),
                () -> servis.formaZaUredjivanje(0L), () -> servis.uredi(0L, forma)}) {
            assertThatThrownBy(radnja::run).isInstanceOfSatisfying(ResponseStatusException.class,
                    greska -> assertThat(greska.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        }
    }

    @Test
    void prviZapisUspijeADrugiZaIstogZupljaninaSeOdbija() {
        KrizmaForma forma = forma(osoba("Ana", "Anić"));
        Krizma prvi = servis.dodaj(forma);
        osvjezi();
        long broj = repository.count();

        assertThat(prvi.getId()).isNotNull();
        assertThatThrownBy(() -> servis.dodaj(forma))
                .isInstanceOf(KrizmaPodaciException.class)
                .hasMessage("Odabrani župljanin već ima evidentiranu krizmu.");
        assertThat(repository.count()).isEqualTo(broj);
        assertThat(servis.formaZaUredjivanje(prvi.getId())).usingRecursiveComparison().isEqualTo(forma);
    }

    @Test
    void uredjivanjeIstogZapisaNijeKonflikt() {
        KrizmaForma forma = forma(osoba("Ana", "Anić"));
        Krizma zapis = servis.dodaj(forma);
        osvjezi();
        long broj = repository.count();
        forma.setMjestoKrizme("Čitluk");

        servis.uredi(zapis.getId(), forma);
        osvjezi();

        assertThat(repository.count()).isEqualTo(broj);
        assertThat(servis.formaZaUredjivanje(zapis.getId())).usingRecursiveComparison().isEqualTo(forma);
    }

    @Test
    void uredjivanjeNeMozePreuzetiZupljaninaKojiVecImaKrizmu() {
        Krizma prvi = servis.dodaj(forma(osoba("Ana", "Anić")));
        Krizma drugi = servis.dodaj(forma(osoba("Ivan", "Ivić")));
        osvjezi();
        KrizmaForma prije = servis.formaZaUredjivanje(prvi.getId());
        KrizmaForma drugiPrije = servis.formaZaUredjivanje(drugi.getId());
        KrizmaForma izmjene = servis.formaZaUredjivanje(prvi.getId());
        izmjene.setZupljaninId(drugiPrije.getZupljaninId());
        izmjene.setMjestoKrizme("Novo mjesto");

        assertThatThrownBy(() -> servis.uredi(prvi.getId(), izmjene))
                .isInstanceOf(KrizmaPodaciException.class)
                .hasMessage("Odabrani župljanin već ima evidentiranu krizmu.");
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
                  and tc.table_name = 'krizme'
                  and tc.constraint_type = 'UNIQUE'
                  and tc.constraint_name = 'uk_krizme_zupljanin'
                """, String.class);
        assertThat(stupci).containsExactly("zupljanin_id");
    }

    @Test
    void bazaOdbijaDuplikatPriIzravnomUnosuBezServisaIJpaValidacije() {
        Zupljanin zupljanin = osoba("Ana", "Anić");
        servis.dodaj(forma(zupljanin));

        assertThatThrownBy(() -> jdbcTemplate.update("""
                insert into krizme (zupljanin_id, datum_krizme, mjesto_krizme, datum_kreiranja)
                values (?, ?, ?, current_timestamp)
                """, zupljanin.getId(), LocalDate.of(2000, 1, 1), "Mostar"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause().isInstanceOfSatisfying(SQLException.class, greska -> {
                    assertThat(greska.getSQLState()).isEqualTo("23505");
                    assertThat(greska.getMessage()).contains("uk_krizme_zupljanin");
                });
        // Nakon SQL greške PostgreSQL transakcija smije samo završiti rollbackom.
    }

    @Test
    void bazaOdbijaDuplikatPriIzravnomUredjivanju() {
        Krizma prvi = servis.dodaj(forma(osoba("Ana", "Anić")));
        Krizma drugi = servis.dodaj(forma(osoba("Ivan", "Ivić")));

        assertThatThrownBy(() -> jdbcTemplate.update(
                "update krizme set zupljanin_id = ? where id = ?",
                prvi.getZupljanin().getId(), drugi.getId()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause().isInstanceOfSatisfying(SQLException.class, greska -> {
                    assertThat(greska.getSQLState()).isEqualTo("23505");
                    assertThat(greska.getMessage()).contains("uk_krizme_zupljanin");
                });
    }

    @Test
    void izborIskljucujeZauzeteZupljaneAliUkljucujeVlastitiZapis() {
        Zupljanin slobodan = osoba("Slobodan", "Test");
        Zupljanin vlastiti = osoba("Vlastiti", "Test");
        Zupljanin zauzeti = osoba("Zauzeti", "Test");
        Krizma zapis = servis.dodaj(forma(vlastiti));
        servis.dodaj(forma(zauzeti));
        osvjezi();

        assertThat(servis.zupljaniZaIzbor(null)).extracting(Zupljanin::getId)
                .contains(slobodan.getId()).doesNotContain(vlastiti.getId(), zauzeti.getId());
        assertThat(servis.zupljaniZaIzbor(zapis.getId())).extracting(Zupljanin::getId)
                .contains(slobodan.getId(), vlastiti.getId()).doesNotContain(zauzeti.getId());

        servis.uredi(zapis.getId(), forma(slobodan));
        osvjezi();
        assertThat(servis.zupljaniZaIzbor(null)).extracting(Zupljanin::getId)
                .contains(vlastiti.getId()).doesNotContain(slobodan.getId(), zauzeti.getId());
    }

    @Test
    void krstenjeIPrvaPricestNeSprjecavajuKrizmuIstogZupljanina() {
        Zupljanin osoba = osoba("Ana", "Anić");
        Krstenje krstenje = new Krstenje();
        krstenje.setZupljanin(osoba);
        krstenje.setDatumKrstenja(LocalDate.of(1999, 2, 1));
        krstenje.setMjestoKrstenja("Mostar");
        entityManager.persist(krstenje);
        PrvaPricest pricest = new PrvaPricest();
        pricest.setZupljanin(osoba);
        pricest.setDatumPricesti(LocalDate.of(2008, 5, 1));
        pricest.setMjestoPricesti("Mostar");
        entityManager.persist(pricest);
        osvjezi();

        assertThat(servis.zupljaniZaIzbor(null)).extracting(Zupljanin::getId).contains(osoba.getId());
        KrizmaForma forma = forma(osoba);
        forma.setDatumKrizme(LocalDate.of(2013, 5, 1));
        Krizma krizma = servis.dodaj(forma);
        osvjezi();
        assertThat(servis.pronadjiPoId(krizma.getId()).getZupljanin().getId()).isEqualTo(osoba.getId());
        assertThat(entityManager.find(Krstenje.class, krstenje.getId()).getZupljanin().getId())
                .isEqualTo(osoba.getId());
        assertThat(entityManager.find(PrvaPricest.class, pricest.getId()).getZupljanin().getId())
                .isEqualTo(osoba.getId());
    }

    private Zupljanin osoba(String ime, String prezime) {
        Zupljanin osoba = new Zupljanin();
        osoba.setIme(ime);
        osoba.setPrezime(prezime);
        osoba.setDatumRodjenja(LocalDate.of(1999, 1, 1));
        return zupljaninService.dodaj(osoba);
    }

    private KrizmaForma forma(Zupljanin osoba) {
        KrizmaForma forma = new KrizmaForma();
        forma.setZupljaninId(osoba.getId());
        forma.setDatumKrizme(LocalDate.of(2000, 5, 12));
        forma.setMjestoKrizme(" Mostar ");
        return forma;
    }

    private void osvjezi() {
        entityManager.flush();
        entityManager.clear();
    }
}
