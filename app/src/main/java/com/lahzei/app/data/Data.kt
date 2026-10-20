package com.lahzei.app.data

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/* ---------------- Models ---------------- */

data class TgjuItem(
    val key: String,
    val title: String,
    val value: Double,        // toman for rial items, USD for items flagged usd
    val isUsd: Boolean,
    val changePct: Double?,
    val up: Boolean?,
    val time: String?
)

data class TgjuData(
    val dollarToman: Double,
    val items: List<TgjuItem>
)

data class Coin(
    val id: String,
    val symbol: String,
    val name: String,
    val usd: Double,
    val change24h: Double?,
    val rank: Int?
)

data class CurrencyEntry(val code: String, val name: String, val flag: String)

data class WeatherData(
    val temp: Double,
    val feels: Double,
    val humidity: Int,
    val wind: Double,
    val code: Int,
    val isDay: Boolean,
    val daily: List<ForecastDay>
)

data class ForecastDay(val date: String, val code: Int, val max: Double, val min: Double)

/* ---------------- Network repository ---------------- */

object Repo {

    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private fun get(url: String): String {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "LahzeiApp/1.0 (Android)")
            .build()
        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            return resp.body?.string() ?: throw IOException("پاسخ خالی از سرور")
        }
    }

    private fun JsonObjectNum(o: com.google.gson.JsonObject, k: String): Double? = try {
        val e = o.get(k) ?: return null
        if (e.isJsonNull || !e.isJsonPrimitive) null
        else {
            val s = e.asString.trim().replace(",", "")
            s.toDoubleOrNull() ?: e.asString.trim().toDoubleOrNull()
        }
    } catch (ex: Exception) { null }

    private fun JsonObjectStr(o: com.google.gson.JsonObject, k: String): String? = try {
        val e = o.get(k) ?: return null
        if (e.isJsonNull || !e.isJsonPrimitive) null else e.asString
    } catch (ex: Exception) { null }

    /* ---- TGJU: gold, dollar, euro (rial) ---- */

    suspend fun tgju(): TgjuData = withContext(Dispatchers.IO) {
        val txt = try {
            get("https://call1.tgju.org/ajax.json")
        } catch (e: Exception) {
            throw IOException("اتصال به سرور طلا ممکن نشد", e)
        }
        val root = JsonParser.parseString(txt).asJsonObject
        val cur = root.getAsJsonObject("current")
            ?: throw IOException("ساختار پاسخ سرور طلا تغییر کرده است")

        fun item(key: String, title: String, usd: Boolean = false): TgjuItem? {
            val o = cur.getAsJsonObject(key) ?: return null
            val p = JsonObjectNum(o, "p") ?: return null
            val dp = JsonObjectNum(o, "dp")
            val dt = JsonObjectStr(o, "dt")
            return TgjuItem(key, title, if (usd) p else p / 10.0, usd, dp,
                dt?.let { it.equals("high", true) }, JsonObjectStr(o, "t"))
        }

        val list = listOfNotNull(
            item("geram18", "طلای ۱۸ عیار (گرم)"),
            item("geram24", "طلای ۲۴ عیار (گرم)"),
            item("mesghal", "مظنه طلا (مثقال)"),
            item("sekee", "سکه امامی"),
            item("nim", "نیم‌سکه"),
            item("rob", "ربع‌سکه"),
            item("gerami", "سکه گرمی"),
            item("ons", "انس طلای جهانی", usd = true),
            item("price_dollar_rl", "دلار آمریکا"),
            item("price_eur", "یورو")
        )
        val dollar = list.firstOrNull { it.key == "price_dollar_rl" }?.value
            ?: throw IOException("نرخ دلار در پاسخ سرور یافت نشد")
        TgjuData(dollar, list)
    }

    /* ---- Crypto: CoinGecko with CryptoCompare fallback ---- */

    suspend fun crypto(): List<Coin> = withContext(Dispatchers.IO) {
        try {
            val txt = get("https://api.coingecko.com/api/v3/coins/markets?vs_currency=usd&order=market_cap_desc&per_page=20&page=1&sparkline=false&price_change_percentage=24h")
            val arr = JsonParser.parseString(txt).asJsonArray
            val out = arr.mapNotNull { el ->
                val o = el.asJsonObject
                val price = JsonObjectNum(o, "current_price") ?: return@mapNotNull null
                Coin(
                    JsonObjectStr(o, "id") ?: "",
                    (JsonObjectStr(o, "symbol") ?: "").uppercase(),
                    JsonObjectStr(o, "name") ?: "",
                    price,
                    JsonObjectNum(o, "price_change_percentage_24h"),
                    JsonObjectNum(o, "market_cap_rank")?.toInt()
                )
            }.filter { it.usd > 0 }
            if (out.isEmpty()) throw IOException("empty")
            out
        } catch (e: Exception) {
            cryptoFallback()
        }
    }

    private fun cryptoFallback(): List<Coin> {
        val syms = "BTC,ETH,USDT,BNB,SOL,XRP,ADA,DOGE,TRX,TON,LTC,SHIB,AVAX,DOT,LINK,ATOM,XLM,BCH,NEAR,ETC"
        val names = mapOf(
            "BTC" to "بیت‌کوین", "ETH" to "اتریوم", "USDT" to "تتر",
            "BNB" to "بایننس کوین", "SOL" to "سولانا", "XRP" to "ریپل",
            "ADA" to "کاردانو", "DOGE" to "دوج‌کوین", "TRX" to "ترون",
            "TON" to "تون‌کوین", "LTC" to "لایت‌کوین", "SHIB" to "شیبا اینو",
            "AVAX" to "آوالانچ", "DOT" to "پولکادات", "LINK" to "چین‌لینک",
            "ATOM" to "کازموس", "XLM" to "استلار", "BCH" to "بیت‌کوین کش",
            "NEAR" to "نیر پروتکل", "ETC" to "اتریوم کلاسیک"
        )
        val txt = get("https://min-api.cryptocompare.com/data/pricemultifull?fsyms=$syms&tsyms=USD")
        val root = JsonParser.parseString(txt).asJsonObject
        val raw = root.getAsJsonObject("RAW") ?: throw IOException("دریافت قیمت رمزارز ناموفق بود")
        val out = raw.entrySet().mapNotNull { (sym, v) ->
            val u = v.asJsonObject.getAsJsonObject("USD") ?: return@mapNotNull null
            val price = JsonObjectNum(u, "PRICE") ?: return@mapNotNull null
            if (price <= 0) return@mapNotNull null
            Coin(sym.lowercase(), sym, names[sym] ?: sym, price,
                JsonObjectNum(u, "CHANGEPCT24HOUR"), null)
        }
        if (out.isEmpty()) throw IOException("قیمت رمزارزی دریافت نشد")
        return out
    }

    /* ---- World currencies vs USD (official cross rates) ---- */

    suspend fun erRates(): Map<String, Double> = withContext(Dispatchers.IO) {
        val txt = try {
            get("https://open.er-api.com/v6/latest/USD")
        } catch (e: Exception) {
            throw IOException("دریافت نرخ ارزهای جهانی ناموفق بود", e)
        }
        val root = JsonParser.parseString(txt).asJsonObject
        val result = JsonObjectStr(root, "result")
        if (result != "success") throw IOException("سرور ارز پاسخ موفق نداد")
        val rates = root.getAsJsonObject("rates") ?: throw IOException("نرخ ارزی یافت نشد")
        val map = mutableMapOf<String, Double>()
        for ((k, v) in rates.entrySet()) {
            val d = try { v.asDouble } catch (ex: Exception) { continue }
            if (d > 0) map[k] = d
        }
        if (map.isEmpty()) throw IOException("جدول نرخ ارز خالی بود")
        map
    }

    /* ---- Weather via Open-Meteo (no API key) ---- */

    suspend fun weather(lat: Double, lon: Double): WeatherData = withContext(Dispatchers.IO) {
        val url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$lat&longitude=$lon" +
                "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m" +
                "&daily=weather_code,temperature_2m_max,temperature_2m_min" +
                "&forecast_days=5&timezone=auto"
        val root = JsonParser.parseString(get(url)).asJsonObject
        val c = root.getAsJsonObject("current") ?: throw IOException("داده آب‌وهوا یافت نشد")
        val d = root.getAsJsonObject("daily")

        val daily = mutableListOf<ForecastDay>()
        if (d != null) {
            val times = d.getAsJsonArray("time")
            val codes = d.getAsJsonArray("weather_code")
            val maxs = d.getAsJsonArray("temperature_2m_max")
            val mins = d.getAsJsonArray("temperature_2m_min")
            if (times != null && codes != null && maxs != null && mins != null) {
                for (i in 0 until times.size()) {
                    daily.add(
                        ForecastDay(
                            times[i].asString,
                            try { codes[i].asInt } catch (e: Exception) { 0 },
                            try { maxs[i].asDouble } catch (e: Exception) { 0.0 },
                            try { mins[i].asDouble } catch (e: Exception) { 0.0 }
                        )
                    )
                }
            }
        }

        WeatherData(
            temp = JsonObjectNum(c, "temperature_2m") ?: 0.0,
            feels = JsonObjectNum(c, "apparent_temperature") ?: 0.0,
            humidity = (JsonObjectNum(c, "relative_humidity_2m") ?: 0.0).toInt(),
            wind = JsonObjectNum(c, "wind_speed_10m") ?: 0.0,
            code = (JsonObjectNum(c, "weather_code") ?: 0.0).toInt(),
            isDay = (JsonObjectNum(c, "is_day") ?: 1.0).toInt() == 1,
            daily = daily
        )
    }
}
