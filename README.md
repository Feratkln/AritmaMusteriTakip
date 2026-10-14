# 💧 WATERNEON • Su Arıtma Müşteri ve Servis Takip Sistemi

**WATERNEON**, su arıtma sistemleri teknik servisleri ve saha operasyonları için geliştirilmiş modern, hızlı ve %100 çevrimdışı (offline-first) çalışan bir Android müşteri ve periyodik bakım yönetim uygulamasıdır.

---

## 🚀 Öne Çıkan Özellikler

### 1. 👥 İkili Modül Mimarisi (Bireysel & Lojistik)
* **Bireysel Müşteriler:** Ev ve kişisel su arıtma kullanıcıları için montaj ve periyodik filtre değişim takibi (+6 Ay, +1 Yıl, +2 Yıl, +3 Yıl).
* **Lojistik / Kurumsal Müşteriler:** Fabrika, ofis ve işletmeler için Şirket Adı ve Kurumsal Sabit Hat desteğiyle birlikte sık periyotlu hızlı bakım seçenekleri (+3, +6, +9, +12 Ay).

### 2. 📊 Canlı Dashboard ve Servis Paneli (Aqua Glass)
* Uygulama açılışında anlık günün iş yükünü gösteren akıllı gösterge kartları:
  * **📅 Bugünün Randevuları:** Bugün için planlanmış servis randevuları.
  * **⚠️ Acil / Geçen Bakımlar:** Bakım süresi dolmuş veya 15 gün içinde dolacak acil müşteriler.

### 3. 🗓️ Randevu & Rezervasyon Yönetimi
* Müşteriyle sözleşilen ileri tarihli randevuları saat ve servis notuyla kaydetme.
* **Akıllı Zaman Filtreleri:** *Tümü*, *📅 Bugün*, *🌅 Yarın* ve *🗓️ Bu Hafta (7 Gün)* seçenekleriyle randevu listeleme.
* Tek tıkla bakım tamamlama veya randevu iptal edebilme.

### 4. 🔇 Müşteri Susturma (Pasife Alma)
* Bakımı erteleyen veya ulaşılamayan müşterileri pasife alarak gri rozetle işaretleme.
* Susturulan müşterileri ana bildirim akışından ve günlük uyarı süzgecinden muaf tutma.

### 5. 📞 Tek Dokunuşla İletişim (Arama & WhatsApp)
* Kartlar üzerinden tek tıkla doğrudan telefon araması başlatma.
* Türkiye formatına göre normalize edilen numaralar üzerinden doğrudan WhatsApp sohbeti başlatma.

### 6. 💾 Tam Yedekleme & Geri Yükleme
* **Excel (.csv):** Müşteri listesini Excel uyumlu (UTF-8 BOM) CSV olarak dışa aktarma ve paylaşma.
* **JSON Veritabanı Yedeği:** Telefon değişikliklerinde tüm müşteri ve geçmiş servis notlarını tek dosya ile yedekleme ve anında geri yükleme.

### 7. ⏰ Otomatik Arka Plan Hatırlatıcı (WorkManager)
* İnternet bağlantısı gerektirmeksizin, günü gelen veya geçen bakımları tespit edip durum çubuğuna sistem uyarısı düşüren arka plan servisi.

---

## 🛠️ Kullanılan Teknolojiler (Tech Stack)

* **Dil:** Kotlin 1.9
* **Mimari:** MVVM (Model-View-ViewModel), Repository Pattern
* **Veritabanı:** Android Jetpack Room Database (SQLite) — Sürüm 4 (Migration 1->2, 2->3, 3->4)
* **Asenkron Veri Akışı:** Kotlin Coroutines & StateFlow / SharedFlow
* **Kullanıcı Arayüzü (UI):** Material Design 3 (Aqua Glass Theme), ViewBinding, CoordinatorLayout
* **Arka Plan Servisleri:** Android Jetpack WorkManager
* **Veri İşleme:** Google Gson

---

## 📂 Proje Dizin Yapısı

```text
com.aritma.musteritakip/
├── AritmaApplication.kt           # Uygulama başlatıcı ve bağımlılık yöneticisi
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt         # Room veritabanı ve migrasyon tanımları
│   │   ├── dao/                   # CustomerDao, DistrictDao, NoteDao
│   │   └── entity/                # CustomerEntity, DistrictEntity, NoteEntity
│   └── repository/
│       └── CustomerRepository.kt  # Merkezi veri erişim katmanı
├── ui/
│   ├── backup/                    # Yedekleme ve geri yükleme ekranı
│   ├── bireysel/                  # Müşteri listesi, detay ve düzenleme ekranları
│   ├── lojistik/                  # Kurumsal müşteri listesi ekranı
│   ├── main/                      # MainActivity ve Dashboard Modül Seçim Fragment'ı
│   ├── reservation/               # Randevular ve zaman filtreli servis ekranı
│   └── viewmodel/                 # CustomerViewModel ve ViewModelFactory
├── util/                          # Tarih, diyalog, WhatsApp, Excel ve JSON yardımcı sınıfları
└── worker/                        # WorkManager periyodik bakım hatırlatıcı servisi
```

---

## ⚙️ Kurulum & Derleme

### Gereksinimler:
* Android Studio Iguana / Jellyfish veya daha yenisi
* JDK 17
* Minimum Android Sürümü: Android 7.0 (API 24)
* Hedef Android Sürümü: Android 14 (API 34)

### Derleme Adımları:
```bash
# Debug APK Derleme:
./gradlew assembleDebug

# İmzalı Release APK Derleme:
./gradlew assembleRelease
```
Çıkan APK dosyası doğrudan Android cihazlara aktarılarak kurulabilir.

---

## 📄 Lisans
Bu proje **WATERNEON Su Arıtma Sistemleri** için özel olarak geliştirilmiştir.
