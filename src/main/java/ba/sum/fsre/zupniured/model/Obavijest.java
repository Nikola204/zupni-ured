package ba.sum.fsre.zupniured.model;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "obavijesti")
public class Obavijest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Naslov je obavezan.")
    @Size(max = 200, message = "Naslov može imati najviše 200 znakova.")
    @Column(nullable = false, length = 200)
    private String naslov;

    @NotBlank(message = "Sadržaj je obavezan.")
    @Column(nullable = false, columnDefinition = "text")
    private String sadrzaj;

    @NotNull(message = "Datum objave je obavezan.")
    @Column(nullable = false)
    private LocalDate datumObjave;

    private LocalDate datumIsteka;

    @Column(nullable = false)
    private boolean istaknuta = false;
    @Column(nullable = false)
    private boolean aktivna = true;

    @AssertTrue(message = "Datum isteka ne smije biti prije datuma objave.")
    @Transient
    public boolean isRazdobljeIspravno() {
        return datumObjave == null || datumIsteka == null || !datumIsteka.isBefore(datumObjave);
    }

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false, updatable = false)
    private Korisnik autor;

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
    public Korisnik getAutor() { return autor; }
    public void setAutor(Korisnik autor) { this.autor = autor; }
    public LocalDateTime getDatumKreiranja() { return datumKreiranja; }
    public LocalDateTime getDatumIzmjene() { return datumIzmjene; }
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
