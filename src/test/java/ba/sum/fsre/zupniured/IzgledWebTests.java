package ba.sum.fsre.zupniured;

import ba.sum.fsre.zupniured.dto.*;
import ba.sum.fsre.zupniured.model.*;
import ba.sum.fsre.zupniured.service.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.pocetni-admin.enabled=false")
@AutoConfigureMockMvc
@Transactional
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class IzgledWebTests {
    private final MockMvc mvc;
    private final KorisnikService korisnici;
    private final ZupljaninService zupljani;
    private final DokumentService dokumenti;
    private final PregledService pregled;
    private final ObavijestService obavijesti;

    IzgledWebTests(MockMvc mvc, KorisnikService korisnici, ZupljaninService zupljani,
                   DokumentService dokumenti, PregledService pregled, ObavijestService obavijesti) {
        this.mvc = mvc; this.korisnici = korisnici; this.zupljani = zupljani;
        this.dokumenti = dokumenti; this.pregled = pregled;
        this.obavijesti = obavijesti;
    }

    @ParameterizedTest
    @EnumSource(Uloga.class)
    void navigacijaPostujeUloguIOznacavaModul(Uloga uloga) throws Exception {
        var session = prijavi(uloga);
        var html = mvc.perform(get("/zupljani").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Ana Marić")))
                .andExpect(content().string(containsString("aria-current=\"page\"")))
                .andExpect(content().string(containsString("href=\"/dokumenti\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(uloga == Uloga.ADMIN ? containsString("href=\"/korisnici\"") : not(containsString("href=\"/korisnici\""))))
                .andReturn().getResponse().getContentAsString();
        assertThat(html).containsOnlyOnce("aria-label=\"Administracijska navigacija\"")
                .containsOnlyOnce("action=\"/logout\"")
                .containsPattern("<a(?=[^>]*href=\"/zupljani\")(?=[^>]*aria-current=\"page\")[^>]*>Župljani</a>");
        mvc.perform(get("/").session(session)).andExpect(status().isOk())
                .andExpect(model().attributeExists("statistike"))
                .andExpect(content().string(containsString("Brze poveznice")));
    }

    @Test
    void javniPrikazNeOtkrivaAdministracijuNiBrojace() throws Exception {
        for (String url : List.of("/", "/obavijesti", "/login")) {
            mvc.perform(get(url)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("app-public")))
                    .andExpect(content().string(not(containsString("Administracijska navigacija"))))
                    .andExpect(content().string(not(containsString("Dokumenti na čekanju"))));
        }
        mvc.perform(get("/obavijesti").session(prijavi(Uloga.ADMIN)))
                .andExpect(content().string(containsString("app-public")))
                .andExpect(content().string(not(containsString("Administracijska navigacija"))));
    }

    @Test
    void statistikaBrojiSamoAktivneZupljaneIDokumenteNaCekanju() {
        long aktivniPrije = broj("Aktivni župljani"), dokumentiPrije = broj("Dokumenti na čekanju");
        osoba("Ivan", "Ivić");
        var neaktivan = osoba("Petar", "Perić");
        zupljani.deaktiviraj(neaktivan.getId());
        var f = new DokumentForma(); f.setImePrimatelja("Ivan"); f.setPrezimePrimatelja("Ivić");
        f.setVrstaDokumenta(VrstaDokumenta.OSTALO); f.setDatumZahtjeva(LocalDate.now());
        dokumenti.dodaj(f, null);
        f.setStatus(StatusDokumenta.U_OBRADI); dokumenti.dodaj(f, null);
        f.setStatus(StatusDokumenta.ODBIJEN); dokumenti.dodaj(f, null);
        assertThat(broj("Aktivni župljani")).isEqualTo(aktivniPrije + 1);
        assertThat(broj("Dokumenti na čekanju")).isEqualTo(dokumentiPrije + 2);
    }

    @Test
    void lokalniStiloviDostupniSuBezPrijave() throws Exception {
        mvc.perform(get("/css/app.css")).andExpect(status().isOk())
                .andExpect(content().string(containsString("@media")));
        mvc.perform(get("/css/vendor/bootstrap-5.3.8.min.css")).andExpect(status().isOk())
                .andExpect(content().string(containsString("Bootstrap  v5.3.8")));
    }

    @Test
    void reprezentativneStraniceKoristeZajednickiIzgled() throws Exception {
        var z = osoba("Marko", "Markić"); osoba("Iva", "Anić");
        var session = prijavi(Uloga.ADMIN);
        Map<String, String> stranice = new LinkedHashMap<>();
        stranice.put("dashboard", "/");
        stranice.put("lista", "/zupljani");
        stranice.put("forma", "/dokumenti/novi");
        stranice.put("profil", "/zupljani/" + z.getId());
        for (var stranica : stranice.entrySet()) {
            String html = mvc.perform(get(stranica.getValue()).session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("/css/app.css")))
                    .andReturn().getResponse().getContentAsString();
            assertThat(html).containsOnlyOnce("<main ").containsOnlyOnce("class=\"app-header\"");
            spremiPregled(stranica.getKey(), html);
        }
        String javno = mvc.perform(get("/")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        spremiPregled("javno", javno);
    }

    @Test
    void brojAktivnihObavijestiUkljucujeBuduceIIstekleAliNeNeaktivne() throws Exception {
        long prije = broj("Aktivne obavijesti");
        var autor = new KorisnikForma();
        autor.setIme("Ana"); autor.setPrezime("Marić"); autor.setUloga(Uloga.ADMIN);
        autor.setEmail(UUID.randomUUID() + "@primjer.hr"); autor.setLozinka("TestnaLozinka123!");
        korisnici.dodaj(autor);
        var forma = new ObavijestForma();
        forma.setNaslov("Susret župne zajednice");
        forma.setSadrzaj("Pozivamo župljane na zajednički susret nakon nedjeljne mise.\n\nPrilika je to za razgovor i druženje u župnoj dvorani. Svi su dobrodošli.");
        forma.setDatumObjave(LocalDate.now()); forma.setIstaknuta(true);
        var javna = obavijesti.dodaj(forma, autor.getEmail());
        forma.setDatumObjave(LocalDate.now().plusDays(1));
        var buduca = obavijesti.dodaj(forma, autor.getEmail());
        forma.setDatumObjave(LocalDate.now().minusDays(2)); forma.setDatumIsteka(LocalDate.now().minusDays(1));
        var istekla = obavijesti.dodaj(forma, autor.getEmail());
        forma.setDatumIsteka(null); forma.setAktivna(false);
        var neaktivna = obavijesti.dodaj(forma, autor.getEmail());
        assertThat(broj("Aktivne obavijesti")).isEqualTo(prije + 3);
        assertThat(obavijesti.javneObavijesti()).extracting(Obavijest::getId)
                .contains(javna.getId()).doesNotContain(buduca.getId(), istekla.getId(), neaktivna.getId());
        for (var stranica : Map.of("polish-public", "/", "polish-login", "/login",
                "polish-login-error", "/login?error", "polish-notice", "/obavijesti/" + javna.getId()).entrySet()) {
            String html = mvc.perform(get(stranica.getValue())).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            spremiPregled(stranica.getKey(), html);
        }
        spremiPregled("polish-dashboard", mvc.perform(get("/").session(prijavi(Uloga.ADMIN)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private void spremiPregled(String naziv, String html) throws Exception {
        if (!Boolean.getBoolean("ui.preview")) return;
        Path direktorij = Path.of("target", "ui-preview");
        Files.createDirectories(direktorij);
        String css = Path.of("src/main/resources/static/css").toAbsolutePath().toUri().toString();
        Files.writeString(direktorij.resolve(naziv + ".html"), html.replace("href=\"/css/", "href=\"" + css));
    }

    private long broj(String naziv) {
        return pregled.statistike().stream().filter(s -> s.naziv().equals(naziv)).findFirst().orElseThrow().broj();
    }
    private Zupljanin osoba(String ime, String prezime) {
        var z = new Zupljanin(); z.setIme(ime); z.setPrezime(prezime); z.setDatumRodjenja(LocalDate.of(1990, 1, 1));
        z.setAdresa("Ulica kralja Tomislava 12"); return zupljani.dodaj(z);
    }
    private MockHttpSession prijavi(Uloga uloga) throws Exception {
        var f = new KorisnikForma(); f.setIme("Ana"); f.setPrezime("Marić");
        f.setEmail(UUID.randomUUID() + "@primjer.hr"); f.setLozinka("TestnaLozinka123!"); f.setUloga(uloga);
        korisnici.dodaj(f);
        return (MockHttpSession) mvc.perform(formLogin("/login").user("email", f.getEmail()).password("lozinka", f.getLozinka()))
                .andExpect(authenticated()).andReturn().getRequest().getSession(false);
    }
}
