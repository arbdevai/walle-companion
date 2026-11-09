# Product Requirement Document (PRD) - Wall-E Desk Companion

## 1. Identitas Proyek & Perangkat Sasaran
- **Nama Aplikasi:** Wall-E Desk Companion (Wall-E Robot Asisten Meja)
- **Package Name:** `com.walle.companion`
- **Target Perangkat:** Samsung Galaxy J1 Ace (Hardware mod: Baterai dibypass modul BMS eksternal 5V/4V, Layar Super AMOLED 4.3 inci 480x800).
- **Min SDK / Target SDK:** 21 (Android 5.0 Lollipop) / 34 (Android 14).
- **Arsitektur Antarmuka:** Android Native View & Canvas System (Tanpa Jetpack Compose demi efisiensi memori RAM 512-768 MB dan performa termal optimal).
- **Tema Desain:** True Neutral OLED (`#000000` murni), bebas AI-slop, kontras tinggi, menyatu sempurna dengan bezel AMOLED.

---

## 2. Karakteristik & Spesifikasi Desain Visual
- **Gaya Lensa (Dual Wall-E Binocular Visor):**
  - Dua modul lensa teropong independen dengan sudut tumpul rounded rectangle / oval khas Wall-E.
  - Sisi luar berbingkai warna netral kontras gelap (`#242424`).
  - Lensa iris dalam menyala dengan warna cyan pekat (`#00E5FF` / `#00B0FF`) dan aksen putih (`#FFFFFF`) untuk refleksi cahaya mata.
  - Sifat dinamis: Lensa mata kiri dan kanan dapat melakukan rotasi/kemiringan (*tilt*) independen untuk mengekspresikan rasa penasaran, bingung, atau fokus.
- **Ekspresi Mata Utama:**
  - `IDLE`: Berkedip acak secara alami, melirik halus ke kiri atau kanan.
  - `HAPPY`: Mata melengkung ke atas membentuk bulan sabit (*crescent/arc*), sedikit menyipit gembira.
  - `CURIOUS` / `CONFUSED`: Satu mata miring ke bawah, satu mata sedikit terangkat (khas Wall-E menatap benda baru).
  - `SURPRISED`: Kedua lensa membesar bulat, iris mengecil.
  - `SLEEPY`: Kedua kelopak mata menutup 75%, intensitas cahaya meredup untuk mode malam/idle panjang.
  - `LISTENING`: Lensa berdenyut lembut (*subtle breathing*) menandakan mikrofon sedang aktif menerima suara.
  - `THINKING`: Iris berputar pelan memproses respons Gemini.
- **Tampilan Informasi Meja (Desk HUD):**
  - Jam digital minimalis (format 24 jam) di bawah mata dengan tipografi monospaced / sans-serif tegas.
  - Indikator status mikro: Indikator koneksi internet dan level pendengaran (audio meter).

---

## 3. Audio & Interaktivitas
- **Audio Engine (Hybrid Sound System):**
  - **Efek Suara Wall-E (Mechanical SFX / Chirps):** Audio sintetis / sample mekanikal pendek untuk reaksi kedip, sentuhan, kaget saat digoyang, dan transisi ekspresi. Dijalankan dengan latensi nol via `android.media.SoundPool`.
  - **Suara Percakapan (Speech Output):** `android.speech.tts.TextToSpeech` bahasa Indonesia bawaan Android untuk menjawab pertanyaan, membaca jam, atau menyapa.
- **Sensor & Input Interaksi:**
  - **Push-to-Talk (Touch Input):** Mengetuk layar/mata memicu mode `LISTENING` dan mengaktifkan `SpeechRecognizer`.
  - **Motion Sensor (Accelerometer):** Mengguncang HP memicu ekspresi `SURPRISED` / pusing dengan suara mechanical chirp.
  - **Light/Time Detection:** Otomatis masuk ke mode `SLEEPY` saat malam hari (di atas jam 22.00).

---

## 4. Lapisan Otak AI (Brain Layer)
- **Penyedia AI:** Google Gemini API (Model: `gemini-1.5-flash` / `gemini-2.0-flash` Free Tier via REST HTTP).
- **Format Output Terstruktur:** Prompt sistem mewajibkan Gemini mengembalikan respons JSON:
  ```json
  {
    "expression": "HAPPY",
    "reply": "Halo bos! Mau lanjut kerja atau istirahat sebentar?"
  }
  ```
- **Fallback Offline:** Jika koneksi internet terputus, robot tetap beroperasi penuh sebagai jam meja ekspresif interaktif dengan respons suara lokal sederhana.

---

## 5. Batasan Arsitektur & Kinerja (Performance Constraints)
- **Zero Local Gradle Build:** Kompilasi biner diserahkan sepenuhnya ke GitHub Actions CI.
- **Batas RAM:** Alokasi heap memori maksimal di bawah 40 MB untuk menjaga kestabilan jangka panjang.
- **Layar Always-On:** `FLAG_KEEP_SCREEN_ON` pada Activity utama dengan efisiensi konsumsi daya AMOLED karena >80% piksel bernilai `#000000` murni.
- **Zero AI-Slop:** Tidak ada emoji dekoratif pada UI, tidak ada teks berjalan, tidak ada warna kebiruan navy `#0F172A`.
