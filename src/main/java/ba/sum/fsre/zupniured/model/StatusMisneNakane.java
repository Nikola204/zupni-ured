package ba.sum.fsre.zupniured.model;
public enum StatusMisneNakane {
    ZAPRIMLJENA("Zaprimljena"), POTVRDENA("Potvrđena"), SLUZENA("Služena"), OTKAZANA("Otkazana");
    private final String naziv;
    StatusMisneNakane(String naziv) { this.naziv = naziv; }
    public String getNaziv() { return naziv; }
}
