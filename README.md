# Primjena modela strojnog učenja za prepoznavanje objekata u mobilnim aplikacijama

## Opis projekta

Projekt predstavlja sustav za detekciju objekata pomoću Android mobilne aplikacije i udaljenog poslužitelja.

Android aplikacija koristi kameru mobilnog uređaja za dohvat slike. Okviri slike pripremaju se za slanje i šalju udaljenom poslužitelju putem WebSocket veze. Na udaljenom poslužitelju izvršava se YOLOv8n model za detekciju objekata. Nakon obrade poslužitelj vraća rezultat mobilnoj aplikaciji, koja prikazuje detektirane objekte pomoću graničnih okvira i naziva klasa.

Sustav se sastoji od dva glavna dijela:

- Android mobilne aplikacije
- poslužiteljskog dijela s FastAPI aplikacijom i YOLOv8n modelom

Android aplikacija nalazi se u mapi `app`, dok se poslužiteljski dio nalazi u mapi `server`.

## Android aplikacija

Android aplikacija razvijena je u Kotlinu i otvorena je u Android Studiju.

Za rad s kamerom koristi se CameraX, a za komunikaciju s poslužiteljem koristi se WebSocket veza pomoću biblioteke OkHttp.

Aplikacija obavlja sljedeće korake:

1. pristupa kameri mobilnog uređaja
2. dohvaća okvire slike
3. priprema okvir za slanje
4. pretvara sliku u JPEG format
5. šalje okvir poslužitelju putem WebSocket veze
6. prima rezultat detekcije u JSON formatu
7. obrađuje podatke o detektiranim objektima
8. prilagođava koordinate graničnih okvira prikazu kamere
9. prikazuje granične okvire i nazive detektiranih klasa

### Pokretanje Android aplikacije

1. Otvoriti ovaj projekt u Android Studiju.
2. Pričekati završetak Gradle sinkronizacije.
3. Spojiti Android uređaj.
4. Omogućiti USB ili bežično otklanjanje pogrešaka prema načinu korištenom za testiranje.
5. Prije pokretanja detekcije pokrenuti poslužiteljski dio i uspostaviti SSH tunel.
6. Pokrenuti aplikaciju iz Android Studija.
7. Dopustiti aplikaciji pristup kameri ako se zatraži.

## Poslužiteljski dio

Poslužiteljski dio nalazi se u mapi `server`.

Glavna Python datoteka poslužiteljske strane je:

    main_ws_roi_v6.py

FastAPI aplikacija koristi WebSocket endpoint:

    /ws/detect

Na poslužitelju se koristi Python virtualno okruženje s potrebnim bibliotekama, a detekcija se izvršava pomoću YOLOv8n modela. U korištenoj konfiguraciji model se izvršava na CPU-u.

### Pokretanje poslužitelja

Na udaljenom poslužitelju potrebno je prijeći u direktorij projekta:

    cd ~/diplomski-object-detection

Zatim aktivirati virtualno Python okruženje:

    source venv/bin/activate

Nakon toga pokrenuti FastAPI aplikaciju:

    uvicorn main_ws_roi_v6:app --host 0.0.0.0 --port 8000

Nakon pokretanja poslužitelj radi na portu `8000`.

Popis potrebnih Python ovisnosti nalazi se u:

    server/requirements.txt

Pristupni podaci za udaljeni poslužitelj nisu dio repozitorija.

## Povezivanje putem SSH tunela

Tijekom razvoja i testiranja Android uređaj pristupa poslužitelju preko SSH tunela uspostavljenog na računalu.

SSH tunel omogućuje prosljeđivanje komunikacije između lokalnog računala i udaljenog poslužitelja.

Pojednostavljeno, komunikacija izgleda ovako:

    Android uređaj
          |
          | WebSocket
          v
       računalo
          |
          | SSH tunel
          v
    udaljeni poslužitelj
          |
          | YOLOv8n
          v
       rezultat
          |
          v
    Android aplikacija

Za uspostavljanje SSH tunela koriste se vlastiti pristupni podaci prema udaljenom poslužitelju.

U konfiguraciji aplikacije potrebno je koristiti adresu odgovarajuću načinu povezivanja koji se koristi tijekom testiranja. Stvarna IP adresa poslužitelja, korisničko ime, lozinka i privatni SSH ključ nisu navedeni u ovom repozitoriju.

## Komunikacija aplikacije i poslužitelja

Android aplikacija šalje JPEG okvir slike putem WebSocket veze.

Poslužitelj prima okvir, izvršava YOLOv8n detekciju i vraća rezultat u JSON formatu.

Rezultat sadrži podatke o dimenzijama slike, broju detektiranih objekata, vremenu izvođenja detekcije i pronađenim objektima.

Primjer rezultata:

    {
        "image_width": 640,
        "image_height": 480,
        "rotation": 0,
        "num_detections": 1,
        "inference_time_ms": 1120.5,
        "detections": [
            {
                "class": "chair",
                "confidence": 0.87,
                "box": {
                    "x1": 120.0,
                    "y1": 85.0,
                    "x2": 310.0,
                    "y2": 420.0
                }
            }
        ]
    }

Android aplikacija koristi dobivene podatke za prikaz graničnog okvira na odgovarajućem mjestu preko slike kamere.

## Osnovno testiranje

Za osnovnu provjeru rada sustava potrebno je:

1. pokrenuti poslužiteljski dio na udaljenom poslužitelju
2. uspostaviti SSH tunel na računalu
3. pokrenuti Android aplikaciju
4. omogućiti aplikaciji pristup kameri
5. usmjeriti kameru prema objektu koji pripada jednoj od klasa koje podržava YOLOv8n model
6. provjeriti uspostavljanje WebSocket veze
7. provjeriti slanje okvira prema poslužitelju
8. provjeriti primitak rezultata detekcije
9. provjeriti prikaz graničnog okvira i naziva detektirane klase u aplikaciji

## Ispitivanje performansi

U sklopu rada provedena su ispitivanja performansi sustava.

Ispitivano je:

- vrijeme izvođenja YOLOv8n modela
- utjecaj ulazne veličine modela
- latencija od slanja slike do primitka rezultata
- utjecaj broja CPU dretvi na vrijeme izvođenja
- ponašanje poslužitelja pri istovremenom radu više klijenata

### Benchmark veličine ulazne slike

Skripta `benchmark_imgsz.py` nalazi se u mapi `server`, a slike korištene za test nalaze se u mapi `server/benchmark_images`.

Za pokretanje testa potrebno je prijeći u mapu `server`:

    cd server

i pokrenuti:

    python benchmark_imgsz.py

### Benchmark broja CPU dretvi

Skripta `benchmark_threads.py` nalazi se u mapi `server`.

Iz mape `server` pokreće se naredbom:

    python benchmark_threads.py

### Test opterećenja s više klijenata

Za ispitivanje ponašanja poslužitelja pri istovremenom radu više klijenata koristi se skripta:

    load_test.py

Za test se koristi slika:

    test_frame.jpg

Primjer pokretanja testa s 10 klijenata u trajanju od 120 sekundi:

    python load_test.py --clients 10 --duration 120 --image test_frame.jpg

Broj klijenata i trajanje testa mogu se prilagoditi prema potrebi.

Sažeti rezultati spremaju se u:

    load_test_results.csv

Pojedinačne latencije svih zahtjeva spremaju se u:

    load_test_latencies.csv

### Test kvalitete detekcije

Materijali za ispitivanje kvalitete detekcije nalaze se u mapi:

    server/quality_test

Test koristi slike iz mape `images` i pripadajuće oznake iz mape `labels`.

Za pokretanje testa potrebno je prijeći u mapu `quality_test`:

    cd ~/diplomski-object-detection/quality_test

Aktivirati virtualno Python okruženje:

    source ../venv/bin/activate

Pokrenuti test:

    python quality_test.py

Rezultati se spremaju u datoteke:

    quality_results_raw.csv
    quality_summary.csv

### Test prekoračenja vremena čekanja WebSocket odgovora

Za provjeru ponašanja mobilne aplikacije u slučaju zakašnjelog odgovora poslužitelja koristi se pomoćni testni poslužitelj:

    server/timeout_test/main_ws_timeout_test.py

Testni poslužitelj namjerno odgađa slanje odgovora za 6 sekundi, dok mobilna aplikacija čeka odgovor najviše 5 sekundi.

Test provjerava sljedeći scenarij:

1. mobilna aplikacija šalje JPEG okvir poslužitelju
2. poslužitelj izvršava detekciju, ali namjerno odgađa odgovor
3. nakon 5 sekundi mobilna aplikacija registrira prekoračenje vremena čekanja
4. stara WebSocket veza se zatvara
5. pokreće se ponovno povezivanje
6. uspostavlja se nova WebSocket veza
7. zakašnjeli odgovor stare veze ne koristi se u mobilnoj aplikaciji

Test je uspješno proveden. Poslužitelj je u testnom scenariju pokušao poslati odgovor nakon 6 sekundi, nakon što je mobilna aplikacija već zatvorila staru vezu zbog isteka vremena čekanja. Nakon toga mobilna aplikacija uspostavila je novu WebSocket vezu i nastavila s obradom novih okvira.

Testni poslužitelj služi isključivo za provjeru ponašanja pri prekoračenju vremena čekanja i ne koristi se za normalan rad aplikacije.

## Korištene tehnologije

### Android

- Kotlin
- Android Studio
- Jetpack Compose
- CameraX
- OkHttp
- WebSocket

### Poslužitelj

- Python
- FastAPI
- Uvicorn
- YOLOv8n
- Ultralytics
- OpenCV
- NumPy
- PyTorch

## Napomena

Ovaj repozitorij ne sadrži lozinke, privatne SSH ključeve, API ključeve niti druge pristupne podatke.

Sustav je razvijen i testiran u konfiguraciji opisanoj u diplomskom radu. Trenutna izvedba koristi udaljeni poslužitelj za izvođenje modela i ne predstavlja kontinuiranu detekciju videa u klasičnom stvarnom vremenu.