package ba.sum.fsre.zupniured.model;

public enum StatusDokumenta {
    ZAPRIMLJEN("Zaprimljen"),
    U_OBRADI("U obradi"),
    IZDAN("Izdan"),
    ODBIJEN("Odbijen");
    private final String naziv;
    StatusDokumenta(String naziv) { this.naziv = naziv; }
    public String getNaziv() { return naziv; }
}
