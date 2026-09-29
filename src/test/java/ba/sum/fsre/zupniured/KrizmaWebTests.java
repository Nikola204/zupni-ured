package ba.sum.fsre.zupniured;

import ba.sum.fsre.zupniured.dto.KorisnikForma;
import ba.sum.fsre.zupniured.dto.KrizmaForma;
import ba.sum.fsre.zupniured.model.Krizma;
import ba.sum.fsre.zupniured.model.Uloga;
import ba.sum.fsre.zupniured.model.Zupljanin;
import ba.sum.fsre.zupniured.repository.KrizmaRepository;
import ba.sum.fsre.zupniured.service.KorisnikService;
import ba.sum.fsre.zupniured.service.KrizmaService;
import ba.sum.fsre.zupniured.service.ZupljaninService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.pocetni-admin.enabled=false")
@AutoConfigureMockMvc
@Transactional
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class KrizmaWebTests {

    private final MockMvc mvc;
    private final KorisnikService korisnikService;
    private final KrizmaService servis;
    private final ZupljaninService zupljaninService;
    private final KrizmaRepository repository;

    KrizmaWebTests(MockMvc mvc, KorisnikService korisnikService, KrizmaService servis,
                    ZupljaninService zupljaninService, KrizmaRepository repository) {
        this.mvc = mvc;
        this.korisnikService = korisnikService;
        this.servis = servis;
        this.zupljaninService = zupljaninService;
        this.repository = repository;
    }

    @ParameterizedTest
    @EnumSource(Uloga.class)
    void sveTriUlogeMoguKoristitiCijeliModul(Uloga uloga) throws Exception {
        MockHttpSession session = prijavi(uloga);
        Zupljanin osoba = osoba();
        mvc.perform(get("/").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Krizme")));
        mvc.perform(get("/krizma").session(session)).andExpect(status().isOk()).andExpect(authenticated());
        mvc.perform(get("/krizma/novi").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("Ana Anić — 01.01.1999.")));

        String oznaka = UUID.randomUUID().toString();
        mvc.perform(post("/krizma/novi").session(session).with(csrf())
                        .param("zupljaninId", osoba.getId().toString()).param("datumKrizme", "2000-05-12")
                        .param("mjestoKrizme", "Mostar").param("brojMatice", oznaka)
                        .param("godinaMatice", "2000")
                        .param("imeKrizmanogKuma", "Ivan Ivić").param("krizmatelj", "Biskup Petar")
                        .param("napomena", "Bilješka").param("id", "0")
                        .param("datumKreiranja", "1900-01-01T00:00:00")
                        .param("zupljanin.ime", "Podmetnuto"))
                .andExpect(status().is3xxRedirection());
        Krizma zapis = servis.pronadjiSve(oznaka).getFirst();
        assertThat(zapis.getId()).isPositive();
        assertThat(zapis.getDatumKreiranja().toLocalDate()).isEqualTo(LocalDate.now());
        assertThat(zapis.getZupljanin().getIme()).isEqualTo("Ana");
        var datumKreiranja = zapis.getDatumKreiranja();

        mvc.perform(get("/krizma").session(session).param("pretraga", oznaka))
                .andExpect(status().isOk()).andExpect(model().attribute("pretraga", oznaka))
                .andExpect(content().string(containsString("Ana Anić")))
                .andExpect(content().string(containsString("12.05.2000.")))
                .andExpect(content().string(containsString(oznaka)));
        mvc.perform(get("/krizma/" + zapis.getId()).session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("01.01.1999.")))
                .andExpect(content().string(containsString("12.05.2000.")))
                .andExpect(content().string(containsString("Ivan Ivić")))
                .andExpect(content().string(containsString("Biskup Petar")))
                .andExpect(content().string(containsString("Bilješka")));
        mvc.perform(get("/krizma/uredi/" + zapis.getId()).session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"2000-05-12\"")))
                .andExpect(content().string(containsString("value=\"Ivan Ivić\"")))
                .andExpect(content().string(containsString("value=\"Biskup Petar\"")))
                .andExpect(content().string(containsString("value=\"" + osoba.getId() + "\" selected=\"selected\"")));
        mvc.perform(post("/krizma/uredi/" + zapis.getId()).session(session).with(csrf())
                        .param("zupljaninId", osoba.getId().toString()).param("datumKrizme", "2001-06-13")
                        .param("mjestoKrizme", "Čitluk").param("brojMatice", "").param("godinaMatice", "")
                        .param("imeKrizmanogKuma", "Marko Marić").param("krizmatelj", "Biskup Ivan")
                        .param("datumKreiranja", "1900-01-01T00:00:00"))
                .andExpect(redirectedUrl("/krizma/" + zapis.getId()));
        Krizma uredjen = servis.pronadjiPoId(zapis.getId());
        assertThat(uredjen.getMjestoKrizme()).isEqualTo("Čitluk");
        assertThat(uredjen.getDatumKrizme()).isEqualTo(LocalDate.of(2001, 6, 13));
        assertThat(uredjen.getBrojMatice()).isNull();
        assertThat(uredjen.getGodinaMatice()).isNull();
        assertThat(uredjen.getImeKrizmanogKuma()).isEqualTo("Marko Marić");
        assertThat(uredjen.getKrizmatelj()).isEqualTo("Biskup Ivan");
        assertThat(uredjen.getDatumKreiranja()).isEqualTo(datumKreiranja);
    }

    @Test
    void anonimniKorisnikNeMozeCitatiNiMijenjatiKrizme() throws Exception {
        for (String putanja : new String[]{"/krizma", "/krizma/novi", "/krizma/uredi/1", "/krizma/1"}) {
            mvc.perform(get(putanja)).andExpect(redirectedUrl("/login")).andExpect(unauthenticated());
        }
        for (String putanja : new String[]{"/krizma/novi", "/krizma/uredi/1"}) {
            mvc.perform(post(putanja).with(csrf())).andExpect(redirectedUrl("/login"))
                    .andExpect(unauthenticated());
        }
    }

    @Test
    void obaObrascaPrikazujuGreskeIPonovnoNudeZupljane() throws Exception {
        MockHttpSession session = prijavi(Uloga.DJELATNIK);
        Krizma zapis = zapis();
        long broj = repository.count();
        for (String putanja : new String[]{"/krizma/novi", "/krizma/uredi/" + zapis.getId()}) {
            mvc.perform(post(putanja).session(session).with(csrf())
                            .param("zupljaninId", "").param("datumKrizme", "")
                            .param("mjestoKrizme", " "))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrors("krizma", "zupljaninId", "datumKrizme", "mjestoKrizme"))
                    .andExpect(model().attributeExists("zupljani"))
                    .andExpect(content().string(containsString("Župljanin je obavezan.")))
                    .andExpect(content().string(containsString("Datum krizme je obavezan.")))
                    .andExpect(content().string(containsString("Mjesto krizme je obavezno.")));
            mvc.perform(post(putanja).session(session).with(csrf())
                            .param("zupljaninId", zapis.getZupljanin().getId().toString())
                            .param("datumKrizme", LocalDate.now().plusDays(1).toString())
                            .param("mjestoKrizme", "Mostar"))
                    .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("krizma", "datumKrizme"))
                    .andExpect(content().string(containsString("Datum krizme ne smije biti u budućnosti.")));
        }
        assertThat(repository.count()).isEqualTo(broj);
        assertThat(servis.pronadjiPoId(zapis.getId()).getDatumKrizme()).isEqualTo(LocalDate.of(2000, 5, 12));
    }

    @Test
    void neispravniTipoviImajuHrvatskePoruke() throws Exception {
        mvc.perform(post("/krizma/novi").session(prijavi(Uloga.ZUPNIK)).with(csrf())
                        .param("zupljaninId", "abc").param("datumKrizme", "nije-datum")
                        .param("mjestoKrizme", "Mostar").param("godinaMatice", "nije-godina"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("krizma", "zupljaninId", "datumKrizme", "godinaMatice"))
                .andExpect(content().string(containsString("Odaberite postojećeg župljanina.")))
                .andExpect(content().string(containsString("Unesite ispravan datum krizme.")))
                .andExpect(content().string(containsString("Unesite ispravnu godinu matice.")));
    }

    @Test
    void nepostojeciZupljaninVracaGreskuNaObrascu() throws Exception {
        MockHttpSession session = prijavi(Uloga.ZUPNIK);
        Krizma zapis = zapis();
        long broj = repository.count();
        for (String putanja : new String[]{"/krizma/novi", "/krizma/uredi/" + zapis.getId()}) {
            mvc.perform(post(putanja).session(session).with(csrf())
                            .param("zupljaninId", "0").param("datumKrizme", "2000-05-12")
                            .param("mjestoKrizme", "Mostar"))
                    .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("krizma", "zupljaninId"))
                    .andExpect(content().string(containsString("Odabrani župljanin ne postoji.")))
                    .andExpect(model().attributeExists("zupljani"));
        }
        assertThat(repository.count()).isEqualTo(broj);
    }

    @Test
    void obaPostZahtjevaTrebajuCsrfToken() throws Exception {
        MockHttpSession session = prijavi(Uloga.ADMIN);
        Krizma zapis = zapis();
        for (String putanja : new String[]{"/krizma/novi", "/krizma/uredi/" + zapis.getId()}) {
            mvc.perform(post(putanja).session(session)).andExpect(status().isForbidden());
        }
    }

    @Test
    void nepostojeciDetaljiIUredjivanjeVracaju404() throws Exception {
        MockHttpSession session = prijavi(Uloga.DJELATNIK);
        mvc.perform(get("/krizma/0").session(session)).andExpect(status().isNotFound());
        mvc.perform(get("/krizma/uredi/0").session(session)).andExpect(status().isNotFound());
        mvc.perform(post("/krizma/uredi/0").session(session).with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    void brisanjeNijeDostupno() throws Exception {
        MockHttpSession session = prijavi(Uloga.ADMIN);
        Krizma zapis = zapis();
        mvc.perform(delete("/krizma/" + zapis.getId()).session(session).with(csrf()))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/krizma/obrisi/" + zapis.getId()).session(session).with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(repository.existsById(zapis.getId())).isTrue();
    }

    @Test
    void izborRazlikujeIstoimeneOsobeIDopustaNepoznatDatumRodjenja() throws Exception {
        osoba();
        Zupljanin druga = new Zupljanin();
        druga.setIme("Ana");
        druga.setPrezime("Anić");
        druga.setDatumRodjenja(LocalDate.of(1980, 2, 3));
        zupljaninService.dodaj(druga);
        druga.setDatumRodjenja(null);
        zupljaninService.dodaj(druga);
        mvc.perform(get("/krizma/novi").session(prijavi(Uloga.DJELATNIK)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ana Anić — 01.01.1999.")))
                .andExpect(content().string(containsString("Ana Anić — 03.02.1980.")))
                .andExpect(content().string(containsString("Ana Anić — datum rođenja nije unesen")));
    }

    @Test
    void obaObrascaPrikazujuPorukuKadZupljaninVecImaKrizmu() throws Exception {
        MockHttpSession session = prijavi(Uloga.DJELATNIK);
        Krizma zauzeto = zapis();
        Krizma uredjivano = zapis();
        KrizmaForma prije = servis.formaZaUredjivanje(uredjivano.getId());
        long broj = repository.count();

        for (String putanja : new String[]{"/krizma/novi", "/krizma/uredi/" + uredjivano.getId()}) {
            mvc.perform(post(putanja).session(session).with(csrf())
                            .param("zupljaninId", zauzeto.getZupljanin().getId().toString())
                            .param("datumKrizme", "2000-05-12")
                            .param("mjestoKrizme", "Novo mjesto"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrors("krizma", "zupljaninId"))
                    .andExpect(model().attributeExists("zupljani"))
                    .andExpect(content().string(containsString(
                            "Odabrani župljanin već ima evidentiranu krizmu.")))
                    .andExpect(content().string(containsString("Novo mjesto")));
        }
        assertThat(repository.count()).isEqualTo(broj);
        assertThat(servis.formaZaUredjivanje(uredjivano.getId())).usingRecursiveComparison().isEqualTo(prije);
    }

    @Test
    void obrasciNudeSamoDostupneZupljaneIUredjivanjeZadrzavaVlastitog() throws Exception {
        MockHttpSession session = prijavi(Uloga.DJELATNIK);
        Krizma vlastiti = zapis();
        Krizma zauzeti = zapis();
        Zupljanin slobodan = osoba();
        String vlastitaOpcija = "<option value=\"" + vlastiti.getZupljanin().getId() + "\"";
        String zauzetaOpcija = "<option value=\"" + zauzeti.getZupljanin().getId() + "\"";
        String slobodnaOpcija = "<option value=\"" + slobodan.getId() + "\"";

        mvc.perform(get("/krizma/novi").session(session)).andExpect(status().isOk())
                .andExpect(content().string(not(containsString(vlastitaOpcija))))
                .andExpect(content().string(not(containsString(zauzetaOpcija))))
                .andExpect(content().string(containsString(slobodnaOpcija)));
        mvc.perform(get("/krizma/uredi/" + vlastiti.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(vlastitaOpcija + " selected=\"selected\"")))
                .andExpect(content().string(not(containsString(zauzetaOpcija))))
                .andExpect(content().string(containsString(slobodnaOpcija)));
        mvc.perform(post("/krizma/uredi/" + vlastiti.getId()).session(session).with(csrf())
                        .param("zupljaninId", vlastiti.getZupljanin().getId().toString())
                        .param("datumKrizme", "").param("mjestoKrizme", "Mostar"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("krizma", "datumKrizme"))
                .andExpect(content().string(containsString(vlastitaOpcija + " selected=\"selected\"")))
                .andExpect(content().string(not(containsString(zauzetaOpcija))))
                .andExpect(content().string(containsString(slobodnaOpcija)));
    }

    private Zupljanin osoba() {
        Zupljanin osoba = new Zupljanin();
        osoba.setIme("Ana");
        osoba.setPrezime("Anić");
        osoba.setDatumRodjenja(LocalDate.of(1999, 1, 1));
        return zupljaninService.dodaj(osoba);
    }

    private Krizma zapis() {
        KrizmaForma forma = new KrizmaForma();
        forma.setZupljaninId(osoba().getId());
        forma.setDatumKrizme(LocalDate.of(2000, 5, 12));
        forma.setMjestoKrizme("Mostar");
        return servis.dodaj(forma);
    }

    private MockHttpSession prijavi(Uloga uloga) throws Exception {
        KorisnikForma forma = new KorisnikForma();
        forma.setIme("Test");
        forma.setPrezime("Djelatnik");
        forma.setEmail(UUID.randomUUID() + "@primjer.hr");
        forma.setLozinka("TestnaLozinka123!");
        forma.setUloga(uloga);
        korisnikService.dodaj(forma);
        return (MockHttpSession) mvc.perform(formLogin("/login")
                        .user("email", forma.getEmail()).password("lozinka", forma.getLozinka()))
                .andExpect(authenticated()).andReturn().getRequest().getSession(false);
    }
}
