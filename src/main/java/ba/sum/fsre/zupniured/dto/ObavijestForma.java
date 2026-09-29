package ba.sum.fsre.zupniured.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class ObavijestForma {

    @NotBlank(message = "Naslov je obavezan.")
    @Size(max = 200, message = "Naslov može imati najviše 200 znakova.")
    private String naslov;

    @NotBlank(message = "Sadržaj je obavezan.")
    private String sadrzaj;

    @NotNull(message = "Datum objave je obavezan.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate datumObjave;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate datumIsteka;

    private boolean istaknuta = false;
    private boolean aktivna = true;

    @AssertTrue(message = "Datum isteka ne smije biti prije datuma objave.")
    public boolean isRazdobljeIspravno() {
        return datumObjave == null || datumIsteka == null || !datumIsteka.isBefore(datumObjave);
    }
    public String getNaslov() { return naslov; }
    public void setNaslov(String naslov) { this.naslov = naslov == null ? null : naslov.strip(); }
    public String getSadrzaj() { return sadrzaj; }
    public void setSadrzaj(String sadrzaj) { this.sadrzaj = sadrzaj == null ? null : sadrzaj.strip(); }
    public LocalDate getDatumObjave() { return datumObjave; }
    public void setDatumObjave(LocalDate datumObjave) { this.datumObjave = datumObjave; }
    public LocalDate getDatumIsteka() { return datumIsteka; }
    public void setDatumIsteka(LocalDate datumIsteka) { this.datumIsteka = datumIsteka; }
    public boolean isIstaknuta() { return istaknuta; }
    public void setIstaknuta(boolean istaknuta) { this.istaknuta = istaknuta; }
    public boolean isAktivna() { return aktivna; }
    public void setAktivna(boolean aktivna) { this.aktivna = aktivna; }
}
