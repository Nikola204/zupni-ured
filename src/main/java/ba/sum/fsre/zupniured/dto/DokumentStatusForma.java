package ba.sum.fsre.zupniured.dto;

import ba.sum.fsre.zupniured.model.StatusDokumenta;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

public class DokumentStatusForma {
    @NotNull(message = "Status je obavezan.")
    private StatusDokumenta status;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate datumIzdavanja;
    public StatusDokumenta getStatus() { return status; }
    public void setStatus(StatusDokumenta status) { this.status = status; }
    public LocalDate getDatumIzdavanja() { return datumIzdavanja; }
    public void setDatumIzdavanja(LocalDate datumIzdavanja) { this.datumIzdavanja = datumIzdavanja; }
}
