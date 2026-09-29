package ba.sum.fsre.zupniured.dto;

import ba.sum.fsre.zupniured.model.Uloga;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.groups.Default;

import java.util.Locale;

// Obrazac nikada ne sadrži hash postojeće lozinke, ID ni datum kreiranja.
public class KorisnikForma {

    public interface Novi extends Default {
    }

    @NotBlank(message = "Ime je obavezno.")
    @Size(max = 100, message = "Ime može imati najviše 100 znakova.")
    private String ime;

    @NotBlank(message = "Prezime je obavezno.")
    @Size(max = 100, message = "Prezime može imati najviše 100 znakova.")
    private String prezime;

    @NotBlank(message = "Email je obavezan.")
    @Email(message = "Unesite ispravnu email adresu.")
    @Size(max = 254, message = "Email može imati najviše 254 znaka.")
    private String email;

    @NotBlank(groups = Novi.class, message = "Lozinka je obavezna.")
    @Pattern(regexp = "(?s)(?:|.{8,72})", message = "Lozinka mora imati između 8 i 72 znaka.")
    private String lozinka = "";

    @NotNull(message = "Uloga je obavezna.")
    private Uloga uloga = Uloga.DJELATNIK;

    private boolean aktivan = true;

    public String getIme() { return ime; }
    public void setIme(String ime) { this.ime = ime == null ? null : ime.strip(); }
    public String getPrezime() { return prezime; }
    public void setPrezime(String prezime) { this.prezime = prezime == null ? null : prezime.strip(); }
    public String getEmail() { return email; }
    public void setEmail(String email) {
        this.email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
    public String getLozinka() { return lozinka; }
    public void setLozinka(String lozinka) { this.lozinka = lozinka; }
    public Uloga getUloga() { return uloga; }
    public void setUloga(Uloga uloga) { this.uloga = uloga; }
    public boolean isAktivan() { return aktivan; }
    public void setAktivan(boolean aktivan) { this.aktivan = aktivan; }
}
