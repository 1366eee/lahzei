package com.lahzei.app.ui.weather

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lahzei.app.R
import com.lahzei.app.data.Repo
import com.lahzei.app.data.WeatherData
import com.lahzei.app.util.F
import com.lahzei.app.util.Wx
import kotlinx.coroutines.launch

class WeatherDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        setContentView(R.layout.activity_weather_detail)

        val name = intent.getStringExtra("name") ?: "شهر"
        val province = intent.getStringExtra("province") ?: ""
        val lat = intent.getDoubleExtra("lat", 35.69)
        val lon = intent.getDoubleExtra("lon", 51.42)

        val tvTitle = findViewById<TextView>(R.id.tv_city)
        val tvProvince = findViewById<TextView>(R.id.tv_province)
        val tvEmoji = findViewById<TextView>(R.id.tv_emoji)
        val tvTemp = findViewById<TextView>(R.id.tv_temp)
        val tvDesc = findViewById<TextView>(R.id.tv_desc)
        val tvFeels = findViewById<TextView>(R.id.tv_feels)
        val tvHum = findViewById<TextView>(R.id.tv_humidity)
        val tvWind = findViewById<TextView>(R.id.tv_wind)
        val progress = findViewById<ProgressBar>(R.id.progress)
        val errorText = findViewById<TextView>(R.id.error_text)
        val content = findViewById<View>(R.id.content)
        val rv = findViewById<RecyclerView>(R.id.rv_forecast)

        tvTitle.text = name
        tvProvince.text = province

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = ForecastAdapter(emptyList())

        progress.visibility = View.VISIBLE
        content.visibility = View.GONE
        errorText.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val w = Repo.weather(lat, lon)
                render(w)
            } catch (e: Exception) {
                errorText.text = "دریافت اطلاعات ناموفق بود: ${e.message}"
                errorText.visibility = View.VISIBLE
                content.visibility = View.GONE
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    private fun render(w: WeatherData) {
        findViewById<TextView>(R.id.tv_emoji).text = Wx.emoji(w.code, w.isDay)
        findViewById<TextView>(R.id.tv_temp).text = F.temp(w.temp)
        findViewById<TextView>(R.id.tv_desc).text = Wx.desc(w.code)
        findViewById<TextView>(R.id.tv_feels).text = "احساس واقعی: ${F.temp(w.feels)}"
        findViewById<TextView>(R.id.tv_humidity).text = "رطوبت: ${F.fa(w.humidity.toString())}٪"
        findViewById<TextView>(R.id.tv_wind).text = "باد: ${F.money(w.wind)} کیلومتر/ساعت"
        findViewById<View>(R.id.content).visibility = View.VISIBLE
        findViewById<RecyclerView>(R.id.rv_forecast).adapter = ForecastAdapter(w.daily)
    }

    /* ---------------- 5-day forecast ---------------- */

    private class ForecastAdapter(private var rows: List<com.lahzei.app.data.ForecastDay>) :
        RecyclerView.Adapter<ForecastAdapter.VH>() {

        class VH(val b: com.lahzei.app.databinding.ItemForecastBinding) :
            RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
            com.lahzei.app.databinding.ItemForecastBinding.inflate(
                android.view.LayoutInflater.from(parent.context), parent, false
            )
        )

        override fun getItemCount() = rows.size

        override fun onBindViewHolder(h: VH, pos: Int) {
            val d = rows[pos]
            h.b.tvDay.text = when (pos) {
                0 -> "امروز"
                1 -> "فردا"
                else -> Wx.weekday(d.date)
            }
            h.b.tvEmoji.text = Wx.emoji(d.code)
            h.b.tvMinMax.text = "${F.temp(d.max)} / ${F.temp(d.min)}"
        }
    }
}
