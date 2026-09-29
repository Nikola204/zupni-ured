package ba.sum.fsre.zupniured.dto;

import ba.sum.fsre.zupniured.validation.IspravniMladenci;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@IspravniMladenci
public class VjencanjeForma {
    private Long mladozenjaZupljaninId;
    private Long nevjestaZupljaninId;

    @Size(max = 100, message = "Ime mladoženje: najviše 100 znakova.")
    private String imeMladozenje;

    @Size(max = 100, message = "Prezime mladoženje: najviše 100 znakova.")
    private String prezimeMladozenje;

    @Size(max = 100, message = "Ime nevjeste: najviše 100 znakova.")
    private String imeNevjeste;

    @Size(max = 100, message = "Prezime nevjeste: najviše 100 znakova.")
    private String prezimeNevjeste;

    @NotNull(message = "Datum vjenčanja je obavezan.")
    @PastOrPresent(message = "Datum vjenčanja ne smije biti u budućnosti.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate datumVjencanja;

    @NotBlank(message = "Mjesto vjenčanja je obavezno.")
    @Size(max = 150, message = "Mjesto vjenčanja: najviše 150 znakova.")
    private String mjestoVjencanja;

    @Size(max = 150, message = "Svjedok mladoženje: najviše 150 znakova.")
    private String svjedokMladozenje;

    @Size(max = 150, message = "Svjedok nevjeste: najviše 150 znakova.")
    private String svjedokNevjeste;

    @Size(max = 150, message = "Služitelj vjenčanja: najviše 150 znakova.")
    private String sluziteljVjencanja;

    @Size(max = 50, message = "Broj matice: najviše 50 znakova.")
    private String brojMatice;

    @Min(value = 1, message = "Godina matice mora biti između 1 i 9999.")
    @Max(value = 9999, message = "Godina matice mora biti između 1 i 9999.")
    private Integer godinaMatice;

    @Size(max = 2000, message = "Napomena: najviše 2000 znakova.")
    private String napomena;

    public Long getMladozenjaZupljaninId() { return mladozenjaZupljaninId; }
    public void setMladozenjaZupljaninId(Long id) { this.mladozenjaZupljaninId = id; }
    public Long getNevjestaZupljaninId() { return nevjestaZupljaninId; }
    public void setNevjestaZupljaninId(Long id) { this.nevjestaZupljaninId = id; }
    public String getImeMladozenje() { return imeMladozenje; }
    public void setImeMladozenje(String imeMladozenje) { this.imeMladozenje = urediTekst(imeMladozenje); }
    public String getPrezimeMladozenje() { return prezimeMladozenje; }
    public void setPrezimeMladozenje(String prezimeMladozenje) { this.prezimeMladozenje = urediTekst(prezimeMladozenje); }
    public String getImeNevjeste() { return imeNevjeste; }
    public void setImeNevjeste(String imeNevjeste) { this.imeNevjeste = urediTekst(imeNevjeste); }
    public String getPrezimeNevjeste() { return prezimeNevjeste; }
    public void setPrezimeNevjeste(String prezimeNevjeste) { this.prezimeNevjeste = urediTekst(prezimeNevjeste); }
    public LocalDate getDatumVjencanja() { return datumVjencanja; }
    public void setDatumVjencanja(LocalDate datumVjencanja) { this.datumVjencanja = datumVjencanja; }
    public String getMjestoVjencanja() { return mjestoVjencanja; }
    public void setMjestoVjencanja(String mjestoVjencanja) { this.mjestoVjencanja = urediTekst(mjestoVjencanja); }
    public String getSvjedokMladozenje() { return svjedokMladozenje; }
    public void setSvjedokMladozenje(String svjedokMladozenje) { this.svjedokMladozenje = urediTekst(svjedokMladozenje); }
    public String getSvjedokNevjeste() { return svjedokNevjeste; }
    public void setSvjedokNevjeste(String svjedokNevjeste) { this.svjedokNevjeste = urediTekst(svjedokNevjeste); }
    public String getSluziteljVjencanja() { return sluziteljVjencanja; }
    public void setSluziteljVjencanja(String sluziteljVjencanja) { this.sluziteljVjencanja = urediTekst(sluziteljVjencanja); }
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
