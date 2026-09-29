package ba.sum.fsre.zupniured;

import ba.sum.fsre.zupniured.dto.KorisnikForma;
import ba.sum.fsre.zupniured.dto.VjencanjeForma;
import ba.sum.fsre.zupniured.model.*;
import ba.sum.fsre.zupniured.repository.VjencanjeRepository;
import ba.sum.fsre.zupniured.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
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
class VjencanjeWebTests {
    private final MockMvc mvc;
    private final KorisnikService korisnikService;
    private final ZupljaninService osobe;
    private final VjencanjeService servis;
    private final VjencanjeRepository repository;

    VjencanjeWebTests(MockMvc mvc, KorisnikService korisnikService, ZupljaninService osobe,
                     VjencanjeService servis, VjencanjeRepository repository) {
        this.mvc = mvc; this.korisnikService = korisnikService; this.osobe = osobe;
        this.servis = servis; this.repository = repository;
    }

    @ParameterizedTest
    @EnumSource(Uloga.class)
    void sveUlogeMoguKoristitiCijeliModul(Uloga uloga) throws Exception {
        MockHttpSession session = prijavi(uloga);
        mvc.perform(get("/").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Vjenčanja")));
        mvc.perform(get("/vjencanja").session(session)).andExpect(status().isOk()).andExpect(authenticated());
        mvc.perform(get("/vjencanja/novi").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("Ručni unos")));
        String oznaka = UUID.randomUUID().toString();
        mvc.perform(rucniUnos("/vjencanja/novi", session).param("brojMatice", oznaka)
                        .param("svjedokMladozenje", "Svjedok").param("svjedokNevjeste", "Svjedokinja")
                        .param("sluziteljVjencanja", "Fra Petar").param("godinaMatice", "2020")
                        .param("napomena", "Bilješka").param("id", "0")
                        .param("datumKreiranja", "1900-01-01T00:00:00"))
                .andExpect(status().is3xxRedirection());
        Vjencanje zapis = servis.pronadjiSve(oznaka).getFirst();
        assertThat(zapis.getId()).isPositive();
        assertThat(zapis.getDatumKreiranja().toLocalDate()).isEqualTo(LocalDate.now());
        mvc.perform(get("/vjencanja").session(session).param("pretraga", oznaka))
                .andExpect(status().isOk()).andExpect(model().attribute("pretraga", oznaka))
                .andExpect(content().string(containsString("Marko Marić")))
                .andExpect(content().string(containsString("Iva Perić")))
                .andExpect(content().string(containsString("12.05.2020.")));
        mvc.perform(get("/vjencanja/" + zapis.getId()).session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Fra Petar")))
                .andExpect(content().string(containsString("Svjedokinja")))
                .andExpect(content().string(containsString("nije povezan sa župljaninom")));
        mvc.perform(get("/vjencanja/uredi/" + zapis.getId()).session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"2020-05-12\"")))
                .andExpect(content().string(containsString("value=\"Marko\"")));
        mvc.perform(rucniUnos("/vjencanja/uredi/" + zapis.getId(), session).param("napomena", "Izmjena"))
                .andExpect(redirectedUrl("/vjencanja/" + zapis.getId()));
        assertThat(servis.pronadjiPoId(zapis.getId()).getNapomena()).isEqualTo("Izmjena");
        assertThat(servis.pronadjiPoId(zapis.getId()).getBrojMatice()).isNull();
    }

    @ParameterizedTest
    @CsvSource({"false,false", "true,false", "false,true", "true,true"})
    void obrasciPodrzavajuSveKombinacije(boolean mladozenja, boolean nevjesta) throws Exception {
        MockHttpSession session = prijavi(Uloga.DJELATNIK);
        Zupljanin m = osoba("Ivan"), n = osoba("Ana");
        String oznaka = UUID.randomUUID().toString();
        var zahtjev = post("/vjencanja/novi").session(session).with(csrf())
                .param("datumVjencanja", "2020-05-12").param("mjestoVjencanja", "Mostar")
                .param("brojMatice", oznaka).param("mladozenjaZupljanin.ime", "Podmetnuto");
        if (mladozenja) zahtjev.param("mladozenjaZupljaninId", m.getId().toString());
        else zahtjev.param("imeMladozenje", "Marko").param("prezimeMladozenje", "Marić");
        if (nevjesta) zahtjev.param("nevjestaZupljaninId", n.getId().toString());
        else zahtjev.param("imeNevjeste", "Iva").param("prezimeNevjeste", "Perić");
        mvc.perform(zahtjev).andExpect(status().is3xxRedirection());
        Vjencanje zapis = servis.pronadjiSve(oznaka).getFirst();
        assertThat(zapis.getImeMladozenje()).isEqualTo(mladozenja ? "Ivan" : "Marko");
        assertThat(zapis.getImeNevjeste()).isEqualTo(nevjesta ? "Ana" : "Iva");
        mvc.perform(get("/vjencanja/" + zapis.getId()).session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString(mladozenja ? "Povezan s postojećim župljaninom" : "nije povezan sa župljaninom")))
                .andExpect(content().string(containsString(nevjesta ? "Povezana s postojećom župljankom" : "nije povezana sa župljaninom")));
    }

    @Test
    void obaObrascaPrikazujuObaveznaPoljaIBuduciDatum() throws Exception {
        MockHttpSession session = prijavi(Uloga.ZUPNIK);
        Vjencanje zapis = zapis();
        long broj = repository.count();
        for (String putanja : new String[]{"/vjencanja/novi", "/vjencanja/uredi/" + zapis.getId()}) {
            mvc.perform(post(putanja).session(session).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrors("vjencanje", "imeMladozenje", "prezimeMladozenje",
                            "imeNevjeste", "prezimeNevjeste", "datumVjencanja", "mjestoVjencanja"))
                    .andExpect(content().string(containsString("Ime mladoženje je obavezno.")))
                    .andExpect(content().string(containsString("Prezime nevjeste je obavezno.")))
                    .andExpect(model().attributeExists("zupljani"));
            mvc.perform(post(putanja).session(session).with(csrf())
                            .param("datumVjencanja", LocalDate.now().plusDays(1).toString()))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Datum vjenčanja ne smije biti u budućnosti.")));
        }
        assertThat(repository.count()).isEqualTo(broj);
    }

    @Test
    void istaOsobaOdbijenaNaObaObrasca() throws Exception {
        MockHttpSession session = prijavi(Uloga.ZUPNIK);
        Zupljanin z = osoba("Ivan");
        Vjencanje zapis = zapis();
        for (String putanja : new String[]{"/vjencanja/novi", "/vjencanja/uredi/" + zapis.getId()}) {
            mvc.perform(post(putanja).session(session).with(csrf())
                            .param("mladozenjaZupljaninId", z.getId().toString())
                            .param("nevjestaZupljaninId", z.getId().toString())
                            .param("datumVjencanja", "2020-05-12").param("mjestoVjencanja", "Mostar"))
                    .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("vjencanje", "nevjestaZupljaninId"))
                    .andExpect(content().string(containsString("Mladoženja i nevjesta ne mogu biti isti župljanin.")));
        }
    }

    @Test
    void nepostojeciZupljaniImajuPorukuUzOdgovarajucuOsobu() throws Exception {
        MockHttpSession session = prijavi(Uloga.ADMIN);
        Vjencanje zapis = zapis();
        for (String polje : new String[]{"mladozenjaZupljaninId", "nevjestaZupljaninId"}) {
            for (String putanja : new String[]{"/vjencanja/novi", "/vjencanja/uredi/" + zapis.getId()}) {
                mvc.perform(rucniUnos(putanja, session).param(polje, "0"))
                        .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("vjencanje", polje))
                        .andExpect(content().string(containsString("nije pronađen u evidenciji župljana.")));
            }
        }
    }

    @Test
    void promjeneVezaPutemObrascaRade() throws Exception {
        MockHttpSession session = prijavi(Uloga.DJELATNIK);
        Vjencanje zapis = zapis();
        Zupljanin m = osoba("Ivan"), n = osoba("Ana"), drugi = osoba("Petar");
        String putanja = "/vjencanja/uredi/" + zapis.getId();
        mvc.perform(rucniUnos(putanja, session).param("mladozenjaZupljaninId", m.getId().toString())
                        .param("nevjestaZupljaninId", n.getId().toString())).andExpect(redirectedUrl("/vjencanja/" + zapis.getId()));
        mvc.perform(rucniUnos(putanja, session).param("mladozenjaZupljaninId", drugi.getId().toString())
                        .param("nevjestaZupljaninId", n.getId().toString())).andExpect(status().is3xxRedirection());
        assertThat(servis.pronadjiPoId(zapis.getId()).getImeMladozenje()).isEqualTo("Petar");
        mvc.perform(rucniUnos(putanja, session)).andExpect(status().is3xxRedirection());
        Vjencanje rezultat = servis.pronadjiPoId(zapis.getId());
        assertThat(rezultat.getMladozenjaZupljanin()).isNull();
        assertThat(rezultat.getNevjestaZupljanin()).isNull();
        assertThat(rezultat.getImeMladozenje()).isEqualTo("Marko");
        assertThat(rezultat.getImeNevjeste()).isEqualTo("Iva");
    }

    @Test
    void izborPrikazujeDatumRodjenjaIIstoimeneOsobe() throws Exception {
        Zupljanin prvi = osoba("Ana");
        Zupljanin drugi = new Zupljanin();
        drugi.setIme("Ana"); drugi.setPrezime("Test"); drugi.setDatumRodjenja(LocalDate.of(1985, 2, 3));
        osobe.dodaj(drugi);
        mvc.perform(get("/vjencanja/novi").session(prijavi(Uloga.DJELATNIK)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ana Test — 01.01.1990.")))
                .andExpect(content().string(containsString("Ana Test — 03.02.1985.")));
    }

    @Test
    void anonimniKorisnikNemaPristup() throws Exception {
        for (String putanja : new String[]{"/vjencanja", "/vjencanja/novi", "/vjencanja/uredi/1", "/vjencanja/1"}) {
            mvc.perform(get(putanja)).andExpect(redirectedUrl("/login")).andExpect(unauthenticated());
        }
        for (String putanja : new String[]{"/vjencanja/novi", "/vjencanja/uredi/1"}) {
            mvc.perform(post(putanja).with(csrf())).andExpect(redirectedUrl("/login")).andExpect(unauthenticated());
        }
        mvc.perform(get("/")).andExpect(content().string(not(containsString("href=\"/vjencanja\""))));
    }

    @Test
    void csrfJeObavezanIBrisanjeNijeDostupno() throws Exception {
        MockHttpSession session = prijavi(Uloga.ADMIN);
        Vjencanje zapis = zapis();
        for (String putanja : new String[]{"/vjencanja/novi", "/vjencanja/uredi/" + zapis.getId()}) {
            mvc.perform(post(putanja).session(session)).andExpect(status().isForbidden());
        }
        mvc.perform(delete("/vjencanja/" + zapis.getId()).session(session).with(csrf())).andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/vjencanja/obrisi/" + zapis.getId()).session(session).with(csrf())).andExpect(status().isNotFound());
        assertThat(repository.existsById(zapis.getId())).isTrue();
    }

    @Test
    void neispravniTipoviImajuHrvatskePoruke() throws Exception {
        mvc.perform(post("/vjencanja/novi").session(prijavi(Uloga.DJELATNIK)).with(csrf())
                        .param("mladozenjaZupljaninId", "abc").param("nevjestaZupljaninId", "abc")
                        .param("datumVjencanja", "abc").param("godinaMatice", "abc"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("vjencanje", "mladozenjaZupljaninId", "nevjestaZupljaninId",
                        "datumVjencanja", "godinaMatice"))
                .andExpect(content().string(containsString("Unesite ispravan datum vjenčanja.")))
                .andExpect(content().string(containsString("Unesite ispravnu godinu matice.")));
    }

    @Test
    void nepostojeciZapisVraca404() throws Exception {
        MockHttpSession session = prijavi(Uloga.ADMIN);
        mvc.perform(get("/vjencanja/0").session(session)).andExpect(status().isNotFound());
        mvc.perform(get("/vjencanja/uredi/0").session(session)).andExpect(status().isNotFound());
        mvc.perform(rucniUnos("/vjencanja/uredi/0", session)).andExpect(status().isNotFound());
    }

    private MockHttpServletRequestBuilder rucniUnos(String putanja, MockHttpSession session) {
        return post(putanja).session(session).with(csrf())
                .param("imeMladozenje", "Marko").param("prezimeMladozenje", "Marić")
                .param("imeNevjeste", "Iva").param("prezimeNevjeste", "Perić")
                .param("datumVjencanja", "2020-05-12").param("mjestoVjencanja", "Mostar");
    }
    private Vjencanje zapis() {
        VjencanjeForma f = new VjencanjeForma();
        f.setImeMladozenje("Marko"); f.setPrezimeMladozenje("Marić");
        f.setImeNevjeste("Iva"); f.setPrezimeNevjeste("Perić");
        f.setDatumVjencanja(LocalDate.of(2020, 5, 12)); f.setMjestoVjencanja("Mostar");
        return servis.dodaj(f);
    }
    private Zupljanin osoba(String ime) {
        Zupljanin z = new Zupljanin(); z.setIme(ime); z.setPrezime("Test");
        z.setDatumRodjenja(LocalDate.of(1990, 1, 1)); return osobe.dodaj(z);
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
