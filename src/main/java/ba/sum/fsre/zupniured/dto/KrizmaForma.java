package ba.sum.fsre.zupniured.dto;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class KrizmaForma {

    @NotNull(message = "Župljanin je obavezan.")
    private Long zupljaninId;

    @NotNull(message = "Datum krizme je obavezan.")
    @PastOrPresent(message = "Datum krizme ne smije biti u budućnosti.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate datumKrizme;

    @NotBlank(message = "Mjesto krizme je obavezno.")
    @Size(max = 150, message = "Mjesto krizme može imati najviše 150 znakova.")
    private String mjestoKrizme;

    @Size(max = 150, message = "Ime krizmanog kuma može imati najviše 150 znakova.")
    private String imeKrizmanogKuma;

    @Size(max = 150, message = "Krizmatelj može imati najviše 150 znakova.")
    private String krizmatelj;

    @Size(max = 50, message = "Broj matice može imati najviše 50 znakova.")
    private String brojMatice;

    @Min(value = 1, message = "Godina matice mora biti između 1 i 9999.")
    @Max(value = 9999, message = "Godina matice mora biti između 1 i 9999.")
    private Integer godinaMatice;

    @Size(max = 2000, message = "Napomena može imati najviše 2000 znakova.")
    private String napomena;

    public Long getZupljaninId() { return zupljaninId; }
    public void setZupljaninId(Long zupljaninId) { this.zupljaninId = zupljaninId; }
    public LocalDate getDatumKrizme() { return datumKrizme; }
    public void setDatumKrizme(LocalDate datumKrizme) { this.datumKrizme = datumKrizme; }
    public String getMjestoKrizme() { return mjestoKrizme; }
    public void setMjestoKrizme(String mjestoKrizme) { this.mjestoKrizme = urediTekst(mjestoKrizme); }
    public String getImeKrizmanogKuma() { return imeKrizmanogKuma; }
    public void setImeKrizmanogKuma(String imeKrizmanogKuma) { this.imeKrizmanogKuma = urediTekst(imeKrizmanogKuma); }
    public String getKrizmatelj() { return krizmatelj; }
    public void setKrizmatelj(String krizmatelj) { this.krizmatelj = urediTekst(krizmatelj); }
    public String getBrojMatice() { return brojMatice; }
    public void setBrojMatice(String brojMatice) { this.brojMatice = urediTekst(brojMatice); }
    public Integer getGodinaMatice() { return godinaMatice; }
    public void setGodinaMatice(Integer godinaMatice) { this.godinaMatice = godinaMatice; }
    public String getNapomena() { return napomena; }
    public void setNapomena(String napomena) { this.napomena = urediTekst(napomena); }

    private String urediTekst(String tekst) {
        return tekst == null || tekst.isBlank() ? null : tekst.strip();
    }
}
