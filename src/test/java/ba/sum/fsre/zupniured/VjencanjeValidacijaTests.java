package ba.sum.fsre.zupniured;

import ba.sum.fsre.zupniured.dto.VjencanjeForma;
import ba.sum.fsre.zupniured.model.Vjencanje;
import jakarta.validation.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class VjencanjeValidacijaTests {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void zatvori() { FACTORY.close(); }

    @ParameterizedTest
    @CsvSource({"false,false", "true,false", "false,true", "true,true"})
    void imenaSuObaveznaSamoZaNepovezaneOsobe(boolean mladozenja, boolean nevjesta) {
        VjencanjeForma f = osnovna();
        if (mladozenja) f.setMladozenjaZupljaninId(1L);
        if (nevjesta) f.setNevjestaZupljaninId(2L);
        var polja = VALIDATOR.validate(f).stream().map(g -> g.getPropertyPath().toString()).toList();
        assertThat(polja).hasSize((mladozenja ? 0 : 2) + (nevjesta ? 0 : 2));
        if (!mladozenja) assertThat(polja).contains("imeMladozenje", "prezimeMladozenje");
        if (!nevjesta) assertThat(polja).contains("imeNevjeste", "prezimeNevjeste");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void praznaRucnaImenaNisuDozvoljena(String tekst) {
        VjencanjeForma f = osnovna();
        f.setImeMladozenje(tekst); f.setPrezimeMladozenje(tekst);
        f.setImeNevjeste(tekst); f.setPrezimeNevjeste(tekst);
        assertThat(VALIDATOR.validate(f)).extracting(g -> g.getPropertyPath().toString())
                .containsExactlyInAnyOrder("imeMladozenje", "prezimeMladozenje", "imeNevjeste", "prezimeNevjeste");
    }

    @Test
    void datumIMjestoSuObavezni() {
        VjencanjeForma f = povezani();
        f.setDatumVjencanja(null); f.setMjestoVjencanja(" ");
        assertThat(VALIDATOR.validate(f)).extracting(g -> g.getPropertyPath().toString())
                .containsExactlyInAnyOrder("datumVjencanja", "mjestoVjencanja");
    }

    @Test
    void datumNeSmijeBitiUBuducnosti() {
        VjencanjeForma f = povezani();
        f.setDatumVjencanja(LocalDate.now().plusDays(1));
        assertThat(VALIDATOR.validate(f)).extracting(g -> g.getPropertyPath().toString())
                .containsExactly("datumVjencanja");
    }

    @Test
    void prosliIDanasnjiDatumSuDozvoljeniAOstalaPoljaOpcionalna() {
        VjencanjeForma f = povezani();
        for (LocalDate datum : new LocalDate[]{LocalDate.now(), LocalDate.of(1900, 1, 1)}) {
            f.setDatumVjencanja(datum);
            assertThat(VALIDATOR.validate(f)).isEmpty();
        }
    }

    @Test
    void istaOsobaNijeDozvoljena() {
        VjencanjeForma f = povezani();
        f.setNevjestaZupljaninId(1L);
        assertThat(VALIDATOR.validate(f)).extracting(g -> g.getPropertyPath().toString())
                .containsExactly("nevjestaZupljaninId");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 10000})
    void godinaAkoJeUnesenaMoraBitiValjana(int godina) {
        VjencanjeForma f = povezani(); f.setGodinaMatice(godina);
        assertThat(VALIDATOR.validate(f)).extracting(g -> g.getPropertyPath().toString())
                .containsExactly("godinaMatice");
    }

    @Test
    void predugiTekstoviSeOdbijaju() {
        VjencanjeForma f = osnovna();
        f.setImeMladozenje("a".repeat(101)); f.setPrezimeMladozenje("a".repeat(101));
        f.setImeNevjeste("a".repeat(101)); f.setPrezimeNevjeste("a".repeat(101));
        f.setMjestoVjencanja("a".repeat(151));
        f.setSvjedokMladozenje("a".repeat(151)); f.setSvjedokNevjeste("a".repeat(151));
        f.setSluziteljVjencanja("a".repeat(151));
        f.setBrojMatice("a".repeat(51)); f.setNapomena("a".repeat(2001));
        assertThat(VALIDATOR.validate(f)).extracting(g -> g.getPropertyPath().toString())
                .containsExactlyInAnyOrder("imeMladozenje", "prezimeMladozenje", "imeNevjeste", "prezimeNevjeste",
                        "mjestoVjencanja", "svjedokMladozenje", "svjedokNevjeste", "sluziteljVjencanja", "brojMatice", "napomena");
    }

    @Test
    void entitetUvijekZahtijevaPovijesnaImenaAliNeIVeze() {
        Vjencanje v = new Vjencanje();
        v.setDatumVjencanja(LocalDate.now()); v.setMjestoVjencanja("Mostar");
        assertThat(VALIDATOR.validate(v)).extracting(g -> g.getPropertyPath().toString())
                .containsExactlyInAnyOrder("imeMladozenje", "prezimeMladozenje", "imeNevjeste", "prezimeNevjeste");
        v.setImeMladozenje("Ivan"); v.setPrezimeMladozenje("Ivić");
        v.setImeNevjeste("Ana"); v.setPrezimeNevjeste("Anić");
        assertThat(VALIDATOR.validate(v)).isEmpty();
    }

    private VjencanjeForma osnovna() {
        VjencanjeForma f = new VjencanjeForma();
        f.setDatumVjencanja(LocalDate.now()); f.setMjestoVjencanja("Mostar");
        return f;
    }
    private VjencanjeForma povezani() {
        VjencanjeForma f = osnovna();
        f.setMladozenjaZupljaninId(1L); f.setNevjestaZupljaninId(2L);
        return f;
    }
}
