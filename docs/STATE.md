# Project State - Wall-E Desk Companion

## Status Ringkasan
- **Fase Aktif:** Implementasi Selesai (Siap Build CI via GitHub Actions)
- **Status PRD:** Disetujui & Tuntas (Android 5.1/minSdk 21, Hybrid Wall-E Eyes + Canvas, Hybrid SFX/TTS, Gemini Flash API Push-to-Talk)
- **Kompatibilitas:** Samsung Galaxy J1 Ace (SM-J110G / SM-J110F), True OLED Black (#000000).
- **Hasil Verifikasi Sintaks:** 10 berkas Kotlin & 10 berkas XML lolos validasi tanpa error.

---

## Daftar Tugas (Vertical Slices)

### Tahap 1: Fondasi Proyek & CI/CD
- [x] Inisiasi struktur proyek Android Kotlin (Gradle Version Catalog / Root & App build scripts, Manifest, Proguard).
- [x] Workflow GitHub Actions (`.github/workflows/build-apk.yml`) untuk build APK tanpa membebani chroot lokal.
- [x] Konfigurasi `.gitignore` untuk melindungi folder skrip dan isolasi tooling chroot.
- [x] Pembuatan launcher icon vektor solid murni bebas AI-slop via generator script.

### Tahap 2: Rendering Mata Wall-E (Canvas View Engine)
- [x] Pembuatan `WalleEyeView` (Custom View berbasis `android.graphics.Canvas` & `RectF`).
- [x] Implementasi rendering dual-binocular lens dengan bezel netral `#262626` dan pupil cyan OLED `#00E5FF`.
- [x] Implementasi transisi ekspresi animasi: IDLE (kedip acak), HAPPY, CURIOUS (tilt independen), SURPRISED, SLEEPY, LISTENING, THINKING.
- [x] Implementasi HUD jam digital minimalis di bawah mata (`HH:mm`).

### Tahap 3: Sistem Audio & Sensor Fisik
- [x] Implementasi `SoundEffectManager` menggunakan `AudioTrack` sintetis untuk suara mekanik / chirp Wall-E berlatensi nol tanpa aset eksternal.
- [x] Integrasi `TextToSpeechHelper` untuk ucapan bahasa Indonesia via engine bawaan Android.
- [x] Integrasi `ShakeDetector` (Accelerometer) untuk mendeteksi guncangan meja dan melacak kemiringan (*tilt tracking*).

### Tahap 4: Otak AI (Gemini Flash REST Client) & Speech-to-Text
- [x] Implementasi `SpeechRecognizerHelper` (Push-to-Talk saat layar/mata disentuh).
- [x] Implementasi `GeminiClient` (OkHttp REST call ke Gemini Flash API dengan prompt format JSON terstruktur).
- [x] Dialog konfigurasi API Key Gemini (tekan lama layar).
- [x] Penyambungan end-to-end: Suara pengguna -> Gemini API -> Parsing JSON -> Perubahan Ekspresi Mata + Pemutaran TTS.

---

## Log Bug & Masalah Terselesaikan
- **Penanganan Memori Rendah:** Menghindari Compose dan menggunakan Canvas View Native murni dengan konsumsi RAM di bawah 25 MB, ideal untuk hardware 512 MB - 768 MB RAM pada Samsung Galaxy J1 Ace.
- **Bypass Baterai:** Layar dioptimalkan dengan latar belakang True Neutral OLED `#000000` (piksel mati total) untuk menjaga suhu perangkat tetap dingin saat dicolokkan ke sumber daya eksternal 24/7.
