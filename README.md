# PhotoPlanner

Aplicație web pentru planificarea sesiunilor foto după lumină: ții o listă de locuri bune de
fotografiat și, pentru fiecare, afli la ce oră răsare și apune soarele, când sunt orele de aur,
din ce parte vine lumina și dacă cerul va fi acoperit.

Proiect de facultate (UTCN), Spring Boot 4 + Thymeleaf + H2.

## Ce face

**Locații.** Adaugi un loc cu nume, descriere, coordonate, temă (portret, peisaj, arhitectură,
astro, stradă) și orientarea scenei. Coordonatele se pot alege de pe hartă sau se citesc automat
din datele EXIF ale unei fotografii. Locațiile sunt private implicit; cele publice apar pe harta
de pe prima pagină și în căutare.

**Lumina.** Pentru ziua aleasă se calculează răsăritul, apusul, orele de aur și amurgul
(bibliotecă `commons-suncalc`), în fusul orar al locului — dedus din coordonate, nu preluat de la
server, ca să fie corect și pentru locuri din alt fus. Dacă locația are orientarea scenei, se
arată și din ce parte cade lumina la fiecare oră de aur: contra-lumină, laterală sau frontală.

**Luna.** Faza, răsăritul și apusul ei — pentru fotografia de noapte.

**Vremea.** Gradul de acoperire cu nori la orele de aur, de la Open-Meteo (fără cheie de acces).
Dacă serviciul nu răspunde, pagina se încarcă oricum, fără prognoză.

**Sesiuni foto.** Îți planifici o ieșire: locația, ziua și momentul (răsărit, oră de aur,
zi, apus, noapte). Lista arată ora exactă a momentului ales și prognoza, dacă e destul de aproape.

**Restul.** Fotografii per locație (cu miniaturi generate la încărcare), comentarii, favorite,
căutare după text, temă și distanță față de un punct.

**Cont.** Înregistrare, autentificare, schimbarea parolei, resetarea parolei prin link, ștergerea
contului cu tot ce ține de el — cu opțiunea de a lăsa locațiile publice pe site, sub un cont
anonim, ca să nu se piardă pentru ceilalți.

## Cum se pornește

Are nevoie de un JDK 21 sau mai nou.

```bash
./mvnw.cmd spring-boot:run
```

Dacă apare „The JAVA_HOME environment variable is not defined correctly”, înseamnă că `JAVA_HOME`
nu e setat în shell. Când singurul JDK de pe mașină e cel gestionat de IntelliJ (în `~/.jdks/`),
se pornește așa — cu versiunea instalată la tine:

```bash
export JAVA_HOME="$HOME/.jdks/openjdk-26.0.2.1"
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw.cmd spring-boot:run
```

Apoi <http://localhost:8080>.

Baza de date e H2 pe fișier, în `./data/photoplanner`, iar fotografiile încărcate stau în
`./data/poze`. Niciuna nu e în repository: sunt date locale, nu cod. Schema se creează singură la
prima pornire (`ddl-auto=update`).

### Pornire cu date de demonstrație

Aplicația pornită așa e goală — nu are niciun cont și nicio locație. Pentru o demonstrație
rapidă există profilul `demo`, cu o bază pregătită dinainte:

```bash
./mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=demo
```

Conține patru conturi, trei locații publice cu fotografii, comentarii, favorite, sesiuni foto
planificate, ponderi personalizate pentru doi utilizatori și o cerere de resetare în așteptare —
adică toate cele zece tabele au conținut. Autentificare:

| Email | Parolă |
| --- | --- |
| `ana.pop@exemplu.ro` | `FotografiePeisaj2026` |
| `radu.ionescu@exemplu.ro` | `FotografiePeisaj2026` |
| `maria.dumitru@exemplu.ro` | `AltaParolaComplet9` |
| `dan.stan@exemplu.ro` | `ZiDeToamna2026rece` |

Ana și Radu au aceeași parolă intenționat: în tabelul `utilizatori` se vede că amprentele lor
BCrypt sunt complet diferite, fiindcă fiecare cont primește altă sare. La fel, în
`tokenuri_resetare` se vede că se păstrează doar amprenta SHA-256 a tokenului, de 64 de
caractere, nu tokenul trimis pe email.

Ponderile sunt puse doar pentru Ana și Maria, nu pentru toți: cine nu și-a schimbat nimic n-are
rând în `ponderi_scor` și primește valorile implicite. Ana ține la direcția luminii (pondere 5) și
ignoră temperatura (0); Maria se uită în primul rând la cer și la ploaie.

Spre deosebire de `./data`, directorul `./demo` **este** în repository — e un instantaneu fix,
gândit să însoțească aplicația. Cele două profiluri scriu în directoare diferite, deci demonstrația
nu atinge niciodată baza de lucru. Fotografiile din demo sunt imagini generate, nu fotografii
reale; se pot înlocui încărcând altele din aplicație în timp ce rulează pe profilul `demo`.

Baza de demonstrație se poate deschide și direct din IntelliJ (`View → Tool Windows → Database`),
cu `jdbc:h2:file:./demo/photoplanner-demo`, utilizator `sa`, fără parolă. H2 pe fișier acceptă o
singură conexiune, deci aplicația trebuie oprită întâi.

O a doua instanță nu poate porni cât timp prima rulează — H2 pe fișier nu se lasă deschis de două
ori. Oprește-o întâi pe prima și așteaptă să se elibereze portul 8080.

## Teste

```bash
./mvnw.cmd test
```

216 de teste. Rulează pe o bază proprie, în memorie, cu director temporar pentru poze și fără
acces la internet (`src/test/resources/application.properties`) — nu ating datele reale și merg și
cu aplicația pornită.

## Configurare

Tot ce se poate schimba stă în `src/main/resources/application.properties`:

| Proprietate | Ce face |
| --- | --- |
| `photoplanner.poze.director` | unde se salvează fotografiile |
| `photoplanner.vreme.activ` | `false` oprește cererile către Open-Meteo |
| `photoplanner.resetare.expeditor` | `log` (implicit) sau `email` |
| `photoplanner.resetare.minute` | cât e valabil un link de resetare |
| `photoplanner.resetare.cereri-pe-ora` | câte resetări poate cere un cont într-o oră |
| `photoplanner.login.esecuri-pe-cont` | după câte greșeli se blochează un cont |
| `photoplanner.login.esecuri-pe-ip` | după câte greșeli se blochează un IP |
| `photoplanner.login.minute-blocare` | cât ține blocarea |

## Cum e împărțit codul

```
controller/   paginile și formularele; clasele *Form validează ce vine din formular
service/      logica: lumină, lună, vreme, fus orar, poze, conturi
model/        entitățile JPA
repository/   interfețele Spring Data
templates/    paginile Thymeleaf; fragmente/ ține bucățile comune
```

Comentariile din cod explică *de ce*, nu *ce* — mai ales acolo unde alegerea nu e evidentă
(de ce delogarea e POST, de ce tokenul de resetare se ține ca amprentă, de ce un câmp nou
obligatoriu are nevoie de `columnDefinition`).

## Ce nu e făcut

- **Emailul de resetare nu e activat.** Implicit linkul se scrie în log, ceea ce e bun la
  dezvoltare și nepotrivit pe un server real. Pentru email: `photoplanner.resetare.expeditor=email`
  plus datele SMTP din `spring.mail.*`.
- **Fără migrări de schemă.** `ddl-auto=update` nu poate adăuga o coloană `NOT NULL` într-un tabel
  care are deja rânduri — o face tăcut, iar aplicația cade abia la prima interogare. Orice câmp nou
  obligatoriu are nevoie de `columnDefinition` cu valoare implicită, cât timp nu există Flyway.
- **Fără rol de administrator.** Locațiile trecute la contul anonim „Utilizator șters” nu mai au
  cine să le modereze.
- **Fără export de date**, perechea firească a ștergerii contului.
- Leaflet se încarcă de pe unpkg.com, deci harta are nevoie de internet.
