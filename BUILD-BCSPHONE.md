# Come compilare BcsPhone Android da zero

Questa guida spiega come preparare un ambiente nuovo, ricompilare linphone-sdk con le modifiche ALCEO e
produrre l'app BcsPhone (APK di prova e AAB per il Play Store). Il dettaglio tecnico della build dell'SDK è in
[`sdk/README-build.md`](sdk/README-build.md).

Riferimento: versione 5.2.8 (versionCode 50017), ottobre 2026.

## 1. I pezzi

BcsPhone è fatta di tre parti:

| Parte | Repository | Branch / commit |
|---|---|---|
| App | [ALCEO-srl/linphone-android](https://github.com/ALCEO-srl/linphone-android) | `alceo/release/5.2.A17` |
| linphone-sdk (contenitore) | [gitlab.linphone.org/BC/public/linphone-sdk](https://gitlab.linphone.org/BC/public/linphone-sdk) | `release/5.2`, commit `2a2fbb7` |
| Parti modificate dell'SDK | [ALCEO-srl/belle-sip](https://github.com/ALCEO-srl/belle-sip), [ALCEO-srl/liblinphone](https://github.com/ALCEO-srl/liblinphone), [ALCEO-srl/linphone-cmake-builder](https://github.com/ALCEO-srl/linphone-cmake-builder) | `alceo/release/5.2`, tag `bcsphone-android-<versione>` |

linphone-sdk è il progetto di Belledonne, usato così com'è. Di nostro ci sono solo tre suoi sottomoduli, pubblicati
nei fork ALCEO e marcati con un tag per ogni versione dell'app che li usa. L'ultimo tag è `bcsphone-android-5.2.7`:
la 5.2.8 usa lo stesso SDK della 5.2.7.

L'app non contiene il codice dell'SDK: usa l'AAR prodotto dalla build di linphone-sdk, letto da un repository Maven
locale. Le due cartelle stanno una accanto all'altra:

```
BcsPhone.5.1/
├── linphone-android.5.1.bcsws/   app (questo repository)
└── linphone-sdk/                 SDK; la build finisce in linphone-sdk/build/
```

Se l'SDK non cambia, non serve ricompilarlo: basta copiare la cartella `linphone-sdk/build` di una build precedente
e passare al [punto 6](#6-compilare-lapp).

## 2. File che non stanno su git

Questi file sono segreti o troppo grandi per git e vanno chiesti al responsabile del progetto.

> **Da completare:** dove sono conservati (la posizione non è ancora stata decisa).

| File | Dove va | A cosa serve |
|---|---|---|
| `keystore.properties` | radice dell'app | Gradle lo legge sempre: senza, il progetto non si apre. Il suo `storeFile` è un percorso relativo ad `app/` |
| il file `.jks` indicato in `keystore.properties` | dove dice `storeFile` | firma configurata in Gradle |
| `bcsphone-release-key.jks` | `keystore/` nella radice dell'app | chiave di caricamento per il Play Store (*Generate Signed Bundle*). Le chiavi di firma vere le tiene Google (Play App Signing) |
| `google-services.json` | `app/` | Firebase: notifiche push e Crashlytics. Senza, l'app si compila ma senza push |
| `bc-dev-android-r20.tar` | ovunque | immagine Docker per compilare l'SDK (vedi [punto 4](#4-immagine-docker)) |
| `bc-dev-android-r25b.tar` | ovunque | immagine di riserva con NDK r25b, oggi non usata |

La cartella `keystore/` non va mai committata.

## 3. Ambiente

Un qualsiasi Linux a 64 bit, in una macchina virtuale o no. La configurazione di riferimento è Manjaro in una VM.

Serve:
- `git`, `bsdtar` (o `unzip`), `readelf` (pacchetto binutils);
- Docker, con il proprio utente nel gruppo `docker` (`sudo usermod -aG docker $USER`, poi rientrare nella sessione);
- spazio su disco: le due immagini Docker occupano circa 27 GB, la build dell'SDK diversi GB, Gradle e Android
  Studio altri ancora. Contare almeno 80 GB liberi;
- memoria: Gradle è configurato con 9 GB (`org.gradle.jvmargs=-Xmx9216m` in `gradle.properties`), quindi almeno
  16 GB di RAM;
- Android Studio recente (riferimento: 2026.2, build AI-262), installato per esempio in `~/android-studio`. Il JDK
  è quello incluso (JBR): per usare Gradle da terminale, `export JAVA_HOME=$HOME/android-studio/jbr`;
- dall'SDK Manager di Android Studio: Android SDK Platform 37, Build-Tools 37 e Platform-Tools (per `adb`).
  L'SDK Android finisce in `~/Android/Sdk`.

Il progetto usa Android Gradle Plugin 9.4 e Gradle 9.8 (scaricato da solo dal wrapper `gradlew`).

## 4. Immagine Docker

L'SDK si compila dentro l'immagine Docker di Belledonne con NDK r20, la stessa usata per le versioni precedenti.
Il loro registro (`gitlab.linphone.org:4567`) non è raggiungibile: la porta non risponde, è un problema loro. Il
`docker pull` quindi non funziona, e i file `.tar` sono l'unica copia dell'immagine: vanno conservati.

```
docker load -i bc-dev-android-r20.tar
docker images      # prendere l'IMAGE ID appena caricato
docker tag <IMAGE ID> gitlab.linphone.org:4567/bc/public/linphone-sdk/bc-dev-android-r20:20210914_update_java11
```

Il nome con cui va etichettata è quello che usa lo script di build.

## 5. Compilare linphone-sdk

### 5.1 Sorgenti

```
mkdir BcsPhone.5.1 && cd BcsPhone.5.1
git clone https://github.com/ALCEO-srl/linphone-android.git linphone-android.5.1.bcsws
git -C linphone-android.5.1.bcsws checkout alceo/release/5.2.A17

git clone https://gitlab.linphone.org/BC/public/linphone-sdk.git
cd linphone-sdk
git checkout 2a2fbb7
git submodule update --init --recursive
```

### 5.2 Le parti ALCEO

`belle-sip` e `liblinphone` vanno sostituiti con i fork, al tag della versione:

```
TAG=bcsphone-android-5.2.7
rm -rf belle-sip liblinphone
git clone --branch $TAG https://github.com/ALCEO-srl/belle-sip.git belle-sip
git clone --branch $TAG https://github.com/ALCEO-srl/liblinphone.git liblinphone
git -C belle-sip tag -d $(git -C belle-sip tag -l 'bcsphone-android-*')
git -C liblinphone tag -d $(git -C liblinphone tag -l 'bcsphone-android-*')
```

I tag `bcsphone-android-*` vanno cancellati **nella copia locale** (non nei fork): la versione di liblinphone viene
da `git describe`, e con il nostro tag presente risulterebbe `bcsphone-android-5.2.7` invece di quella numerica
(per la 5.2.7: `5.2.12-3-g762f2d4a1`).

`cmake-builder` resta il sottomodulo originale, portato al tag del fork (contiene le opzioni del linker per le
pagine da 16 KB richieste dal Play Store):

```
git -C cmake-builder remote add alceo https://github.com/ALCEO-srl/linphone-cmake-builder.git
git -C cmake-builder fetch alceo --tags
git -C cmake-builder checkout $TAG
git add cmake-builder
git commit -m "cmake-builder: 16 KB page size linker flags (BcsPhone Android)"
```

Questo commit locale nel contenitore serve: la versione dell'AAR viene da `git describe` di linphone-sdk. Per la
5.2.7 era `5.2.7-pre.4+1d2788b`; rifacendolo cambia solo l'hash finale, e all'app va bene (prende la versione
5.2 più recente che trova).

### 5.3 Build

Dalla cartella dell'app:

```
sdk/build-sdk.sh            # usa ../linphone-sdk; altrimenti: sdk/build-sdk.sh /percorso/linphone-sdk
```

Lo script avvia il container Docker, monta linphone-sdk e compila in `linphone-sdk/build/` solo per arm64, con le
stesse opzioni delle build precedenti. Se `build/` esiste già, si ferma: rinominarla prima (per esempio
`build.2026-10-19`), così la build precedente resta a disposizione. Ci vuole parecchio tempo.

Alla fine devono esserci:
- `build/maven_repository/org/linphone/linphone-sdk-android/<versione>/…aar`: l'AAR usato dall'app;
- `build/libs/arm64-v8a/` e `build/libs-debug/arm64-v8a/`: le librerie, senza e con i simboli di debug (le seconde
  servono a Crashlytics).

Controllo delle pagine da 16 KB: in ogni libreria i segmenti `LOAD` devono avere allineamento `0x4000`.

```
readelf -lW build/libs/arm64-v8a/*.so | grep -E "^File|LOAD"
```

## 6. Compilare l'app

1. In `gradle.properties`, `LinphoneSdkBuildDir` deve puntare alla cartella `linphone-sdk/build` (percorso
   assoluto). Nel repository c'è quello della VM di riferimento: se le cartelle sono altrove, correggerlo in locale.
2. Copiare i file del [punto 2](#2-file-che-non-stanno-su-git) al loro posto.
3. Aprire la cartella dell'app in Android Studio e fare il *Sync* di Gradle. Nella finestra Build compare la riga
   `Linphone SDK used is <versione> from local linphone-sdk maven repository`: controllare che sia quella appena
   compilata.

### Prova sul telefono

- **Debug** (`debug`): si installa come app separata, `com.alceo.bcsphone.debug`, con il proprio nome e il proprio
  Firebase, quindi convive con quella del Play Store. Se le due app hanno lo stesso account, squillano entrambe:
  disattivare l'account in una delle due.
- **Release** (`releaseWithCrashlytics`): è quella che va agli utenti, con R8 che riduce e ottimizza il codice. Va
  provata prima di ogni pubblicazione, perché alcuni problemi (classi tolte da R8, Gson, Firebase) compaiono solo
  qui.

### AAB per il Play Store

1. La versione è in cima ad `app/build.gradle`: `appVersionName` (5.2.x) e `appVersionCode` (da aumentare di uno
   a ogni pubblicazione).
2. *Build → Generate Signed Bundle / APK → Android App Bundle*, chiave `keystore/bcsphone-release-key.jks`,
   variante `releaseWithCrashlytics`.
3. La build carica su Crashlytics la mappa di R8 e i simboli nativi dell'SDK
   (`uploadCrashlyticsMappingFileReleaseWithCrashlytics`, `uploadCrashlyticsSymbolFileReleaseWithCrashlytics`):
   serve la connessione. Senza, i crash arrivano illeggibili.
4. L'AAB è in `app/build/outputs/bundle/releaseWithCrashlytics/` (o nella cartella scelta nella procedura).
   Caricarlo sul Play Console, prima in test interno.

## 7. Cose da sapere

- **Log in release:** i messaggi informativi dell'app non arrivano a `adb logcat`, solo avvisi ed errori. Per
  seguire nel dettaglio cosa succede, usare la debug.
- **Suoneria muta e niente notifica a comparsa:** prima di cercare un bug, controllare che sul telefono non sia
  attivo *Non disturbare*.
- **R8:** se la release si chiude all'avvio e la debug no, quasi sempre R8 ha tolto qualcosa che serve. Le regole
  sono in `app/proguard-rules.pro`. Per esempio, Firebase richiede che restino i costruttori dei
  `ComponentRegistrar`.
- **Registrazione delle chiamate:** l'SDK è compilato senza supporto MKV (`ENABLE_MKV=OFF`), quindi la registrazione
  non è disponibile e i relativi comandi sono nascosti.
- **Immagini Docker:** i `.tar` sono l'unica copia raggiungibile. Non cancellarli.
- **Segreti:** `keystore/`, `keystore.properties` e `google-services.json` non vanno mai committati.
