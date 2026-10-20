package com.lahzei.app.ui.weather

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lahzei.app.data.Cities
import com.lahzei.app.data.Repo
import com.lahzei.app.data.WeatherData
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class WeatherViewModel : ViewModel() {

    /** city name -> weather (null = fetch failed for that city) */
    val major = MutableLiveData<Map<String, WeatherData?>>(emptyMap())
    val loading = MutableLiveData(false)
    val error = MutableLiveData<String?>()

    fun load(force: Boolean = false) {
        if (!force && major.value?.isNotEmpty() == true) return
        if (loading.value == true) return
        loading.value = true
        error.value = null
        viewModelScope.launch {
            try {
                val results = Cities.majorCities.map { c ->
                    async {
                        try {
                            c.name to (Repo.weather(c.lat, c.lon) as WeatherData?)
                        } catch (e: Exception) {
                            c.name to null
                        }
                    }
                }.awaitAll().toMap()
                major.value = results
                if (results.values.all { it == null }) {
                    error.value = "دریافت وضعیت آب‌وهوا ناموفق بود؛ اتصال اینترنت را بررسی کنید"
                }
            } catch (e: Exception) {
                error.value = e.message ?: "خطای نامشخص"
            } finally {
                loading.value = false
            }
        }
    }
}
