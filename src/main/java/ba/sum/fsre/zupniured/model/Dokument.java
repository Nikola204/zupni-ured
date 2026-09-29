package ba.sum.fsre.zupniured.model;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dokumenti")
public class Dokument {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank
    @Size(max = 100, message = "Ime i prezime mogu imati najviše 100 znakova.")
    @Column(nullable = false, length = 100)
    private String imePrimatelja;

    @NotBlank
    @Size(max = 100, message = "Ime i prezime mogu imati najviše 100 znakova.")
    @Column(nullable = false, length = 100)
    private String prezimePrimatelja;

    @NotNull(message = "Vrsta dokumenta je obavezna.")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private VrstaDokumenta vrstaDokumenta;

    @NotNull(message = "Status je obavezan.")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private StatusDokumenta status = StatusDokumenta.ZAPRIMLJEN;

    @NotNull(message = "Datum zahtjeva je obavezan.")
    @Column(nullable = false)
    private LocalDate datumZahtjeva;

    private LocalDate datumIzdavanja;

    @Size(max = 2000, message = "Dopušteno je najviše 2000 znakova.")
    @Column(length = 2000)
    private String svrha;

    @Size(max = 2000, message = "Dopušteno je najviše 2000 znakova.")
    @Column(length = 2000)
    private String napomena;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zupljanin_id")
    private Zupljanin zupljanin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "izdao_korisnik_id")
    private Korisnik izdaoKorisnik;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "krstenje_id")
    private Krstenje krstenje;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prva_pricest_id")
    private PrvaPricest prvaPricest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "krizma_id")
    private Krizma krizma;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vjencanje_id")
    private Vjencanje vjencanje;


    @Column(nullable = false, updatable = false)
    private LocalDateTime datumKreiranja;
    @Column(nullable = false)
    private LocalDateTime datumIzmjene;
    @PrePersist
    void prijeStvaranja() {
        datumKreiranja = LocalDateTime.now();
        datumIzmjene = datumKreiranja;
    }
    @PreUpdate
    void prijeIzmjene() { datumIzmjene = LocalDateTime.now(); }
    public Long getId() { return id; }
    public LocalDateTime getDatumKreiranja() { return datumKreiranja; }
    public LocalDateTime getDatumIzmjene() { return datumIzmjene; }
    public String getImePrimatelja() { return imePrimatelja; }
    public void setImePrimatelja(String imePrimatelja) { this.imePrimatelja = imePrimatelja == null || imePrimatelja.isBlank() ? null : imePrimatelja.strip(); }
    public String getPrezimePrimatelja() { return prezimePrimatelja; }
    public void setPrezimePrimatelja(String prezimePrimatelja) { this.prezimePrimatelja = prezimePrimatelja == null || prezimePrimatelja.isBlank() ? null : prezimePrimatelja.strip(); }
    public VrstaDokumenta getVrstaDokumenta() { return vrstaDokumenta; }
    public void setVrstaDokumenta(VrstaDokumenta vrstaDokumenta) { this.vrstaDokumenta = vrstaDokumenta; }
    public StatusDokumenta getStatus() { return status; }
    public void setStatus(StatusDokumenta status) { this.status = status; }
    public LocalDate getDatumZahtjeva() { return datumZahtjeva; }
    public void setDatumZahtjeva(LocalDate datumZahtjeva) { this.datumZahtjeva = datumZahtjeva; }
    public LocalDate getDatumIzdavanja() { return datumIzdavanja; }
    public void setDatumIzdavanja(LocalDate datumIzdavanja) { this.datumIzdavanja = datumIzdavanja; }
    public String getSvrha() { return svrha; }
    public void setSvrha(String svrha) { this.svrha = svrha == null || svrha.isBlank() ? null : svrha.strip(); }
    public String getNapomena() { return napomena; }
    public void setNapomena(String napomena) { this.napomena = napomena == null || napomena.isBlank() ? null : napomena.strip(); }
    public Zupljanin getZupljanin() { return zupljanin; }
    public void setZupljanin(Zupljanin zupljanin) { this.zupljanin = zupljanin; }
    public Korisnik getIzdaoKorisnik() { return izdaoKorisnik; }
    public void setIzdaoKorisnik(Korisnik izdaoKorisnik) { this.izdaoKorisnik = izdaoKorisnik; }
    public Krstenje getKrstenje() { return krstenje; }
    public void setKrstenje(Krstenje krstenje) { this.krstenje = krstenje; }
    public PrvaPricest getPrvaPricest() { return prvaPricest; }
    public void setPrvaPricest(PrvaPricest prvaPricest) { this.prvaPricest = prvaPricest; }
    public Krizma getKrizma() { return krizma; }
    public void setKrizma(Krizma krizma) { this.krizma = krizma; }
    public Vjencanje getVjencanje() { return vjencanje; }
    public void setVjencanje(Vjencanje vjencanje) { this.vjencanje = vjencanje; }
}
