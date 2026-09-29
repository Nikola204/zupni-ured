package ba.sum.fsre.zupniured;

import ba.sum.fsre.zupniured.dto.KorisnikForma;
import ba.sum.fsre.zupniured.dto.KrstenjeForma;
import ba.sum.fsre.zupniured.model.Krstenje;
import ba.sum.fsre.zupniured.model.Uloga;
import ba.sum.fsre.zupniured.model.Zupljanin;
import ba.sum.fsre.zupniured.repository.KrstenjeRepository;
import ba.sum.fsre.zupniured.service.KorisnikService;
import ba.sum.fsre.zupniured.service.KrstenjeService;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.pocetni-admin.enabled=false")
@AutoConfigureMockMvc
@Transactional
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class KrstenjeWebTests {

    private final MockMvc mvc;
    private final KorisnikService korisnikService;
    private final KrstenjeService servis;
    private final ZupljaninService zupljaninService;
    private final KrstenjeRepository repository;

    KrstenjeWebTests(MockMvc mvc, KorisnikService korisnikService, KrstenjeService servis,
                    ZupljaninService zupljaninService, KrstenjeRepository repository) {
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
                .andExpect(content().string(containsString("Krštenja")));
        mvc.perform(get("/krstenja").session(session)).andExpect(status().isOk()).andExpect(authenticated());
        mvc.perform(get("/krstenja/novi").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("Ana Anić — 01.01.1999.")));

        String oznaka = UUID.randomUUID().toString();
        mvc.perform(post("/krstenja/novi").session(session).with(csrf())
                        .param("zupljaninId", osoba.getId().toString()).param("datumKrstenja", "2000-05-12")
                        .param("mjestoKrstenja", "Mostar").param("brojMatice", oznaka)
                        .param("godinaMatice", "2000").param("imeOca", "Ivan").param("imeMajke", "Marija")
                        .param("imeKuma", "Marko").param("imeKume", "Iva").param("krstitelj", "Fra Petar")
                        .param("napomena", "Bilješka").param("id", "0")
                        .param("datumKreiranja", "1900-01-01T00:00:00")
                        .param("zupljanin.ime", "Podmetnuto"))
                .andExpect(status().is3xxRedirection());
        Krstenje zapis = servis.pronadjiSve(oznaka).getFirst();
        assertThat(zapis.getId()).isPositive();
        assertThat(zapis.getDatumKreiranja().toLocalDate()).isEqualTo(LocalDate.now());
        assertThat(zapis.getZupljanin().getIme()).isEqualTo("Ana");
        var datumKreiranja = zapis.getDatumKreiranja();

        mvc.perform(get("/krstenja").session(session).param("pretraga", oznaka))
                .andExpect(status().isOk()).andExpect(model().attribute("pretraga", oznaka))
                .andExpect(content().string(containsString("Ana Anić")))
                .andExpect(content().string(containsString("12.05.2000.")))
                .andExpect(content().string(containsString(oznaka)));
        mvc.perform(get("/krstenja/" + zapis.getId()).session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("01.01.1999.")))
                .andExpect(content().string(containsString("12.05.2000.")))
                .andExpect(content().string(containsString("Fra Petar")))
                .andExpect(content().string(containsString("Bilješka")));
        mvc.perform(get("/krstenja/uredi/" + zapis.getId()).session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"2000-05-12\"")))
                .andExpect(content().string(containsString("value=\"" + osoba.getId() + "\" selected=\"selected\"")));
        mvc.perform(post("/krstenja/uredi/" + zapis.getId()).session(session).with(csrf())
                        .param("zupljaninId", osoba.getId().toString()).param("datumKrstenja", "2001-06-13")
                        .param("mjestoKrstenja", "Čitluk").param("brojMatice", "").param("godinaMatice", "")
                        .param("datumKreiranja", "1900-01-01T00:00:00"))
                .andExpect(redirectedUrl("/krstenja/" + zapis.getId()));
        Krstenje uredjen = servis.pronadjiPoId(zapis.getId());
        assertThat(uredjen.getMjestoKrstenja()).isEqualTo("Čitluk");
        assertThat(uredjen.getDatumKrstenja()).isEqualTo(LocalDate.of(2001, 6, 13));
        assertThat(uredjen.getBrojMatice()).isNull();
        assertThat(uredjen.getGodinaMatice()).isNull();
        assertThat(uredjen.getDatumKreiranja()).isEqualTo(datumKreiranja);
    }

    @Test
    void anonimniKorisnikNeMozeCitatiNiMijenjatiKrstenja() throws Exception {
        for (String putanja : new String[]{"/krstenja", "/krstenja/novi", "/krstenja/uredi/1", "/krstenja/1"}) {
            mvc.perform(get(putanja)).andExpect(redirectedUrl("/login")).andExpect(unauthenticated());
        }
        for (String putanja : new String[]{"/krstenja/novi", "/krstenja/uredi/1"}) {
            mvc.perform(post(putanja).with(csrf())).andExpect(redirectedUrl("/login"))
                    .andExpect(unauthenticated());
        }
    }

    @Test
    void obaObrascaPrikazujuGreskeIPonovnoNudeZupljane() throws Exception {
        MockHttpSession session = prijavi(Uloga.DJELATNIK);
        Krstenje zapis = zapis();
        long broj = repository.count();
        for (String putanja : new String[]{"/krstenja/novi", "/krstenja/uredi/" + zapis.getId()}) {
            mvc.perform(post(putanja).session(session).with(csrf())
                            .param("zupljaninId", "").param("datumKrstenja", "")
                            .param("mjestoKrstenja", " "))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrors("krstenje", "zupljaninId", "datumKrstenja", "mjestoKrstenja"))
                    .andExpect(model().attributeExists("zupljani"))
                    .andExpect(content().string(containsString("Župljanin je obavezan.")))
                    .andExpect(content().string(containsString("Datum krštenja je obavezan.")))
                    .andExpect(content().string(containsString("Mjesto krštenja je obavezno.")));
            mvc.perform(post(putanja).session(session).with(csrf())
                            .param("zupljaninId", zapis.getZupljanin().getId().toString())
                            .param("datumKrstenja", LocalDate.now().plusDays(1).toString())
                            .param("mjestoKrstenja", "Mostar"))
                    .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("krstenje", "datumKrstenja"))
                    .andExpect(content().string(containsString("Datum krštenja ne smije biti u budućnosti.")));
        }
        assertThat(repository.count()).isEqualTo(broj);
        assertThat(servis.pronadjiPoId(zapis.getId()).getDatumKrstenja()).isEqualTo(LocalDate.of(2000, 5, 12));
    }

    @Test
    void neispravniTipoviImajuHrvatskePoruke() throws Exception {
        mvc.perform(post("/krstenja/novi").session(prijavi(Uloga.ZUPNIK)).with(csrf())
                        .param("zupljaninId", "abc").param("datumKrstenja", "nije-datum")
                        .param("mjestoKrstenja", "Mostar").param("godinaMatice", "nije-godina"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("krstenje", "zupljaninId", "datumKrstenja", "godinaMatice"))
                .andExpect(content().string(containsString("Odaberite postojećeg župljanina.")))
                .andExpect(content().string(containsString("Unesite ispravan datum krštenja.")))
                .andExpect(content().string(containsString("Unesite ispravnu godinu matice.")));
    }

    @Test
    void nepostojeciZupljaninVracaGreskuNaObrascu() throws Exception {
        MockHttpSession session = prijavi(Uloga.ZUPNIK);
        Krstenje zapis = zapis();
        long broj = repository.count();
        for (String putanja : new String[]{"/krstenja/novi", "/krstenja/uredi/" + zapis.getId()}) {
            mvc.perform(post(putanja).session(session).with(csrf())
                            .param("zupljaninId", "0").param("datumKrstenja", "2000-05-12")
                            .param("mjestoKrstenja", "Mostar"))
                    .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("krstenje", "zupljaninId"))
                    .andExpect(content().string(containsString("Odabrani župljanin ne postoji.")))
                    .andExpect(model().attributeExists("zupljani"));
        }
        assertThat(repository.count()).isEqualTo(broj);
    }

    @Test
    void obaPostZahtjevaTrebajuCsrfToken() throws Exception {
        MockHttpSession session = prijavi(Uloga.ADMIN);
        Krstenje zapis = zapis();
        for (String putanja : new String[]{"/krstenja/novi", "/krstenja/uredi/" + zapis.getId()}) {
            mvc.perform(post(putanja).session(session)).andExpect(status().isForbidden());
        }
    }

    @Test
    void nepostojeciDetaljiIUredjivanjeVracaju404() throws Exception {
        MockHttpSession session = prijavi(Uloga.DJELATNIK);
        mvc.perform(get("/krstenja/0").session(session)).andExpect(status().isNotFound());
        mvc.perform(get("/krstenja/uredi/0").session(session)).andExpect(status().isNotFound());
        mvc.perform(post("/krstenja/uredi/0").session(session).with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    void brisanjeNijeDostupno() throws Exception {
        MockHttpSession session = prijavi(Uloga.ADMIN);
        Krstenje zapis = zapis();
        mvc.perform(delete("/krstenja/" + zapis.getId()).session(session).with(csrf()))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/krstenja/obrisi/" + zapis.getId()).session(session).with(csrf()))
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
        mvc.perform(get("/krstenja/novi").session(prijavi(Uloga.DJELATNIK)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ana Anić — 01.01.1999.")))
                .andExpect(content().string(containsString("Ana Anić — 03.02.1980.")))
                .andExpect(content().string(containsString("Ana Anić — datum rođenja nije unesen")));
    }

    @Test
    void obaObrascaPrikazujuPorukuKadZupljaninVecImaKrstenje() throws Exception {
        MockHttpSession session = prijavi(Uloga.DJELATNIK);
        Krstenje zauzeto = zapis();
        Krstenje uredjivano = zapis();
        KrstenjeForma prije = servis.formaZaUredjivanje(uredjivano.getId());
        long broj = repository.count();

        for (String putanja : new String[]{"/krstenja/novi", "/krstenja/uredi/" + uredjivano.getId()}) {
            mvc.perform(post(putanja).session(session).with(csrf())
                            .param("zupljaninId", zauzeto.getZupljanin().getId().toString())
                            .param("datumKrstenja", "2000-05-12")
                            .param("mjestoKrstenja", "Novo mjesto"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrors("krstenje", "zupljaninId"))
                    .andExpect(model().attributeExists("zupljani"))
                    .andExpect(content().string(containsString(
                            "Odabrani župljanin već ima evidentirano krštenje.")))
                    .andExpect(content().string(containsString("Novo mjesto")));
        }
        assertThat(repository.count()).isEqualTo(broj);
        assertThat(servis.formaZaUredjivanje(uredjivano.getId())).usingRecursiveComparison().isEqualTo(prije);
    }

    private Zupljanin osoba() {
        Zupljanin osoba = new Zupljanin();
        osoba.setIme("Ana");
        osoba.setPrezime("Anić");
        osoba.setDatumRodjenja(LocalDate.of(1999, 1, 1));
        return zupljaninService.dodaj(osoba);
    }

    private Krstenje zapis() {
        KrstenjeForma forma = new KrstenjeForma();
        forma.setZupljaninId(osoba().getId());
        forma.setDatumKrstenja(LocalDate.of(2000, 5, 12));
        forma.setMjestoKrstenja("Mostar");
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
