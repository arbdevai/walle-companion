# Project State - Wall-E Desk Companion

## Status Ringkasan
- **Fase Aktif:** Perombakan Visual Mata ke Gaya Cozmo / Anki Vector (v1.1.0)
- **Status PRD:** Disetujui & Tuntas (Android 5.1/minSdk 21, Cozmo/Vector Organic Eye Engine, Hybrid SFX/TTS, Gemini Flash API Push-to-Talk)
- **Kompatibilitas:** Samsung Galaxy J1 Ace (SM-J110G / SM-J110F), True OLED Black (#000000).
- **SemVer Canonical VersionCode:** 10100 (v1.1.0).

---

## Log Perubahan Desain Mata (v1.1.0)
- **Migrasi ke Gaya Cozmo / Anki Vector:**
  - Menghilangkan bingkai kaku abu-abu (`#262626`) yang membuat tampilan tampak kaku/jelek.
  - Mata kini berbentuk *organic rounded polygon* tanpa bezel (*borderless floating OLED eyes*) dengan rasio horizontal gemuk khas Cozmo.
  - Penambahan *dynamic top-slant* untuk ekspresi alis miring alami (penasaran, santai, cemberut, kaget).
  - Penambahan *bottom-arch cutout bezier* untuk ekspresi tertawa/senang (`^ ^`).
  - Animasi pernapasan halus (*subtle breathing pulse*) agar mata terasa hidup saat standby.
  - HUD jam dan status dipindahkan menjadi overlay minimalis di bagian bawah agar mata robot mendominasi layar penuh.
