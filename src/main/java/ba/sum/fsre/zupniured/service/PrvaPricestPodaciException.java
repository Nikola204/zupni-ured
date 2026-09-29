package ba.sum.fsre.zupniured.service;

public class PrvaPricestPodaciException extends RuntimeException {

    public PrvaPricestPodaciException() {
        super("Odabrani župljanin ne postoji. Odaberite postojećeg župljanina.");
    }

    public PrvaPricestPodaciException(String poruka) {
        super(poruka);
    }
}
