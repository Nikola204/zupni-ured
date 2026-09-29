package ba.sum.fsre.zupniured.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;
import ba.sum.fsre.zupniured.model.*;
import org.springframework.format.annotation.DateTimeFormat;

public class DokumentForma {
    @Size(max = 100, message = "Ime i prezime mogu imati najviše 100 znakova.")
    private String imePrimatelja;

    @Size(max = 100, message = "Ime i prezime mogu imati najviše 100 znakova.")
    private String prezimePrimatelja;

    @NotNull(message = "Vrsta dokumenta je obavezna.")
    private VrstaDokumenta vrstaDokumenta;

    @NotNull(message = "Status je obavezan.")
    private StatusDokumenta status = StatusDokumenta.ZAPRIMLJEN;

    @NotNull(message = "Datum zahtjeva je obavezan.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate datumZahtjeva;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate datumIzdavanja;

    @Size(max = 2000, message = "Dopušteno je najviše 2000 znakova.")
    private String svrha;

    @Size(max = 2000, message = "Dopušteno je najviše 2000 znakova.")
    private String napomena;

    private Long zupljaninId;
    private Long krstenjeId;
    private Long prvaPricestId;
    private Long krizmaId;
    private Long vjencanjeId;
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
    public Long getZupljaninId() { return zupljaninId; }
    public void setZupljaninId(Long zupljaninId) { this.zupljaninId = zupljaninId; }
    public Long getKrstenjeId() { return krstenjeId; }
    public void setKrstenjeId(Long krstenjeId) { this.krstenjeId = krstenjeId; }
    public Long getPrvaPricestId() { return prvaPricestId; }
    public void setPrvaPricestId(Long prvaPricestId) { this.prvaPricestId = prvaPricestId; }
    public Long getKrizmaId() { return krizmaId; }
    public void setKrizmaId(Long krizmaId) { this.krizmaId = krizmaId; }
    public Long getVjencanjeId() { return vjencanjeId; }
    public void setVjencanjeId(Long vjencanjeId) { this.vjencanjeId = vjencanjeId; }
}
