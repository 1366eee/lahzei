package com.lahzei.app.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/* ---------- Persian number formatting ---------- */

object F {

    private val faDigits: Map<Char, Char> =
        ('0'..'9').zip("۰۱۲۳۴۵۶۷۸۹".toList()).toMap()

    /** Convert western digits to Persian digits */
    fun fa(s: String): String = s.map { faDigits[it] ?: it }.joinToString("")

    /** Money with thousands separator + Persian digits. 111590.0 -> ۱۱۱,۵۹۰ */
    fun money(v: Double, dec: Int = 0): String {
        val sym = DecimalFormatSymbols(Locale.US)
        val pattern = if (dec > 0) "#,##0." + "0".repeat(dec) else "#,##0"
        val df = DecimalFormat(pattern, sym)
        return fa(df.format(v).replace('.', '/'))
    }

    /** Small numbers (rates) e.g. 0.92 -> ۰/۹۲ */
    fun rate(v: Double): String = money(v, if (v < 10) 2 else 0)

    /** +1.25 -> +۱/۲۵٪ */
    fun pct(v: Double): String =
        fa(String.format(Locale.US, "%+.1f", v).replace('.', '/')) + "٪"

    fun temp(v: Double): String = fa(
        DecimalFormat("#,##0", DecimalFormatSymbols(Locale.US)).format(v)
    ) + "°"
}

/* ---------- WMO weather codes -> Persian ---------- */

object Wx {

    fun desc(code: Int): String = when (code) {
        0 -> "صاف"
        1 -> "عمدتاً صاف"
        2 -> "نیمه‌ابری"
        3 -> "ابری"
        45, 48 -> "مه‌آلود"
        51 -> "نم‌نام سبک"
        53 -> "نم‌نام"
        55 -> "نم‌نام سنگین"
        56, 57 -> "نم‌نام یخ‌زده"
        61 -> "باران سبک"
        63 -> "باران"
        65 -> "باران شدید"
        66, 67 -> "باران یخ‌زده"
        71 -> "برف سبک"
        73 -> "برف"
        75 -> "برف سنگین"
        77 -> "دانه‌های برف"
        80 -> "رگبار سبک"
        81 -> "رگبار باران"
        82 -> "رگبار شدید"
        85, 86 -> "رگبار برف"
        95 -> "رعد و برق"
        96, 99 -> "رعد و برق با تگرگ"
        else -> "نامشخص"
    }

    fun emoji(code: Int, isDay: Boolean = true): String = when (code) {
        0 -> if (isDay) "☀️" else "🌙"
        1 -> if (isDay) "🌤️" else "🌙"
        2 -> if (isDay) "⛅" else "☁️"
        3 -> "☁️"
        45, 48 -> "🌫️"
        51, 53, 55 -> "🌦️"
        56, 57 -> "🌧️"
        61, 63 -> "🌧️"
        65 -> "🌧️"
        66, 67 -> "🌧️"
        71, 73, 75, 77 -> "🌨️"
        80, 81, 82 -> "🌧️"
        85, 86 -> "🌨️"
        95, 96, 99 -> "⛈️"
        else -> "🌡️"
    }

    /** Persian weekday from ISO date "2026-10-20" */
    fun weekday(dateStr: String): String {
        return try {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)
            val cal = Calendar.getInstance()
            if (parsed != null) cal.time = parsed
            when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SATURDAY -> "شنبه"
                Calendar.SUNDAY -> "یکشنبه"
                Calendar.MONDAY -> "دوشنبه"
                Calendar.TUESDAY -> "سه‌شنبه"
                Calendar.WEDNESDAY -> "چهارشنبه"
                Calendar.THURSDAY -> "پنجشنبه"
                Calendar.FRIDAY -> "جمعه"
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }
}
