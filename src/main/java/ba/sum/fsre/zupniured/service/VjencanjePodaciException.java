package ba.sum.fsre.zupniured.service;

public class VjencanjePodaciException extends RuntimeException {
    private final String polje;

    public VjencanjePodaciException(String polje, String poruka) {
        super(poruka);
        this.polje = polje;
    }

    public String getPolje() { return polje; }
}
