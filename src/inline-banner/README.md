# Inline Banner

Reserved for the host-based inline banner design and implementation batch.
Inline banner Android sekarang diimplementasikan sebagai engine host-based terpisah.

Boundary yang dikunci:

- bukan perluasan dari anchored `BannerAdController`
- identity runtime utama nanti memakai `slotId`
- binding visual memakai `hostId` dan `hostRect`
- boleh reuse helper kecil dari native host, tetapi tidak menggabungkan lifecycle engine
