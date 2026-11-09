# Project State - Wall-E Desk Companion

## Status Ringkasan
- **Fase Aktif:** Standarisasi CI/CD & Keystore Selesai
- **Status PRD:** Disetujui & Tuntas (Android 5.1/minSdk 21, Hybrid Wall-E Eyes + Canvas, Hybrid SFX/TTS, Gemini Flash API Push-to-Talk)
- **Kompatibilitas:** Samsung Galaxy J1 Ace (SM-J110G / SM-J110F), True OLED Black (#000000).
- **SemVer Canonical VersionCode:** 10000 (v1.0.0).

---

## Standarisasi Aturan CI/CD (Berdasarkan Skill)
1. **Zero Local Gradle Build:** Dilarang menjalankan `./gradlew assembleRelease` secara lokal. Kompilasi dan signing dilakukan 100% oleh runner GitHub Actions (`.github/workflows/build-apk.yml`).
2. **Formula Canonical VersionCode:** `versionCode = MAJOR * 10000 + MINOR * 100 + PATCH` -> `10000`.
3. **ABI Split:** Diaktifkan untuk `armeabi-v7a` (arsitektur CPU J1 Ace) dan `arm64-v8a` agar ukuran APK ramping.
4. **Otomatisasi Keystore & Secrets:** Mendukung 4 GitHub Secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) yang didecode langsung pada runner temp.
5. **Ekstraksi Release Notes:** Menggunakan `.github/scripts/generate_release_notes.py` yang memicu auto-publish GitHub Release saat commit diawali dengan `Release v*`.
6. **Pemantauan Run 60 Detik:** Pemantauan workflow dilakukan melalui `gh run watch <RUN_ID> --interval 60`.
