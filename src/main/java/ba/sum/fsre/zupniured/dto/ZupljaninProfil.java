package ba.sum.fsre.zupniured.dto;

import ba.sum.fsre.zupniured.model.Zupljanin;
import ba.sum.fsre.zupniured.model.Dokument;
import java.time.LocalDate;
import java.util.List;

public record ZupljaninProfil(Zupljanin zupljanin, List<Sakrament> sakramenti,
                              List<VjencanjePregled> vjencanja, List<Dokument> dokumenti) {
    public record Sakrament(String naziv, String poruka, LocalDate datum, String mjesto,
                            String matica, String poveznica) {}
    public record VjencanjePregled(Long id, LocalDate datum, String supruznik,
                                   String mjesto, String matica) {}
}
