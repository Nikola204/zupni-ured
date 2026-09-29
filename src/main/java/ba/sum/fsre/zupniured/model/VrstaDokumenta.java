package ba.sum.fsre.zupniured.model;

public enum VrstaDokumenta {
    KRSNI_LIST("Krsni list"),
    POTVRDA_O_PRVOJ_PRICESTI("Potvrda o prvoj pričesti"),
    POTVRDA_O_KRIZMI("Potvrda o krizmi"),
    POTVRDA_O_SLOBODNOM_STANJU("Potvrda o slobodnom stanju"),
    POTVRDA_O_VJENCANJU("Potvrda o vjenčanju"),
    OSTALO("Ostalo");
    private final String naziv;
    VrstaDokumenta(String naziv) { this.naziv = naziv; }
    public String getNaziv() { return naziv; }
}
