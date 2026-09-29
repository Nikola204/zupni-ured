package ba.sum.fsre.zupniured.model;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "misne_nakane")
public class MisnaNakana {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "Nakana je obavezna.")
    @Size(max = 2000, message = "Dopušteno je najviše 2000 znakova.")
    @Column(length = 2000, nullable = false)
    private String nakana;

    @NotNull(message = "Datum je obavezan podatak.")
    @Column(nullable = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate datum;

    @NotNull(message = "Vrijeme je obavezan podatak.")
    @Column(nullable = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime vrijeme;

    @NotBlank(message = "Mjesto je obavezno.")
    @Size(max = 150, message = "Dopušteno je najviše 150 znakova.")
    @Column(length = 150, nullable = false)
    private String mjesto;

    @NotBlank(message = "Ime naručitelja je obavezno.")
    @Size(max = 100, message = "Dopušteno je najviše 100 znakova.")
    @Column(length = 100, nullable = false)
    private String naruciteljIme;

    @Size(max = 100, message = "Dopušteno je najviše 100 znakova.")
    @Column(length = 100)
    private String naruciteljPrezime;

    @Size(max = 50, message = "Dopušteno je najviše 50 znakova.")
    @Column(length = 50)
    private String naruciteljTelefon;

    @Size(max = 254, message = "Dopušteno je najviše 254 znakova.")
    @Column(length = 254)
    @Email(message = "Unesite ispravnu email adresu.")
    private String naruciteljEmail;

    @Size(max = 150, message = "Dopušteno je najviše 150 znakova.")
    @Column(length = 150)
    private String svecenik;

    @NotNull(message = "Status je obavezan.")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusMisneNakane status = StatusMisneNakane.ZAPRIMLJENA;

    @DecimalMin(value = "0", message = "Prilog ne smije biti negativan.")
    @Digits(integer = 12, fraction = 2, message = "Prilog može imati najviše 12 znamenki i dvije decimale.")
    @Column(precision = 14, scale = 2)
    private BigDecimal prilog;

    @Size(max = 2000, message = "Dopušteno je najviše 2000 znakova.")
    @Column(length = 2000)
    private String napomena;

    @Column(nullable = false, updatable = false)
    private LocalDateTime datumKreiranja;
    @PrePersist
    void postaviDatumKreiranja() { datumKreiranja = LocalDateTime.now(); }
    public Long getId() { return id; }
    public LocalDateTime getDatumKreiranja() { return datumKreiranja; }
    public String getNakana() { return nakana; }
    public void setNakana(String nakana) { this.nakana = nakana == null || nakana.isBlank() ? null : nakana.strip(); }
    public LocalDate getDatum() { return datum; }
    public void setDatum(LocalDate datum) { this.datum = datum; }
    public LocalTime getVrijeme() { return vrijeme; }
    public void setVrijeme(LocalTime vrijeme) { this.vrijeme = vrijeme; }
    public String getMjesto() { return mjesto; }
    public void setMjesto(String mjesto) { this.mjesto = mjesto == null || mjesto.isBlank() ? null : mjesto.strip(); }
    public String getNaruciteljIme() { return naruciteljIme; }
    public void setNaruciteljIme(String naruciteljIme) { this.naruciteljIme = naruciteljIme == null || naruciteljIme.isBlank() ? null : naruciteljIme.strip(); }
    public String getNaruciteljPrezime() { return naruciteljPrezime; }
    public void setNaruciteljPrezime(String naruciteljPrezime) { this.naruciteljPrezime = naruciteljPrezime == null || naruciteljPrezime.isBlank() ? null : naruciteljPrezime.strip(); }
    public String getNaruciteljTelefon() { return naruciteljTelefon; }
    public void setNaruciteljTelefon(String naruciteljTelefon) { this.naruciteljTelefon = naruciteljTelefon == null || naruciteljTelefon.isBlank() ? null : naruciteljTelefon.strip(); }
    public String getNaruciteljEmail() { return naruciteljEmail; }
    public void setNaruciteljEmail(String naruciteljEmail) { this.naruciteljEmail = naruciteljEmail == null || naruciteljEmail.isBlank() ? null : naruciteljEmail.strip(); }
    public String getSvecenik() { return svecenik; }
    public void setSvecenik(String svecenik) { this.svecenik = svecenik == null || svecenik.isBlank() ? null : svecenik.strip(); }
    public StatusMisneNakane getStatus() { return status; }
    public void setStatus(StatusMisneNakane status) { this.status = status; }
    public BigDecimal getPrilog() { return prilog; }
    public void setPrilog(BigDecimal prilog) { this.prilog = prilog; }
    public String getNapomena() { return napomena; }
    public void setNapomena(String napomena) { this.napomena = napomena == null || napomena.isBlank() ? null : napomena.strip(); }
}
