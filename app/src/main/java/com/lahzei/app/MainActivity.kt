package com.lahzei.app

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.lahzei.app.ui.currency.CurrencyFragment
import com.lahzei.app.ui.converter.ConverterFragment
import com.lahzei.app.ui.crypto.CryptoFragment
import com.lahzei.app.ui.gold.GoldFragment
import com.lahzei.app.ui.weather.WeatherFragment

class MainActivity : AppCompatActivity() {

    private lateinit var titleBar: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Force right-to-left because the whole UI is Persian
        window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL
        setContentView(R.layout.activity_main)

        titleBar = findViewById(R.id.title_bar)
        val nav = findViewById<BottomNavigationView>(R.id.bottom_nav)

        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_weather -> { titleBar.setText(R.string.tab_weather); show { WeatherFragment() } }
                R.id.nav_gold -> { titleBar.setText(R.string.tab_gold); show { GoldFragment() } }
                R.id.nav_crypto -> { titleBar.setText(R.string.tab_crypto); show { CryptoFragment() } }
                R.id.nav_currency -> { titleBar.setText(R.string.tab_currency); show { CurrencyFragment() } }
                R.id.nav_convert -> { titleBar.setText(R.string.tab_convert); show { ConverterFragment() } }
                else -> false
            }
        }

        if (savedInstanceState == null) {
            nav.selectedItemId = R.id.nav_weather
        }
    }

    private fun show(factory: () -> Fragment): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, factory())
            .commit()
        return true
    }
}
