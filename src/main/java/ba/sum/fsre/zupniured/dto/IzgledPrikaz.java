package ba.sum.fsre.zupniured.dto;

public record IzgledPrikaz(boolean prijavljen, boolean interni, boolean administrator,
                           String ime, String uloga, String putanja) {
}
