package com.aritma.musteritakip.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Servis Bilgi Formu verilerini tutan veri modeli.
 * Fiyat/tutar içermez; sadece servis tarihi, müşteri no/bilgileri,
 * kullanılan malzemeler, adetler ve servis raporunu barındırır.
 */
data class ServiceFormData(
    val formNo: String,
    val serviceDate: String,
    val customerName: String,
    val customerPhone: String,
    val customerAddress: String,
    val materials: List<Pair<String, String>>, // (Malzeme Tanımı, Adet)
    val serviceReport: String,
    val technicianName: String = "Muhammed Talha ÇETİNTAŞ"
)

/**
 * WATERNEON dijital "Servis Bilgi Formu" A4 PDF belgesi oluşturan,
 * önizleyen ve WhatsApp/yazıcı üzerinden paylaşılmasını sağlayan yardımcı sınıf.
 */
object ServiceFormPdfGenerator {

    private const val PAGE_WIDTH = 595  // Standard A4 width in points (72 dpi)
    private const val PAGE_HEIGHT = 842 // Standard A4 height in points (72 dpi)
    private const val MARGIN = 28f

    fun generatePdf(context: Context, data: ServiceFormData): File? {
        val document = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            drawFormOnCanvas(canvas, data)

            document.finishPage(page)

            val exportDir = File(context.cacheDir, "service_forms").apply { if (!exists()) mkdirs() }
            val fileName = "Servis_Formu_${data.formNo.replace("[^a-zA-Z0-9]".toRegex(), "_")}_${DateUtils.formatFileDate()}.pdf"
            val file = File(exportDir, fileName)

            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            document.close()
        }
    }

    private fun drawFormOnCanvas(canvas: Canvas, data: ServiceFormData) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)

        val left = MARGIN
        val right = PAGE_WIDTH - MARGIN
        val top = MARGIN
        val bottom = PAGE_HEIGHT - MARGIN
        val contentWidth = right - left

        // 1. Dış Çerçeve
        paint.color = Color.rgb(15, 23, 42) // Dark slate
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        canvas.drawRect(left, top, right, bottom, paint)

        // 2. ÜST BAŞLIK ALANI (Top to y=120)
        val headerBottom = top + 102f
        canvas.drawLine(left, headerBottom, right, headerBottom, paint)

        // Sol Firma Bilgileri
        val colDividerX = left + (contentWidth * 0.58f)
        canvas.drawLine(colDividerX, top, colDividerX, headerBottom, paint)

        // WATERNEON Logosu & Başlık
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(36, 89, 255) // Cobalt Blue
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("💧 WATERNEON", left + 10f, top + 22f, paint)

        paint.color = Color.rgb(10, 31, 92)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SU ARITMA SİSTEMLERİ", left + 12f, top + 34f, paint)

        // Firma Sahibi & Yeni Bağcılar Adresi
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("MUHAMMED TALHA ÇETİNTAŞ", left + 12f, top + 48f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Kazım Karabekir Mah. 263. Sokak No: 21/B", left + 12f, top + 60f, paint)
        canvas.drawText("Semt: Bağcılar / İSTANBUL", left + 12f, top + 71f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Tel: +90 535 011 59 39", left + 12f, top + 83f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("www.waterneon.com  |  info@waterneon.com", left + 12f, top + 94f, paint)

        // Sağ Taraf: Form Başlığı & Form/Tarih Bilgileri
        // Başlık Banner Kutusu
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(15, 23, 42)
        canvas.drawRect(colDividerX, top, right, top + 26f, paint)

        paint.color = Color.WHITE
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("SERVİS BİLGİ FORMU", colDividerX + ((right - colDividerX) / 2f), top + 17f, paint)
        paint.textAlign = Paint.Align.LEFT

        // Form No, Tarih, Müşteri No Satırları
        paint.color = Color.rgb(15, 23, 42)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        val rightRow1 = top + 51f
        val rightRow2 = top + 76f
        canvas.drawLine(colDividerX, rightRow1, right, rightRow1, paint)
        canvas.drawLine(colDividerX, rightRow2, right, rightRow2, paint)

        paint.style = Paint.Style.FILL
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Sıra / Form No:", colDividerX + 8f, top + 42f, paint)
        paint.color = Color.rgb(229, 72, 77) // Kırmızı No
        canvas.drawText(data.formNo.ifBlank { "000341" }, colDividerX + 85f, top + 42f, paint)

        paint.color = Color.rgb(15, 23, 42)
        canvas.drawText("Servis Tarihi:", colDividerX + 8f, top + 67f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(data.serviceDate.ifBlank { DateUtils.formatDate(System.currentTimeMillis()) }, colDividerX + 85f, top + 67f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Servis Türü:", colDividerX + 8f, top + 92f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Periyodik Bakım / Değişim", colDividerX + 85f, top + 92f, paint)

        // 3. MÜŞTERİ BİLGİLERİ GRİDİ (y: headerBottom to y: 175)
        var currentY = headerBottom
        val customerGridBottom = currentY + 60f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.rgb(15, 23, 42)
        canvas.drawLine(left, customerGridBottom, right, customerGridBottom, paint)

        val customerRow1 = currentY + 30f
        canvas.drawLine(left, customerRow1, right, customerRow1, paint)

        val custColDividerX = left + (contentWidth * 0.60f)
        canvas.drawLine(custColDividerX, currentY, custColDividerX, customerRow1, paint)

        paint.style = Paint.Style.FILL
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("MÜŞTERİ ADI SOYADI:", left + 8f, currentY + 14f, paint)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(data.customerName.ifBlank { "---" }, left + 8f, currentY + 25f, paint)

        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TELEFON / GSM:", custColDividerX + 8f, currentY + 14f, paint)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(data.customerPhone.ifBlank { "---" }, custColDividerX + 8f, currentY + 25f, paint)

        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SERVİS ADRESİ:", left + 8f, customerRow1 + 12f, paint)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val addressText = data.customerAddress.ifBlank { "Adres belirtilmedi" }
        canvas.drawText(addressText.take(90), left + 8f, customerRow1 + 23f, paint)

        // 4. KULLANILAN MALZEMELER TABLOSU (FİYATSIZ!)
        currentY = customerGridBottom
        val tableHeaderY = currentY + 20f
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(241, 245, 249) // Soft gray header
        canvas.drawRect(left, currentY, right, tableHeaderY, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawLine(left, tableHeaderY, right, tableHeaderY, paint)

        val colSiraWidth = 38f
        val colAdetWidth = 85f
        val colMalzemeWidth = contentWidth - colSiraWidth - colAdetWidth
        val xCol1 = left + colSiraWidth
        val xCol2 = xCol1 + colMalzemeWidth

        canvas.drawLine(xCol1, currentY, xCol1, currentY + 20f, paint)
        canvas.drawLine(xCol2, currentY, xCol2, currentY + 20f, paint)

        paint.style = Paint.Style.FILL
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("SIRA", left + (colSiraWidth / 2f), currentY + 14f, paint)
        canvas.drawText("ADET", xCol2 + (colAdetWidth / 2f), currentY + 14f, paint)
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("KULLANILAN MALZEMELER / YEDEK PARÇALAR", left + colSiraWidth + 8f, currentY + 14f, paint)

        // Malzeme Satırları (Maksimum 8 satır çizilir, girilmeyenler boş satır olarak kalır)
        val rowHeight = 22f
        val maxRows = 8
        currentY = tableHeaderY

        for (i in 0 until maxRows) {
            val rowY = currentY + rowHeight
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 0.75f
            paint.color = Color.rgb(203, 213, 225)
            canvas.drawLine(left, rowY, right, rowY, paint)
            canvas.drawLine(xCol1, currentY, xCol1, rowY, paint)
            canvas.drawLine(xCol2, currentY, xCol2, rowY, paint)

            paint.style = Paint.Style.FILL
            paint.color = Color.rgb(15, 23, 42)
            paint.textSize = 8.5f

            // Sıra No
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("${i + 1}", left + (colSiraWidth / 2f), currentY + 15f, paint)

            // Malzeme Tanımı ve Adet
            val materialItem = data.materials.getOrNull(i)
            if (materialItem != null && materialItem.first.isNotBlank()) {
                paint.textAlign = Paint.Align.LEFT
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText(materialItem.first.take(65), left + colSiraWidth + 8f, currentY + 15f, paint)

                paint.textAlign = Paint.Align.CENTER
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(materialItem.second.ifBlank { "1 Adet" }, xCol2 + (colAdetWidth / 2f), currentY + 15f, paint)
            } else {
                paint.textAlign = Paint.Align.LEFT
                paint.color = Color.rgb(203, 213, 225)
                canvas.drawText("- - -", left + colSiraWidth + 8f, currentY + 15f, paint)
            }

            paint.textAlign = Paint.Align.LEFT
            currentY = rowY
        }

        // 5. SERVİS RAPORU / AÇIKLAMA BÖLÜMÜ (y: currentY to y: ~680)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(241, 245, 249)
        val reportHeaderY = currentY + 18f
        canvas.drawRect(left, currentY, right, reportHeaderY, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawLine(left, reportHeaderY, right, reportHeaderY, paint)

        paint.style = Paint.Style.FILL
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SERVİS RAPORU & TEKNİK DEĞERLENDİRME (YAPILAN İŞLEMLER)", left + 8f, currentY + 13f, paint)

        val reportBoxHeight = 160f
        val reportBottomY = reportHeaderY + reportBoxHeight
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawLine(left, reportBottomY, right, reportBottomY, paint)

        // Rapor Metni Çizimi (Multi-line StaticLayout)
        val reportText = data.serviceReport.ifBlank {
            "Cihazın periyodik bakım ve kontrol işlemleri tamamlanmıştır. Sistemde herhangi bir su kaçağı veya basınç düzensizliği bulunmamaktadır. Cihaz çalışır ve faal durumda teslim edilmiştir."
        }
        textPaint.textSize = 8.5f
        textPaint.color = Color.rgb(30, 41, 59)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        val layoutWidth = (contentWidth - 16f).toInt()
        val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(reportText, 0, reportText.length, textPaint, layoutWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(3f, 1f)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(reportText, textPaint, layoutWidth, Layout.Alignment.ALIGN_NORMAL, 1f, 3f, false)
        }

        canvas.save()
        canvas.translate(left + 8f, reportHeaderY + 8f)
        staticLayout.draw(canvas)
        canvas.restore()

        // 6. KARŞILIKLI İMZA KUTULARI (y: reportBottomY to y: bottom - 26)
        val signaturesBottomY = bottom - 28f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        paint.color = Color.rgb(15, 23, 42)
        canvas.drawLine(left, signaturesBottomY, right, signaturesBottomY, paint)

        val sigColWidth = contentWidth / 3f
        val sigX1 = left + sigColWidth
        val sigX2 = sigX1 + sigColWidth
        canvas.drawLine(sigX1, reportBottomY, sigX1, signaturesBottomY, paint)
        canvas.drawLine(sigX2, reportBottomY, sigX2, signaturesBottomY, paint)

        paint.style = Paint.Style.FILL
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER

        // Kutu 1: Servis Sorumlusu
        canvas.drawText("SERVİS SORUMLUSU", left + (sigColWidth / 2f), reportBottomY + 14f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText("(İsim & İmza)", left + (sigColWidth / 2f), reportBottomY + 26f, paint)
        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(data.technicianName.ifBlank { "Muhammed Talha ÇETİNTAŞ" }, left + (sigColWidth / 2f), signaturesBottomY - 10f, paint)

        // Kutu 2: Müşteri Onayı
        paint.color = Color.rgb(15, 23, 42)
        canvas.drawText("MÜŞTERİ ONAYI", sigX1 + (sigColWidth / 2f), reportBottomY + 14f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText("(İsim & İmza)", sigX1 + (sigColWidth / 2f), reportBottomY + 26f, paint)
        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(data.customerName.ifBlank { "Müşteri Adı" }, sigX1 + (sigColWidth / 2f), signaturesBottomY - 10f, paint)

        // Kutu 3: Yetkili Bayi & Kaşe
        paint.color = Color.rgb(15, 23, 42)
        canvas.drawText("YETKİLİ BAYİ & SERVİS", sigX2 + (sigColWidth / 2f), reportBottomY + 14f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText("(Kaşe / Mühür)", sigX2 + (sigColWidth / 2f), reportBottomY + 26f, paint)
        paint.color = Color.rgb(36, 89, 255)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("WATERNEON SERVİS ONAYLI", sigX2 + (sigColWidth / 2f), signaturesBottomY - 10f, paint)

        paint.textAlign = Paint.Align.LEFT

        // 7. ALT YASAL NOT (Formun en altındaki resmi uyarı)
        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 6.8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val legalNote = "NOT: Servis hizmet süresi azami 30 iş günüdür. Servis hizmet süresi müteakiben 90 takvim günü içerisinde alınmayan üründen firmamız sorumlu değildir."
        canvas.drawText(legalNote, left + 8f, bottom - 10f, paint)
    }

    /**
     * Oluşturulan PDF dosyasını Android paylaşım menüsü üzerinden açar/paylaşır.
     */
    fun sharePdfFile(context: Context, file: File, subject: String = "WATERNEON Servis Bilgi Formu") {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "Servis Bilgi Formunu Paylaş"))
    }

    /**
     * PDF'i doğrudan müşterinin WhatsApp sohbetine göndermek için başlatır.
     */
    fun sendPdfViaWhatsApp(context: Context, file: File, phone: String, customerName: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val cleanPhone = WhatsAppHelper.normalizePhoneNumber(phone)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(
                Intent.EXTRA_TEXT,
                "Merhaba Sayın $customerName, WATERNEON Su Arıtma Sistemleri tarafından gerçekleştirilen servis işleminize ait 'Servis Bilgi Formu' ektedir. Sağlıklı günler dileriz."
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (cleanPhone.isNotBlank()) {
                putExtra("jid", "$cleanPhone@s.whatsapp.net")
            }
            setPackage("com.whatsapp")
        }

        try {
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            // If WhatsApp package is not installed or direct sending fails, fallback to chooser
            sharePdfFile(context, file, "Servis Bilgi Formu - $customerName")
        }
    }
}
