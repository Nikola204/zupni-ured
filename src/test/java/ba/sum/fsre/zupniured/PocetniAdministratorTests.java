package ba.sum.fsre.zupniured;

import ba.sum.fsre.zupniured.config.PocetniAdministrator;
import ba.sum.fsre.zupniured.dto.KorisnikForma;
import ba.sum.fsre.zupniured.service.KorisnikService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class PocetniAdministratorTests {

    @Test
    void inicijalizacijaJeZadanoIskljucena() {
        new ApplicationContextRunner()
                .withUserConfiguration(PocetniAdministrator.class)
                .run(context -> assertThat(context).doesNotHaveBean(PocetniAdministrator.class));
    }

    @Test
    void inicijalizacijaCitaDokumentiraneVarijableOkruzenja() {
        KorisnikService servis = mock(KorisnikService.class);
        doAnswer(invocation -> {
            KorisnikForma forma = invocation.getArgument(0);
            assertThat(forma.getEmail()).isEqualTo("admin@primjer.hr");
            assertThat(forma.getIme()).isEqualTo("Ana");
            assertThat(forma.getPrezime()).isEqualTo("Anić");
            assertThat(forma.getLozinka()).isEqualTo("TestnaLozinka123!");
            return null;
        }).when(servis).kreirajPocetnogAdministratora(any(KorisnikForma.class));

        new ApplicationContextRunner()
                .withUserConfiguration(PocetniAdministrator.class)
                .withBean(KorisnikService.class, () -> servis)
                .withInitializer(context -> {
                    context.getEnvironment().getPropertySources().addFirst(
                            new SystemEnvironmentPropertySource("test-systemEnvironment", Map.of(
                                    "APP_POCETNIADMIN_ENABLED", "true",
                                    "APP_POCETNIADMIN_IME", "Ana",
                                    "APP_POCETNIADMIN_PREZIME", "Anić",
                                    "APP_POCETNIADMIN_EMAIL", "ADMIN@primjer.hr",
                                    "APP_POCETNIADMIN_LOZINKA", "TestnaLozinka123!")));
                    ConfigurationPropertySources.attach(context.getEnvironment());
                })
                .run(context -> {
                    assertThat(context).hasSingleBean(PocetniAdministrator.class);
                    context.getBean(PocetniAdministrator.class).run(new DefaultApplicationArguments());
                    verify(servis).kreirajPocetnogAdministratora(argThat(forma -> forma.getLozinka().isEmpty()));
                });
    }
}
