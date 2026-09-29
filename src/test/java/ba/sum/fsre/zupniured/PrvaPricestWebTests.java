package ba.sum.fsre.zupniured;

import ba.sum.fsre.zupniured.dto.KorisnikForma;
import ba.sum.fsre.zupniured.dto.PrvaPricestForma;
import ba.sum.fsre.zupniured.model.PrvaPricest;
import ba.sum.fsre.zupniured.model.Uloga;
import ba.sum.fsre.zupniured.model.Zupljanin;
import ba.sum.fsre.zupniured.repository.PrvaPricestRepository;
import ba.sum.fsre.zupniured.service.KorisnikService;
import ba.sum.fsre.zupniured.service.PrvaPricestService;
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
class PrvaPricestWebTests {

    private final MockMvc mvc;
    private final KorisnikService korisnikService;
    private final PrvaPricestService servis;
    private final ZupljaninService zupljaninService;
    private final PrvaPricestRepository repository;

    PrvaPricestWebTests(MockMvc mvc, KorisnikService korisnikService, PrvaPricestService servis,
                    ZupljaninService zupljaninService, PrvaPricestRepository repository) {
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
                .andExpect(content().string(containsString("Prve pričesti")));
        mvc.perform(get("/prva-pricest").session(session)).andExpect(status().isOk()).andExpect(authenticated());
        mvc.perform(get("/prva-pricest/novi").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("Ana Anić — 01.01.1999.")));

        String oznaka = UUID.randomUUID().toString();
        mvc.perform(post("/prva-pricest/novi").session(session).with(csrf())
                        .param("zupljaninId", osoba.getId().toString()).param("datumPricesti", "2000-05-12")
                        .param("mjestoPricesti", "Mostar").param("brojMatice", oznaka)
                        .param("godinaMatice", "2000")
                        .param("napomena", "Bilješka").param("id", "0")
                        .param("datumKreiranja", "1900-01-01T00:00:00")
                        .param("zupljanin.ime", "Podmetnuto"))
                .andExpect(status().is3xxRedirection());
        PrvaPricest zapis = servis.pronadjiSve(oznaka).getFirst();
        assertThat(zapis.getId()).isPositive();
        assertThat(zapis.getDatumKreiranja().toLocalDate()).isEqualTo(LocalDate.now());
        assertThat(zapis.getZupljanin().getIme()).isEqualTo("Ana");
        var datumKreiranja = zapis.getDatumKreiranja();

        mvc.perform(get("/prva-pricest").session(session).param("pretraga", oznaka))
                .andExpect(status().isOk()).andExpect(model().attribute("pretraga", oznaka))
                .andExpect(content().string(containsString("Ana Anić")))
                .andExpect(content().string(containsString("12.05.2000.")))
                .andExpect(content().string(containsString(oznaka)));
        mvc.perform(get("/prva-pricest/" + zapis.getId()).session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("01.01.1999.")))
                .andExpect(content().string(containsString("12.05.2000.")))
                .andExpect(content().string(containsString("Bilješka")));
        mvc.perform(get("/prva-pricest/uredi/" + zapis.getId()).session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"2000-05-12\"")))
                .andExpect(content().string(containsString("value=\"" + osoba.getId() + "\" selected=\"selected\"")));
        mvc.perform(post("/prva-pricest/uredi/" + zapis.getId()).session(session).with(csrf())
                        .param("zupljaninId", osoba.getId().toString()).param("datumPricesti", "2001-06-13")
                        .param("mjestoPricesti", "Čitluk").param("brojMatice", "").param("godinaMatice", "")
                        .param("datumKreiranja", "1900-01-01T00:00:00"))
                .andExpect(redirectedUrl("/prva-pricest/" + zapis.getId()));
        PrvaPricest uredjen = servis.pronadjiPoId(zapis.getId());
        assertThat(uredjen.getMjestoPricesti()).isEqualTo("Čitluk");
        assertThat(uredjen.getDatumPricesti()).isEqualTo(LocalDate.of(2001, 6, 13));
        assertThat(uredjen.getBrojMatice()).isNull();
        assertThat(uredjen.getGodinaMatice()).isNull();
        assertThat(uredjen.getDatumKreiranja()).isEqualTo(datumKreiranja);
    }

    @Test
    void anonimniKorisnikNeMozeCitatiNiMijenjatiPricesti() throws Exception {
        for (String putanja : new String[]{"/prva-pricest", "/prva-pricest/novi", "/prva-pricest/uredi/1", "/prva-pricest/1"}) {
            mvc.perform(get(putanja)).andExpect(redirectedUrl("/login")).andExpect(unauthenticated());
        }
        for (String putanja : new String[]{"/prva-pricest/novi", "/prva-pricest/uredi/1"}) {
            mvc.perform(post(putanja).with(csrf())).andExpect(redirectedUrl("/login"))
                    .andExpect(unauthenticated());
        }
    }

    @Test
    void obaObrascaPrikazujuGreskeIPonovnoNudeZupljane() throws Exception {
        MockHttpSession session = prijavi(Uloga.DJELATNIK);
        PrvaPricest zapis = zapis();
        long broj = repository.count();
        for (String putanja : new String[]{"/prva-pricest/novi", "/prva-pricest/uredi/" + zapis.getId()}) {
            mvc.perform(post(putanja).session(session).with(csrf())
                            .param("zupljaninId", "").param("datumPricesti", "")
                            .param("mjestoPricesti", " "))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrors("prvaPricest", "zupljaninId", "datumPricesti", "mjestoPricesti"))
                    .andExpect(model().attributeExists("zupljani"))
                    .andExpect(content().string(containsString("Župljanin je obavezan.")))
                    .andExpect(content().string(containsString("Datum pričesti je obavezan.")))
                    .andExpect(content().string(containsString("Mjesto pričesti je obavezno.")));
            mvc.perform(post(putanja).session(session).with(csrf())
                            .param("zupljaninId", zapis.getZupljanin().getId().toString())
                            .param("datumPricesti", LocalDate.now().plusDays(1).toString())
                            .param("mjestoPricesti", "Mostar"))
                    .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("prvaPricest", "datumPricesti"))
                    .andExpect(content().string(containsString("Datum pričesti ne smije biti u budućnosti.")));
        }
        assertThat(repository.count()).isEqualTo(broj);
        assertThat(servis.pronadjiPoId(zapis.getId()).getDatumPricesti()).isEqualTo(LocalDate.of(2000, 5, 12));
    }

    @Test
    void neispravniTipoviImajuHrvatskePoruke() throws Exception {
        mvc.perform(post("/prva-pricest/novi").session(prijavi(Uloga.ZUPNIK)).with(csrf())
                        .param("zupljaninId", "abc").param("datumPricesti", "nije-datum")
                        .param("mjestoPricesti", "Mostar").param("godinaMatice", "nije-godina"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("prvaPricest", "zupljaninId", "datumPricesti", "godinaMatice"))
                .andExpect(content().string(containsString("Odaberite postojećeg župljanina.")))
                .andExpect(content().string(containsString("Unesite ispravan datum pričesti.")))
                .andExpect(content().string(containsString("Unesite ispravnu godinu matice.")));
    }

    @Test
    void nepostojeciZupljaninVracaGreskuNaObrascu() throws Exception {
        MockHttpSession session = prijavi(Uloga.ZUPNIK);
        PrvaPricest zapis = zapis();
        long broj = repository.count();
        for (String putanja : new String[]{"/prva-pricest/novi", "/prva-pricest/uredi/" + zapis.getId()}) {
            mvc.perform(post(putanja).session(session).with(csrf())
                            .param("zupljaninId", "0").param("datumPricesti", "2000-05-12")
                            .param("mjestoPricesti", "Mostar"))
                    .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("prvaPricest", "zupljaninId"))
                    .andExpect(content().string(containsString("Odabrani župljanin ne postoji.")))
                    .andExpect(model().attributeExists("zupljani"));
        }
        assertThat(repository.count()).isEqualTo(broj);
    }

    @Test
    void obaPostZahtjevaTrebajuCsrfToken() throws Exception {
        MockHttpSession session = prijavi(Uloga.ADMIN);
        PrvaPricest zapis = zapis();
        for (String putanja : new String[]{"/prva-pricest/novi", "/prva-pricest/uredi/" + zapis.getId()}) {
            mvc.perform(post(putanja).session(session)).andExpect(status().isForbidden());
        }
    }

    @Test
    void nepostojeciDetaljiIUredjivanjeVracaju404() throws Exception {
        MockHttpSession session = prijavi(Uloga.DJELATNIK);
        mvc.perform(get("/prva-pricest/0").session(session)).andExpect(status().isNotFound());
        mvc.perform(get("/prva-pricest/uredi/0").session(session)).andExpect(status().isNotFound());
        mvc.perform(post("/prva-pricest/uredi/0").session(session).with(csrf())).andExpect(status().isNotFound());
    }

    @Test
    void brisanjeNijeDostupno() throws Exception {
        MockHttpSession session = prijavi(Uloga.ADMIN);
        PrvaPricest zapis = zapis();
        mvc.perform(delete("/prva-pricest/" + zapis.getId()).session(session).with(csrf()))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/prva-pricest/obrisi/" + zapis.getId()).session(session).with(csrf()))
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
        mvc.perform(get("/prva-pricest/novi").session(prijavi(Uloga.DJELATNIK)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ana Anić — 01.01.1999.")))
                .andExpect(content().string(containsString("Ana Anić — 03.02.1980.")))
                .andExpect(content().string(containsString("Ana Anić — datum rođenja nije unesen")));
    }

    @Test
    void obaObrascaPrikazujuPorukuKadZupljaninVecImaPrvaPricest() throws Exception {
        MockHttpSession session = prijavi(Uloga.DJELATNIK);
        PrvaPricest zauzeto = zapis();
        PrvaPricest uredjivano = zapis();
        PrvaPricestForma prije = servis.formaZaUredjivanje(uredjivano.getId());
        long broj = repository.count();

        for (String putanja : new String[]{"/prva-pricest/novi", "/prva-pricest/uredi/" + uredjivano.getId()}) {
            mvc.perform(post(putanja).session(session).with(csrf())
                            .param("zupljaninId", zauzeto.getZupljanin().getId().toString())
                            .param("datumPricesti", "2000-05-12")
                            .param("mjestoPricesti", "Novo mjesto"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrors("prvaPricest", "zupljaninId"))
                    .andExpect(model().attributeExists("zupljani"))
                    .andExpect(content().string(containsString(
                            "Odabrani župljanin već ima evidentiranu prvu pričest.")))
                    .andExpect(content().string(containsString("Novo mjesto")));
        }
        assertThat(repository.count()).isEqualTo(broj);
        assertThat(servis.formaZaUredjivanje(uredjivano.getId())).usingRecursiveComparison().isEqualTo(prije);
    }

    @Test
    void obrasciNudeSamoDostupneZupljaneIUredjivanjeZadrzavaVlastitog() throws Exception {
        MockHttpSession session = prijavi(Uloga.DJELATNIK);
        PrvaPricest vlastiti = zapis();
        PrvaPricest zauzeti = zapis();
        Zupljanin slobodan = osoba();
        String vlastitaOpcija = "<option value=\"" + vlastiti.getZupljanin().getId() + "\"";
        String zauzetaOpcija = "<option value=\"" + zauzeti.getZupljanin().getId() + "\"";
        String slobodnaOpcija = "<option value=\"" + slobodan.getId() + "\"";

        mvc.perform(get("/prva-pricest/novi").session(session)).andExpect(status().isOk())
                .andExpect(content().string(not(containsString(vlastitaOpcija))))
                .andExpect(content().string(not(containsString(zauzetaOpcija))))
                .andExpect(content().string(containsString(slobodnaOpcija)));
        mvc.perform(get("/prva-pricest/uredi/" + vlastiti.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(vlastitaOpcija + " selected=\"selected\"")))
                .andExpect(content().string(not(containsString(zauzetaOpcija))))
                .andExpect(content().string(containsString(slobodnaOpcija)));
        mvc.perform(post("/prva-pricest/uredi/" + vlastiti.getId()).session(session).with(csrf())
                        .param("zupljaninId", vlastiti.getZupljanin().getId().toString())
                        .param("datumPricesti", "").param("mjestoPricesti", "Mostar"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("prvaPricest", "datumPricesti"))
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

    private PrvaPricest zapis() {
        PrvaPricestForma forma = new PrvaPricestForma();
        forma.setZupljaninId(osoba().getId());
        forma.setDatumPricesti(LocalDate.of(2000, 5, 12));
        forma.setMjestoPricesti("Mostar");
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
