# WATERNEON - Su Arıtma Müşteri ve Servis Takip Sistemi

WATERNEON, su arıtma sistemleri teknik servis ve saha operasyonları için geliştirilmiş çevrimdışı (offline-first) Android müşteri ve periyodik bakım yönetim uygulamasıdır.

## Genel Bakış

Uygulama, teknik servis ekiplerinin müşteri kayıtlarını, periyodik filtre değişim tarihlerini, randevuları ve geçmiş servis notlarını internet bağlantısına ihtiyaç duymadan cihaz üzerinde güvenle yönetmesini sağlar.

## Temel Özellikler

### Modül Mimarisi
* **Bireysel Müşteriler:** Ev ve kişisel kullanım için montaj ve periyodik bakım takibi (+6 Ay, +1 Yıl, +2 Yıl, +3 Yıl periyot seçenekleri).
* **Lojistik ve Kurumsal Müşteriler:** İşletme, fabrika ve ofisler için firma adı, kurumsal telefon desteği ve kısa periyotlu bakım takibi (+3, +6, +9, +12 Ay seçenekleri).

### Servis ve Randevu Yönetimi
* **Servis Kontrol Paneli:** Günlük iş yükünü, bugünkü randevu sayısını ve acil bakım bekleyen müşteri sayılarını gösteren durum kartları.
* **Randevu Takibi:** İleri tarihli servis randevularının oluşturulması; Tümü, Bugün, Yarın ve Bu Hafta zaman filtreleri ile listeleme.
* **Müşteri Durumu:** Bakımı geciken, yaklaşan, normal süresinde olan veya pasife alınan (susturulan) müşterilerin durum ayrımı.
* **Müşteri Susturma:** Ulaşılamayan veya bakımı erteleyen müşterileri pasife alarak bildirim akışından muaf tutma.

### İletişim ve Entegrasyon
* **Hızlı Arama:** Müşteri kartı üzerinden tek dokunuşla telefon araması başlatma.
* **WhatsApp Entegrasyonu:** Formatı normalize edilmiş telefon numaraları üzerinden doğrudan mesajlaşma başlatma.

### Veri Yönetimi ve Yedekleme
* **Çevrimdışı Mimari:** Veriler cihazın yerel hafızasında SQLite (Room) üzerinde saklanır.
* **Excel Dışa Aktarma:** Müşteri listesini UTF-8 BOM kodlamalı CSV formatında dışa aktarma.
* **JSON Veritabanı Yedeği:** Tüm müşteri ve servis notlarının tam yedeğini alma ve yedekten geri yükleme.
* **Arka Plan Hatırlatıcı:** Günü gelen veya geçen bakımlar için sistem bildirim servisi (WorkManager).

## Mimari ve Teknolojiler

* **Programlama Dili:** Kotlin 1.9
* **Mimari:** MVVM (Model-View-ViewModel), Repository Pattern
* **Veritabanı:** Android Jetpack Room Database (SQLite, Schema v4)
* **Reaktif Veri Akışı:** Kotlin Coroutines & StateFlow
* **Kullanıcı Arayüzü:** Material Design 3, ViewBinding, CoordinatorLayout
* **Arka Plan Görevleri:** Android Jetpack WorkManager
* **Serileştirme:** Google Gson

## Dizin Yapısı

```text
com.aritma.musteritakip/
├── AritmaApplication.kt           # Uygulama başlatıcı ve singleton bağımlılıklar
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt         # Room veritabanı ve göç (migration) tanımları
│   │   ├── dao/                   # CustomerDao, DistrictDao, NoteDao
│   │   └── entity/                # CustomerEntity, DistrictEntity, NoteEntity
│   └── repository/
│       └── CustomerRepository.kt  # Veri erişim katmanı soyutlaması
├── ui/
│   ├── backup/                    # Excel/JSON yedekleme ve geri yükleme
│   ├── bireysel/                  # Müşteri listesi, detay, notlar ve düzenleme
│   ├── lojistik/                  # Kurumsal müşteri modülü listesi
│   ├── main/                      # MainActivity ve modül seçim paneli
│   ├── reservation/               # Randevular ve zaman filtreli servis ekranı
│   └── viewmodel/                 # CustomerViewModel ve ViewModelFactory
├── util/                          # Tarih, diyalog, WhatsApp, Excel ve JSON araçları
└── worker/                        # WorkManager bakım hatırlatıcı servisi
```

## Gereksinimler ve Derleme

### Sistem Gereksinimleri
* Android Studio Iguana / Jellyfish veya daha güncel sürüm
* JDK 17
* Minimum SDK: Android 7.0 (API 24)
* Hedef SDK: Android 14 (API 34)

### Derleme Komutları

```bash
# Debug APK derleme:
./gradlew assembleDebug

# Release APK derleme:
./gradlew assembleRelease
```
