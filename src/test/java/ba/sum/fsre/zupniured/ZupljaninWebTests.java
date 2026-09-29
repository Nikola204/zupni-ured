package ba.sum.fsre.zupniured;

import ba.sum.fsre.zupniured.dto.KorisnikForma;
import ba.sum.fsre.zupniured.model.Uloga;
import ba.sum.fsre.zupniured.model.Zupljanin;
import ba.sum.fsre.zupniured.repository.ZupljaninRepository;
import ba.sum.fsre.zupniured.service.KorisnikService;
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
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.pocetni-admin.enabled=false")
@AutoConfigureMockMvc
@Transactional
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ZupljaninWebTests {

    private final MockMvc mvc;
    private final KorisnikService korisnikService;
    private final ZupljaninService servis;
    private final ZupljaninRepository repository;

    ZupljaninWebTests(MockMvc mvc, KorisnikService korisnikService,
                     ZupljaninService servis, ZupljaninRepository repository) {
        this.mvc = mvc;
        this.korisnikService = korisnikService;
        this.servis = servis;
        this.repository = repository;
    }

    @ParameterizedTest
    @EnumSource(Uloga.class)
    void sveTriUlogeMoguKoristitiCijeliModul(Uloga uloga) throws Exception {
        MockHttpSession session = prijavi(uloga);
        mvc.perform(get("/").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Župljani")));
        mvc.perform(get("/zupljani").session(session)).andExpect(status().isOk())
                .andExpect(authenticated());
        mvc.perform(get("/zupljani/novi").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")));

        String oznaka = "Test" + UUID.randomUUID();
        mvc.perform(post("/zupljani/novi").session(session).with(csrf())
                        .param("ime", oznaka).param("prezime", "Anić")
                        .param("datumRodjenja", "1990-05-12").param("adresa", "Ulica 1")
                        .param("telefon", "123456").param("email", "ana@primjer.hr")
                        .param("id", "0").param("aktivan", "false").param("datumKreiranja", "2000-01-01T00:00:00"))
                .andExpect(redirectedUrl("/zupljani"));
        Zupljanin spremljen = servis.pronadjiSve(oznaka).getFirst();
        assertThat(spremljen.isAktivan()).isTrue();

        mvc.perform(get("/zupljani").param("pretraga", oznaka.toUpperCase(Locale.ROOT)).session(session))
                .andExpect(status().isOk()).andExpect(model().attribute("pretraga", oznaka.toUpperCase(Locale.ROOT)))
                .andExpect(content().string(containsString(oznaka + " Anić")))
                .andExpect(content().string(containsString("12.05.1990.")))
                .andExpect(content().string(containsString("Ulica 1")))
                .andExpect(content().string(containsString("123456")))
                .andExpect(content().string(containsString("ana@primjer.hr")))
                .andExpect(content().string(containsString("Aktivan")));

        mvc.perform(get("/zupljani/uredi/" + spremljen.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"1990-05-12\"")));
        mvc.perform(post("/zupljani/uredi/" + spremljen.getId()).session(session).with(csrf())
                        .param("ime", oznaka).param("prezime", "Novo prezime")
                        .param("email", "").param("datumRodjenja", ""))
                .andExpect(redirectedUrl("/zupljani"));
        assertThat(servis.pronadjiPoId(spremljen.getId()).getPrezime()).isEqualTo("Novo prezime");
        assertThat(servis.pronadjiPoId(spremljen.getId()).getEmail()).isNull();
        assertThat(servis.pronadjiPoId(spremljen.getId()).getDatumRodjenja()).isNull();

        mvc.perform(post("/zupljani/deaktiviraj/" + spremljen.getId()).session(session).with(csrf()))
                .andExpect(redirectedUrl("/zupljani"));
        mvc.perform(get("/zupljani").param("pretraga", oznaka).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Neaktivan")))
                .andExpect(content().string(containsString("Reaktiviraj")));
        mvc.perform(post("/zupljani/reaktiviraj/" + spremljen.getId()).session(session).with(csrf()))
                .andExpect(redirectedUrl("/zupljani"));
        assertThat(servis.pronadjiPoId(spremljen.getId()).isAktivan()).isTrue();
        mvc.perform(get("/zupljani").session(session)).andExpect(authenticated());
    }

    @Test
    void anonimniKorisnikNeMozeCitatiNiMijenjatiZupljane() throws Exception {
        for (String putanja : new String[]{"/zupljani", "/zupljani/novi", "/zupljani/uredi/1"}) {
            mvc.perform(get(putanja)).andExpect(redirectedUrl("/login")).andExpect(unauthenticated());
        }
        for (String putanja : new String[]{"/zupljani/novi", "/zupljani/uredi/1",
                "/zupljani/deaktiviraj/1", "/zupljani/reaktiviraj/1"}) {
            mvc.perform(post(putanja).with(csrf()))
                    .andExpect(redirectedUrl("/login")).andExpect(unauthenticated());
        }
    }

    @Test
    void obaObrascaPrikazujuValidacijskePoruke() throws Exception {
        MockHttpSession session = prijavi(Uloga.DJELATNIK);
        Zupljanin podaci = new Zupljanin();
        podaci.setIme("Ana");
        podaci.setPrezime("Anić");
        Zupljanin spremljen = servis.dodaj(podaci);
        long brojZapisa = repository.count();
        for (String putanja : new String[]{"/zupljani/novi", "/zupljani/uredi/" + spremljen.getId()}) {
            mvc.perform(post(putanja).session(session).with(csrf())
                            .param("ime", "").param("prezime", "")
                            .param("datumRodjenja", LocalDate.now().plusDays(1).toString())
                            .param("email", "neispravno"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrors("zupljanin", "ime", "prezime", "datumRodjenja", "email"))
                    .andExpect(content().string(containsString("Ime je obavezno.")))
                    .andExpect(content().string(containsString("Prezime je obavezno.")))
                    .andExpect(content().string(containsString("Datum rođenja ne smije biti u budućnosti.")))
                    .andExpect(content().string(containsString("Unesite ispravnu email adresu.")));
        }
        assertThat(repository.count()).isEqualTo(brojZapisa);
        assertThat(servis.pronadjiPoId(spremljen.getId()).getIme()).isEqualTo("Ana");
    }

    @Test
    void neispravnoZapisanDatumPrikazujeHrvatskuPoruku() throws Exception {
        mvc.perform(post("/zupljani/novi").session(prijavi(Uloga.DJELATNIK)).with(csrf())
                        .param("ime", "Ana").param("prezime", "Anić").param("datumRodjenja", "nije-datum"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("zupljanin", "datumRodjenja"))
                .andExpect(content().string(containsString("Unesite ispravan datum rođenja.")));
    }

    @Test
    void promjenaBezCsrfTokenaJeZabranjena() throws Exception {
        mvc.perform(post("/zupljani/novi").session(prijavi(Uloga.ADMIN))
                        .param("ime", "Ana").param("prezime", "Anić"))
                .andExpect(status().isForbidden());
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
