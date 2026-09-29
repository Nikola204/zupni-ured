package ba.sum.fsre.zupniured.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "krizme", uniqueConstraints =
        @UniqueConstraint(name = "uk_krizme_zupljanin", columnNames = "zupljanin_id"))
public class Krizma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Župljanin je obavezan.")
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zupljanin_id", nullable = false, unique = true)
    private Zupljanin zupljanin;

    @NotNull(message = "Datum krizme je obavezan.")
    @PastOrPresent(message = "Datum krizme ne smije biti u budućnosti.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(nullable = false)
    private LocalDate datumKrizme;

    @NotBlank(message = "Mjesto krizme je obavezno.")
    @Size(max = 150, message = "Mjesto krizme može imati najviše 150 znakova.")
    @Column(nullable = false, length = 150)
    private String mjestoKrizme;

    @Size(max = 150, message = "Ime krizmanog kuma može imati najviše 150 znakova.")
    @Column(length = 150)
    private String imeKrizmanogKuma;

    @Size(max = 150, message = "Krizmatelj može imati najviše 150 znakova.")
    @Column(length = 150)
    private String krizmatelj;

    @Size(max = 50, message = "Broj matice može imati najviše 50 znakova.")
    @Column(length = 50)
    private String brojMatice;

    @Min(value = 1, message = "Godina matice mora biti između 1 i 9999.")
    @Max(value = 9999, message = "Godina matice mora biti između 1 i 9999.")
    private Integer godinaMatice;

    @Size(max = 2000, message = "Napomena može imati najviše 2000 znakova.")
    @Column(length = 2000)
    private String napomena;

    @Column(nullable = false, updatable = false)
    private LocalDateTime datumKreiranja;

    @PrePersist
    private void postaviDatumKreiranja() {
        datumKreiranja = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Zupljanin getZupljanin() { return zupljanin; }
    public void setZupljanin(Zupljanin zupljanin) { this.zupljanin = zupljanin; }
    public LocalDateTime getDatumKreiranja() { return datumKreiranja; }
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
