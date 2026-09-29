package ba.sum.fsre.zupniured.dto;

import ba.sum.fsre.zupniured.model.*;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

public class DokumentFilter {
    private String pretraga;
    private VrstaDokumenta vrstaDokumenta;
    private StatusDokumenta status;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate od;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate doDatuma;
    @AssertTrue(message = "Početni datum ne smije biti nakon završnog datuma.")
    public boolean isRasponIspravan() { return od == null || doDatuma == null || !od.isAfter(doDatuma); }
    public String getPretraga() { return pretraga; }
    public void setPretraga(String pretraga) { this.pretraga = pretraga; }
    public VrstaDokumenta getVrstaDokumenta() { return vrstaDokumenta; }
    public void setVrstaDokumenta(VrstaDokumenta vrstaDokumenta) { this.vrstaDokumenta = vrstaDokumenta; }
    public StatusDokumenta getStatus() { return status; }
    public void setStatus(StatusDokumenta status) { this.status = status; }
    public LocalDate getOd() { return od; }
    public void setOd(LocalDate od) { this.od = od; }
    public LocalDate getDoDatuma() { return doDatuma; }
    public void setDoDatuma(LocalDate doDatuma) { this.doDatuma = doDatuma; }
}
