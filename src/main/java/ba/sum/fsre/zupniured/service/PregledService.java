package ba.sum.fsre.zupniured.service;

import ba.sum.fsre.zupniured.dto.IzgledPrikaz;
import ba.sum.fsre.zupniured.dto.StatistikaPrikaz;
import ba.sum.fsre.zupniured.model.StatusDokumenta;
import ba.sum.fsre.zupniured.model.Uloga;
import ba.sum.fsre.zupniured.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PregledService {
    private final KorisnikRepository korisnici;
    private final ZupljaninRepository zupljani;
    private final KrstenjeRepository krstenja;
    private final PrvaPricestRepository pricesti;
    private final KrizmaRepository krizme;
    private final VjencanjeRepository vjencanja;
    private final MisnaNakanaRepository nakane;
    private final DokumentRepository dokumenti;
    private final ObavijestRepository obavijesti;

    public PregledService(KorisnikRepository korisnici, ZupljaninRepository zupljani,
                          KrstenjeRepository krstenja, PrvaPricestRepository pricesti,
                          KrizmaRepository krizme, VjencanjeRepository vjencanja,
                          MisnaNakanaRepository nakane, DokumentRepository dokumenti,
                          ObavijestRepository obavijesti) {
        this.korisnici = korisnici; this.zupljani = zupljani; this.krstenja = krstenja;
        this.pricesti = pricesti; this.krizme = krizme; this.vjencanja = vjencanja;
        this.nakane = nakane; this.dokumenti = dokumenti; this.obavijesti = obavijesti;
    }

    public IzgledPrikaz izgled(String email, String putanja) {
        if (email == null) return new IzgledPrikaz(false, false, false, "", "", putanja);
        return korisnici.findByEmailIgnoreCase(email).filter(k -> k.isAktivan()).map(k -> {
            String uloga = switch (k.getUloga()) {
                case ADMIN -> "Administrator";
                case ZUPNIK -> "Župnik";
                case DJELATNIK -> "Djelatnik";
            };
            boolean interni = !putanja.equals("/login") && !putanja.startsWith("/obavijesti");
            return new IzgledPrikaz(true, interni, k.getUloga() == Uloga.ADMIN,
                    k.getIme() + " " + k.getPrezime(), uloga, putanja);
        }).orElse(new IzgledPrikaz(false, false, false, "", "", putanja));
    }

    public List<StatistikaPrikaz> statistike() {
        return List.of(
            new StatistikaPrikaz("Aktivni župljani", zupljani.countByAktivanTrue(), "/zupljani"),
            new StatistikaPrikaz("Krštenja", krstenja.count(), "/krstenja"),
            new StatistikaPrikaz("Prve pričesti", pricesti.count(), "/prva-pricest"),
            new StatistikaPrikaz("Krizme", krizme.count(), "/krizma"),
            new StatistikaPrikaz("Vjenčanja", vjencanja.count(), "/vjencanja"),
            new StatistikaPrikaz("Misne nakane", nakane.count(), "/misne-nakane"),
            new StatistikaPrikaz("Aktivne obavijesti",
                    obavijesti.count((root, query, cb) -> cb.isTrue(root.get("aktivna"))), "/admin/obavijesti"),
            new StatistikaPrikaz("Dokumenti na čekanju",
                    dokumenti.count((root, query, cb) -> root.get("status")
                            .in(StatusDokumenta.ZAPRIMLJEN, StatusDokumenta.U_OBRADI)), "/dokumenti"));
    }
}
