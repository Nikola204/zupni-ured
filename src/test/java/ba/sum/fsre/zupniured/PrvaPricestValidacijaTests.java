package ba.sum.fsre.zupniured;

import ba.sum.fsre.zupniured.dto.PrvaPricestForma;
import ba.sum.fsre.zupniured.model.PrvaPricest;
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

class PrvaPricestValidacijaTests {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void zatvoriValidator() {
        FACTORY.close();
    }

    @Test
    void zupljaninDatumIMjestoSuObavezniIUNaEntitetuIUNaFormi() {
        assertThat(VALIDATOR.validate(new PrvaPricest())).extracting(g -> g.getPropertyPath().toString())
                .containsExactlyInAnyOrder("zupljanin", "datumPricesti", "mjestoPricesti");
        assertThat(VALIDATOR.validate(new PrvaPricestForma())).extracting(g -> g.getPropertyPath().toString())
                .containsExactlyInAnyOrder("zupljaninId", "datumPricesti", "mjestoPricesti");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void mjestoNeMozeBitiPrazno(String mjesto) {
        PrvaPricest zapis = ispravan();
        zapis.setMjestoPricesti(mjesto);
        assertThat(VALIDATOR.validate(zapis)).extracting(g -> g.getPropertyPath().toString())
                .containsExactly("mjestoPricesti");
    }

    @Test
    void buduciDatumSeOdbijaNaEntitetuIFormi() {
        PrvaPricest zapis = ispravan();
        zapis.setDatumPricesti(LocalDate.now().plusDays(1));
        assertThat(VALIDATOR.validate(zapis)).extracting(g -> g.getPropertyPath().toString())
                .containsExactly("datumPricesti");
        PrvaPricestForma forma = new PrvaPricestForma();
        forma.setZupljaninId(1L);
        forma.setMjestoPricesti("Mostar");
        forma.setDatumPricesti(zapis.getDatumPricesti());
        assertThat(VALIDATOR.validate(forma)).extracting(g -> g.getPropertyPath().toString())
                .containsExactly("datumPricesti");
    }

    @Test
    void prosliIDanasnjiDatumSuDozvoljeniAMaticaJeOpcionalna() {
        PrvaPricest zapis = ispravan();
        for (LocalDate datum : new LocalDate[]{LocalDate.now(), LocalDate.of(1900, 1, 1)}) {
            zapis.setDatumPricesti(datum);
            assertThat(VALIDATOR.validate(zapis)).isEmpty();
        }
    }

    @Test
    void duljinePoljaOdgovarajuOgranicenjimaBaze() {
        PrvaPricest zapis = ispravan();
        zapis.setMjestoPricesti("a".repeat(151));
        zapis.setBrojMatice("a".repeat(51));
        zapis.setNapomena("a".repeat(2001));
        assertThat(VALIDATOR.validate(zapis)).extracting(g -> g.getPropertyPath().toString())
                .containsExactlyInAnyOrder("mjestoPricesti", "brojMatice", "napomena");
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 10000})
    void unesenaGodinaMoraBitiValjana(int godina) {
        PrvaPricest zapis = ispravan();
        zapis.setGodinaMatice(godina);
        assertThat(VALIDATOR.validate(zapis)).extracting(g -> g.getPropertyPath().toString())
                .containsExactly("godinaMatice");
    }

    private PrvaPricest ispravan() {
        PrvaPricest zapis = new PrvaPricest();
        zapis.setZupljanin(new Zupljanin());
        zapis.setDatumPricesti(LocalDate.now());
        zapis.setMjestoPricesti("Mostar");
        return zapis;
    }
}
