package ba.sum.fsre.zupniured.service;

public class KrstenjePodaciException extends RuntimeException {

    public KrstenjePodaciException() {
        super("Odabrani župljanin ne postoji. Odaberite postojećeg župljanina.");
    }

    public KrstenjePodaciException(String poruka) {
        super(poruka);
    }
}
