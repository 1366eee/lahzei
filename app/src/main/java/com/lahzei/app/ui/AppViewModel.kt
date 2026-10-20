package com.lahzei.app.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lahzei.app.data.Coin
import com.lahzei.app.data.Repo
import com.lahzei.app.data.TgjuData
import kotlinx.coroutines.launch

/**
 * Shared app-wide ViewModel.
 * Holds gold/dollar (TGJU), crypto and world-currency rates so the
 * converter tab can route every unit through the free-market dollar.
 */
class AppViewModel : ViewModel() {

    val tgju = MutableLiveData<TgjuData?>()
    val tgjuLoading = MutableLiveData(false)
    val tgjuError = MutableLiveData<String?>()

    val crypto = MutableLiveData<List<Coin>?>()
    val cryptoLoading = MutableLiveData(false)
    val cryptoError = MutableLiveData<String?>()

    val erRates = MutableLiveData<Map<String, Double>?>()
    val erLoading = MutableLiveData(false)
    val erError = MutableLiveData<String?>()

    val dollarToman: Double?
        get() = tgju.value?.dollarToman

    fun refreshTgju() {
        if (tgjuLoading.value == true) return
        tgjuLoading.value = true
        tgjuError.value = null
        viewModelScope.launch {
            try {
                tgju.value = Repo.tgju()
            } catch (e: Exception) {
                tgjuError.value = e.message ?: "خطای نامشخص"
            } finally {
                tgjuLoading.value = false
            }
        }
    }

    fun refreshCrypto() {
        if (cryptoLoading.value == true) return
        cryptoLoading.value = true
        cryptoError.value = null
        viewModelScope.launch {
            try {
                crypto.value = Repo.crypto()
            } catch (e: Exception) {
                cryptoError.value = e.message ?: "خطای نامشخص"
            } finally {
                cryptoLoading.value = false
            }
        }
    }

    fun refreshEr() {
        if (erLoading.value == true) return
        erLoading.value = true
        erError.value = null
        viewModelScope.launch {
            try {
                erRates.value = Repo.erRates()
            } catch (e: Exception) {
                erError.value = e.message ?: "خطای نامشخص"
            } finally {
                erLoading.value = false
            }
        }
    }

    /** Fetch anything that has not been loaded yet (used by converter). */
    fun ensureAll() {
        if (tgju.value == null) refreshTgju()
        if (crypto.value == null) refreshCrypto()
        if (erRates.value == null) refreshEr()
    }
}
