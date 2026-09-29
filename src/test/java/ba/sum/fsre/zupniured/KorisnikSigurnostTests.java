package ba.sum.fsre.zupniured;

import ba.sum.fsre.zupniured.dto.KorisnikForma;
import ba.sum.fsre.zupniured.model.Korisnik;
import ba.sum.fsre.zupniured.model.Uloga;
import ba.sum.fsre.zupniured.repository.KorisnikRepository;
import ba.sum.fsre.zupniured.service.KorisnikService;
import ba.sum.fsre.zupniured.service.KorisnikUserDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.pocetni-admin.enabled=false")
@AutoConfigureMockMvc
@Transactional
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class KorisnikSigurnostTests {

    private static final String LOZINKA = "TestnaLozinka123!";
    private final MockMvc mvc;
    private final KorisnikService servis;
    private final KorisnikRepository repository;
    private final KorisnikUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    KorisnikSigurnostTests(MockMvc mvc, KorisnikService servis, KorisnikRepository repository,
                          KorisnikUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        this.mvc = mvc;
        this.servis = servis;
        this.repository = repository;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @Test
    void pocetnaIPrijavaSuJavne() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Prijava djelatnika")));
        mvc.perform(get("/login?error")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Prijava nije uspjela")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void neprijavljeniKorisnikNemaPristupZasticenimStranicama() throws Exception {
        for (String putanja : new String[]{"/korisnici", "/korisnici/novi", "/korisnici/uredi/1",
                "/dogadjaji", "/dogadjaji/novi"}) {
            mvc.perform(get(putanja)).andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"));
        }
        mvc.perform(post("/korisnici/novi").with(csrf()))
                .andExpect(status().is3xxRedirection()).andExpect(unauthenticated());
    }

    @ParameterizedTest
    @EnumSource(Uloga.class)
    void ulogeImajuPristupDogadjajimaASamoAdminKorisnicima(Uloga uloga) throws Exception {
        Korisnik korisnik = kreiraj(uloga);
        mvc.perform(get("/dogadjaji").with(prijava(korisnik))).andExpect(status().isOk());
        mvc.perform(get("/dogadjaji/novi").with(prijava(korisnik))).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(get("/korisnici").with(prijava(korisnik)))
                .andExpect(status().is(uloga == Uloga.ADMIN ? 200 : 403));
        mvc.perform(get("/").with(prijava(korisnik))).andExpect(status().isOk())
                .andExpect(content().string(uloga == Uloga.ADMIN
                        ? containsString("Upravljanje korisnicima") : not(containsString("Upravljanje korisnicima"))));
    }

    @ParameterizedTest
    @EnumSource(value = Uloga.class, names = {"ZUPNIK", "DJELATNIK"})
    void neadministratoriNeMoguMijenjatiKorisnike(Uloga uloga) throws Exception {
        Korisnik korisnik = kreiraj(uloga);
        Korisnik admin = kreiraj(Uloga.ADMIN);
        for (String putanja : new String[]{"/korisnici/novi", "/korisnici/uredi/" + admin.getId(),
                "/korisnici/deaktiviraj/" + admin.getId()}) {
            mvc.perform(post(putanja).with(prijava(korisnik)).with(csrf())
                            .param("ime", "Promjena").param("uloga", "ADMIN"))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/korisnici/uredi/" + admin.getId()).with(prijava(korisnik)))
                .andExpect(status().isForbidden());
        assertThat(repository.findById(admin.getId()).orElseThrow().isAktivan()).isTrue();
    }

    @Test
    void prijavaEmailomNeOvisiOVeliciniSlova() throws Exception {
        Korisnik korisnik = kreiraj(Uloga.DJELATNIK);
        mvc.perform(formLogin("/login").user("email", korisnik.getEmail().toUpperCase(Locale.ROOT))
                        .password("lozinka", LOZINKA))
                .andExpect(authenticated().withUsername(korisnik.getEmail()))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void pogresnaLozinkaINeaktivanRacunOdbijajuPrijavu() throws Exception {
        Korisnik korisnik = kreiraj(Uloga.DJELATNIK);
        mvc.perform(formLogin("/login").user("email", korisnik.getEmail()).password("lozinka", "Pogresno"))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error"));
        servis.deaktiviraj(korisnik.getId());
        mvc.perform(formLogin("/login").user("email", korisnik.getEmail()).password("lozinka", LOZINKA))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void adminDodajeKorisnikaSHashiranomLozinkom() throws Exception {
        Korisnik admin = kreiraj(Uloga.ADMIN);
        String email = UUID.randomUUID() + "@primjer.hr";
        mvc.perform(get("/korisnici/novi").with(prijava(admin))).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")));
        mvc.perform(post("/korisnici/novi").with(prijava(admin)).with(csrf())
                        .param("ime", "Ana").param("prezime", "Anić")
                        .param("email", " " + email.toUpperCase(Locale.ROOT) + " ")
                        .param("lozinka", LOZINKA).param("uloga", "DJELATNIK").param("aktivan", "true"))
                .andExpect(redirectedUrl("/korisnici"));
        Korisnik spremljen = repository.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(spremljen.getEmail()).isEqualTo(email);
        assertThat(spremljen.getDatumKreiranja()).isNotNull();
        assertThat(spremljen.getLozinka()).isNotEqualTo(LOZINKA).startsWith("$2");
        assertThat(passwordEncoder.matches(LOZINKA, spremljen.getLozinka())).isTrue();
    }

    @Test
    void validacijaIDuplikatEmailaPrikazujuPorukeBezLozinke() throws Exception {
        Korisnik admin = kreiraj(Uloga.ADMIN);
        mvc.perform(post("/korisnici/novi").with(prijava(admin)).with(csrf())
                        .param("ime", "").param("prezime", "").param("email", "neispravno")
                        .param("lozinka", "").param("uloga", ""))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("korisnik", "ime", "prezime", "email", "lozinka", "uloga"))
                .andExpect(content().string(containsString("Ime je obavezno.")));

        mvc.perform(post("/korisnici/novi").with(prijava(admin)).with(csrf())
                        .param("ime", "Ana").param("prezime", "Anić").param("email", admin.getEmail())
                        .param("lozinka", LOZINKA).param("uloga", "DJELATNIK"))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("korisnik", "email"))
                .andExpect(content().string(containsString("već postoji")))
                .andExpect(content().string(not(containsString(LOZINKA))))
                .andExpect(content().string(not(containsString(admin.getLozinka()))));
    }

    @Test
    void uredjivanjeZadrzavaLozinkuIDatumKreiranjaKadaJeLozinkaPrazna() throws Exception {
        Korisnik admin = kreiraj(Uloga.ADMIN);
        Korisnik korisnik = kreiraj(Uloga.DJELATNIK);
        String hash = korisnik.getLozinka();
        var datum = korisnik.getDatumKreiranja();
        mvc.perform(get("/korisnici/uredi/" + korisnik.getId()).with(prijava(admin)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(hash))));
        mvc.perform(post("/korisnici/uredi/" + korisnik.getId()).with(prijava(admin)).with(csrf())
                        .param("ime", "Novo ime").param("prezime", "Anić")
                        .param("email", korisnik.getEmail()).param("lozinka", "")
                        .param("uloga", "ZUPNIK").param("aktivan", "true")
                        .param("id", admin.getId().toString()).param("datumKreiranja", "2000-01-01T00:00:00"))
                .andExpect(redirectedUrl("/korisnici"));
        Korisnik uredjen = repository.findById(korisnik.getId()).orElseThrow();
        assertThat(uredjen.getIme()).isEqualTo("Novo ime");
        assertThat(uredjen.getUloga()).isEqualTo(Uloga.ZUPNIK);
        assertThat(uredjen.getLozinka()).isEqualTo(hash);
        assertThat(uredjen.getDatumKreiranja()).isEqualTo(datum);
        assertThat(repository.findById(admin.getId()).orElseThrow().getUloga()).isEqualTo(Uloga.ADMIN);
    }

    @Test
    void novaLozinkaJeHashiranaAStaraViseNeVrijedi() {
        Korisnik korisnik = kreiraj(Uloga.DJELATNIK);
        KorisnikForma forma = servis.pripremiZaUredjivanje(korisnik.getId());
        forma.setLozinka("NovaLozinka456!");
        servis.uredi(korisnik.getId(), forma);
        assertThat(passwordEncoder.matches("NovaLozinka456!", korisnik.getLozinka())).isTrue();
        assertThat(passwordEncoder.matches(LOZINKA, korisnik.getLozinka())).isFalse();
    }

    @Test
    void deaktivacijaCuvaKorisnikaIPrekidaPostojecuPrijavu() throws Exception {
        Korisnik admin = kreiraj(Uloga.ADMIN);
        Korisnik korisnik = kreiraj(Uloga.DJELATNIK);
        MockHttpSession session = prijaviSesiju(korisnik);
        mvc.perform(post("/korisnici/deaktiviraj/" + korisnik.getId()).with(prijava(admin)).with(csrf()))
                .andExpect(redirectedUrl("/korisnici"));
        assertThat(repository.findById(korisnik.getId()).orElseThrow().isAktivan()).isFalse();
        mvc.perform(get("/dogadjaji").session(session)).andExpect(status().is3xxRedirection())
                .andExpect(unauthenticated());
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void promjenaUlogeUklanjaStareOvlastiIzSesije() throws Exception {
        Korisnik admin = kreiraj(Uloga.ADMIN);
        MockHttpSession session = prijaviSesiju(admin);
        KorisnikForma forma = servis.pripremiZaUredjivanje(admin.getId());
        forma.setUloga(Uloga.DJELATNIK);
        servis.uredi(admin.getId(), forma);
        mvc.perform(get("/korisnici").session(session)).andExpect(status().is3xxRedirection())
                .andExpect(unauthenticated());
    }

    @Test
    void csrfJeObavezanZaPromjeneIOdjavu() throws Exception {
        Korisnik admin = kreiraj(Uloga.ADMIN);
        mvc.perform(post("/korisnici/deaktiviraj/" + admin.getId()).with(prijava(admin)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/dogadjaji/obrisi/1").with(prijava(admin)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/logout").with(prijava(admin))).andExpect(status().isForbidden());
        mvc.perform(post("/logout").with(prijava(admin)).with(csrf()))
                .andExpect(redirectedUrl("/login?logout")).andExpect(unauthenticated());
        assertThat(repository.findById(admin.getId()).orElseThrow().isAktivan()).isTrue();
    }

    @Test
    void adminOstajePrijavljenNakonPocetneIPristupaKorisnicima() throws Exception {
        Korisnik admin = kreiraj(Uloga.ADMIN);
        MockHttpSession session = prijaviSesiju(admin);

        mvc.perform(get("/").session(session))
                .andExpect(status().isOk())
                .andExpect(authenticated().withUsername(admin.getEmail()).withRoles("ADMIN"));
        mvc.perform(get("/korisnici").session(session))
                .andExpect(status().isOk())
                .andExpect(authenticated().withUsername(admin.getEmail()).withRoles("ADMIN"));
        assertThat(session.isInvalid()).isFalse();
    }

    @Test
    void nepostojeciKorisnikVraca404() throws Exception {
        Korisnik admin = kreiraj(Uloga.ADMIN);
        mvc.perform(get("/korisnici/uredi/0").with(prijava(admin))).andExpect(status().isNotFound());
    }

    private Korisnik kreiraj(Uloga uloga) {
        KorisnikForma forma = new KorisnikForma();
        forma.setIme("Test");
        forma.setPrezime("Korisnik");
        forma.setEmail(UUID.randomUUID() + "@primjer.hr");
        forma.setLozinka(LOZINKA);
        forma.setUloga(uloga);
        return servis.dodaj(forma);
    }

    private RequestPostProcessor prijava(Korisnik korisnik) {
        return user(userDetailsService.loadUserByUsername(korisnik.getEmail()));
    }

    private MockHttpSession prijaviSesiju(Korisnik korisnik) throws Exception {
        return (MockHttpSession) mvc.perform(formLogin("/login").user("email", korisnik.getEmail())
                        .password("lozinka", LOZINKA))
                .andExpect(authenticated()).andReturn().getRequest().getSession(false);
    }
}
