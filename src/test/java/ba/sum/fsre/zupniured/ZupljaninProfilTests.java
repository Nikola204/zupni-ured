package ba.sum.fsre.zupniured;

import ba.sum.fsre.zupniured.dto.KorisnikForma;
import ba.sum.fsre.zupniured.dto.ZupljaninProfil;
import ba.sum.fsre.zupniured.model.*;
import ba.sum.fsre.zupniured.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.pocetni-admin.enabled=false")
@AutoConfigureMockMvc
@Transactional
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ZupljaninProfilTests {
    private final MockMvc mvc;
    private final EntityManager em;
    private final ZupljaninService osobe;
    private final ZupljaninProfilService profili;
    private final KorisnikService korisnici;
    private final LocalDate datum = LocalDate.of(2020, 5, 12);

    ZupljaninProfilTests(MockMvc mvc, EntityManager em, ZupljaninService osobe,
                        ZupljaninProfilService profili, KorisnikService korisnici) {
        this.mvc = mvc; this.em = em; this.osobe = osobe;
        this.profili = profili; this.korisnici = korisnici;
    }

    @ParameterizedTest
    @EnumSource(Uloga.class)
    void dozvoljeneUlogeVideProfilINavigaciju(Uloga uloga) throws Exception {
        Zupljanin z = osoba("Ivan");
        MockHttpSession session = prijavi(uloga);
        mvc.perform(get("/zupljani/" + z.getId()).session(session))
                .andExpect(status().isOk()).andExpect(view().name("zupljani/detalji"))
                .andExpect(content().string(containsString("Ivan")))
                .andExpect(content().string(containsString("Uredi župljanina")))
                .andExpect(content().string(containsString("href=\"/zupljani/uredi/" + z.getId() + "\"")))
                .andExpect(content().string(containsString("Povratak na popis župljana")));
        mvc.perform(get("/zupljani").session(session))
                .andExpect(content().string(containsString("href=\"/zupljani/" + z.getId() + "\"")));
    }

    @Test
    void anonimniNemaPristup() throws Exception {
        mvc.perform(get("/zupljani/1")).andExpect(redirectedUrl("/login")).andExpect(unauthenticated());
    }

    @Test
    void nepostojeciIdVraca404() throws Exception {
        mvc.perform(get("/zupljani/0").session(prijavi(Uloga.ADMIN))).andExpect(status().isNotFound());
    }

    @Test
    void osobniPodaciIDatumiPrikazaniSuBezNullVrijednosti() throws Exception {
        Zupljanin z = osoba("Ivan");
        z.setDatumRodjenja(LocalDate.of(1990, 2, 3)); z.setMjestoRodjenja("Mostar");
        z.setAdresa("Ulica 12"); z.setTelefon("123456"); z.setEmail("ivan@primjer.hr");
        z.setNapomena("Važna bilješka"); z.setAktivan(false);
        em.flush();
        String html = mvc.perform(get("/zupljani/" + z.getId()).session(prijavi(Uloga.ADMIN)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Ivan", "Test", "03.02.1990.", "Mostar", "Ulica 12",
                "123456", "ivan@primjer.hr", "Važna bilješka", "Neaktivan",
                z.getDatumKreiranja().format(DateTimeFormatter.ofPattern("dd.MM.yyyy.")))
                .doesNotContain(">null<");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 4, 7})
    void profilPrikazujePripadajuceSakramente(int maska) throws Exception {
        Zupljanin z = osoba("Ivan");
        // Druga osoba uvijek ima sve sakramente; njezini zapisi ne smiju ući u profil.
        dodajSakramente(osoba("Druga osoba"), 7);
        dodajSakramente(z, maska);
        em.flush(); em.clear();
        ZupljaninProfil profil = profili.pronadjiPoId(z.getId());
        assertThat(profil.vjencanja()).isEmpty();
        for (int i = 0; i < 3; i++) {
            var s = profil.sakramenti().get(i);
            if ((maska & (1 << i)) != 0) {
                assertThat(s.datum()).isEqualTo(datum);
                assertThat(s.mjesto()).isEqualTo("Mostar");
                assertThat(s.poveznica()).isNotNull();
            } else {
                assertThat(s.poveznica()).isNull();
                assertThat(s.poruka()).startsWith("Nije evidentiran");
            }
        }
        String html = mvc.perform(get("/zupljani/" + z.getId()).session(prijavi(Uloga.DJELATNIK)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Krštenje", "Prva pričest", "Krizma", "Nema evidentiranih vjenčanja.")
                .doesNotContain(">null<");
        for (var s : profil.sakramenti()) {
            if (s.poveznica() != null) assertThat(html).contains("href=\"" + s.poveznica() + "\"", "12.05.2020.", s.matica());
            else assertThat(html).contains(s.poruka());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void vjencanjePronalaziObjeUlogeIRucnoUnesenogSupruznika(boolean mladozenja) throws Exception {
        Zupljanin z = osoba("Ivan");
        Vjencanje v = brak(mladozenja ? z : null, mladozenja ? null : z, datum);
        em.flush(); em.clear();
        var profil = profili.pronadjiPoId(z.getId());
        assertThat(profil.vjencanja()).hasSize(1);
        assertThat(profil.vjencanja().getFirst().supruznik())
                .isEqualTo(mladozenja ? "Povijesna Nevjesta" : "Povijesni Mladoženja");
        mvc.perform(get("/zupljani/" + z.getId()).session(prijavi(Uloga.ZUPNIK)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(profil.vjencanja().getFirst().supruznik())))
                .andExpect(content().string(containsString("href=\"/vjencanja/" + v.getId() + "\"")))
                .andExpect(content().string(containsString("12.05.2020.")))
                .andExpect(content().string(not(containsString("Nema evidentiranih vjenčanja."))));
    }

    @Test
    void viseVjencanjaObjeUlogePovijesnaImenaISviSakramenti() throws Exception {
        Zupljanin z = osoba("Ivan"), drugi = osoba("Ana");
        dodajSakramente(z, 7);
        Vjencanje prvo = brak(z, drugi, datum.minusYears(5));
        Vjencanje drugo = brak(drugi, z, datum);
        brak(null, null, datum); // Ista povijesna imena bez veze ne pripadaju profilu.
        brak(drugi, null, datum);
        drugi.setIme("Promijenjeno"); drugi.setPrezime("Novo prezime");
        em.flush(); em.clear();
        var profil = profili.pronadjiPoId(z.getId());
        assertThat(profil.vjencanja()).extracting(v -> v.id()).containsExactly(drugo.getId(), prvo.getId());
        assertThat(profil.vjencanja()).extracting(v -> v.supruznik())
                .containsExactly("Povijesni Mladoženja", "Povijesna Nevjesta");
        assertThat(profil.sakramenti()).allMatch(s -> s.poveznica() != null);
        mvc.perform(get("/zupljani/" + z.getId()).session(prijavi(Uloga.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Povijesni Mladoženja")))
                .andExpect(content().string(containsString("Povijesna Nevjesta")))
                .andExpect(content().string(not(containsString("Promijenjeno"))));
    }

    @Test
    void neobaveznaPoljaIMaticeImajuUredanPrikaz() throws Exception {
        Zupljanin z = osoba("Ivan");
        Vjencanje bez = brak(z, null, datum);
        Vjencanje broj = brak(z, null, datum.minusDays(1)); broj.setBrojMatice("42");
        Vjencanje godina = brak(z, null, datum.minusDays(2)); godina.setGodinaMatice(2020);
        Vjencanje oba = brak(z, null, datum.minusDays(3)); oba.setBrojMatice("43"); oba.setGodinaMatice(2019);
        em.flush(); em.clear();
        assertThat(profili.pronadjiPoId(z.getId()).vjencanja()).extracting(v -> v.matica())
                .containsExactly("—", "42", "2020", "43/2019");
        mvc.perform(get("/zupljani/" + z.getId()).session(prijavi(Uloga.ADMIN)))
                .andExpect(status().isOk()).andExpect(content().string(not(containsString(">null<"))))
                .andExpect(content().string(containsString("Aktivan")))
                .andExpect(content().string(containsString("43/2019")));
    }

    private void dodajSakramente(Zupljanin z, int maska) {
        if ((maska & 1) != 0) {
            Krstenje k = new Krstenje(); k.setZupljanin(z); k.setDatumKrstenja(datum);
            k.setMjestoKrstenja("Mostar"); k.setBrojMatice("K12"); k.setGodinaMatice(2020); em.persist(k);
        }
        if ((maska & 2) != 0) {
            PrvaPricest p = new PrvaPricest(); p.setZupljanin(z); p.setDatumPricesti(datum);
            p.setMjestoPricesti("Mostar"); p.setBrojMatice("P12"); em.persist(p);
        }
        if ((maska & 4) != 0) {
            Krizma k = new Krizma(); k.setZupljanin(z); k.setDatumKrizme(datum);
            k.setMjestoKrizme("Mostar"); k.setGodinaMatice(2020); em.persist(k);
        }
    }

    private Vjencanje brak(Zupljanin m, Zupljanin n, LocalDate datum) {
        Vjencanje v = new Vjencanje();
        v.setMladozenjaZupljanin(m); v.setNevjestaZupljanin(n);
        v.setImeMladozenje("Povijesni"); v.setPrezimeMladozenje("Mladoženja");
        v.setImeNevjeste("Povijesna"); v.setPrezimeNevjeste("Nevjesta");
        v.setDatumVjencanja(datum); v.setMjestoVjencanja("Mostar"); em.persist(v);
        return v;
    }

    private Zupljanin osoba(String ime) {
        Zupljanin z = new Zupljanin(); z.setIme(ime); z.setPrezime("Test");
        return osobe.dodaj(z);
    }
    private MockHttpSession prijavi(Uloga uloga) throws Exception {
        KorisnikForma forma = new KorisnikForma();
        forma.setIme("Test");
        forma.setPrezime("Djelatnik");
        forma.setEmail(UUID.randomUUID() + "@primjer.hr");
        forma.setLozinka("TestnaLozinka123!");
        forma.setUloga(uloga);
        korisnici.dodaj(forma);
        return (MockHttpSession) mvc.perform(formLogin("/login")
                        .user("email", forma.getEmail()).password("lozinka", forma.getLozinka()))
                .andExpect(authenticated()).andReturn().getRequest().getSession(false);
    }
}
