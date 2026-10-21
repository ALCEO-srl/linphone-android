# linphone-sdk per BcsPhone Android

L'app usa l'AAR di linphone-sdk 5.2 compilato in locale. `LinphoneSdkBuildDir` in `gradle.properties` contiene il
percorso assoluto di `linphone-sdk/build`, che contiene il repository Maven locale (`maven_repository`).
La guida passo passo per un ambiente nuovo è [`BUILD-BCSPHONE.md`](../BUILD-BCSPHONE.md).

## Sorgenti
`linphone-sdk` è il clone Belledonne `release/5.2` @2a2fbb7. Le parti modificate per BcsPhone sono pubblicate nei fork
`ALCEO-srl`, branch `alceo/release/5.2`, e ogni versione dell'app ha il suo tag `bcsphone-android-<versione>`:

| Sottomodulo di linphone-sdk | Fork | 5.2.6 | 5.2.7 |
|---|---|---|---|
| `belle-sip` | [ALCEO-srl/belle-sip](https://github.com/ALCEO-srl/belle-sip) | c702a4a (RC1) | c702a4a |
| `liblinphone` | [ALCEO-srl/liblinphone](https://github.com/ALCEO-srl/liblinphone) | 4594cd266 (RC1) | 762f2d4a1 (RC1 + Fix 1.1) |
| `cmake-builder` | [ALCEO-srl/linphone-cmake-builder](https://github.com/ALCEO-srl/linphone-cmake-builder) | originale (18f0fd9) | db232b8 (16 KB) |

L'AAR della 5.2.6 è del 16/05/2024, quindi è stato compilato dai sorgenti di quella data: il tag
`bcsphone-android-5.2.6` di liblinphone (96cf9900f) è invece il punto di partenza della 5.2.7.

Prima della build:
- `belle-sip` e `liblinphone` vanno copiati dai fork al posto dei sottomoduli. Nella copia vanno cancellati i tag
  `bcsphone-android-*` (`git tag -d`): altrimenti `git describe` restituisce quel tag al posto della versione numerica,
  che finisce in `LIBLINPHONE_GIT_VERSION` (per la 5.2.7: `5.2.12-3-g762f2d4a1`).
- In `cmake-builder` si fa il checkout del tag del fork.
- Nel contenitore `linphone-sdk` serve un commit locale che aggiorni il puntatore a `cmake-builder`: da lì viene la
  versione dell'AAR (per la 5.2.7: `5.2.7-pre.4+1d2788b`).

## Immagine Docker
`gitlab.linphone.org:4567/bc/public/linphone-sdk/bc-dev-android-r20:20210914_update_java11`
(Debian stretch, NDK r20 20.0.5594570, cmake 3.20, utente `bc` uid 1000).

Il registro di Belledonne non è raggiungibile (la porta 4567 non risponde, il problema è dal loro lato), quindi il
`docker pull` non funziona. L'immagine è salvata in
`bc-dev-android-r20.tar`, insieme a `bc-dev-android-r25b.tar` di riserva (NDK r25b). Per caricarla:
```
docker load -i bc-dev-android-r20.tar
docker tag <ID stampato da docker load> gitlab.linphone.org:4567/bc/public/linphone-sdk/bc-dev-android-r20:20210914_update_java11
```

## Build
`sdk/build-sdk.sh [percorso linphone-sdk]` monta la cartella su `/home/bc/linphone-sdk` e compila in `build/`.
Le opzioni sono quelle dell'AAR del 2024, ricavate dal suo `CMakeCache.txt`: solo arm64, `RelWithDebInfo`, e
ADVANCED_IM, LIME_X3DH, ZRTP, LDAP, MKV, ILBC, ISAC disattivati. Se `build/` esiste già, va prima rinominata.
La build del 2024 è conservata in `linphone-sdk/build.2024-05-16`.

## 16 KB
Google Play richiede librerie allineate alle pagine da 16 KB: segmenti LOAD con allineamento di almeno 0x4000 e
fine di GNU_RELRO multipla di 16 KB. Con NDK r20 la fine di RELRO va allineata con
`-Wl,-z,common-page-size=16384`, aggiunto insieme a `max-page-size` in
`cmake-builder/toolchains/android/toolchain-android.cmake` (commit db232b8 del fork).

Per controllare le librerie: `readelf -lW build/libs/arm64-v8a/*.so`.
