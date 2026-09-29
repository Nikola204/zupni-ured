package ba.sum.fsre.zupniured;

import ba.sum.fsre.zupniured.dto.*;
import ba.sum.fsre.zupniured.model.*;
import ba.sum.fsre.zupniured.service.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
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
class DokumentWebTests {
    private final MockMvc mvc;
    private final DokumentService service;
    private final KorisnikService korisnici;
    private final ZupljaninService osobe;
    private final EntityManager em;
    DokumentWebTests(MockMvc mvc, DokumentService service, KorisnikService korisnici,
                     ZupljaninService osobe, EntityManager em) {
        this.mvc = mvc; this.service = service; this.korisnici = korisnici; this.osobe = osobe; this.em = em;
    }

    @ParameterizedTest
    @EnumSource(Uloga.class)
    void sveUlogeMoguKoristitiModulIIzdavanjeNePrihvacaPodmetnutogDjelatnika(Uloga uloga) throws Exception {
        var autor = korisnik(uloga); var session = prijavi(autor);
        mvc.perform(get("/").session(session)).andExpect(content().string(containsString("href=\"/dokumenti\"")));
        mvc.perform(get("/dokumenti/novi").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(not(containsString("name=\"izdaoKorisnik\""))));
        String oznaka = UUID.randomUUID().toString();
        mvc.perform(unos("/dokumenti/novi", session).param("svrha", oznaka)
                .param("id", "0").param("izdaoKorisnik.id", "0")
                .param("datumKreiranja", "1900-01-01T00:00:00")).andExpect(status().is3xxRedirection());
        var filter = new DokumentFilter(); filter.setPretraga(oznaka);
        var d = service.pronadjiSve(filter).getFirst();
        assertThat(d.getDatumKreiranja().toLocalDate()).isEqualTo(LocalDate.now());
        assertThat(d.getIzdaoKorisnik()).isNull();
        String url = "/dokumenti/" + d.getId();
        mvc.perform(get(url).session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Marko Markić")))
                .andExpect(content().string(containsString("01.06.2020.")))
                .andExpect(content().string(containsString("Primatelj nije povezan sa župljaninom.")));
        mvc.perform(get("/dokumenti/uredi/" + d.getId()).session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"2020-06-01\"")));
        mvc.perform(unos("/dokumenti/uredi/" + d.getId(), session).param("svrha", oznaka).param("napomena", "Izmjena"))
                .andExpect(redirectedUrl(url));
        for (String status : List.of("U_OBRADI", "ODBIJEN")) {
            mvc.perform(post(url + "/status").session(session).with(csrf()).param("status", status))
                    .andExpect(redirectedUrl(url));
        }
        mvc.perform(post(url + "/status").session(session).with(csrf()).param("status", "IZDAN")
                .param("datumIzdavanja", "2020-06-02").param("izdaoKorisnik.id", "0").param("izdaoKorisnikId", "0"))
                .andExpect(redirectedUrl(url));
        assertThat(d.getIzdaoKorisnik().getId()).isEqualTo(autor.getId());
        mvc.perform(unos("/dokumenti/uredi/" + d.getId(), prijavi(korisnik(uloga)))
                .param("status", "IZDAN").param("datumIzdavanja", "2020-06-02").param("svrha", oznaka))
                .andExpect(redirectedUrl(url));
        assertThat(d.getIzdaoKorisnik().getId()).isEqualTo(autor.getId());
        mvc.perform(get("/dokumenti").session(session).param("pretraga", oznaka)
                .param("vrstaDokumenta", "OSTALO").param("status", "IZDAN")
                .param("od", "2020-06-01").param("doDatuma", "2020-06-01"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Marko Markić")))
                .andExpect(content().string(containsString("02.06.2020.")));
    }

    @Test
    void anonimniNemaPristup() throws Exception {
        for (String url : List.of("/dokumenti", "/dokumenti/novi", "/dokumenti/uredi/1", "/dokumenti/1"))
            mvc.perform(get(url)).andExpect(redirectedUrl("/login")).andExpect(unauthenticated());
        for (String url : List.of("/dokumenti/novi", "/dokumenti/uredi/1", "/dokumenti/1/status"))
            mvc.perform(post(url).with(csrf())).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/")).andExpect(content().string(not(containsString("href=\"/dokumenti\""))));
    }

    @Test
    void csrfIBrisanje() throws Exception {
        var session = prijavi(korisnik(Uloga.ADMIN)); var d = zapis();
        for (String url : List.of("/dokumenti/novi", "/dokumenti/uredi/" + d.getId(), "/dokumenti/" + d.getId() + "/status"))
            mvc.perform(post(url).session(session)).andExpect(status().isForbidden());
        mvc.perform(delete("/dokumenti/" + d.getId()).session(session).with(csrf())).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void obaObrascaPrikazujuValidacijskeGreske() throws Exception {
        var session = prijavi(korisnik(Uloga.ADMIN)); var d = zapis();
        for (String url : List.of("/dokumenti/novi", "/dokumenti/uredi/" + d.getId())) {
            mvc.perform(post(url).session(session).with(csrf()).param("status", ""))
                    .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("dokument", "vrstaDokumenta", "datumZahtjeva", "status"));
            mvc.perform(unos(url, session).with(request -> {
                        request.setParameter("vrstaDokumenta", "KRSNI_LIST");
                        return request;
                    }))
                    .andExpect(status().isOk()).andExpect(content().string(containsString("odaberite krštenje")));
            mvc.perform(post(url).session(session).with(csrf()).param("vrstaDokumenta", "OSTALO").param("datumZahtjeva", "2020-06-01"))
                    .andExpect(status().isOk()).andExpect(content().string(containsString("Ime primatelja je obavezno.")));
            mvc.perform(unos(url, session).param("status", "IZDAN").param("datumIzdavanja", "2020-05-31"))
                    .andExpect(status().isOk()).andExpect(content().string(containsString("Datum izdavanja ne smije biti prije datuma zahtjeva.")));
            mvc.perform(unos(url, session).param("zupljaninId", "0"))
                    .andExpect(status().isOk()).andExpect(content().string(containsString("Odabrani župljanin nije pronađen.")));
        }
    }

    @Test
    void neispravniTipoviIFiltriImajuHrvatskePoruke() throws Exception {
        var session = prijavi(korisnik(Uloga.DJELATNIK));
        mvc.perform(post("/dokumenti/novi").session(session).with(csrf())
                .param("datumZahtjeva", "abc").param("datumIzdavanja", "abc").param("status", "XYZ")
                .param("vrstaDokumenta", "XYZ").param("vjencanjeId", "abc"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Unesite ispravan datum zahtjeva.")))
                .andExpect(content().string(containsString("Odaberite ispravnu vrstu dokumenta.")))
                .andExpect(content().string(containsString("Odaberite ispravan status dokumenta.")));
        mvc.perform(get("/dokumenti").session(session).param("status", "XYZ").param("vrstaDokumenta", "XYZ"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("dokumentFilter", "status", "vrstaDokumenta"));
        mvc.perform(get("/dokumenti").session(session).param("od", "2021-01-01").param("doDatuma", "2020-01-01"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Početni datum ne smije biti nakon završnog datuma.")));
    }

    @Test
    void neispravnaPromjenaStatusaNeIzdajeDokument() throws Exception {
        var session = prijavi(korisnik(Uloga.DJELATNIK)); var d = zapis();
        mvc.perform(post("/dokumenti/" + d.getId() + "/status").session(session).with(csrf()).param("status", "IZDAN"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Za izdani dokument unesite datum izdavanja.")));
        mvc.perform(post("/dokumenti/" + d.getId() + "/status").session(session).with(csrf()).param("status", "XYZ"))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Odaberite ispravan status dokumenta.")));
        assertThat(d.getStatus()).isEqualTo(StatusDokumenta.ZAPRIMLJEN);
        assertThat(d.getIzdaoKorisnik()).isNull();
    }

    @Test
    void izvoriIZupljaniSuPrepoznatljiviNaFormi() throws Exception {
        var z = osoba(); var k = new Krstenje();
        k.setZupljanin(z); k.setDatumKrstenja(LocalDate.of(2000, 2, 3)); k.setMjestoKrstenja("Mostar");
        em.persist(k); em.flush();
        var session = prijavi(korisnik(Uloga.ADMIN));
        mvc.perform(get("/dokumenti/novi").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Marko Test — 01.01.1990.")))
                .andExpect(content().string(containsString("Marko Test — 03.02.2000.")));
        var f = new DokumentForma(); f.setVrstaDokumenta(VrstaDokumenta.KRSNI_LIST);
        f.setKrstenjeId(k.getId()); f.setDatumZahtjeva(LocalDate.of(2020, 6, 1));
        var d = service.dodaj(f, null);
        mvc.perform(get("/dokumenti/" + d.getId()).session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/krstenja/" + k.getId() + "\"")))
                .andExpect(content().string(containsString("href=\"/zupljani/" + z.getId() + "\"")));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3})
    void profilPrikazujeSamoPovezaneDokumente(int broj) throws Exception {
        var z = osoba(); var nepovezan = zapis();
        var f = service.formaZaUredjivanje(nepovezan.getId()); f.setPrezimePrimatelja("Test");
        service.uredi(nepovezan.getId(), f, null);
        f.setZupljaninId(z.getId());
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < broj; i++) ids.add(service.dodaj(f, null).getId());
        String html = mvc.perform(get("/zupljani/" + z.getId()).session(prijavi(korisnik(Uloga.ADMIN))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("Dokumenti").doesNotContain("href=\"/dokumenti/" + nepovezan.getId() + "\"");
        if (broj == 0) assertThat(html).contains("Nema povezanih dokumenata.");
        for (Long id : ids) assertThat(html).contains("href=\"/dokumenti/" + id + "\"");
    }

    @Test
    void nepostojeciIdVraca404() throws Exception {
        var session = prijavi(korisnik(Uloga.ADMIN));
        mvc.perform(get("/dokumenti/0").session(session)).andExpect(status().isNotFound());
        mvc.perform(get("/dokumenti/uredi/0").session(session)).andExpect(status().isNotFound());
    }

    private MockHttpServletRequestBuilder unos(String url, MockHttpSession session) {
        return post(url).session(session).with(csrf()).param("imePrimatelja", "Marko").param("prezimePrimatelja", "Markić")
                .param("vrstaDokumenta", "OSTALO").param("datumZahtjeva", "2020-06-01");
    }
    private Dokument zapis() {
        var f = new DokumentForma(); f.setImePrimatelja("Marko"); f.setPrezimePrimatelja("Markić");
        f.setVrstaDokumenta(VrstaDokumenta.OSTALO); f.setDatumZahtjeva(LocalDate.of(2020, 6, 1));
        return service.dodaj(f, null);
    }
    private Zupljanin osoba() {
        var z = new Zupljanin(); z.setIme("Marko"); z.setPrezime("Test");
        z.setDatumRodjenja(LocalDate.of(1990, 1, 1)); return osobe.dodaj(z);
    }
    private Korisnik korisnik(Uloga uloga) {
        var f = new KorisnikForma(); f.setIme("Test"); f.setPrezime("Djelatnik");
        f.setEmail(UUID.randomUUID() + "@primjer.hr"); f.setLozinka("TestnaLozinka123!"); f.setUloga(uloga);
        return korisnici.dodaj(f);
    }
    private MockHttpSession prijavi(Korisnik k) throws Exception {
        return (MockHttpSession) mvc.perform(formLogin("/login").user("email", k.getEmail()).password("lozinka", "TestnaLozinka123!"))
                .andExpect(authenticated()).andReturn().getRequest().getSession(false);
    }
}
