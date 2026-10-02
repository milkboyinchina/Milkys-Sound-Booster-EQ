# Kebijakan Privasi Aplikasi Umum

*Tanggal berlaku: 23 Juli 2026 · Terakhir direvisi: 2 Oktober 2026 (penulisan ulang penuh demi kejelasan; substansi tidak berubah, Bagian B khusus aplikasi diperluas).*

## 1. Ringkasan

- Pengembang **tidak mengumpulkan informasi identitas pribadi (PII)**: tanpa nama, email, akun, lokasi, kontak, atau rekaman audio.
- Peningkatan audio (boost, equalizer, preset) diproses **di perangkat Anda secara real-time**. Audio tidak pernah meninggalkan perangkat.
- Saat iklan diaktifkan, **Google (AdMob)** dapat memproses pengenal perangkat dan diagnostik berdasarkan [Kebijakan Privasi Google](https://policies.google.com/privacy). Iklan **nonaktif secara default**.
- Halaman ini terdiri dari dua bagian: **Bagian A** (umum, §§2–7, berlaku untuk semua aplikasi pengembang ini) dan **Bagian B** (khusus aplikasi, §8).

## 2. Cakupan

Kebijakan ini berlaku untuk semua aplikasi seluler yang diterbitkan pengembang, termasuk **Milkys Sound Booster & EQ** (`com.milkys.soundbooster`, lihat §8). Jika ada konflik antara pengungkapan khusus aplikasi dan Bagian A, bagian khusus aplikasi berlaku untuk aplikasi tersebut.

## 3. Data yang dikumpulkan pengembang

**Tidak ada yang mengidentifikasi Anda.** Pengembang tidak mengoperasikan akun, server pelacakan, atau profil pengguna. Data teknis yang strictly anonim dan agregat (mis. diagnostik crash dari kerangka sistem) hanya digunakan untuk memperbaiki stabilitas dan kinerja. Data tersebut tidak dapat mengidentifikasi, melacak lokasi, atau menghubungi individu mana pun.

Pengembang **tidak** mengumpulkan, dan aplikasi **tidak** mengirimkan: lokasi presisi, kontak, isi panggilan atau pesan, foto, rekaman mikrofon, atau profil perilaku untuk iklan.

## 4. Data yang diproses pihak ketiga

Aplikasi mengandalkan kerangka sistem dan layanan iklan dari **Google LLC**. Layanan tersebut tunduk pada kebijakannya sendiri — [Kebijakan Privasi Google](https://policies.google.com/privacy) dan [Privasi Layanan Google Play](https://policies.google.com/privacy) — dan dapat memproses otomatis:

| Data | Tujuan | Pengendali |
|---|---|---|
| ID Iklan | Penayangan iklan, pembatasan frekuensi, pencegahan penyalahgunaan | Google |
| Aktivitas aplikasi (interaksi, status sesi) | Pengukuran relevansi iklan, diagnostik | Google |
| Diagnostik crash & kinerja (stack trace, versi OS, model perangkat) | Stabilitas dan perbaikan bug | Google / pengembang (anonim) |

Tidak ada SDK pihak ketiga lain yang disematkan. Tidak ada login sosial, pelacak analitik, atau broker data lintas aplikasi.

## 5. Periklanan dan persetujuan (Google AdMob / UMP)

- Iklan **nonaktif secara default**. Tidak ada yang terkait iklan ditampilkan sebelum Anda mengaktifkan **Setelan → Iklan**.
- Saat diaktifkan, iklan disajikan oleh Google AdMob. Google dapat menggunakan ID Iklan, aktivitas aplikasi, dan diagnostik sebagaimana dijelaskan di §4.
- **Iklan yang dipersonalisasi** memerlukan persetujuan eksplisit tambahan Anda: **Setelan → Iklan yang Dipersonalisasi**, plus formulir persetujuan Google (UMP) yang ditampilkan bila diwajibkan hukum (mis. EEA/Inggris). Anda dapat mencabut persetujuan kapan saja dari sakelar yang sama; aplikasi kembali ke iklan non-personalisasi atau tanpa iklan sesuai pilihan Setelan → Iklan Anda.
- Selama pengujian tertutup, materi iklan dapat tampil sebagai **iklan pengujian** resmi Google. Tidak perlu tindakan apa pun; unit iklan produksi berlaku pada rilis publik.
- Kelola pengenal Anda kapan saja: Android `Setelan > Google > Iklan > Reset atau Hapus ID iklan`.

## 6. Kendali Anda

- **ID Iklan:** reset atau hapus (`Setelan > Google > Iklan`). Menghapusnya tidak merusak aplikasi.
- **Izin:** tinjau atau cabut izin kapan saja (`Setelan > Aplikasi > [Nama aplikasi] > Izin`). Aplikasi menurun gracefully (mis. mencabut izin overlay hanya menghilangkan widget mengambang).
- **Uninstal:** menghapus aplikasi ikut menghapus semua pengaturan lokal (preset, favorit); tidak ada yang tersisa di server pengembang karena server semacam itu tidak ada.

## 7. Anak, perubahan, kontak

- **Anak:** aplikasi adalah utilitas untuk khalayak umum, bukan untuk anak di bawah 13 tahun. Tidak ada pembuatan profil yang diarahkan pada anak.
- **Perubahan:** pembaruan material diterbitkan di halaman ini dengan tanggal revisi. Penggunaan berlanjut setelah revisi berarti penerimaan.
- **Kontak:** 📧 [webmaster@milkboy.my.id](mailto:webmaster@milkboy.my.id) · 🌐 [milkboy.my.id](https://www.milkboy.my.id)

---

## 8. Bagian B — Milkys Sound Booster & EQ (`com.milkys.soundbooster`)

Penguat audio fidelitas tinggi (hingga 200% / +15 dB) dan equalizer 5-band (60 Hz / 230 Hz / 910 Hz / 3,6 kHz / 14 kHz) untuk Android, dengan tile Pengaturan Cepat, layanan foreground pemutaran media, dan widget mengambang opsional.

**Dirancang di perangkat.** Boost, kurva EQ, preset, favorit, dan pengaturan tidak pernah meninggalkan ponsel. Aplikasi tidak merekam, menyimpan, atau mengunggah audio.

**Izin dan alasan masing-masing dibutuhkan:**

| Izin | Digunakan untuk |
|---|---|
| Notifikasi | Kontrol booster saat peningkatan berjalan |
| Ubah pengaturan audio | Menerapkan equalizer dan peningkatan kenyaringan |
| Tampilkan di atas aplikasi lain (opsional) | Widget kontrol mengambang |
| Layanan foreground, pemutaran media | Menjaga peningkatan berjalan saat digunakan |
| Internet | Menayangkan iklan saat Anda mengaktifkannya (§5) |

*§8 ini diperluas 2 Oktober 2026; mengikuti tanggal berlaku 23 Juli 2026 di atas.*
