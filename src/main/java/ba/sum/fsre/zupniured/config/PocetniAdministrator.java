package ba.sum.fsre.zupniured.config;

import ba.sum.fsre.zupniured.dto.KorisnikForma;
import ba.sum.fsre.zupniured.service.KorisnikService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.pocetni-admin.enabled", havingValue = "true")
public class PocetniAdministrator implements ApplicationRunner {

    private final KorisnikService korisnikService;
    private final KorisnikForma forma;

    public PocetniAdministrator(KorisnikService korisnikService,
                               @Value("${app.pocetni-admin.ime:}") String ime,
                               @Value("${app.pocetni-admin.prezime:}") String prezime,
                               @Value("${app.pocetni-admin.email:}") String email,
                               @Value("${app.pocetni-admin.lozinka:}") String lozinka) {
        this.korisnikService = korisnikService;
        this.forma = new KorisnikForma();
        forma.setIme(ime);
        forma.setPrezime(prezime);
        forma.setEmail(email);
        forma.setLozinka(lozinka);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            korisnikService.kreirajPocetnogAdministratora(forma);
        } finally {
            forma.setLozinka("");
        }
    }
}
