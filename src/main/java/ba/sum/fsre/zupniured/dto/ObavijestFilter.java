package ba.sum.fsre.zupniured.dto;

public enum ObavijestFilter {
    SVE("Sve"), AKTIVNE("Aktivne"), NEAKTIVNE("Neaktivne"), ISTAKNUTE("Istaknute");
    private final String naziv;
    ObavijestFilter(String naziv) { this.naziv = naziv; }
    public String getNaziv() { return naziv; }
}
