package ba.sum.fsre.zupniured;

import ba.sum.fsre.zupniured.dto.KrstenjeForma;
import ba.sum.fsre.zupniured.model.Krstenje;
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

class KrstenjeValidacijaTests {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void zatvoriValidator() {
        FACTORY.close();
    }

    @Test
    void zupljaninDatumIMjestoSuObavezniIUNaEntitetuIUNaFormi() {
        assertThat(VALIDATOR.validate(new Krstenje())).extracting(g -> g.getPropertyPath().toString())
                .containsExactlyInAnyOrder("zupljanin", "datumKrstenja", "mjestoKrstenja");
        assertThat(VALIDATOR.validate(new KrstenjeForma())).extracting(g -> g.getPropertyPath().toString())
                .containsExactlyInAnyOrder("zupljaninId", "datumKrstenja", "mjestoKrstenja");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void mjestoNeMozeBitiPrazno(String mjesto) {
        Krstenje zapis = ispravan();
        zapis.setMjestoKrstenja(mjesto);
        assertThat(VALIDATOR.validate(zapis)).extracting(g -> g.getPropertyPath().toString())
                .containsExactly("mjestoKrstenja");
    }

    @Test
    void buduciDatumSeOdbijaNaEntitetuIFormi() {
        Krstenje zapis = ispravan();
        zapis.setDatumKrstenja(LocalDate.now().plusDays(1));
        assertThat(VALIDATOR.validate(zapis)).extracting(g -> g.getPropertyPath().toString())
                .containsExactly("datumKrstenja");
        KrstenjeForma forma = new KrstenjeForma();
        forma.setZupljaninId(1L);
        forma.setMjestoKrstenja("Mostar");
        forma.setDatumKrstenja(zapis.getDatumKrstenja());
        assertThat(VALIDATOR.validate(forma)).extracting(g -> g.getPropertyPath().toString())
                .containsExactly("datumKrstenja");
    }

    @Test
    void prosliIDanasnjiDatumSuDozvoljeniAMaticaJeOpcionalna() {
        Krstenje zapis = ispravan();
        for (LocalDate datum : new LocalDate[]{LocalDate.now(), LocalDate.of(1900, 1, 1)}) {
            zapis.setDatumKrstenja(datum);
            assertThat(VALIDATOR.validate(zapis)).isEmpty();
        }
    }

    @Test
    void duljinePoljaOdgovarajuOgranicenjimaBaze() {
        Krstenje zapis = ispravan();
        zapis.setMjestoKrstenja("a".repeat(151));
        zapis.setImeOca("a".repeat(151));
        zapis.setImeMajke("a".repeat(151));
        zapis.setImeKuma("a".repeat(151));
        zapis.setImeKume("a".repeat(151));
        zapis.setKrstitelj("a".repeat(151));
        zapis.setBrojMatice("a".repeat(51));
        zapis.setNapomena("a".repeat(2001));
        assertThat(VALIDATOR.validate(zapis)).extracting(g -> g.getPropertyPath().toString())
                .containsExactlyInAnyOrder("mjestoKrstenja", "imeOca", "imeMajke", "imeKuma",
                        "imeKume", "krstitelj", "brojMatice", "napomena");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 10000})
    void unesenaGodinaMoraBitiValjana(int godina) {
        Krstenje zapis = ispravan();
        zapis.setGodinaMatice(godina);
        assertThat(VALIDATOR.validate(zapis)).extracting(g -> g.getPropertyPath().toString())
                .containsExactly("godinaMatice");
    }

    private Krstenje ispravan() {
        Krstenje zapis = new Krstenje();
        zapis.setZupljanin(new Zupljanin());
        zapis.setDatumKrstenja(LocalDate.now());
        zapis.setMjestoKrstenja("Mostar");
        return zapis;
    }
}

