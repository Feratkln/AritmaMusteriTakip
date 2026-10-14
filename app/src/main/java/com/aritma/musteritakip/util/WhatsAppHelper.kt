package com.aritma.musteritakip.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Müşterilerle tek dokunuşla WhatsApp üzerinden iletişime geçilmesini ve
 * Türkiye telefon numarası formatlarının (+90) normalize edilmesini sağlayan yardımcı sınıf.
 */
object WhatsAppHelper {

    fun openWhatsAppChat(context: Context, rawPhone: String, message: String = "") {
        val cleanPhone = normalizePhoneNumber(rawPhone)
        val encodedMessage = Uri.encode(message)
        val url = if (encodedMessage.isEmpty()) {
            "https://wa.me/$cleanPhone"
        } else {
            "https://wa.me/$cleanPhone?text=$encodedMessage"
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
            setPackage("com.whatsapp")
        }

        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to browser if WhatsApp app isn't installed
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(browserIntent)
        }
    }

    fun openDialer(context: Context, rawPhone: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$rawPhone")
        }
        context.startActivity(intent)
    }

    private fun normalizePhoneNumber(phone: String): String {
        var digits = phone.filter { it.isDigit() }
        if (digits.startsWith("0")) {
            digits = "9" + digits
        } else if (!digits.startsWith("90") && digits.length == 10) {
            digits = "90$digits"
        }
        return digits
    }
}
