package ba.sum.fsre.zupniured.service;

import ba.sum.fsre.zupniured.dto.ZupljaninProfil;
import ba.sum.fsre.zupniured.dto.ZupljaninProfil.Sakrament;
import ba.sum.fsre.zupniured.dto.ZupljaninProfil.VjencanjePregled;
import ba.sum.fsre.zupniured.model.Zupljanin;
import ba.sum.fsre.zupniured.repository.KrstenjeRepository;
import ba.sum.fsre.zupniured.repository.PrvaPricestRepository;
import ba.sum.fsre.zupniured.repository.KrizmaRepository;
import ba.sum.fsre.zupniured.repository.VjencanjeRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ZupljaninProfilService {
    private final ZupljaninService zupljani;
    private final KrstenjeRepository krstenja;
    private final PrvaPricestRepository pricesti;
    private final KrizmaRepository krizme;
    private final VjencanjeRepository vjencanja;
    private final DokumentService dokumenti;

    public ZupljaninProfilService(ZupljaninService zupljani, KrstenjeRepository krstenja,
                                 PrvaPricestRepository pricesti, KrizmaRepository krizme,
                                 VjencanjeRepository vjencanja, DokumentService dokumenti) {
        this.zupljani = zupljani;
        this.krstenja = krstenja;
        this.pricesti = pricesti;
        this.krizme = krizme;
        this.vjencanja = vjencanja;
        this.dokumenti = dokumenti;
    }

    public ZupljaninProfil pronadjiPoId(Long id) {
        Zupljanin zupljanin = zupljani.pronadjiPoId(id);
        Sakrament krstenje = krstenja.findByZupljaninId(id)
                .map(k -> new Sakrament("Krštenje", "Evidentirano", k.getDatumKrstenja(),
                        k.getMjestoKrstenja(), matica(k.getBrojMatice(), k.getGodinaMatice()),
                        "/krstenja/" + k.getId()))
                .orElse(new Sakrament("Krštenje", "Nije evidentirano", null, null, null, null));
        Sakrament pricest = pricesti.findByZupljaninId(id)
                .map(p -> new Sakrament("Prva pričest", "Evidentirana", p.getDatumPricesti(),
                        p.getMjestoPricesti(), matica(p.getBrojMatice(), p.getGodinaMatice()),
                        "/prva-pricest/" + p.getId()))
                .orElse(new Sakrament("Prva pričest", "Nije evidentirana", null, null, null, null));
        Sakrament krizma = krizme.findByZupljaninId(id)
                .map(k -> new Sakrament("Krizma", "Evidentirana", k.getDatumKrizme(),
                        k.getMjestoKrizme(), matica(k.getBrojMatice(), k.getGodinaMatice()),
                        "/krizma/" + k.getId()))
                .orElse(new Sakrament("Krizma", "Nije evidentirana", null, null, null, null));
        List<VjencanjePregled> brakovi = vjencanja.findByMladozenjaZupljaninIdOrNevjestaZupljaninId(
                        id, id, Sort.by(Sort.Direction.DESC, "datumVjencanja", "id"))
                .stream().map(v -> {
                    boolean mladozenja = v.getMladozenjaZupljanin() != null
                            && id.equals(v.getMladozenjaZupljanin().getId());
                    String supruznik = mladozenja
                            ? v.getImeNevjeste() + " " + v.getPrezimeNevjeste()
                            : v.getImeMladozenje() + " " + v.getPrezimeMladozenje();
                    return new VjencanjePregled(v.getId(), v.getDatumVjencanja(), supruznik,
                            v.getMjestoVjencanja(), matica(v.getBrojMatice(), v.getGodinaMatice()));
                }).toList();
        return new ZupljaninProfil(zupljanin, List.of(krstenje, pricest, krizma), brakovi, dokumenti.zaZupljanina(id));
    }

    private String matica(String broj, Integer godina) {
        if (broj == null && godina == null) return "—";
        if (broj == null) return godina.toString();
        if (godina == null) return broj;
        return broj + "/" + godina;
    }
}
