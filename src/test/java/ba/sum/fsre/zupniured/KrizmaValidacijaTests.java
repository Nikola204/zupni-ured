package ba.sum.fsre.zupniured;

import ba.sum.fsre.zupniured.dto.KrizmaForma;
import ba.sum.fsre.zupniured.model.Krizma;
import ba.sum.fsre.zupniured.model.Zupljanin;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class KrizmaValidacijaTests {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void zatvoriValidator() {
        FACTORY.close();
    }

    @Test
    void zupljaninDatumIMjestoSuObavezniIUNaEntitetuIUNaFormi() {
        assertThat(VALIDATOR.validate(new Krizma())).extracting(g -> g.getPropertyPath().toString())
                .containsExactlyInAnyOrder("zupljanin", "datumKrizme", "mjestoKrizme");
        assertThat(VALIDATOR.validate(new KrizmaForma())).extracting(g -> g.getPropertyPath().toString())
                .containsExactlyInAnyOrder("zupljaninId", "datumKrizme", "mjestoKrizme");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void mjestoNeMozeBitiPrazno(String mjesto) {
        Krizma zapis = ispravan();
        zapis.setMjestoKrizme(mjesto);
        assertThat(VALIDATOR.validate(zapis)).extracting(g -> g.getPropertyPath().toString())
                .containsExactly("mjestoKrizme");
    }

    @Test
    void buduciDatumSeOdbijaNaEntitetuIFormi() {
        Krizma zapis = ispravan();
        zapis.setDatumKrizme(LocalDate.now().plusDays(1));
        assertThat(VALIDATOR.validate(zapis)).extracting(g -> g.getPropertyPath().toString())
                .containsExactly("datumKrizme");
        KrizmaForma forma = new KrizmaForma();
        forma.setZupljaninId(1L);
        forma.setMjestoKrizme("Mostar");
        forma.setDatumKrizme(zapis.getDatumKrizme());
        assertThat(VALIDATOR.validate(forma)).extracting(g -> g.getPropertyPath().toString())
                .containsExactly("datumKrizme");
    }

    @Test
    void prosliIDanasnjiDatumSuDozvoljeniAMaticaJeOpcionalna() {
        Krizma zapis = ispravan();
        for (LocalDate datum : new LocalDate[]{LocalDate.now(), LocalDate.of(1900, 1, 1)}) {
            zapis.setDatumKrizme(datum);
            assertThat(VALIDATOR.validate(zapis)).isEmpty();
        }
    }

    @Test
    void duljinePoljaOdgovarajuOgranicenjimaBaze() {
        Krizma zapis = ispravan();
        zapis.setMjestoKrizme("a".repeat(151));
        zapis.setImeKrizmanogKuma("a".repeat(151));
        zapis.setKrizmatelj("a".repeat(151));
        zapis.setBrojMatice("a".repeat(51));
        zapis.setNapomena("a".repeat(2001));
        assertThat(VALIDATOR.validate(zapis)).extracting(g -> g.getPropertyPath().toString())
                .containsExactlyInAnyOrder("mjestoKrizme", "imeKrizmanogKuma", "krizmatelj", "brojMatice", "napomena");
        KrizmaForma forma = new KrizmaForma();
        forma.setZupljaninId(1L);
        forma.setDatumKrizme(LocalDate.now());
        forma.setMjestoKrizme("Mostar");
        forma.setImeKrizmanogKuma(zapis.getImeKrizmanogKuma());
        forma.setKrizmatelj(zapis.getKrizmatelj());
        assertThat(VALIDATOR.validate(forma)).extracting(g -> g.getPropertyPath().toString())
                .containsExactlyInAnyOrder("imeKrizmanogKuma", "krizmatelj");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "Ivan Ivić"})
    void kumIKrizmateljSuOpcionalni(String vrijednost) {
        Krizma zapis = ispravan();
        zapis.setImeKrizmanogKuma(vrijednost);
        zapis.setKrizmatelj(vrijednost);
        assertThat(VALIDATOR.validate(zapis)).isEmpty();
        KrizmaForma forma = new KrizmaForma();
        forma.setZupljaninId(1L);
        forma.setDatumKrizme(LocalDate.now());
        forma.setMjestoKrizme("Mostar");
        forma.setImeKrizmanogKuma(vrijednost);
        forma.setKrizmatelj(vrijednost);
        assertThat(VALIDATOR.validate(forma)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 10000})
    void unesenaGodinaMoraBitiValjana(int godina) {
        Krizma zapis = ispravan();
        zapis.setGodinaMatice(godina);
        assertThat(VALIDATOR.validate(zapis)).extracting(g -> g.getPropertyPath().toString())
                .containsExactly("godinaMatice");
    }

    private Krizma ispravan() {
        Krizma zapis = new Krizma();
        zapis.setZupljanin(new Zupljanin());
        zapis.setDatumKrizme(LocalDate.now());
        zapis.setMjestoKrizme("Mostar");
        return zapis;
    }
}
