package ba.sum.fsre.zupniured.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "zupljani")
public class Zupljanin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Ime je obavezno.")
    @Size(max = 100, message = "Ime može imati najviše 100 znakova.")
    @Column(nullable = false, length = 100)
    private String ime;

    @NotBlank(message = "Prezime je obavezno.")
    @Size(max = 100, message = "Prezime može imati najviše 100 znakova.")
    @Column(nullable = false, length = 100)
    private String prezime;

    @PastOrPresent(message = "Datum rođenja ne smije biti u budućnosti.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate datumRodjenja;

    @Size(max = 150, message = "Mjesto rođenja može imati najviše 150 znakova.")
    @Column(length = 150)
    private String mjestoRodjenja;

    @Size(max = 255, message = "Adresa može imati najviše 255 znakova.")
    private String adresa;

    @Size(max = 50, message = "Telefon može imati najviše 50 znakova.")
    @Column(length = 50)
    private String telefon;

    @Email(message = "Unesite ispravnu email adresu.")
    @Size(max = 254, message = "Email može imati najviše 254 znaka.")
    @Column(length = 254)
    private String email;

    @Size(max = 2000, message = "Napomena može imati najviše 2000 znakova.")
    @Column(length = 2000)
    private String napomena;

    @Column(nullable = false)
    private boolean aktivan = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime datumKreiranja;

    public Zupljanin() {
    }

    @PrePersist
    private void postaviDatumKreiranja() {
        datumKreiranja = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getIme() { return ime; }
    public void setIme(String ime) { this.ime = urediTekst(ime); }
    public String getPrezime() { return prezime; }
    public void setPrezime(String prezime) { this.prezime = urediTekst(prezime); }
    public LocalDate getDatumRodjenja() { return datumRodjenja; }
    public void setDatumRodjenja(LocalDate datumRodjenja) { this.datumRodjenja = datumRodjenja; }
    public String getMjestoRodjenja() { return mjestoRodjenja; }
    public void setMjestoRodjenja(String mjestoRodjenja) { this.mjestoRodjenja = urediTekst(mjestoRodjenja); }
    public String getAdresa() { return adresa; }
    public void setAdresa(String adresa) { this.adresa = urediTekst(adresa); }
    public String getTelefon() { return telefon; }
    public void setTelefon(String telefon) { this.telefon = urediTekst(telefon); }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = urediTekst(email); }
    public String getNapomena() { return napomena; }
    public void setNapomena(String napomena) { this.napomena = urediTekst(napomena); }
    public boolean isAktivan() { return aktivan; }
    public void setAktivan(boolean aktivan) { this.aktivan = aktivan; }
    public LocalDateTime getDatumKreiranja() { return datumKreiranja; }

    private String urediTekst(String tekst) {
        return tekst == null || tekst.isBlank() ? null : tekst.strip();
    }
}
