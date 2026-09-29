package ba.sum.fsre.zupniured;

import ba.sum.fsre.zupniured.dto.PrvaPricestForma;
import ba.sum.fsre.zupniured.model.PrvaPricest;
import ba.sum.fsre.zupniured.model.Zupljanin;
import ba.sum.fsre.zupniured.repository.PrvaPricestRepository;
import ba.sum.fsre.zupniured.repository.ZupljaninRepository;
import ba.sum.fsre.zupniured.service.PrvaPricestPodaciException;
import ba.sum.fsre.zupniured.service.PrvaPricestService;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrvaPricestSpremanjeTests {

    @Mock
    private PrvaPricestRepository repository;
    @Mock
    private ZupljaninRepository zupljaninRepository;
    @Mock
    private Validator validator;

    private PrvaPricestService servis;
    private PrvaPricestForma forma;

    @BeforeEach
    void pripremi() {
        servis = new PrvaPricestService(repository, zupljaninRepository, validator);
        forma = new PrvaPricestForma();
        forma.setZupljaninId(1L);
        forma.setDatumPricesti(LocalDate.of(2000, 5, 12));
        forma.setMjestoPricesti("Mostar");
        Zupljanin zupljanin = mock(Zupljanin.class);
        when(zupljanin.getId()).thenReturn(1L);
        when(zupljaninRepository.findById(1L)).thenReturn(Optional.of(zupljanin));
        when(validator.validate(forma)).thenReturn(Set.of());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void duplikatOtkrivenTekPriSpremanjuImaRazumljivuPoruku(boolean uredjivanje) {
        pripremiUredjivanje(uredjivanje);
        var uzrok = new org.hibernate.exception.ConstraintViolationException(
                "Duplikat", new SQLException("Duplikat", "23505"), "uk_prve_pricesti_zupljanin");
        when(repository.saveAndFlush(any(PrvaPricest.class)))
                .thenThrow(new DataIntegrityViolationException("Spremanje nije uspjelo", uzrok));

        assertThatThrownBy(() -> spremi(uredjivanje))
                .isInstanceOf(PrvaPricestPodaciException.class)
                .hasMessage("Odabrani župljanin već ima evidentiranu prvu pričest.");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void drugaOgranicenjaBazeNePrikazujeKaoDuplikatPricesti(boolean uredjivanje) {
        pripremiUredjivanje(uredjivanje);
        var uzrok = new org.hibernate.exception.ConstraintViolationException(
                "Drugo ograničenje", new SQLException("Strani ključ", "23503"), "drugo_ogranicenje");
        var greska = new DataIntegrityViolationException("Spremanje nije uspjelo", uzrok);
        when(repository.saveAndFlush(any(PrvaPricest.class))).thenThrow(greska);

        assertThatThrownBy(() -> spremi(uredjivanje)).isSameAs(greska);
    }

    private void pripremiUredjivanje(boolean uredjivanje) {
        if (uredjivanje) {
            when(repository.findById(2L)).thenReturn(Optional.of(new PrvaPricest()));
        }
    }

    private void spremi(boolean uredjivanje) {
        if (uredjivanje) {
            servis.uredi(2L, forma);
        } else {
            servis.dodaj(forma);
        }
    }
}
