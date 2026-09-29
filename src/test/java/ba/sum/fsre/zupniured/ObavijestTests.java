package ba.sum.fsre.zupniured;

import ba.sum.fsre.zupniured.dto.*;
import ba.sum.fsre.zupniured.model.*;
import ba.sum.fsre.zupniured.repository.ObavijestRepository;
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
import org.springframework.web.server.ResponseStatusException;
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
class ObavijestTests {
    private final ObavijestService service;
    private final KorisnikService korisnici;
    private final ObavijestRepository repository;
    private final EntityManager em;
    private final MockMvc mvc;

    ObavijestTests(ObavijestService service, KorisnikService korisnici,
                  ObavijestRepository repository, EntityManager em, MockMvc mvc) {
        this.service = service;
        this.korisnici = korisnici;
        this.repository = repository;
        this.em = em;
        this.mvc = mvc;
    }

    @Test
    void stvaranjePostavljaAutoraZadaneOznakeIVremena() {
        Korisnik autor = korisnik(Uloga.ADMIN);
        var zapis = service.dodaj(forma(), autor.getEmail());
        em.clear();
        var ucitan = service.pronadjiPoId(zapis.getId());
        assertThat(ucitan.getAutor().getId()).isEqualTo(autor.getId());
        assertThat(ucitan.isAktivna()).isTrue();
        assertThat(ucitan.isIstaknuta()).isFalse();
        assertThat(ucitan.getDatumKreiranja()).isNotNull();
        assertThat(ucitan.getDatumIzmjene()).isEqualTo(ucitan.getDatumKreiranja());
    }

    @Test
    void uredjivanjeCuvaAutoraIKreiranjeTeAzuriraIzmjenu() {
        var zapis = service.dodaj(forma(), korisnik(Uloga.ADMIN).getEmail());
        // Ranija vrijednost uklanja ovisnost o preciznosti sata; bulk upit ne poziva @PreUpdate.
        var ranijaIzmjena = LocalDate.now().minusDays(1).atStartOfDay();
        assertThat(em.createQuery("update Obavijest o set o.datumIzmjene = :vrijeme where o.id = :id")
                .setParameter("vrijeme", ranijaIzmjena)
                .setParameter("id", zapis.getId())
                .executeUpdate()).isEqualTo(1);
        em.clear();
        var original = service.pronadjiPoId(zapis.getId());
        var kreiranje = original.getDatumKreiranja();
        var izmjena = original.getDatumIzmjene();
        assertThat(izmjena).isEqualTo(ranijaIzmjena);
        var autorId = original.getAutor().getId();
        var forma = service.formaZaUredjivanje(zapis.getId());
        forma.setNaslov("Novi naslov"); forma.setSadrzaj("Novi sadržaj");
        forma.setIstaknuta(true); forma.setAktivna(false);
        forma.setDatumObjave(LocalDate.now().plusDays(1));
        forma.setDatumIsteka(LocalDate.now().plusDays(2));
        service.uredi(zapis.getId(), forma);
        em.clear();
        var ucitan = service.pronadjiPoId(zapis.getId());
        assertThat(ucitan.getNaslov()).isEqualTo("Novi naslov");
        assertThat(ucitan.getSadrzaj()).isEqualTo("Novi sadržaj");
        assertThat(ucitan.getAutor().getId()).isEqualTo(autorId);
        assertThat(ucitan.getDatumKreiranja()).isEqualTo(kreiranje);
        assertThat(ucitan.getDatumIzmjene()).isAfter(izmjena);
        assertThat(ucitan.isIstaknuta()).isTrue();
        assertThat(ucitan.isAktivna()).isFalse();
        assertThat(ucitan.getDatumIsteka()).isEqualTo(LocalDate.now().plusDays(2));
    }

    @Test
    void deaktivacijaIPonovnoAktiviranjeMijenjajuJavnuDostupnost() {
        var zapis = service.dodaj(forma(), korisnik(Uloga.ADMIN).getEmail());
        em.clear();
        var prije = service.pronadjiPoId(zapis.getId()).getDatumIzmjene();
        service.postaviAktivnost(zapis.getId(), false);
        em.clear();
        assertThat(service.pronadjiPoId(zapis.getId()).getDatumIzmjene()).isAfter(prije);
        assertThatThrownBy(() -> service.javnaPoId(zapis.getId())).isInstanceOf(ResponseStatusException.class);
        service.postaviAktivnost(zapis.getId(), true);
        em.clear();
        assertThat(service.javnaPoId(zapis.getId()).isAktivna()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"naslov", "dugNaslov", "sadrzaj", "datum", "istek"})
    void validacijaVrijediPriDodavanjuIUredjivanju(String slucaj) {
        var email = korisnik(Uloga.ADMIN).getEmail();
        var zapis = service.dodaj(forma(), email);
        var los = forma();
        switch (slucaj) {
            case "naslov" -> los.setNaslov(" ");
            case "dugNaslov" -> los.setNaslov("a".repeat(201));
            case "sadrzaj" -> los.setSadrzaj(" ");
            case "datum" -> los.setDatumObjave(null);
            case "istek" -> los.setDatumIsteka(los.getDatumObjave().minusDays(1));
        }
        assertThatThrownBy(() -> service.dodaj(los, email)).isInstanceOf(ConstraintViolationException.class);
        assertThatThrownBy(() -> service.uredi(zapis.getId(), los)).isInstanceOf(ConstraintViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"objavljena", "buduca", "istekla", "neaktivna", "istjeceDanas"})
    void javnePutanjeIPocetnaPostujuDostupnost(String slucaj) throws Exception {
        var f = forma();
        switch (slucaj) {
            case "buduca" -> f.setDatumObjave(LocalDate.now().plusDays(1));
            case "istekla" -> { f.setDatumObjave(LocalDate.now().minusDays(2)); f.setDatumIsteka(LocalDate.now().minusDays(1)); }
            case "neaktivna" -> f.setAktivna(false);
            case "istjeceDanas" -> f.setDatumIsteka(LocalDate.now());
        }
        var zapis = service.dodaj(f, korisnik(Uloga.ADMIN).getEmail());
        boolean vidljiva = slucaj.equals("objavljena") || slucaj.equals("istjeceDanas");
        mvc.perform(get("/obavijesti/" + zapis.getId()))
                .andExpect(vidljiva ? status().isOk() : status().isNotFound());
        for (String url : List.of("/obavijesti", "/")) {
            mvc.perform(get(url)).andExpect(status().isOk()).andExpect(content().string(
                    vidljiva ? containsString(f.getNaslov()) : not(containsString(f.getNaslov()))));
        }
    }

    @Test
    void javniRedoslijedIPocetnaImajuPrednostIstaknutihILimitTri() throws Exception {
        String email = korisnik(Uloga.ADMIN).getEmail();
        List<Obavijest> zapisi = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            var f = forma(); f.setDatumObjave(LocalDate.now().minusDays(i));
            f.setIstaknuta(i == 4);
            zapisi.add(service.dodaj(f, email));
        }
        var javne = service.javneObavijesti().stream().filter(o -> zapisi.stream()
                .anyMatch(z -> z.getId().equals(o.getId()))).toList();
        assertThat(javne).extracting(Obavijest::getId).containsExactly(
                zapisi.get(4).getId(), zapisi.get(0).getId(), zapisi.get(1).getId(), zapisi.get(2).getId(), zapisi.get(3).getId());
        var pocetna = service.obavijestiZaPocetnu();
        assertThat(pocetna).hasSize(3);
        assertThat(pocetna.getFirst().getId()).isEqualTo(zapisi.get(4).getId());
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Istaknuta obavijest")))
                .andExpect(content().string(containsString("Sve obavijesti")))
                .andExpect(content().string(not(containsString(zapisi.get(3).getNaslov()))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"naslov", "sadrzaj"})
    void pretragaNeRazlikujeVelicinuSlova(String polje) {
        var email = korisnik(Uloga.ADMIN).getEmail();
        var f = forma(); String oznaka = "Test" + UUID.randomUUID();
        if (polje.equals("naslov")) f.setNaslov(oznaka); else f.setSadrzaj(oznaka);
        var zapis = service.dodaj(f, email);
        service.dodaj(forma(), email);
        assertThat(service.pronadjiSve(" " + oznaka.toUpperCase(Locale.ROOT) + " ", ObavijestFilter.SVE))
                .extracting(Obavijest::getId).containsExactly(zapis.getId());
    }

    @ParameterizedTest
    @EnumSource(ObavijestFilter.class)
    void administracijskiFiltriObuhvacajuINeobjavljene(ObavijestFilter filter) {
        var email = korisnik(Uloga.ADMIN).getEmail();
        String oznaka = UUID.randomUUID().toString();
        var f = forma(); f.setNaslov(oznaka); f.setDatumObjave(LocalDate.now().plusDays(3));
        var aktivna = service.dodaj(f, email);
        f.setAktivna(false); f.setIstaknuta(true);
        var neaktivna = service.dodaj(f, email);
        var rezultat = service.pronadjiSve(oznaka, filter);
        switch (filter) {
            case SVE -> assertThat(rezultat).extracting(Obavijest::getId).containsExactly(neaktivna.getId(), aktivna.getId());
            case AKTIVNE -> assertThat(rezultat).extracting(Obavijest::getId).containsExactly(aktivna.getId());
            case NEAKTIVNE, ISTAKNUTE -> assertThat(rezultat).extracting(Obavijest::getId).containsExactly(neaktivna.getId());
        }
    }

    @Test
    void administracijaSortiraPrvoPoObjaviZatimKreiranju() {
        String email = korisnik(Uloga.ADMIN).getEmail(), oznaka = UUID.randomUUID().toString();
        var f = forma(); f.setNaslov(oznaka); f.setDatumObjave(LocalDate.now());
        var prvi = service.dodaj(f, email);
        var drugi = service.dodaj(f, email);
        f.setDatumObjave(LocalDate.now().minusDays(1)); f.setIstaknuta(true);
        var stariji = service.dodaj(f, email);
        assertThat(service.pronadjiSve(oznaka, ObavijestFilter.SVE)).extracting(Obavijest::getId)
                .containsExactly(drugi.getId(), prvi.getId(), stariji.getId());
    }

    @ParameterizedTest
    @EnumSource(Uloga.class)
    void ovlasteneUlogeAdministrirajuAutorJeIzPrijave(Uloga uloga) throws Exception {
        Korisnik autor = korisnik(uloga);
        var session = prijavi(autor);
        mvc.perform(get("/").session(session)).andExpect(content().string(containsString("Upravljanje obavijestima")));
        mvc.perform(get("/admin/obavijesti/novi").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(not(containsString("name=\"autor\""))));
        String oznaka = UUID.randomUUID().toString();
        mvc.perform(post("/admin/obavijesti/novi").session(session).with(csrf())
                .param("naslov", oznaka).param("sadrzaj", "Sadržaj").param("datumObjave", LocalDate.now().toString())
                .param("autor", "0").param("autor.id", "0").param("id", "0")
                .param("datumKreiranja", "1900-01-01T00:00:00"))
                .andExpect(status().is3xxRedirection());
        var zapis = service.pronadjiSve(oznaka, ObavijestFilter.SVE).getFirst();
        assertThat(zapis.getAutor().getId()).isEqualTo(autor.getId());
        assertThat(zapis.getDatumKreiranja().toLocalDate()).isEqualTo(LocalDate.now());
        String url = "/admin/obavijesti/" + zapis.getId();
        mvc.perform(get(url).session(session)).andExpect(status().isOk()).andExpect(content().string(containsString(autor.getIme())));
        mvc.perform(get("/admin/obavijesti/uredi/" + zapis.getId()).session(session)).andExpect(status().isOk());
        mvc.perform(post("/admin/obavijesti/uredi/" + zapis.getId()).session(prijavi(korisnik(uloga))).with(csrf())
                .param("naslov", oznaka).param("sadrzaj", "Novi sadržaj").param("datumObjave", LocalDate.now().toString())
                .param("autor.id", "0").param("aktivna", "true").param("istaknuta", "true"))
                .andExpect(redirectedUrl(url));
        assertThat(service.pronadjiPoId(zapis.getId()).getAutor().getId()).isEqualTo(autor.getId());
        mvc.perform(post("/admin/obavijesti/deaktiviraj/" + zapis.getId()).session(session).with(csrf()))
                .andExpect(redirectedUrl(url));
        mvc.perform(get("/admin/obavijesti").session(session).param("filter", "NEAKTIVNE").param("pretraga", oznaka))
                .andExpect(status().isOk()).andExpect(content().string(containsString(oznaka)));
        mvc.perform(get("/obavijesti/" + zapis.getId())).andExpect(status().isNotFound());
        mvc.perform(post("/admin/obavijesti/aktiviraj/" + zapis.getId()).session(session).with(csrf()))
                .andExpect(redirectedUrl(url));
        mvc.perform(get("/obavijesti/" + zapis.getId())).andExpect(status().isOk());
    }

    @Test
    void anonimniNeMozePristupitiAdministraciji() throws Exception {
        for (String url : List.of("/admin/obavijesti", "/admin/obavijesti/novi", "/admin/obavijesti/uredi/1", "/admin/obavijesti/1"))
            mvc.perform(get(url)).andExpect(redirectedUrl("/login")).andExpect(unauthenticated());
        for (String url : List.of("/admin/obavijesti/novi", "/admin/obavijesti/uredi/1",
                "/admin/obavijesti/aktiviraj/1", "/admin/obavijesti/deaktiviraj/1"))
            mvc.perform(post(url).with(csrf())).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/")).andExpect(content().string(not(containsString("Upravljanje obavijestima"))));
    }

    @Test
    void csrfOstajeObavezanIBrisanjeNijeDostupno() throws Exception {
        var autor = korisnik(Uloga.ADMIN); var session = prijavi(autor);
        var zapis = service.dodaj(forma(), autor.getEmail());
        for (String url : List.of("/admin/obavijesti/novi", "/admin/obavijesti/uredi/" + zapis.getId(),
                "/admin/obavijesti/aktiviraj/" + zapis.getId(), "/admin/obavijesti/deaktiviraj/" + zapis.getId()))
            mvc.perform(post(url).session(session)).andExpect(status().isForbidden());
        mvc.perform(delete("/admin/obavijesti/" + zapis.getId()).session(session).with(csrf())).andExpect(status().isMethodNotAllowed());
        assertThat(repository.existsById(zapis.getId())).isTrue();
    }

    @Test
    void obaObrascaPrikazujuHrvatskeGreske() throws Exception {
        var autor = korisnik(Uloga.ADMIN); var session = prijavi(autor);
        var zapis = service.dodaj(forma(), autor.getEmail());
        for (String url : List.of("/admin/obavijesti/novi", "/admin/obavijesti/uredi/" + zapis.getId())) {
            mvc.perform(post(url).session(session).with(csrf())).andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrors("obavijest", "naslov", "sadrzaj", "datumObjave"))
                    .andExpect(content().string(containsString("Naslov je obavezan.")))
                    .andExpect(content().string(containsString("Sadržaj je obavezan.")));
            mvc.perform(post(url).session(session).with(csrf()).param("datumObjave", "2020-05-12").param("datumIsteka", "2020-05-11"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Datum isteka ne smije biti prije datuma objave.")));
            mvc.perform(post(url).session(session).with(csrf()).param("datumObjave", "abc").param("datumIsteka", "abc"))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Unesite ispravan datum objave.")))
                    .andExpect(content().string(containsString("Unesite ispravan datum isteka.")));
        }
    }

    @Test
    void javniSadrzajJeSigurnoEscapiranINeOtkrivaAutora() throws Exception {
        var autor = korisnik(Uloga.ADMIN); var f = forma();
        f.setSadrzaj("<script>alert('x')</script>"); f.setDatumObjave(LocalDate.of(2020, 5, 12));
        var zapis = service.dodaj(f, autor.getEmail());
        mvc.perform(get("/obavijesti/" + zapis.getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;script&gt;")))
                .andExpect(content().string(not(containsString("<script>"))))
                .andExpect(content().string(not(containsString(autor.getEmail()))))
                .andExpect(content().string(containsString("12.05.2020.")));
    }

    @Test
    void nepostojeciIdVraca404() throws Exception {
        mvc.perform(get("/obavijesti/0")).andExpect(status().isNotFound());
        var session = prijavi(korisnik(Uloga.ADMIN));
        mvc.perform(get("/admin/obavijesti/0").session(session)).andExpect(status().isNotFound());
        mvc.perform(get("/admin/obavijesti/uredi/0").session(session)).andExpect(status().isNotFound());
    }

    private ObavijestForma forma() {
        var f = new ObavijestForma(); f.setNaslov("Obavijest " + UUID.randomUUID());
        f.setSadrzaj("Sadržaj obavijesti"); f.setDatumObjave(LocalDate.now()); return f;
    }

    private Korisnik korisnik(Uloga uloga) {
        var f = new KorisnikForma(); f.setIme("Autor"); f.setPrezime("Obavijesti");
        f.setEmail(UUID.randomUUID() + "@primjer.hr"); f.setLozinka("TestnaLozinka123!");
        f.setUloga(uloga); return korisnici.dodaj(f);
    }

    private MockHttpSession prijavi(Korisnik korisnik) throws Exception {
        return (MockHttpSession) mvc.perform(formLogin("/login").user("email", korisnik.getEmail())
                .password("lozinka", "TestnaLozinka123!"))
                .andExpect(authenticated()).andReturn().getRequest().getSession(false);
    }
}
