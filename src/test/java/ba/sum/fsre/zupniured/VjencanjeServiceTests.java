package ba.sum.fsre.zupniured;

import ba.sum.fsre.zupniured.dto.VjencanjeForma;
import ba.sum.fsre.zupniured.model.*;
import ba.sum.fsre.zupniured.repository.VjencanjeRepository;
import ba.sum.fsre.zupniured.repository.ZupljaninRepository;
import ba.sum.fsre.zupniured.service.*;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.UUID;
import java.util.Locale;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = "app.pocetni-admin.enabled=false")
@Transactional
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class VjencanjeServiceTests {
    private final VjencanjeService servis;
    private final ZupljaninService osobe;
    private final VjencanjeRepository repository;
    private final ZupljaninRepository osobaRepository;
    private final EntityManager em;
    private final JdbcTemplate jdbc;

    VjencanjeServiceTests(VjencanjeService servis, ZupljaninService osobe, VjencanjeRepository repository,
                         ZupljaninRepository osobaRepository, EntityManager em, JdbcTemplate jdbc) {
        this.servis = servis; this.osobe = osobe; this.repository = repository;
        this.osobaRepository = osobaRepository; this.em = em; this.jdbc = jdbc;
    }

    @ParameterizedTest
    @CsvSource({"false,false", "true,false", "false,true", "true,true"})
    void podrzavaSveKombinacijeBezStvaranjaNovihZupljana(boolean mladozenja, boolean nevjesta) {
        Zupljanin m = osoba("Ivan", "Ivić");
        Zupljanin n = osoba("Ana", "Anić");
        long brojOsoba = osobaRepository.count();
        VjencanjeForma forma = forma();
        if (mladozenja) {
            forma.setMladozenjaZupljaninId(m.getId());
            forma.setImeMladozenje(null);
            forma.setPrezimeMladozenje("Ignorirano");
        }
        if (nevjesta) {
            forma.setNevjestaZupljaninId(n.getId());
            forma.setImeNevjeste("Ignorirano");
            forma.setPrezimeNevjeste(null);
        }
        Vjencanje zapis = servis.dodaj(forma);
        osvjezi();
        zapis = servis.pronadjiPoId(zapis.getId());
        assertThat(zapis.getImeMladozenje()).isEqualTo(mladozenja ? "Ivan" : "Marko");
        assertThat(zapis.getPrezimeMladozenje()).isEqualTo(mladozenja ? "Ivić" : "Marić");
        assertThat(zapis.getImeNevjeste()).isEqualTo(nevjesta ? "Ana" : "Iva");
        assertThat(zapis.getPrezimeNevjeste()).isEqualTo(nevjesta ? "Anić" : "Perić");
        assertThat(zapis.getMladozenjaZupljanin() == null ? null : zapis.getMladozenjaZupljanin().getId())
                .isEqualTo(mladozenja ? m.getId() : null);
        assertThat(zapis.getNevjestaZupljanin() == null ? null : zapis.getNevjestaZupljanin().getId())
                .isEqualTo(nevjesta ? n.getId() : null);
        assertThat(zapis.getDatumKreiranja()).isNotNull();
        assertThat(osobaRepository.count()).isEqualTo(brojOsoba);
    }

    @Test
    void spremaIUredjujeSvaPoljaCuvaIdIDatumKreiranja() {
        VjencanjeForma forma = forma();
        forma.setSvjedokMladozenje(" Svjedok ");
        forma.setSvjedokNevjeste(" Svjedokinja ");
        forma.setSluziteljVjencanja(" Fra Petar ");
        forma.setBrojMatice(" V-123 ");
        forma.setGodinaMatice(2020);
        forma.setNapomena(" Napomena ");
        Vjencanje zapis = servis.dodaj(forma);
        osvjezi();
        var datum = servis.pronadjiPoId(zapis.getId()).getDatumKreiranja();
        assertThat(servis.formaZaUredjivanje(zapis.getId())).usingRecursiveComparison().isEqualTo(forma);
        forma.setImeMladozenje("Novo");
        forma.setPrezimeMladozenje("Prezime");
        forma.setImeNevjeste("Nova");
        forma.setPrezimeNevjeste("Drugo prezime");
        forma.setDatumVjencanja(LocalDate.of(2021, 6, 2));
        forma.setMjestoVjencanja("Čitluk");
        forma.setSvjedokMladozenje("Drugi svjedok");
        forma.setSvjedokNevjeste("Druga svjedokinja");
        forma.setSluziteljVjencanja("Fra Ivan");
        forma.setBrojMatice("V-124");
        forma.setGodinaMatice(2021);
        forma.setNapomena("Izmjena");
        servis.uredi(zapis.getId(), forma);
        osvjezi();
        assertThat(servis.formaZaUredjivanje(zapis.getId())).usingRecursiveComparison().isEqualTo(forma);
        assertThat(servis.pronadjiPoId(zapis.getId()).getDatumKreiranja()).isEqualTo(datum);
    }

    @Test
    void promjeneZupljanaINaknadnoUredjivanjeVjencanjaCuvajuPovijesnaImena() {
        Zupljanin m = osoba("StaroImeM", "StaroPrezimeM");
        Zupljanin n = osoba("StaroImeN", "StaroPrezimeN");
        VjencanjeForma forma = forma();
        forma.setMladozenjaZupljaninId(m.getId());
        forma.setNevjestaZupljaninId(n.getId());
        Vjencanje zapis = servis.dodaj(forma);
        for (Long id : new Long[]{m.getId(), n.getId()}) {
            Zupljanin noviPodaci = new Zupljanin();
            noviPodaci.setIme("NovoIme");
            noviPodaci.setPrezime("NovoPrezime");
            osobe.uredi(id, noviPodaci);
        }
        osvjezi();
        VjencanjeForma povijesni = servis.formaZaUredjivanje(zapis.getId());
        assertThat(povijesni.getImeMladozenje()).isEqualTo("StaroImeM");
        assertThat(povijesni.getPrezimeMladozenje()).isEqualTo("StaroPrezimeM");
        assertThat(povijesni.getImeNevjeste()).isEqualTo("StaroImeN");
        assertThat(povijesni.getPrezimeNevjeste()).isEqualTo("StaroPrezimeN");
        povijesni.setNapomena("Naknadna bilješka");
        servis.uredi(zapis.getId(), povijesni);
        osvjezi();
        assertThat(servis.formaZaUredjivanje(zapis.getId())).usingRecursiveComparison().isEqualTo(povijesni);
        assertThat(servis.pronadjiSve("StaroPrezimeN")).extracting(Vjencanje::getId).contains(zapis.getId());
        assertThat(servis.pronadjiSve("NovoPrezime")).extracting(Vjencanje::getId).doesNotContain(zapis.getId());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void povezujeMijenjaIUklanjaVezuZaObaMladenca(boolean mladozenja) {
        Vjencanje zapis = servis.dodaj(forma());
        Zupljanin prvi = osoba("Prvo ime", "Prvo prezime");
        Zupljanin drugi = osoba("Drugo ime", "Drugo prezime");
        for (Zupljanin osoba : new Zupljanin[]{prvi, drugi}) {
            VjencanjeForma izmjene = servis.formaZaUredjivanje(zapis.getId());
            postaviVezu(izmjene, mladozenja, osoba.getId());
            servis.uredi(zapis.getId(), izmjene);
            osvjezi();
            VjencanjeForma rezultat = servis.formaZaUredjivanje(zapis.getId());
            assertThat(mladozenja ? rezultat.getImeMladozenje() : rezultat.getImeNevjeste()).isEqualTo(osoba.getIme());
            assertThat(mladozenja ? rezultat.getPrezimeMladozenje() : rezultat.getPrezimeNevjeste()).isEqualTo(osoba.getPrezime());
        }
        VjencanjeForma rucno = servis.formaZaUredjivanje(zapis.getId());
        postaviVezu(rucno, mladozenja, null);
        if (mladozenja) {
            rucno.setImeMladozenje("Ručno"); rucno.setPrezimeMladozenje("Uneseno");
        } else {
            rucno.setImeNevjeste("Ručno"); rucno.setPrezimeNevjeste("Uneseno");
        }
        servis.uredi(zapis.getId(), rucno);
        osvjezi();
        assertThat(servis.formaZaUredjivanje(zapis.getId())).usingRecursiveComparison().isEqualTo(rucno);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void nepostojecaVezaOdbijenaPrijeIzmjenePodataka(boolean mladozenja) {
        Vjencanje zapis = servis.dodaj(forma());
        VjencanjeForma prije = servis.formaZaUredjivanje(zapis.getId());
        VjencanjeForma losa = forma();
        postaviVezu(losa, mladozenja, 0L);
        losa.setNapomena("Ne spremati");
        for (Runnable radnja : new Runnable[]{() -> servis.dodaj(losa), () -> servis.uredi(zapis.getId(), losa)}) {
            assertThatThrownBy(radnja::run).isInstanceOfSatisfying(VjencanjePodaciException.class,
                    e -> assertThat(e.getPolje()).isEqualTo(mladozenja ? "mladozenjaZupljaninId" : "nevjestaZupljaninId"));
        }
        osvjezi();
        assertThat(servis.formaZaUredjivanje(zapis.getId())).usingRecursiveComparison().isEqualTo(prije);
    }

    @ParameterizedTest
    @ValueSource(strings = {"imeM", "prezimeM", "imeN", "prezimeN", "datum", "buducnost", "mjesto", "istaOsoba"})
    void servisOdbijaNeispravanUnosIUredjivanje(String slucaj) {
        Vjencanje zapis = servis.dodaj(forma());
        VjencanjeForma losa = forma();
        switch (slucaj) {
            case "imeM" -> losa.setImeMladozenje(" ");
            case "prezimeM" -> losa.setPrezimeMladozenje(null);
            case "imeN" -> losa.setImeNevjeste("");
            case "prezimeN" -> losa.setPrezimeNevjeste(null);
            case "datum" -> losa.setDatumVjencanja(null);
            case "buducnost" -> losa.setDatumVjencanja(LocalDate.now().plusDays(1));
            case "mjesto" -> losa.setMjestoVjencanja(" ");
            case "istaOsoba" -> {
                Long id = osoba("Ista", "Osoba").getId();
                losa.setMladozenjaZupljaninId(id); losa.setNevjestaZupljaninId(id);
            }
        }
        assertThatThrownBy(() -> servis.dodaj(losa)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> servis.uredi(zapis.getId(), losa)).isInstanceOf(ConstraintViolationException.class);
        osvjezi();
        assertThat(servis.formaZaUredjivanje(zapis.getId())).usingRecursiveComparison().isEqualTo(forma());
    }

    @ParameterizedTest
    @ValueSource(strings = {"imeM", "prezimeM", "imeN", "prezimeN", "broj"})
    void pretragaPoSvimTekstualnimPoljimaNeOvisiOVeliciniSlova(String polje) {
        String pojam = "Test" + UUID.randomUUID();
        VjencanjeForma forma = forma();
        switch (polje) {
            case "imeM" -> forma.setImeMladozenje("A" + pojam);
            case "prezimeM" -> forma.setPrezimeMladozenje("A" + pojam);
            case "imeN" -> forma.setImeNevjeste("A" + pojam);
            case "prezimeN" -> forma.setPrezimeNevjeste("A" + pojam);
            case "broj" -> forma.setBrojMatice("A" + pojam);
        }
        Vjencanje zapis = servis.dodaj(forma);
        servis.dodaj(forma());
        assertThat(servis.pronadjiSve("  " + pojam.toUpperCase(Locale.ROOT) + "  "))
                .extracting(Vjencanje::getId).containsExactly(zapis.getId());
    }

    @Test
    void pretragaGodinePraznogPojmaINepostojecegPojma() {
        VjencanjeForma forma = forma();
        forma.setGodinaMatice(1889);
        Vjencanje zapis = servis.dodaj(forma);
        Vjencanje drugi = servis.dodaj(forma());
        assertThat(servis.pronadjiSve("1889")).extracting(Vjencanje::getId).contains(zapis.getId()).doesNotContain(drugi.getId());
        for (String pojam : new String[]{null, "", " "}) {
            assertThat(servis.pronadjiSve(pojam)).extracting(Vjencanje::getId).contains(zapis.getId(), drugi.getId());
        }
        assertThat(servis.pronadjiSve(UUID.randomUUID().toString())).isEmpty();
        assertThat(servis.pronadjiSve("99999999999999999999999999")).isEmpty();
    }

    @Test
    void sviSakramentiIViseVjencanjaMoguPripadatiIstojOsobi() {
        Zupljanin m = osoba("Ivan", "Ivić");
        Zupljanin n = osoba("Ana", "Anić");
        for (Zupljanin osoba : new Zupljanin[]{m, n}) {
            Krstenje k = new Krstenje(); k.setZupljanin(osoba);
            k.setDatumKrstenja(LocalDate.of(1990, 1, 1)); k.setMjestoKrstenja("Mostar"); em.persist(k);
            PrvaPricest p = new PrvaPricest(); p.setZupljanin(osoba);
            p.setDatumPricesti(LocalDate.of(2000, 1, 1)); p.setMjestoPricesti("Mostar"); em.persist(p);
            Krizma kr = new Krizma(); kr.setZupljanin(osoba);
            kr.setDatumKrizme(LocalDate.of(2005, 1, 1)); kr.setMjestoKrizme("Mostar"); em.persist(kr);
        }
        VjencanjeForma forma = forma();
        forma.setMladozenjaZupljaninId(m.getId()); forma.setNevjestaZupljaninId(n.getId());
        Vjencanje prvi = servis.dodaj(forma);
        Vjencanje drugi = servis.dodaj(forma);
        osvjezi();
        assertThat(prvi.getId()).isNotEqualTo(drugi.getId());
        // Izravan SQL unos potvrđuje da baza dopušta ponavljanje obiju veza.
        assertThat(jdbc.update("""
                insert into vjencanja (mladozenja_zupljanin_id, nevjesta_zupljanin_id,
                    ime_mladozenje, prezime_mladozenje, ime_nevjeste, prezime_nevjeste,
                    datum_vjencanja, mjesto_vjencanja, datum_kreiranja)
                select mladozenja_zupljanin_id, nevjesta_zupljanin_id,
                    ime_mladozenje, prezime_mladozenje, ime_nevjeste, prezime_nevjeste,
                    datum_vjencanja, mjesto_vjencanja, current_timestamp
                from vjencanja where id = ?
                """, prvi.getId())).isEqualTo(1);
        assertThat(servis.zupljaniZaIzbor()).extracting(Zupljanin::getId).contains(m.getId(), n.getId());
    }

    @Test
    void vezeSuOpcionalneIStraniKljuceviUBazi() {
        assertThat(jdbc.queryForList("""
                select column_name from information_schema.columns
                where table_schema = current_schema() and table_name = 'vjencanja'
                  and column_name in ('mladozenja_zupljanin_id', 'nevjesta_zupljanin_id')
                  and is_nullable = 'YES'
                """, String.class)).containsExactlyInAnyOrder("mladozenja_zupljanin_id", "nevjesta_zupljanin_id");
        assertThat(jdbc.queryForObject("""
                select count(*) from information_schema.table_constraints
                where table_schema = current_schema() and table_name = 'vjencanja'
                  and constraint_type = 'FOREIGN KEY'
                """, Long.class)).isEqualTo(2);
    }

    @Test
    void izborJeSortiranPoPrezimenuIImenu() {
        String prezime = UUID.randomUUID().toString();
        Zupljanin c = osoba("Ana", prezime + "B"), b = osoba("Zoran", prezime + "A"), a = osoba("Ana", prezime + "A");
        assertThat(servis.zupljaniZaIzbor().stream().filter(z -> z.getPrezime().startsWith(prezime)))
                .extracting(Zupljanin::getId).containsExactly(a.getId(), b.getId(), c.getId());
    }

    @Test
    void nepostojeciZapisVraca404() {
        for (Runnable radnja : new Runnable[]{() -> servis.pronadjiPoId(0L),
                () -> servis.formaZaUredjivanje(0L), () -> servis.uredi(0L, forma())}) {
            assertThatThrownBy(radnja::run).isInstanceOfSatisfying(ResponseStatusException.class,
                    e -> assertThat(e.getStatusCode().value()).isEqualTo(404));
        }
    }

    private void postaviVezu(VjencanjeForma forma, boolean mladozenja, Long id) {
        if (mladozenja) forma.setMladozenjaZupljaninId(id); else forma.setNevjestaZupljaninId(id);
    }
    private Zupljanin osoba(String ime, String prezime) {
        Zupljanin z = new Zupljanin(); z.setIme(ime); z.setPrezime(prezime);
        return osobe.dodaj(z);
    }
    private VjencanjeForma forma() {
        VjencanjeForma f = new VjencanjeForma();
        f.setImeMladozenje(" Marko "); f.setPrezimeMladozenje(" Marić ");
        f.setImeNevjeste(" Iva "); f.setPrezimeNevjeste(" Perić ");
        f.setDatumVjencanja(LocalDate.of(2020, 5, 12)); f.setMjestoVjencanja(" Mostar ");
        return f;
    }
    private void osvjezi() { em.flush(); em.clear(); }
}
