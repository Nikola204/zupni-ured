# Župni ured

Diplomski projekt za vođenje župnih evidencija kroz web aplikaciju na hrvatskom jeziku.

## Tehnologije i preduvjeti

Java **21 (JDK)**, Spring Boot 4.1.1, Spring MVC, Spring Data JPA/Hibernate,
Spring Security, PostgreSQL, Thymeleaf, Bootstrap 5.3.8 i Apache PDFBox 3.0.8.
Potreban je pokrenut PostgreSQL te JDK 21 postavljen kroz `JAVA_HOME`.
Maven se preuzima priloženim Maven Wrapperom; za prvo preuzimanje Mavena i ovisnosti
potreban je internet. Bootstrap i PDF font nalaze se u projektu.

## Baza i konfiguracija

U pgAdmin Query Toolu ili psql-u, kao PostgreSQL korisnik s pravom kreiranja baze:

```sql
CREATE DATABASE zupni_ured;
```

Ako baza već postoji, koristite je bez ponovnog kreiranja. Tablice priprema Hibernate
(`spring.jpa.hibernate.ddl-auto=update`). To je zadržano radi jednostavnog pokretanja
diplomskog projekta; `update` nije zamjena za sigurnosnu kopiju baze. Ne koristiti
`create` ili `create-drop` nad bazom s podacima.

| Varijabla | Značenje / zadano |
| --- | --- |
| `JAVA_HOME` | Putanja do lokalne instalacije JDK-a 21 |
| `DB_URL` | `jdbc:postgresql://localhost:5432/zupni_ured` |
| `DB_USERNAME` | `postgres` |
| `DB_PASSWORD` | Stvarna lozinka PostgreSQL korisnika; nema ugrađene lozinke |
| `APP_ZUPA_NAZIV` | Naziv župe u PDF-u; zadano „Župni ured” |
| `APP_ZUPA_ADRESA` | Adresa župe u PDF-u; zadano prazno |
| `APP_ZUPA_MJESTO` | Mjesto župe u PDF-u; zadano prazno |

Primjer postavljanja u PowerShellu (putanju prilagodite svom računalu):

```powershell
$env:JAVA_HOME = 'C:\putanja\do\jdk-21'
$env:DB_URL = 'jdbc:postgresql://localhost:5432/zupni_ured'
$env:DB_USERNAME = 'postgres'
$dbLozinka = Read-Host 'Lozinka PostgreSQL korisnika' -AsSecureString
try {
    $env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', $dbLozinka).Password
} finally {
    $dbLozinka.Dispose()
}
```

Varijable vrijede za taj terminal i procese pokrenute iz njega. Za IntelliJ run
konfiguraciju postavite iste varijable lokalno. Ne spremati lozinke u repozitorij.
Aplikacija ne učitava `.env` automatski.

## Pokretanje i prvi administrator

U korijenu projekta, nakon postavljanja varijabli:

```powershell
.\mvnw.cmd spring-boot:run
```

Otvorite **http://localhost:8080**, odnosno `/login` za prijavu. Zaustavljanje: `Ctrl+C`.
Nema zadane administratorske lozinke ni javne registracije.
Za praznu tablicu korisnika jednokratno postavite `APP_POCETNIADMIN_ENABLED=true`
te `APP_POCETNIADMIN_IME`, `APP_POCETNIADMIN_PREZIME`, `APP_POCETNIADMIN_EMAIL`
i `APP_POCETNIADMIN_LOZINKA`. Cijeli postupak sa skrivenim unosom lozinke i uklanjanjem
varijabli nalazi se u [PRIJAVA.md](PRIJAVA.md).
Ako korisnici već postoje, inicijalizacija ih ne mijenja. Lozinka se sprema BCryptom.

## Testovi i pakiranje

Testovi trebaju dostupan PostgreSQL i iste `DB_*` varijable; nema ugrađene testne baze.
Isključite početnu inicijalizaciju administratora prije testiranja.
Integracijski testovi koriste rollback, ali PostgreSQL sekvence i promjene sheme nisu
obuhvaćene tim rollbackom. Za ponovljiva testiranja može se pripremiti zasebna prazna
baza `zupni_ured_test` i privremeno promijeniti `DB_URL`; vratite URL prije demonstracije.

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean test
.\mvnw.cmd clean package
```

`clean package` ponovno izvršava testove i stvara izvršni JAR. Pokretanje JAR-a
uz iste varijable i dostupnu bazu:

```powershell
& "$env:JAVA_HOME\bin\java.exe" -jar target\zupni-ured-0.0.1-SNAPSHOT.jar
```

Izvještaji testova su u `target/surefire-reports/`. `target/`, uključujući testne PDF-ove,
ne predaje se kao izvorni kod. Nakon rada uklonite lozinku iz terminala:
`Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue`.

## Moduli i dokumentacija

- Korisnici i uloge ADMIN / ZUPNIK / DJELATNIK.
- Župljani i njihov objedinjeni profil; krštenja, prve pričesti, krizme i vjenčanja.
- Događaji, misne nakane, javne obavijesti i interni dashboard.
- Evidencija dokumenata te PDF krsnog lista i potvrde o vjenčanju.

[ARHITEKTURA.md](ARHITEKTURA.md) je praktična skripta za prezentaciju koda.
Za drugo računalo prenesite izvorni projekt ili JAR, postavite JDK i bazu te varijable.
Sami JAR i Git repozitorij ne sadrže podatke baze; za postojeće demonstracijske podatke
potrebna je zasebna sigurnosna kopija i obnova PostgreSQL baze.
