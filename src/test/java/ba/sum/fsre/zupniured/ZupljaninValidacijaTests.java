package ba.sum.fsre.zupniured;

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

class ZupljaninValidacijaTests {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void zatvoriValidator() {
        FACTORY.close();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void imeIPrezimeSuObavezni(String vrijednost) {
        Zupljanin zupljanin = new Zupljanin();
        zupljanin.setIme(vrijednost);
        zupljanin.setPrezime(vrijednost);
        assertThat(VALIDATOR.validate(zupljanin)).extracting(greska -> greska.getPropertyPath().toString())
                .containsExactlyInAnyOrder("ime", "prezime");
    }

    @Test
    void datumRodjenjaNeMozeBitiUBuducnosti() {
        Zupljanin zupljanin = ispravan();
        zupljanin.setDatumRodjenja(LocalDate.now().plusDays(1));
        assertThat(VALIDATOR.validate(zupljanin)).extracting(greska -> greska.getPropertyPath().toString())
                .containsExactly("datumRodjenja");
    }

    @Test
    void datumRodjenjaMozeBitiDanasUProslostiIliNeunesen() {
        Zupljanin zupljanin = ispravan();
        for (LocalDate datum : new LocalDate[]{null, LocalDate.now(), LocalDate.of(1990, 1, 1)}) {
            zupljanin.setDatumRodjenja(datum);
            assertThat(VALIDATOR.validate(zupljanin)).isEmpty();
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "ana@primjer.hr"})
    void emailJeNeobavezanAliPrihvacaIspravnuAdresu(String email) {
        Zupljanin zupljanin = ispravan();
        zupljanin.setEmail(email);
        assertThat(VALIDATOR.validate(zupljanin)).isEmpty();
    }

    @Test
    void neispravanEmailSeOdbija() {
        Zupljanin zupljanin = ispravan();
        zupljanin.setEmail("neispravno");
        assertThat(VALIDATOR.validate(zupljanin)).extracting(greska -> greska.getPropertyPath().toString())
                .containsExactly("email");
    }

    private Zupljanin ispravan() {
        Zupljanin zupljanin = new Zupljanin();
        zupljanin.setIme("Ana");
        zupljanin.setPrezime("Anić");
        return zupljanin;
    }
}
