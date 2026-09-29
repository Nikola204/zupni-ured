package ba.sum.fsre.zupniured.service;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Jednostavan A4 ispis s prelamanjem teksta i novom stranicom kada ponestane prostora. */
final class PdfPisac implements AutoCloseable {
    private static final float RUB = 55;
    private static final float SIRINA = PDRectangle.A4.getWidth() - 2 * RUB;
    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy.");
    private final PDDocument pdf;
    private final PDFont font;
    private PDPageContentStream stream;
    private float y;

    PdfPisac(PDDocument pdf, PDFont font) throws IOException {
        this.pdf = pdf;
        this.font = font;
        novaStranica();
    }

    private void novaStranica() throws IOException {
        if (stream != null) stream.close();
        var stranica = new PDPage(PDRectangle.A4);
        pdf.addPage(stranica);
        stream = new PDPageContentStream(pdf, stranica);
        y = PDRectangle.A4.getHeight() - RUB;
    }

    void razmak(float visina) { y -= visina; }

    void centrirano(String tekst, float velicina) throws IOException {
        for (String red : prelomi(tekst, SIRINA, velicina)) {
            if (y < RUB + 30) novaStranica();
            ispisi(red, RUB + (SIRINA - sirina(red, velicina)) / 2, y, velicina);
            y -= velicina + 7;
        }
    }

    void polje(String naziv, Object vrijednost) throws IOException {
        List<String> redovi = prelomi(tekst(vrijednost), SIRINA - 165, 11);
        float visina = Math.max(27, redovi.size() * 16 + 11);
        if (y - visina < RUB + 30) novaStranica();
        ispisi(naziv + ":", RUB, y, 10);
        for (String red : redovi) {
            ispisi(red, RUB + 165, y, 11);
            y -= 16;
        }
        y -= 11;
    }

    void zavrsetak(LocalDate datum, String djelatnik) throws IOException {
        var redovi = prelomi("Izdao djelatnik: " + djelatnik, SIRINA, 10);
        float visina = 95 + redovi.size() * 16;
        if (y < RUB + visina + 25) novaStranica();
        y = RUB + visina;
        ispisi("Datum izdavanja: " + DATUM.format(datum), RUB, y, 11);
        y -= 25;
        for (String red : redovi) {
            ispisi(red, RUB, y, 10);
            y -= 16;
        }
        y -= 30;
        ispisi("________________________", RUB, y, 11);
        ispisi("Potpis i pečat", RUB, y - 20, 10);
    }

    private String tekst(Object vrijednost) {
        if (vrijednost == null || vrijednost.toString().isBlank()) return "—";
        return vrijednost instanceof LocalDate datum ? DATUM.format(datum) : vrijednost.toString();
    }

    private float sirina(String tekst, float velicina) throws IOException {
        return font.getStringWidth(tekst) / 1000 * velicina;
    }

    private List<String> prelomi(String tekst, float sirina, float velicina) throws IOException {
        String ostatak = tekst.replaceAll("\\s+", " ").strip();
        List<String> redovi = new ArrayList<>();
        while (!ostatak.isEmpty()) {
            int kraj = 0, razmak = -1;
            while (kraj < ostatak.length()) {
                int sljedeci = ostatak.offsetByCodePoints(kraj, 1);
                if (sirina(ostatak.substring(0, sljedeci), velicina) > sirina) break;
                if (ostatak.charAt(kraj) == ' ') razmak = kraj;
                kraj = sljedeci;
            }
            if (kraj == 0) kraj = ostatak.offsetByCodePoints(0, 1);
            if (kraj < ostatak.length() && razmak > 0) kraj = razmak;
            redovi.add(ostatak.substring(0, kraj).strip());
            ostatak = ostatak.substring(kraj).strip();
        }
        return redovi;
    }

    private void ispisi(String tekst, float x, float y, float velicina) throws IOException {
        stream.beginText();
        stream.setFont(font, velicina);
        stream.newLineAtOffset(x, y);
        stream.showText(tekst);
        stream.endText();
    }

    @Override
    public void close() throws IOException { stream.close(); }
}
