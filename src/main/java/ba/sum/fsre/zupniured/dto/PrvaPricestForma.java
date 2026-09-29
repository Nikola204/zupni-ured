package ba.sum.fsre.zupniured.dto;

import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class PrvaPricestForma {

    @NotNull(message = "Župljanin je obavezan.")
    private Long zupljaninId;

    @NotNull(message = "Datum pričesti je obavezan.")
    @PastOrPresent(message = "Datum pričesti ne smije biti u budućnosti.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate datumPricesti;

    @NotBlank(message = "Mjesto pričesti je obavezno.")
    @Size(max = 150, message = "Mjesto pričesti može imati najviše 150 znakova.")
    private String mjestoPricesti;

    @Size(max = 50, message = "Broj matice može imati najviše 50 znakova.")
    private String brojMatice;

    @Min(value = 1, message = "Godina matice mora biti između 1 i 9999.")
    @Max(value = 9999, message = "Godina matice mora biti između 1 i 9999.")
    private Integer godinaMatice;

    @Size(max = 2000, message = "Napomena može imati najviše 2000 znakova.")
    private String napomena;

    public Long getZupljaninId() { return zupljaninId; }
    public void setZupljaninId(Long zupljaninId) { this.zupljaninId = zupljaninId; }
    public LocalDate getDatumPricesti() { return datumPricesti; }
    public void setDatumPricesti(LocalDate datumPricesti) { this.datumPricesti = datumPricesti; }
    public String getMjestoPricesti() { return mjestoPricesti; }
    public void setMjestoPricesti(String mjestoPricesti) { this.mjestoPricesti = urediTekst(mjestoPricesti); }
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
