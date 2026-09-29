package ba.sum.fsre.zupniured;

import ba.sum.fsre.zupniured.dto.KorisnikForma;
import ba.sum.fsre.zupniured.model.*;
import ba.sum.fsre.zupniured.repository.MisnaNakanaRepository;
import ba.sum.fsre.zupniured.service.*;
import jakarta.persistence.EntityManager;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
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
class MisnaNakanaTests {
    private final MockMvc mvc;
    private final MisnaNakanaService service;
    private final MisnaNakanaRepository repository;
    private final KorisnikService korisnici;
    private final EntityManager em;
    MisnaNakanaTests(MockMvc mvc, MisnaNakanaService service, MisnaNakanaRepository repository,
                    KorisnikService korisnici, EntityManager em) {
        this.mvc = mvc; this.service = service; this.repository = repository;
        this.korisnici = korisnici; this.em = em;
    }

    @Test
    void stvaranjeProslostZadaniStatusITocanPrilog() {
        MisnaNakana podaci = podaci();
        podaci.setPrilog(new BigDecimal("123456789012.34"));
        podaci.setNaruciteljPrezime("Anić"); podaci.setNaruciteljTelefon("123");
        podaci.setNaruciteljEmail("ana@primjer.hr"); podaci.setSvecenik("Fra Ivan");
        podaci.setNapomena("Bilješka");
        MisnaNakana zapis = service.dodaj(podaci);
        em.clear();
        MisnaNakana ucitan = service.pronadjiPoId(zapis.getId());
        assertThat(ucitan.getStatus()).isEqualTo(StatusMisneNakane.ZAPRIMLJENA);
        assertThat(ucitan.getDatum()).isEqualTo(LocalDate.of(2020, 5, 12));
        assertThat(ucitan.getDatumKreiranja()).isNotNull();
        assertThat(ucitan.getPrilog()).isEqualByComparingTo("123456789012.34");
        assertThat(ucitan.getNaruciteljPrezime()).isEqualTo("Anić");
        assertThat(ucitan.getNaruciteljTelefon()).isEqualTo("123");
        assertThat(ucitan.getNaruciteljEmail()).isEqualTo("ana@primjer.hr");
        assertThat(ucitan.getSvecenik()).isEqualTo("Fra Ivan");
        assertThat(ucitan.getNapomena()).isEqualTo("Bilješka");
    }

    @Test
    void uredjivanjeCuvaIdentitetIDatumKreiranja() {
        MisnaNakana zapis = service.dodaj(podaci());
        em.clear();
        var kreiran = service.pronadjiPoId(zapis.getId()).getDatumKreiranja();
        MisnaNakana izmjena = podaci();
        izmjena.setNakana("Nova nakana"); izmjena.setDatum(LocalDate.now().plusDays(2));
        izmjena.setVrijeme(LocalTime.of(19, 30)); izmjena.setMjesto("Kapela");
        izmjena.setStatus(StatusMisneNakane.SLUZENA);
        service.uredi(zapis.getId(), izmjena);
        em.clear();
        var ucitan = service.pronadjiPoId(zapis.getId());
        assertThat(ucitan.getNakana()).isEqualTo("Nova nakana");
        assertThat(ucitan.getDatumKreiranja()).isEqualTo(kreiran);
        assertThat(ucitan.getVrijeme()).isEqualTo(LocalTime.of(19, 30));
        assertThat(ucitan.getMjesto()).isEqualTo("Kapela");
        assertThat(ucitan.getStatus()).isEqualTo(StatusMisneNakane.SLUZENA);
        assertThat(ucitan.getPrilog()).isNull();
    }

    @ParameterizedTest
    @EnumSource(StatusMisneNakane.class)
    void statusSeMozePromijeniti(StatusMisneNakane status) {
        MisnaNakana zapis = service.dodaj(podaci());
        service.promijeniStatus(zapis.getId(), status);
        em.clear();
        assertThat(service.pronadjiPoId(zapis.getId()).getStatus()).isEqualTo(status);
    }

    @ParameterizedTest
    @ValueSource(strings = {"nakana", "datum", "vrijeme", "mjesto", "naruciteljIme",
            "status", "email", "negativan", "decimale", "predugaNakana"})
    void servisOdbijaNevaljanePodatkeKodDodavanjaIUredjivanja(String slucaj) {
        MisnaNakana zapis = service.dodaj(podaci());
        MisnaNakana los = podaci();
        switch (slucaj) {
            case "nakana" -> los.setNakana(" ");
            case "datum" -> los.setDatum(null);
            case "vrijeme" -> los.setVrijeme(null);
            case "mjesto" -> los.setMjesto("");
            case "naruciteljIme" -> los.setNaruciteljIme("");
            case "status" -> los.setStatus(null);
            case "email" -> los.setNaruciteljEmail("neispravno");
            case "negativan" -> los.setPrilog(new BigDecimal("-0.01"));
            case "decimale" -> los.setPrilog(new BigDecimal("1.001"));
            case "predugaNakana" -> los.setNakana("a".repeat(2001));
        }
        assertThatThrownBy(() -> service.dodaj(los)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> service.uredi(zapis.getId(), los)).isInstanceOf(ConstraintViolationException.class);
        assertThat(zapis.getNakana()).isEqualTo("Za obitelj");
    }

    @ParameterizedTest
    @ValueSource(strings = {"nakana", "ime", "prezime", "svecenik"})
    void pretragaNeRazlikujeVelicinuSlova(String polje) {
        String oznaka = "Test" + UUID.randomUUID();
        MisnaNakana p = podaci();
        switch (polje) {
            case "nakana" -> p.setNakana(oznaka);
            case "ime" -> p.setNaruciteljIme(oznaka);
            case "prezime" -> p.setNaruciteljPrezime(oznaka);
            case "svecenik" -> p.setSvecenik(oznaka);
        }
        var zapis = service.dodaj(p);
        service.dodaj(podaci());
        assertThat(service.pronadjiSve("  " + oznaka.toUpperCase(Locale.ROOT) + "  ", null, null, null))
                .extracting(MisnaNakana::getId).containsExactly(zapis.getId());
    }

    @Test
    void kombiniraniFiltriISortiranjeUkljucujuProslost() {
        String oznaka = UUID.randomUUID().toString();
        List<Long> ids = new ArrayList<>();
        for (int dan : new int[]{14, 12, 13}) {
            for (int sat : new int[]{19, 8}) {
                MisnaNakana p = podaci(); p.setNakana(oznaka);
                p.setDatum(LocalDate.of(2020, 5, dan)); p.setVrijeme(LocalTime.of(sat, 0));
                p.setStatus(sat == 8 ? StatusMisneNakane.POTVRDENA : StatusMisneNakane.OTKAZANA);
                ids.add(service.dodaj(p).getId());
            }
        }
        assertThat(service.pronadjiSve(oznaka, null, null, null)).extracting(MisnaNakana::getId)
                .containsExactly(ids.get(3), ids.get(2), ids.get(5), ids.get(4), ids.get(1), ids.get(0));
        assertThat(service.pronadjiSve(oznaka, StatusMisneNakane.POTVRDENA,
                LocalDate.of(2020, 5, 12), LocalDate.of(2020, 5, 13)))
                .extracting(MisnaNakana::getId).containsExactly(ids.get(3), ids.get(5));
        assertThat(service.pronadjiSve(oznaka, null, LocalDate.of(2020, 5, 14), null)).hasSize(2);
        assertThat(service.pronadjiSve(oznaka, null, null, LocalDate.of(2020, 5, 12))).hasSize(2);
    }

    @ParameterizedTest
    @EnumSource(Uloga.class)
    void sveTriUlogeMoguKoristitiModul(Uloga uloga) throws Exception {
        MockHttpSession session = prijavi(uloga);
        mvc.perform(get("/").session(session)).andExpect(content().string(containsString("Misne nakane")));
        mvc.perform(get("/misne-nakane/novi").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Zaprimljena")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
        String oznaka = UUID.randomUUID().toString();
        mvc.perform(post("/misne-nakane/novi").session(session).with(csrf())
                .param("nakana", oznaka).param("datum", "2020-05-12").param("vrijeme", "08:30")
                .param("mjesto", "Mostar").param("naruciteljIme", "Ana").param("prilog", "12.30")
                .param("id", "0").param("datumKreiranja", "1900-01-01T00:00:00"))
                .andExpect(status().is3xxRedirection());
        var zapis = service.pronadjiSve(oznaka, null, null, null).getFirst();
        assertThat(zapis.getId()).isPositive();
        assertThat(zapis.getDatumKreiranja().toLocalDate()).isEqualTo(LocalDate.now());
        String url = "/misne-nakane/" + zapis.getId();
        mvc.perform(get(url).session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("12.05.2020.")))
                .andExpect(content().string(containsString("08:30")))
                .andExpect(content().string(containsString("12,30")))
                .andExpect(content().string(not(containsString(">null<"))));
        mvc.perform(get("/misne-nakane/uredi/" + zapis.getId()).session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"2020-05-12\"")));
        mvc.perform(post("/misne-nakane/uredi/" + zapis.getId()).session(session).with(csrf())
                .param("nakana", oznaka).param("datum", "2021-05-12").param("vrijeme", "09:00")
                .param("mjesto", "Kapela").param("naruciteljIme", "Ivan").param("status", "POTVRDENA"))
                .andExpect(redirectedUrl(url));
        mvc.perform(post(url + "/status").session(session).with(csrf()).param("status", "SLUZENA"))
                .andExpect(redirectedUrl(url));
        assertThat(service.pronadjiPoId(zapis.getId()).getStatus()).isEqualTo(StatusMisneNakane.SLUZENA);
        mvc.perform(get("/misne-nakane").session(session).param("pretraga", oznaka).param("status", "SLUZENA")
                .param("od", "2021-05-12").param("doDatuma", "2021-05-12"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Kapela")))
                .andExpect(content().string(containsString("Služena")));
    }

    @Test
    void anonimniKorisnikNemaPristup() throws Exception {
        for (String url : List.of("/misne-nakane", "/misne-nakane/novi", "/misne-nakane/uredi/1", "/misne-nakane/1"))
            mvc.perform(get(url)).andExpect(redirectedUrl("/login")).andExpect(unauthenticated());
        for (String url : List.of("/misne-nakane/novi", "/misne-nakane/uredi/1", "/misne-nakane/1/status"))
            mvc.perform(post(url).with(csrf())).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/")).andExpect(content().string(not(containsString("href=\"/misne-nakane\""))));
    }

    @Test
    void csrfJeObavezanIBrisanjeNijeDostupno() throws Exception {
        var session = prijavi(Uloga.ADMIN);
        var zapis = service.dodaj(podaci());
        for (String url : List.of("/misne-nakane/novi", "/misne-nakane/uredi/" + zapis.getId(),
                "/misne-nakane/" + zapis.getId() + "/status"))
            mvc.perform(post(url).session(session)).andExpect(status().isForbidden());
        mvc.perform(delete("/misne-nakane/" + zapis.getId()).session(session).with(csrf()))
                .andExpect(status().isMethodNotAllowed());
        assertThat(repository.existsById(zapis.getId())).isTrue();
    }

    @Test
    void obrasciPrikazujuValidacijskePoruke() throws Exception {
        var session = prijavi(Uloga.DJELATNIK);
        var zapis = service.dodaj(podaci());
        for (String url : List.of("/misne-nakane/novi", "/misne-nakane/uredi/" + zapis.getId())) {
            mvc.perform(post(url).session(session).with(csrf()).param("status", "")
                    .param("naruciteljEmail", "krivo").param("prilog", "-1"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrors("misnaNakana", "nakana", "datum", "vrijeme",
                            "mjesto", "naruciteljIme", "status", "naruciteljEmail", "prilog"))
                    .andExpect(content().string(containsString("Prilog ne smije biti negativan.")))
                    .andExpect(content().string(containsString("Unesite ispravnu email adresu.")));
            mvc.perform(post(url).session(session).with(csrf()).param("datum", "abc")
                    .param("vrijeme", "abc").param("prilog", "abc").param("status", "NEPOSTOJECI"))
                    .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("misnaNakana",
                            "datum", "vrijeme", "prilog", "status"))
                    .andExpect(content().string(containsString("Odaberite ispravan status misne nakane.")));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "NEPOSTOJECI"})
    void neispravanStatusNeMijenjaZapis(String status) throws Exception {
        var zapis = service.dodaj(podaci());
        mvc.perform(post("/misne-nakane/" + zapis.getId() + "/status").session(prijavi(Uloga.ZUPNIK))
                .with(csrf()).param("status", status))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("promjenaStatusa", "status"));
        assertThat(zapis.getStatus()).isEqualTo(StatusMisneNakane.ZAPRIMLJENA);
    }

    @Test
    void neispravniFiltriImajuHrvatskePoruke() throws Exception {
        var session = prijavi(Uloga.ADMIN);
        mvc.perform(get("/misne-nakane").session(session).param("od", "2025-01-01").param("doDatuma", "2020-01-01"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Početni datum ne smije biti nakon završnog datuma.")));
        mvc.perform(get("/misne-nakane").session(session).param("status", "XYZ").param("od", "abc"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Unesite ispravan početni datum.")))
                .andExpect(content().string(containsString("Odaberite ispravan status misne nakane.")));
    }

    @Test
    void nepostojeciZapisVraca404() throws Exception {
        var session = prijavi(Uloga.ADMIN);
        mvc.perform(get("/misne-nakane/0").session(session)).andExpect(status().isNotFound());
        mvc.perform(get("/misne-nakane/uredi/0").session(session)).andExpect(status().isNotFound());
        mvc.perform(post("/misne-nakane/0/status").session(session).with(csrf()).param("status", "OTKAZANA"))
                .andExpect(status().isNotFound());
    }

    private MisnaNakana podaci() {
        MisnaNakana p = new MisnaNakana(); p.setNakana("Za obitelj");
        p.setDatum(LocalDate.of(2020, 5, 12)); p.setVrijeme(LocalTime.of(8, 30));
        p.setMjesto("Mostar"); p.setNaruciteljIme("Ana"); return p;
    }
    private MockHttpSession prijavi(Uloga uloga) throws Exception {
        KorisnikForma forma = new KorisnikForma(); forma.setIme("Test"); forma.setPrezime("Djelatnik");
        forma.setEmail(UUID.randomUUID() + "@primjer.hr"); forma.setLozinka("TestnaLozinka123!");
        forma.setUloga(uloga); korisnici.dodaj(forma);
        return (MockHttpSession) mvc.perform(formLogin("/login")
                .user("email", forma.getEmail()).password("lozinka", forma.getLozinka()))
                .andExpect(authenticated()).andReturn().getRequest().getSession(false);
    }
}
