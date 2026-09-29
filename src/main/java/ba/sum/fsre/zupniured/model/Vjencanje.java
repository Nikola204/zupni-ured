package ba.sum.fsre.zupniured.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vjencanja")
public class Vjencanje {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "mladozenja_zupljanin_id")
    private Zupljanin mladozenjaZupljanin;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "nevjesta_zupljanin_id")
    private Zupljanin nevjestaZupljanin;

    @NotBlank(message = "Ime mladoženje je obavezno.")
    @Size(max = 100, message = "Ime mladoženje: najviše 100 znakova.")
    @Column(length = 100, nullable = false)
    private String imeMladozenje;

    @NotBlank(message = "Prezime mladoženje je obavezno.")
    @Size(max = 100, message = "Prezime mladoženje: najviše 100 znakova.")
    @Column(length = 100, nullable = false)
    private String prezimeMladozenje;

    @NotBlank(message = "Ime nevjeste je obavezno.")
    @Size(max = 100, message = "Ime nevjeste: najviše 100 znakova.")
    @Column(length = 100, nullable = false)
    private String imeNevjeste;

    @NotBlank(message = "Prezime nevjeste je obavezno.")
    @Size(max = 100, message = "Prezime nevjeste: najviše 100 znakova.")
    @Column(length = 100, nullable = false)
    private String prezimeNevjeste;

    @NotNull(message = "Datum vjenčanja je obavezan.")
    @PastOrPresent(message = "Datum vjenčanja ne smije biti u budućnosti.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(nullable = false)
    private LocalDate datumVjencanja;

    @NotBlank(message = "Mjesto vjenčanja je obavezno.")
    @Size(max = 150, message = "Mjesto vjenčanja: najviše 150 znakova.")
    @Column(length = 150, nullable = false)
    private String mjestoVjencanja;

    @Size(max = 150, message = "Svjedok mladoženje: najviše 150 znakova.")
    @Column(length = 150)
    private String svjedokMladozenje;

    @Size(max = 150, message = "Svjedok nevjeste: najviše 150 znakova.")
    @Column(length = 150)
    private String svjedokNevjeste;

    @Size(max = 150, message = "Služitelj vjenčanja: najviše 150 znakova.")
    @Column(length = 150)
    private String sluziteljVjencanja;

    @Size(max = 50, message = "Broj matice: najviše 50 znakova.")
    @Column(length = 50)
    private String brojMatice;

    @Min(value = 1, message = "Godina matice mora biti između 1 i 9999.")
    @Max(value = 9999, message = "Godina matice mora biti između 1 i 9999.")
    private Integer godinaMatice;

    @Size(max = 2000, message = "Napomena: najviše 2000 znakova.")
    @Column(length = 2000)
    private String napomena;

    @Column(nullable = false, updatable = false)
    private LocalDateTime datumKreiranja;

    @PrePersist
    private void postaviDatumKreiranja() {
        datumKreiranja = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public LocalDateTime getDatumKreiranja() { return datumKreiranja; }
    public Zupljanin getMladozenjaZupljanin() { return mladozenjaZupljanin; }
    public void setMladozenjaZupljanin(Zupljanin osoba) { this.mladozenjaZupljanin = osoba; }
    public Zupljanin getNevjestaZupljanin() { return nevjestaZupljanin; }
    public void setNevjestaZupljanin(Zupljanin osoba) { this.nevjestaZupljanin = osoba; }
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
