package ba.sum.fsre.zupniured;

import ba.sum.fsre.zupniured.dto.KorisnikForma;
import ba.sum.fsre.zupniured.model.Korisnik;
import ba.sum.fsre.zupniured.model.Uloga;
import ba.sum.fsre.zupniured.repository.KorisnikRepository;
import ba.sum.fsre.zupniured.service.KorisnikPodaciException;
import ba.sum.fsre.zupniured.service.KorisnikService;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KorisnikServiceTests {

    private KorisnikRepository repository;
    private KorisnikService servis;
    private ValidatorFactory validatorFactory;

    @BeforeEach
    void pripremi() {
        repository = mock(KorisnikRepository.class);
        validatorFactory = Validation.buildDefaultValidatorFactory();
        servis = new KorisnikService(repository, new BCryptPasswordEncoder(), validatorFactory.getValidator());
    }

    @AfterEach
    void zatvori() {
        validatorFactory.close();
    }

    @Test
    void noviKorisnikMoraImatiLozinku() {
        KorisnikForma forma = forma();
        forma.setLozinka("");
        assertThatThrownBy(() -> servis.dodaj(forma)).isInstanceOf(KorisnikPodaciException.class)
                .hasMessage("Lozinka je obavezna.");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void bcryptOgranicenjeProvjeravaBajtoveVisebajtnihZnakova() {
        KorisnikForma forma = forma();
        forma.setLozinka("č".repeat(37));
        assertThatThrownBy(() -> servis.dodaj(forma)).isInstanceOf(KorisnikPodaciException.class)
                .hasMessageContaining("72 UTF-8 bajta");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void duplikatEmailaNeSpremaKorisnika() {
        KorisnikForma forma = forma();
        when(repository.existsByEmailIgnoreCase(forma.getEmail())).thenReturn(true);
        assertThatThrownBy(() -> servis.dodaj(forma)).isInstanceOf(KorisnikPodaciException.class)
                .hasMessageContaining("već postoji");
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void prviAdministratorNastajeSamoUPraznojTablici() {
        when(repository.count()).thenReturn(0L);
        when(repository.saveAndFlush(any(Korisnik.class))).thenAnswer(invocation -> {
            Korisnik korisnik = invocation.getArgument(0);
            assertThat(korisnik.getUloga()).isEqualTo(Uloga.ADMIN);
            assertThat(korisnik.isAktivan()).isTrue();
            assertThat(new BCryptPasswordEncoder().matches("TestnaLozinka123!", korisnik.getLozinka())).isTrue();
            return korisnik;
        });
        servis.kreirajPocetnogAdministratora(forma());
        verify(repository).saveAndFlush(any(Korisnik.class));
    }

    @Test
    void inicijalizacijaNePrepisujePostojeceRacune() {
        when(repository.count()).thenReturn(1L);
        servis.kreirajPocetnogAdministratora(forma());
        verify(repository, never()).saveAndFlush(any());
    }

    private KorisnikForma forma() {
        KorisnikForma forma = new KorisnikForma();
        forma.setIme("Ana");
        forma.setPrezime("Anić");
        forma.setEmail("ana@primjer.hr");
        forma.setLozinka("TestnaLozinka123!");
        forma.setUloga(Uloga.DJELATNIK);
        return forma;
    }
}
