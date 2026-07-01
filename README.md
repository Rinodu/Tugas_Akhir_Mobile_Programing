# CampusMate Pro Native Rebuild from MD

Project ini adalah rebuild native Android Studio dari file `CampusMate Pro — Build Guide` yang sebelumnya berbasis Base44/React.

## Teknologi

- Java
- Android Studio
- XML resource untuk Manifest, Theme, Menu, dan Vector Drawable
- UI utama dibuat programmatically di Java agar mudah dipindahkan dari prototype Base44
- SharedPreferences + Gson sebagai local JSON storage
- BottomNavigationView
- RecyclerView
- AlarmManager + NotificationManager
- Speech Recognition
- Text-to-Speech
- Media picker lokal
- VideoView dan MediaPlayer
- Sensor accelerometer untuk peringatan mode fokus

## Fitur yang Sudah Ada

1. Dashboard
   - Statistik tugas aktif, tugas selesai, catatan, menit fokus, media, dan persentase penyelesaian.
   - Rekomendasi lokal tanpa API.

2. Tugas
   - List tugas dengan RecyclerView.
   - Filter: Semua, Aktif, Selesai, Hari Ini.
   - Search.
   - Tambah/edit/detail/hapus tugas.
   - Toggle status selesai.
   - Reminder deadline lokal.

3. Catatan
   - List catatan dengan RecyclerView.
   - Filter mata kuliah.
   - Search.
   - Tambah/edit/detail/hapus catatan.
   - Speech-to-text untuk input catatan.
   - Text-to-Speech untuk membacakan catatan.

4. Media
   - Pilih file gambar/audio/video dari penyimpanan perangkat.
   - Simpan data media lokal.
   - Preview gambar.
   - Pemutar audio.
   - Pemutar video.

5. Fokus
   - Timer Fokus 25 menit.
   - Timer Istirahat 5 menit.
   - Timer Fokus 50 menit.
   - Riwayat sesi.
   - Sensor accelerometer untuk peringatan jika perangkat sering bergerak.
   - Notifikasi saat sesi selesai.

6. Profil
   - Data profil mahasiswa.
   - Statistik belajar.
   - Edit profil.
   - Reset data.
   - Hapus akun lokal.
   - Footer Riyadhu Jawhar.

## Cara Membuka Project

1. Extract ZIP.
2. Buka Android Studio.
3. Pilih `Open`.
4. Pilih folder `campusmate_pro_native_md`.
5. Tunggu Gradle Sync selesai.
6. Klik Run.

## Build APK

Di Android Studio:

```text
Build > Build Bundle(s) / APK(s) > Build APK(s)
```

APK debug biasanya berada di:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Catatan Penting

- Project ini bukan export resmi dari Base44.
- Project ini adalah rebuild native berdasarkan file MD dan screenshot aplikasi.
- Tidak ada permission INTERNET di AndroidManifest.
- Semua data disimpan lokal.
- Karena UI dibuat cepat dan padat di Java, versi akademik yang lebih rapi bisa dipecah lagi menjadi Fragment, Adapter, Model, Storage, dan XML layout terpisah.


## Update Logo Usage
Logo CampusMate Pro hanya dipakai sebagai launcher icon dan icon notifikasi. Logo tidak ditampilkan lagi di halaman/dashboard/profil aplikasi.
