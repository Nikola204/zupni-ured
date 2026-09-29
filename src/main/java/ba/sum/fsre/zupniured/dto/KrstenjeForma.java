package ba.sum.fsre.zupniured.dto;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class KrstenjeForma {

    @NotNull(message = "Župljanin je obavezan.")
    private Long zupljaninId;

    @NotNull(message = "Datum krštenja je obavezan.")
    @PastOrPresent(message = "Datum krštenja ne smije biti u budućnosti.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate datumKrstenja;

    @NotBlank(message = "Mjesto krštenja je obavezno.")
    @Size(max = 150, message = "Mjesto krštenja može imati najviše 150 znakova.")
    private String mjestoKrstenja;

    @Size(max = 150, message = "Ime može imati najviše 150 znakova.")
    private String imeOca;

    @Size(max = 150, message = "Ime može imati najviše 150 znakova.")
    private String imeMajke;

    @Size(max = 150, message = "Ime može imati najviše 150 znakova.")
    private String imeKuma;

    @Size(max = 150, message = "Ime može imati najviše 150 znakova.")
    private String imeKume;

    @Size(max = 150, message = "Ime može imati najviše 150 znakova.")
    private String krstitelj;

    @Size(max = 50, message = "Broj matice može imati najviše 50 znakova.")
    private String brojMatice;

    @Min(value = 1, message = "Godina matice mora biti između 1 i 9999.")
    @Max(value = 9999, message = "Godina matice mora biti između 1 i 9999.")
    private Integer godinaMatice;

    @Size(max = 2000, message = "Napomena može imati najviše 2000 znakova.")
    private String napomena;

    public Long getZupljaninId() { return zupljaninId; }
    public void setZupljaninId(Long zupljaninId) { this.zupljaninId = zupljaninId; }
    public LocalDate getDatumKrstenja() { return datumKrstenja; }
    public void setDatumKrstenja(LocalDate datumKrstenja) { this.datumKrstenja = datumKrstenja; }
    public String getMjestoKrstenja() { return mjestoKrstenja; }
    public void setMjestoKrstenja(String mjestoKrstenja) { this.mjestoKrstenja = urediTekst(mjestoKrstenja); }
    public String getImeOca() { return imeOca; }
    public void setImeOca(String imeOca) { this.imeOca = urediTekst(imeOca); }
    public String getImeMajke() { return imeMajke; }
    public void setImeMajke(String imeMajke) { this.imeMajke = urediTekst(imeMajke); }
    public String getImeKuma() { return imeKuma; }
    public void setImeKuma(String imeKuma) { this.imeKuma = urediTekst(imeKuma); }
    public String getImeKume() { return imeKume; }
    public void setImeKume(String imeKume) { this.imeKume = urediTekst(imeKume); }
    public String getKrstitelj() { return krstitelj; }
    public void setKrstitelj(String krstitelj) { this.krstitelj = urediTekst(krstitelj); }
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

