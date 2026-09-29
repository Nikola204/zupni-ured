package ba.sum.fsre.zupniured;

import ba.sum.fsre.zupniured.dto.*;
import ba.sum.fsre.zupniured.model.*;
import ba.sum.fsre.zupniured.service.*;
import jakarta.persistence.EntityManager;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.pocetni-admin.enabled=false")
@AutoConfigureMockMvc
@Transactional
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class DokumentPdfTests {
    private final DokumentPdfService pdf;
    private final DokumentService dokumenti;
    private final KorisnikService korisnici;
    private final EntityManager em;
    private final MockMvc mvc;

    DokumentPdfTests(DokumentPdfService pdf, DokumentService dokumenti, KorisnikService korisnici,
                     EntityManager em, MockMvc mvc) {
        this.pdf = pdf; this.dokumenti = dokumenti; this.korisnici = korisnici; this.em = em; this.mvc = mvc;
    }

    @Test
    void krsniListCitaPovezaniIzvorIPovijesnoImeBezPromjeneEvidencije() throws Exception {
        var d = krsniList();
        // Drugi zapis ne smije biti izvor podataka za ovaj PDF.
        var drugi = krstenje(); drugi.setMjestoKrstenja("Drugo krštenje"); em.flush();
        d.getKrstenje().getZupljanin().setIme("Promijenjeno ime"); em.flush(); em.clear();
        d = dokumenti.pronadjiPoId(d.getId());
        var izmjena = d.getDatumIzmjene(); var kreiranje = d.getDatumKreiranja();
        var djelatnik = d.getIzdaoKorisnik().getId(); var izdavanje = d.getDatumIzdavanja();
        var rezultat = pdf.generiraj(d.getId());
        String tekst = tekst(rezultat.sadrzaj());
        assertThat(tekst).contains("KRSNI LIST", "Župni ured", "Čedomir Šimić", "02.03.1990.", "Đakovo",
                "04.04.1990.", "Široki Brijeg", "Otac Željko", "Majka Đurđa", "Kum Ćiro", "Kuma Štefica",
                "Krstitelj Čović", "Č-42", "1990", "17.09.2026.", "Ana Đurić", "Potpis i pečat")
                .doesNotContain("Promijenjeno ime", "Drugo krštenje", "null");
        assertThat(rezultat.nazivDatoteke()).startsWith("krsni-list-Cedomir-Simic-").endsWith(".pdf");
        em.flush(); em.clear();
        var nakon = dokumenti.pronadjiPoId(d.getId());
        assertThat(nakon.getDatumIzmjene()).isEqualTo(izmjena);
        assertThat(nakon.getDatumKreiranja()).isEqualTo(kreiranje);
        assertThat(nakon.getDatumIzdavanja()).isEqualTo(izdavanje);
        assertThat(nakon.getIzdaoKorisnik().getId()).isEqualTo(djelatnik);
        assertThat(nakon.getStatus()).isEqualTo(StatusDokumenta.IZDAN);
        assertThat(nakon.getImePrimatelja()).isEqualTo("Čedomir");
        pregled("krsni-list", rezultat.sadrzaj());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void vjencanjeRadiSaSvimKombinacijamaVezaIPovijesnimImenima(int veze) throws Exception {
        var v = vjencanje(veze);
        var d = izdani(VrstaDokumenta.POTVRDA_O_VJENCANJU, null, v);
        if (v.getMladozenjaZupljanin() != null) v.getMladozenjaZupljanin().setIme("Novo ime mladoženje");
        if (v.getNevjestaZupljanin() != null) v.getNevjestaZupljanin().setPrezime("Novo prezime nevjeste");
        em.flush(); em.clear();
        long prije = brojOsoba();
        var rezultat = pdf.generiraj(d.getId());
        assertThat(tekst(rezultat.sadrzaj())).contains("POTVRDA O VJENČANJU", "Željko Čović", "Đurđa Šimić",
                "06.06.2020.", "Čitluk", "Svjedok Ćosić", "Svjedokinja Žilić", "Služitelj Šarić", "V-15", "2020",
                "17.09.2026.", "Ana Đurić")
                .doesNotContain("Novo ime mladoženje", "Novo prezime nevjeste", "null");
        assertThat(brojOsoba()).isEqualTo(prije);
        assertThat(dokumenti.pronadjiPoId(d.getId()).getZupljanin()).isNull();
        if (veze == 0) pregled("potvrda-o-vjencanju", rezultat.sadrzaj());
    }

    @ParameterizedTest
    @EnumSource(value = StatusDokumenta.class, names = "IZDAN", mode = EnumSource.Mode.EXCLUDE)
    void neizdaniDokumentNemaPdfNiGumb(StatusDokumenta status) throws Exception {
        var d = krsniList();
        var f = new DokumentStatusForma(); f.setStatus(status);
        dokumenti.promijeniStatus(d.getId(), f, null);
        mvc.perform(get("/dokumenti/" + d.getId() + "/pdf").session(prijavi(Uloga.ADMIN)))
                .andExpect(status().isConflict());
        assertThatThrownBy(() -> pdf.generiraj(d.getId())).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getReason()).contains("samo za izdani"));
        mvc.perform(get("/dokumenti/" + d.getId()).session(prijavi(Uloga.ADMIN)))
                .andExpect(status().isOk()).andExpect(content().string(not(containsString("Generiraj PDF"))));
    }

    @ParameterizedTest
    @EnumSource(value = VrstaDokumenta.class, names = {"KRSNI_LIST", "POTVRDA_O_VJENCANJU"}, mode = EnumSource.Mode.EXCLUDE)
    void nepodrzanaVrstaNeMozeZaobiciProvjeruPutemUrla(VrstaDokumenta vrsta) throws Exception {
        var d = izdani(VrstaDokumenta.OSTALO, null, null);
        d.setVrstaDokumenta(vrsta); em.flush();
        var session = prijavi(Uloga.ADMIN);
        mvc.perform(get("/dokumenti/" + d.getId() + "/pdf").session(session))
                .andExpect(status().isUnprocessableEntity());
        assertThatThrownBy(() -> pdf.generiraj(d.getId())).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getReason()).contains("još nije podržan"));
        mvc.perform(get("/dokumenti/" + d.getId()).session(session))
                .andExpect(status().isOk()).andExpect(content().string(not(containsString("Generiraj PDF"))));
    }

    @ParameterizedTest
    @EnumSource(value = VrstaDokumenta.class, names = {"KRSNI_LIST", "POTVRDA_O_VJENCANJU"})
    void ostecenZapisBezIzvoraNeGeneriraPdf(VrstaDokumenta vrsta) throws Exception {
        // Simulira nepotpun stari zapis mimo servisne validacije izdavanja.
        var d = izdani(VrstaDokumenta.OSTALO, null, null);
        d.setVrstaDokumenta(vrsta); em.flush(); em.clear();
        mvc.perform(get("/dokumenti/" + d.getId() + "/pdf").session(prijavi(Uloga.ADMIN)))
                .andExpect(status().isConflict());
        assertThatThrownBy(() -> pdf.generiraj(d.getId())).isInstanceOfSatisfying(ResponseStatusException.class,
                e -> assertThat(e.getReason()).contains("nedostaje povezani zapis"));
    }

    @ParameterizedTest
    @EnumSource(Uloga.class)
    void sveTriUlogePreuzimajuObaPdfSaIspravnimZaglavljima(Uloga uloga) throws Exception {
        var session = prijavi(uloga);
        for (var d : new Dokument[]{krsniList(), izdani(VrstaDokumenta.POTVRDA_O_VJENCANJU, null, vjencanje(0))}) {
            byte[] odgovor = mvc.perform(get("/dokumenti/" + d.getId() + "/pdf").session(session))
                    .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andExpect(header().string("Content-Disposition", matchesPattern("attachment; filename=\"[A-Za-z0-9-]+\\.pdf\"")))
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andReturn().getResponse().getContentAsByteArray();
            assertThat(tekst(odgovor)).contains("Ana Đurić");
            mvc.perform(get("/dokumenti/" + d.getId()).session(session)).andExpect(status().isOk())
                    .andExpect(content().string(containsString("href=\"/dokumenti/" + d.getId() + "/pdf\"")));
        }
    }

    @Test
    void anonimniKorisnikNeMozePreuzetiPdf() throws Exception {
        var d = krsniList();
        mvc.perform(get("/dokumenti/" + d.getId() + "/pdf")).andExpect(redirectedUrl("/login"));
    }

    @Test
    void nepostojeciDokumentVraca404() throws Exception {
        mvc.perform(get("/dokumenti/0/pdf").session(prijavi(Uloga.ADMIN))).andExpect(status().isNotFound());
    }

    @Test
    void hrvatskaSlovaINepoznataOpcionalnaPolja() throws Exception {
        var d = krsniList(); var k = d.getKrstenje();
        k.setImeOca("č ć ž š đ Č Ć Ž Š Đ"); k.setImeMajke(null); k.setImeKuma(null); k.setImeKume(null);
        k.setKrstitelj(null); k.setBrojMatice(null); k.setGodinaMatice(null);
        k.getZupljanin().setMjestoRodjenja(null); em.flush();
        assertThat(tekst(pdf.generiraj(d.getId()).sadrzaj())).contains("č ć ž š đ Č Ć Ž Š Đ", "—").doesNotContain("null");
    }

    @Test
    void opcionalniPodaciVjencanjaMoguBitiPrazni() throws Exception {
        var v = vjencanje(0); v.setSvjedokMladozenje(null); v.setSvjedokNevjeste(null);
        v.setSluziteljVjencanja(null); v.setBrojMatice(null); v.setGodinaMatice(null); em.flush();
        var d = izdani(VrstaDokumenta.POTVRDA_O_VJENCANJU, null, v);
        assertThat(tekst(pdf.generiraj(d.getId()).sadrzaj())).contains("—").doesNotContain("null");
    }

    @Test
    void nazivDatotekeNeSadrziPutanjeNavodnikeNiKontrolneZnakove() throws Exception {
        var d = izdani(VrstaDokumenta.POTVRDA_O_VJENCANJU, null, vjencanje(0));
        d.setImePrimatelja("../Željko\r\n\"\\"); em.flush();
        assertThat(pdf.generiraj(d.getId()).nazivDatoteke()).matches("[A-Za-z0-9-]+\\.pdf");
    }

    @Test
    void konfiguracijaZupeKoristiSeUZaglavlju() throws Exception {
        var servis = new DokumentPdfService(dokumenti, "Župa test", "Ulica župe 12", "Čitluk");
        assertThat(tekst(servis.generiraj(krsniList().getId()).sadrzaj())).contains("Župa test", "Ulica župe 12", "Čitluk");
    }

    @Test
    void dugiPodaciPrelamajuSeBezGubitkaTeksta() throws Exception {
        var d = krsniList(); var k = d.getKrstenje();
        String dugo = "Ž".repeat(145) + "KRAJ";
        k.setMjestoKrstenja(dugo); k.setImeOca(dugo); k.setImeMajke(dugo); k.setImeKuma(dugo);
        k.setImeKume(dugo); k.setKrstitelj(dugo); k.getZupljanin().setMjestoRodjenja(dugo); em.flush();
        byte[] podaci = pdf.generiraj(d.getId()).sadrzaj();
        try (var otvoren = Loader.loadPDF(podaci)) {
            assertThat(otvoren.getNumberOfPages()).isGreaterThan(1);
            assertThat(new PDFTextStripper().getText(otvoren).replaceAll("\\s", "")).contains(dugo);
            for (int i = 0; i < otvoren.getNumberOfPages(); i++)
                assertThat(new PDFRenderer(otvoren).renderImage(i)).isNotNull();
        }
    }

    private String tekst(byte[] podaci) throws Exception {
        assertThat(podaci).isNotEmpty();
        assertThat(new String(podaci, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        try (var otvoren = Loader.loadPDF(podaci)) {
            assertThat(otvoren.getNumberOfPages()).isGreaterThan(0);
            for (var stranica : otvoren.getPages())
                for (var naziv : stranica.getResources().getFontNames())
                    assertThat(stranica.getResources().getFont(naziv).isEmbedded()).isTrue();
            return new PDFTextStripper().getText(otvoren);
        }
    }

    private void pregled(String naziv, byte[] podaci) throws Exception {
        try (var otvoren = Loader.loadPDF(podaci)) {
            var slika = new PDFRenderer(otvoren).renderImageWithDPI(0, 110);
            assertThat(slika.getWidth()).isGreaterThan(500);
            if (Boolean.getBoolean("pdf.preview")) {
                Path putanja = Path.of("target", "pdf-preview"); Files.createDirectories(putanja);
                Files.write(putanja.resolve(naziv + ".pdf"), podaci);
                ImageIO.write(slika, "png", putanja.resolve(naziv + ".png").toFile());
            }
        }
    }

    private Dokument krsniList() { return izdani(VrstaDokumenta.KRSNI_LIST, krstenje(), null); }

    private Dokument izdani(VrstaDokumenta vrsta, Krstenje k, Vjencanje v) {
        var f = new DokumentForma(); f.setVrstaDokumenta(vrsta); f.setStatus(StatusDokumenta.IZDAN);
        f.setDatumZahtjeva(LocalDate.of(2026, 9, 16)); f.setDatumIzdavanja(LocalDate.of(2026, 9, 17));
        f.setImePrimatelja("Željko"); f.setPrezimePrimatelja("Čović");
        if (k != null) f.setKrstenjeId(k.getId());
        if (v != null) f.setVjencanjeId(v.getId());
        return dokumenti.dodaj(f, korisnik(Uloga.ADMIN).getEmail());
    }

    private Krstenje krstenje() {
        var k = new Krstenje(); k.setZupljanin(osoba()); k.setDatumKrstenja(LocalDate.of(1990, 4, 4));
        k.setMjestoKrstenja("Široki Brijeg"); k.setImeOca("Otac Željko"); k.setImeMajke("Majka Đurđa");
        k.setImeKuma("Kum Ćiro"); k.setImeKume("Kuma Štefica"); k.setKrstitelj("Krstitelj Čović");
        k.setBrojMatice("Č-42"); k.setGodinaMatice(1990); em.persist(k); em.flush(); return k;
    }

    private Vjencanje vjencanje(int veze) {
        var v = new Vjencanje(); v.setImeMladozenje("Željko"); v.setPrezimeMladozenje("Čović");
        v.setImeNevjeste("Đurđa"); v.setPrezimeNevjeste("Šimić");
        if ((veze & 1) != 0) v.setMladozenjaZupljanin(osoba());
        if ((veze & 2) != 0) v.setNevjestaZupljanin(osoba());
        v.setDatumVjencanja(LocalDate.of(2020, 6, 6)); v.setMjestoVjencanja("Čitluk");
        v.setSvjedokMladozenje("Svjedok Ćosić"); v.setSvjedokNevjeste("Svjedokinja Žilić");
        v.setSluziteljVjencanja("Služitelj Šarić"); v.setBrojMatice("V-15"); v.setGodinaMatice(2020);
        em.persist(v); em.flush(); return v;
    }

    private Zupljanin osoba() {
        var z = new Zupljanin(); z.setIme("Čedomir"); z.setPrezime("Šimić");
        z.setDatumRodjenja(LocalDate.of(1990, 3, 2)); z.setMjestoRodjenja("Đakovo");
        em.persist(z); em.flush(); return z;
    }

    private long brojOsoba() { return em.createQuery("select count(z) from Zupljanin z", Long.class).getSingleResult(); }

    private Korisnik korisnik(Uloga uloga) {
        var f = new KorisnikForma(); f.setIme("Ana"); f.setPrezime("Đurić"); f.setUloga(uloga);
        f.setEmail(UUID.randomUUID() + "@primjer.hr"); f.setLozinka("TestnaLozinka123!"); return korisnici.dodaj(f);
    }

    private MockHttpSession prijavi(Uloga uloga) throws Exception {
        var k = korisnik(uloga);
        return (MockHttpSession) mvc.perform(formLogin("/login").user("email", k.getEmail()).password("lozinka", "TestnaLozinka123!"))
                .andExpect(authenticated()).andReturn().getRequest().getSession(false);
    }
}
