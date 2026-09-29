package ba.sum.fsre.zupniured.service;

public class KorisnikPodaciException extends RuntimeException {

    private final String polje;

    public KorisnikPodaciException(String polje, String poruka) {
        super(poruka);
        this.polje = polje;
    }

    public String getPolje() {
        return polje;
    }
}
