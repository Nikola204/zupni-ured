package ba.sum.fsre.zupniured.dto;
import ba.sum.fsre.zupniured.model.StatusMisneNakane;
import jakarta.validation.constraints.NotNull;
public class MisnaNakanaStatusForma {
    @NotNull(message = "Status je obavezan.")
    private StatusMisneNakane status;
    public StatusMisneNakane getStatus() { return status; }
    public void setStatus(StatusMisneNakane status) { this.status = status; }
}
