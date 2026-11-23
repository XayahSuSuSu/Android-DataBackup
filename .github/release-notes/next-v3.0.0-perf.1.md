Build fork `source-next` yang fokus ke backup lebih cepat dan lebih kecil, serta restore lebih cepat.

> ⚠️ Build percobaan. Ditandatangani dengan kunci fork ini, jadi nggak bisa meng-update APK resmi/Next lain yang sudah terpasang: uninstall dulu (pengaturan app ikut hilang, file backup aman). Coba dulu di data yang punya cadangan lain.

## Baru di Backup → Pengaturan
- **Lewati cache aplikasi** (default: aktif): `cache/` dan `code_cache/` (internal) serta `Android/data/<app>/cache` tidak ikut dicadangkan, di Archive maupun Rustic. Saat restore, Android membuat ulang folder cache dengan izin yang benar, dan aplikasi mengisinya lagi sendiri.
- **Kompresi Archive**: *Cepat* (zstd 1), *Seimbang* (zstd 3 + long-distance matching, default), *Kecil* (zstd 9 + long matching, satu aplikasi sekaligus). Semua arsip tetap bisa dibuka build lama.
- **Pencadangan aplikasi paralel** (Archive): 1–3 aplikasi sekaligus (default 2).

## Di balik layar
- **Rustic incremental benar-benar incremental**: snapshot sebelumnya dengan config yang sama dipakai sebagai parent walau daftar path berubah (misalnya setelah app update). File yang tidak berubah tidak dibaca ulang.
- **Restore Rustic**: index repository dimuat sekali lalu dipakai ulang untuk semua bagian aplikasi, bukan dibuka ulang 6–10 kali per aplikasi.
- **Restore APK** (Android 12+): install memakai `INSTALL_SCENARIO_BULK`, jadi kompilasi ART berat dijalankan nanti di background, tidak saat restore.
- Buffer stream 1 MB, worker zstd dibatasi 4 per stream, dan tar wrapper menyiapkan argumen sebelum `fork()` supaya aman saat beberapa arsip jalan bersamaan.

---

Fork build of `source-next` focused on faster, smaller backups and faster restores: optional app-cache skipping, Archive compression presets, parallel Archive app backups, tag-based Rustic parent snapshots, a reused Rustic index during restores, and bulk APK installs on Android 12+.
