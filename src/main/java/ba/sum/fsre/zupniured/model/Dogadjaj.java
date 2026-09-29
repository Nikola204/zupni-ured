package ba.sum.fsre.zupniured.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "dogadjaji")
public class Dogadjaj {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Naziv događaja je obavezan.")
    @Size(min = 3, max = 100, message = "Naziv mora imati između 3 i 100 znakova.")
    private String naziv;

    @NotBlank(message = "Opis događaja je obavezan.")
    @Size(min = 5, max = 1000, message = "Opis mora imati između 5 i 1000 znakova.")
    private String opis;

    @NotNull(message = "Datum događaja je obavezan.")
    @FutureOrPresent(message = "Datum događaja ne može biti u prošlosti.")
    private LocalDate datum;

    @NotNull(message = "Vrijeme događaja je obavezno.")
    private LocalTime vrijeme;

    @NotBlank(message = "Lokacija je obavezna.")
    private String lokacija;

    @NotNull(message = "Maksimalan broj sudionika je obavezan.")
    @Min(value = 1, message = "Maksimalan broj sudionika mora biti najmanje 1.")
    private Integer maksimalanBrojSudionika;

    public Dogadjaj() {
    }

    public Dogadjaj(String naziv, String opis, LocalDate datum,
                    LocalTime vrijeme, String lokacija,
                    Integer maksimalanBrojSudionika) {
        this.naziv = naziv;
        this.opis = opis;
        this.datum = datum;
        this.vrijeme = vrijeme;
        this.lokacija = lokacija;
        this.maksimalanBrojSudionika = maksimalanBrojSudionika;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNaziv() {
        return naziv;
    }

    public void setNaziv(String naziv) {
        this.naziv = naziv;
    }

    public String getOpis() {
        return opis;
    }

    public void setOpis(String opis) {
        this.opis = opis;
    }

    public LocalDate getDatum() {
        return datum;
    }

    public void setDatum(LocalDate datum) {
        this.datum = datum;
    }

    public LocalTime getVrijeme() {
        return vrijeme;
    }

    public void setVrijeme(LocalTime vrijeme) {
        this.vrijeme = vrijeme;
    }

    public String getLokacija() {
        return lokacija;
    }

    public void setLokacija(String lokacija) {
        this.lokacija = lokacija;
    }

    public Integer getMaksimalanBrojSudionika() {
        return maksimalanBrojSudionika;
    }

    public void setMaksimalanBrojSudionika(Integer maksimalanBrojSudionika) {
        this.maksimalanBrojSudionika = maksimalanBrojSudionika;
    }
}