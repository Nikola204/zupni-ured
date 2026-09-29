package ba.sum.fsre.zupniured.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(name = "korisnici")
public class Korisnik {

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

    @NotBlank(message = "Email je obavezan.")
    @Email(message = "Unesite ispravnu email adresu.")
    @Size(max = 254, message = "Email može imati najviše 254 znaka.")
    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @NotBlank(message = "Lozinka je obavezna.")
    @Column(nullable = false, length = 60)
    private String lozinka;

    @NotNull(message = "Uloga je obavezna.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Uloga uloga;

    @Column(nullable = false)
    private boolean aktivan = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime datumKreiranja;

    public Korisnik() {
    }

    @PrePersist
    private void prijeStvaranja() {
        datumKreiranja = LocalDateTime.now();
        normalizirajEmail();
    }

    @PreUpdate
    private void normalizirajEmail() {
        if (email != null) {
            email = email.strip().toLowerCase(Locale.ROOT);
        }
    }

    public Long getId() { return id; }
    public String getIme() { return ime; }
    public void setIme(String ime) { this.ime = ime; }
    public String getPrezime() { return prezime; }
    public void setPrezime(String prezime) { this.prezime = prezime; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getLozinka() { return lozinka; }
    public void setLozinka(String lozinka) { this.lozinka = lozinka; }
    public Uloga getUloga() { return uloga; }
    public void setUloga(Uloga uloga) { this.uloga = uloga; }
    public boolean isAktivan() { return aktivan; }
    public void setAktivan(boolean aktivan) { this.aktivan = aktivan; }
    public LocalDateTime getDatumKreiranja() { return datumKreiranja; }
}
