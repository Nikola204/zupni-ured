# Župni ured — skripta za prezentaciju koda

## 1. Struktura projekta

Ulazna klasa je `ba.sum.fsre.zupniured.ZupniUredApplication`.
`src/main/java` sadrži Java kod, `src/main/resources/templates` HTML predloške,
`static/css` Bootstrap i vlastite stilove, a `fonts` PDF font i licencu.
Konfiguracija je u `application.properties`, ovisnosti u `pom.xml`, a testovi u `src/test/java`.

| Paket | Odgovornost i stvarni primjeri |
| --- | --- |
| `config` | Prijava i inicijalizacija: `SecurityConfig`, `KorisnikStatusFilter`, `PocetniAdministrator` |
| `controller` | HTTP zahtjevi i odabir prikaza: `ZupljaninController`, `KrstenjeController`, `DokumentController` |
| `dto` | Podaci obrazaca i prikaza: `KorisnikForma`, `DokumentForma`, `ZupljaninProfil` |
| `model` | Entiteti baze: `Zupljanin`, `Krstenje`, `Dokument`, `Korisnik` |
| `repository` | Dohvat i spremanje: `ZupljaninRepository`, `KrstenjeRepository`, `DokumentRepository` |
| `service` | Poslovna pravila: `KrstenjeService`, `DokumentService`, `ZupljaninProfilService` |
| `validation` | Provjera mladenaca: `IspravniMladenci`, `MladenciValidator` |

## 2. Controller → Service → Repository → PostgreSQL

Primjer je unos krštenja. `KrstenjeController` prima `KrstenjeForma`, provjerava
`BindingResult` i poziva `KrstenjeService`. Servis provjerava odabranog župljanina,
datume i postojeći zapis. `KrstenjeRepository` sprema entitet u PostgreSQL.
Kod pogreške korisnik ostaje na obrascu s porukom; nakon uspjeha slijedi preusmjeravanje.
Controller upravlja web tokom, a poslovna pravila ostaju u servisu.
Ovisnosti se predaju konstruktorima, što olakšava testiranje.

## 3. JPA i transakcije

`@Entity` povezuje Java klasu s tablicom, `@Id` označava primarni ključ,
a `@JoinColumn` strani ključ. Hibernate provodi JPA preslikavanje i SQL operacije.
Repozitoriji nasljeđuju `JpaRepository`; npr. `findByZupljaninId` izvodi upit prema
nazivu metode. Za složenije dohvaćanje koriste se `@Query` i `JpaSpecificationExecutor`.
`@Transactional` u servisima objedinjuje promjene; `readOnly=true` označava čitanje.
`LAZY` veze dohvaćaju se prema potrebi, a `@EntityGraph` u odabranim upitima učitava
potrebne povezane entitete. Postojeća konfiguracija zadržava Open EntityManager in View
za prikaz povezanih podataka u predlošcima. `ddl-auto=update` priprema shemu pri pokretanju.

## 4. Prijava i sigurnost

`SecurityConfig` postavlja pravila URL-ova, obrazac `/login`, odjavu i BCrypt encoder.
`KorisnikUserDetailsService` učitava račun po emailu i predaje ulogu Spring Securityju.
Spring Security provjerava hash lozinke i čuva prijavu u HTTP sesiji.
`KorisnikStatusFilter` provjerava aktivnost i ulogu računa pri zahtjevu; deaktivacija,
promjena emaila ili uloge prekida staru prijavu. CSRF ostaje uključen, odjava koristi POST.
`PocetniAdministrator` je opcionalan; preko `KorisnikService` stvara ADMIN račun
samo kada nema korisnika. Nema javne registracije ni ugrađene lozinke.

## 5. Uloge i javni dio

ADMIN upravlja korisnicima i svim poslovnim modulima. ZUPNIK i DJELATNIK imaju iste
ovlasti nad postojećim poslovnim modulima, bez pristupa `/korisnici/**`.
Javni su početna stranica, prijava, statički resursi, prikaz grešaka i GET prikazi obavijesti.
`ObavijestService` javno vraća samo aktivne obavijesti kojima je počela objava i nisu istekle.
Dashboard statistike vide prijavljeni korisnici. PDF nije javno dostupan.
Neprepoznate putanje prema sigurnosnoj konfiguraciji zahtijevaju ADMIN ulogu.

## 6. Entiteti i veze

| Veza | Kako je modelirana |
| --- | --- |
| `Krstenje`, `PrvaPricest`, `Krizma` → `Zupljanin` | Obavezni `OneToOne`, jedinstveni strani ključ: najviše jedan zapis svake vrste po osobi |
| `Vjencanje` → mladenci | Dvije opcionalne `ManyToOne` veze na župljane; čuvaju se i imena, pa mladenci mogu biti izvan evidencije |
| `Dokument` → primatelj | Opcionalna `ManyToOne` veza na župljanina uz spremljeno ime i prezime primatelja |
| `Dokument` → sakramentalni zapis | Opcionalne veze na krštenje, pričest, krizmu ili vjenčanje; servis provjerava izvor prema vrsti dokumenta |
| `Obavijest` → autor | Obavezna `ManyToOne` veza na korisnika koji je unio obavijest |
| `Dokument` → `izdaoKorisnik` | Opcionalna veza prije izdavanja; pri prvom izdavanju servis bilježi prijavljenog djelatnika |

`Korisnik` je račun djelatnika, a `Zupljanin` osoba iz evidencije: to nisu isti pojmovi.
`Dogadjaj` i `MisnaNakana` imaju vlastite evidencije. Korisnici i župljani mogu se
deaktivirati; njihove postojeće veze ostaju sačuvane. Veze nemaju kaskadno brisanje.

## 7. Validacija

Bean Validation koristi `@NotBlank`, `@NotNull`, `@Size`, provjere datuma i druge anotacije.
DTO-i poput `KrstenjeForma` i `KorisnikForma` ograničavaju ulazne podatke;
jednostavniji moduli koriste i validaciju entiteta. `@Valid` i `BindingResult`
omogućuju poruke uz polja u Thymeleaf obrascu. `@InitBinder` u kontrolerima koji ga
definiraju ograničava dopuštena polja. Servisi provjeravaju dodatna poslovna pravila,
a baza strani ključ i jedinstvenost. `MladenciValidator` traži ručno ime kada osoba
nije odabrana iz evidencije i odbija istog župljanina u obje uloge mladenaca.

## 8. Profil župljanina

`ZupljaninController` poziva `ZupljaninProfilService`, koji sastavlja DTO `ZupljaninProfil`:
osnovne podatke, pregled triju sakramenata, vjenčanja u kojima je osoba jedan od mladenaca
i povezane dokumente. Profil ne stvara novu tablicu ni kopije sakramenata.
Predložak `zupljani/detalji.html` prikazuje rezultate i poveznice na izvorne evidencije.

## 9. Evidencija dokumenata

`DokumentController` obrađuje obrasce, pretragu i promjenu statusa.
`DokumentService` povezuje primatelja i izvorni zapis te provjerava odgovara li izvor
vrsti dokumenta. `DokumentForma`, `DokumentFilter` i `DokumentStatusForma` prenose ulaz.
Pri prvom izdavanju čuvaju se datum i djelatnik; kasnija promjena statusa ih ne briše.
Evidencija podržava više vrsta dokumenata, ali PDF je implementiran za dvije vrste.

## 10. PDF generiranje

`GET /dokumenti/{id}/pdf` poziva `DokumentPdfService`. Dostupan je samo prijavljenim
djelatnicima i samo za izdani krsni list ili potvrdu o vjenčanju s potrebnim vezama.
PDFBox stvara `PDDocument`; `PdfPisac` uređuje A4 stranice, prelama tekst i ispisuje polja.
Noto Sans učitava se iz classpath resursa i ugrađuje u PDF radi hrvatskih znakova.
Font i licenca SIL OFL 1.1 su u `src/main/resources/fonts`.
`ByteArrayOutputStream` i povratni `byte[]` znače da se PDF generira u memoriji i šalje
kao HTTP odgovor. Produkcijski kod ga ne sprema na disk. PDF se izrađuje pri zahtjevu
iz tadašnjih podataka, nije arhivirana nepromjenjiva datoteka.

## 11. Thymeleaf i Bootstrap

Controller stavlja podatke u `Model`, a Thymeleaf na poslužitelju stvara završni HTML.
`th:each` ponavlja retke, `th:text` sigurno prikazuje tekst, a `th:field` povezuje polja obrasca.
Fragmenti `fragments/stranica.html` i `fragments/layout.html` dijele izgled stranica.
`IzgledAdvice` i `PregledService` pripremaju zajedničke podatke za navigaciju.
Lokalni Bootstrap CSS daje osnovne komponente, a `app.css` stilove projekta.
Pregledniku za te resurse nije potreban CDN.

## 12. Testiranje

JUnit i AssertJ provjeravaju rezultate, Mockito izolira ovisnosti, a Spring Boot testovi
podižu aplikacijski kontekst uz PostgreSQL. MockMvc provjerava HTTP zahtjeve bez
pokretanja zasebnog web poslužitelja. Integracijski testovi koriste transakcijski rollback.

- `KorisnikSigurnostTests`: prijava, uloge, CSRF i promjene sesije.
- `PocetniAdministratorTests` i `KorisnikServiceTests`: inicijalizacija i računi.
- `KrstenjeValidacijaTests`, `KrstenjeServiceTests`, `KrstenjeSpremanjeTests`, `KrstenjeWebTests`:
  validacija, poslovna pravila, ograničenja pri spremanju i web prikaz; slične skupine postoje za ostale sakramente.
- `ZupljaninProfilTests`, `DokumentServiceTests`, `DokumentWebTests`: objedinjeni profil i dokumenti.
- `DokumentPdfTests`: sadržaj PDF-a, hrvatski znakovi, pristup i odbijanje neispravnih zahtjeva.
- `IzgledWebTests` i `ZupniUredApplicationTests`: prikazi i učitavanje konteksta.

Naredbe za čistu provjeru su `./mvnw.cmd clean test` i `./mvnw.cmd clean package`
u PowerShellu (može i zapis `.\mvnw.cmd`). PostgreSQL mora biti dostupan.
Testni PDF/HTML prikazi nastaju samo ispod ignoriranog `target/` direktorija.
