package ba.sum.fsre.zupniured.service;

public class KrizmaPodaciException extends RuntimeException {

    public KrizmaPodaciException() {
        super("Odabrani župljanin ne postoji. Odaberite postojećeg župljanina.");
    }

    public KrizmaPodaciException(String poruka) {
        super(poruka);
    }
}
