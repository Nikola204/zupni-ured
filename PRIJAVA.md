# Prijava djelatnika župnog ureda

Korisnik je djelatnik koji se prijavljuje u aplikaciju. Župljani nisu korisnički računi.

| Putanja | Pristup |
| --- | --- |
| / i /login | Javno |
| /css/**, /js/**, /images/**, /webjars/**, /favicon.ico | Javno |
| /korisnici i /korisnici/** | ADMIN |
| /dogadjaji, /zupljani, /krstenja, /prva-pricest, /krizma, /vjencanja, /misne-nakane, /dokumenti i njihove podputanje | ADMIN, ZUPNIK, DJELATNIK |
| GET /obavijesti i GET /obavijesti/{id} | Javno, samo aktivne i vremenski važeće obavijesti |
| /admin/obavijesti i /admin/obavijesti/** | ADMIN, ZUPNIK, DJELATNIK |
| Ostale putanje | ADMIN (osim javnog prikaza grešaka /error) |

ADMIN dodaje i uređuje sve korisnike, mijenja njihove uloge te ih deaktivira ili ponovno aktivira.
ZUPNIK i DJELATNIK imaju pristup svim poslovnim modulima, ali ne upravljanju korisnicima.
PDF endpoint `/dokumenti/{id}/pdf` zahtijeva prijavu s jednom od triju uloga.
Početna stranica javno prikazuje obavijesti; statistike prikazuje samo prijavljenima.

Prijava koristi email (bez razlikovanja velikih i malih slova) i lozinku.
Uspješna prijava vodi na početnu stranicu s poveznicama prema dopuštenim modulima.
Odjava se šalje POST obrascem. CSRF zaštita ostaje uključena za sve obrasce.

Lozinke se spremaju kao BCrypt hash. Nova lozinka mora imati najmanje 8 znakova,
a najviše 72 UTF-8 bajta. Pri uređivanju prazno polje zadržava postojeću lozinku.
Hash i unesena lozinka nikada se ne popunjavaju u HTML obrascu.
Email je normaliziran i ima jedinstveno ograničenje u bazi; datum kreiranja postavlja JPA pri unosu.
Korisnici se deaktiviraju, ne brišu. Nakon deaktivacije, promjene emaila ili uloge
postojeća prijava prestaje vrijediti pri sljedećem zahtjevu.

## Prvi administrator

Nema ugrađenog računa, zadane lozinke ni javne registracije.
Opcionalna inicijalizacija radi samo kada je tablica korisnika prazna.
Postojeće račune i lozinke nikada ne prepisuje.

Najprije postavite JDK 21, pripremite PostgreSQL bazu i varijable `DB_*`
prema [README.md](README.md). U PowerShellu u korijenu projekta pokrenite sljedeće.
Lozinka se unosi skriveno i ne piše se u projektne datoteke:

```powershell
$env:APP_POCETNIADMIN_ENABLED = 'true'
$env:APP_POCETNIADMIN_IME = Read-Host 'Ime administratora'
$env:APP_POCETNIADMIN_PREZIME = Read-Host 'Prezime administratora'
$env:APP_POCETNIADMIN_EMAIL = Read-Host 'Email administratora'
$pocetnaLozinka = Read-Host 'Lozinka administratora' -AsSecureString
try {
    $env:APP_POCETNIADMIN_LOZINKA = [System.Net.NetworkCredential]::new('', $pocetnaLozinka).Password
    .\mvnw.cmd spring-boot:run
} finally {
    Remove-Item Env:APP_POCETNIADMIN_ENABLED, Env:APP_POCETNIADMIN_IME,
        Env:APP_POCETNIADMIN_PREZIME, Env:APP_POCETNIADMIN_EMAIL,
        Env:APP_POCETNIADMIN_LOZINKA -ErrorAction SilentlyContinue
    $pocetnaLozinka.Dispose()
}
```

Otvorite http://localhost:8080/login i prijavite se unesenim podacima.
Dok aplikacija radi, inicijalizacijski podaci ostaju u okruženju tog procesa.
Nakon prvog pokretanja zaustavite aplikaciju (Ctrl+C) i ubuduće je pokrećite
samo naredbom ` .\mvnw.cmd spring-boot:run `, bez inicijalizacijskih varijabli.
Neispravni ili nepotpuni podaci za prvi račun zaustavljaju inicijalizaciju
s validacijskom porukom, umjesto stvaranja neupotrebljivog računa.

## Testovi

```powershell
.\mvnw.cmd clean test
```

Testovi koriste PostgreSQL iz varijabli `DB_*`; detalji su u README-u.
Hibernate prema postavci `ddl-auto=update` priprema tablice i ograničenja.
Integracijski testovi svoje podatke vraćaju transakcijskim rollbackom.
Prije testiranja uklonite inicijalizacijske varijable prvog administratora.
Provjeravaju prijavu, ovlasti, CSRF, hrvatske obrasce, hashiranje lozinki,
uređivanje i prekid sesije nakon deaktivacije ili promjene uloge.
