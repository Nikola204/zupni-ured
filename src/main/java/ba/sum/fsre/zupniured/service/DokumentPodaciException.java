package ba.sum.fsre.zupniured.service;

public class DokumentPodaciException extends RuntimeException {
    private final String polje;
    public DokumentPodaciException(String polje, String poruka) {
        super(poruka);
        this.polje = polje;
    }
    public String getPolje() { return polje; }
}
