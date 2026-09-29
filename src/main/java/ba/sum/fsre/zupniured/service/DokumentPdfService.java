package ba.sum.fsre.zupniured.service;

import ba.sum.fsre.zupniured.model.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;

@Service
@Transactional(readOnly = true)
public class DokumentPdfService {
    private final DokumentService dokumenti;
    private final String nazivZupe;
    private final String adresaZupe;
    private final String mjestoZupe;

    public DokumentPdfService(DokumentService dokumenti,
                              @Value("${app.zupa.naziv:Župni ured}") String nazivZupe,
                              @Value("${app.zupa.adresa:}") String adresaZupe,
                              @Value("${app.zupa.mjesto:}") String mjestoZupe) {
        this.dokumenti = dokumenti;
        this.nazivZupe = nazivZupe;
        this.adresaZupe = adresaZupe;
        this.mjestoZupe = mjestoZupe;
    }

    public record PdfDokument(byte[] sadrzaj, String nazivDatoteke) {}

    public PdfDokument generiraj(Long id) {
        Dokument d = dokumenti.pronadjiPoId(id);
        if (d.getStatus() != StatusDokumenta.IZDAN) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "PDF je dostupan samo za izdani dokument.");
        }
        if (d.getVrstaDokumenta() != VrstaDokumenta.KRSNI_LIST
                && d.getVrstaDokumenta() != VrstaDokumenta.POTVRDA_O_VJENCANJU) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "PDF za ovu vrstu dokumenta još nije podržan.");
        }
        boolean krsni = d.getVrstaDokumenta() == VrstaDokumenta.KRSNI_LIST;
        if (krsni && (d.getKrstenje() == null || d.getKrstenje().getZupljanin() == null)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Za krsni list nedostaje povezani zapis krštenja i župljanina.");
        }
        if (!krsni && d.getVjencanje() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Za potvrdu o vjenčanju nedostaje povezani zapis vjenčanja.");
        }
        if (d.getDatumIzdavanja() == null || d.getIzdaoKorisnik() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Nedostaju datum ili djelatnik izdavanja dokumenta.");
        }
        String naslov = krsni ? "KRSNI LIST" : "POTVRDA O VJENČANJU";
        try (var pdf = new PDDocument();
             var fontStream = new ClassPathResource("fonts/NotoSans-Regular.ttf").getInputStream();
             var izlaz = new ByteArrayOutputStream()) {
            var font = PDType0Font.load(pdf, fontStream, true);
            pdf.getDocumentInformation().setTitle(naslov);
            pdf.getDocumentInformation().setAuthor(nazivZupe);
            try (var pisac = new PdfPisac(pdf, font)) {
                pisac.centrirano(nazivZupe.isBlank() ? "Župni ured" : nazivZupe, 15);
                if (!adresaZupe.isBlank()) pisac.centrirano(adresaZupe, 10);
                if (!mjestoZupe.isBlank()) pisac.centrirano(mjestoZupe, 10);
                pisac.razmak(25);
                pisac.centrirano(naslov, 19);
                pisac.razmak(25);
                if (krsni) ispisiKrstenje(pisac, d);
                else ispisiVjencanje(pisac, d.getVjencanje());
                pisac.zavrsetak(d.getDatumIzdavanja(),
                        d.getIzdaoKorisnik().getIme() + " " + d.getIzdaoKorisnik().getPrezime());
            }
            pdf.save(izlaz);
            String primatelj = Normalizer.normalize(d.getImePrimatelja() + "-" + d.getPrezimePrimatelja(), Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "").replace('đ', 'd').replace('Đ', 'D')
                    .replaceAll("[^A-Za-z0-9-]", "-").replaceAll("-+", "-");
            if (primatelj.length() > 80) primatelj = primatelj.substring(0, 80);
            return new PdfDokument(izlaz.toByteArray(),
                    (krsni ? "krsni-list-" : "potvrda-o-vjencanju-") + primatelj + "-" + d.getId() + ".pdf");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "PDF nije moguće generirati.", e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Tekst sadrži znak koji odabrani PDF font ne podržava.", e);
        }
    }

    private void ispisiKrstenje(PdfPisac p, Dokument d) throws IOException {
        Krstenje k = d.getKrstenje();
        Zupljanin z = k.getZupljanin();
        p.polje("Ime i prezime", d.getImePrimatelja() + " " + d.getPrezimePrimatelja());
        p.polje("Datum rođenja", z.getDatumRodjenja());
        p.polje("Mjesto rođenja", z.getMjestoRodjenja());
        p.polje("Datum krštenja", k.getDatumKrstenja());
        p.polje("Mjesto krštenja", k.getMjestoKrstenja());
        p.polje("Ime oca", k.getImeOca());
        p.polje("Ime majke", k.getImeMajke());
        p.polje("Ime kuma", k.getImeKuma());
        p.polje("Ime kume", k.getImeKume());
        p.polje("Krstitelj", k.getKrstitelj());
        p.polje("Broj matice", k.getBrojMatice());
        p.polje("Godina matice", k.getGodinaMatice());
    }

    private void ispisiVjencanje(PdfPisac p, Vjencanje v) throws IOException {
        p.polje("Mladoženja", v.getImeMladozenje() + " " + v.getPrezimeMladozenje());
        p.polje("Nevjesta", v.getImeNevjeste() + " " + v.getPrezimeNevjeste());
        p.polje("Datum vjenčanja", v.getDatumVjencanja());
        p.polje("Mjesto vjenčanja", v.getMjestoVjencanja());
        p.polje("Svjedok mladoženje", v.getSvjedokMladozenje());
        p.polje("Svjedok nevjeste", v.getSvjedokNevjeste());
        p.polje("Služitelj vjenčanja", v.getSluziteljVjencanja());
        p.polje("Broj matice", v.getBrojMatice());
        p.polje("Godina matice", v.getGodinaMatice());
    }
}
