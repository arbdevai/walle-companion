# Project State - Wall-E Desk Companion

## Status Ringkasan
- **Fase Aktif:** Hotfix Force Close pada Android 5.1 Lollipop (v1.2.1)
- **Status PRD:** Disetujui & Tuntas (Android 5.1/minSdk 21, Cozmo/Vector Organic Eye Engine, Dynamic 7-Segment/OLED Digital Clock, Hybrid SFX/TTS, Gemini Flash API)
- **Kompatibilitas:** Samsung Galaxy J1 Ace (SM-J111F / SM-J110G), True OLED Black (#000000).
- **SemVer Canonical VersionCode:** 10201 (v1.2.1).

---

## Analisis & Solusi Bug Crash Android 5.1 (v1.2.1)
- **Akar Penyebab (Root Cause):**
  1. `SoundEffectManager.kt` menggunakan class `AudioTrack.Builder`, yang baru diperkenalkan pada **API Level 23 (Android 6.0 Marshmallow)**.
  2. Pada Samsung Galaxy J1 Ace (Android 5.1.1 / API Level 22), class ini tidak ada di sistem framework (`java.lang.NoClassDefFoundError: android.media.AudioTrack$Builder`).
  3. Kesalahan ini merupakan turunan `java.lang.Error` (bukan `Exception`), sehingga blok `catch (e: Exception)` tidak menangkapnya dan menyebabkan Force Close 1-2 detik setelah aplikasi terbuka (tepat saat efek suara selamat datang atau kedip mata pertama kali dieksekusi).
  4. Pada HP modern (Android 10+), API level sudah >= 29 sehingga aplikasi berjalan normal.
- **Solusi yang Diterapkan:**
  1. Menambahkan conditional fallback: jika perangkat berjalan di bawah Android 6.0 (`Build.VERSION.SDK_INT < Build.VERSION_CODES.M`), gunakan konstruktor legacy `AudioTrack(AudioManager.STREAM_MUSIC, ...)` yang kompatibel dengan Android 5.0 dan 5.1.
  2. Mengganti blok penanganan error menjadi `catch (_: Throwable)` agar menjamin thread audio tidak akan pernah merusak activity.
  3. Menambahkan null-safety dan try-catch pada `SpeechRecognizerHelper.createSpeechRecognizer()`.
  4. Memasang `Thread.setDefaultUncaughtExceptionHandler` dan pencatatan `last_crash.log`.
