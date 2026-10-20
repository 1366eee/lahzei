package com.lahzei.app.ui.currency

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.lahzei.app.R
import com.lahzei.app.data.CurrencyEntry
import com.lahzei.app.ui.AppViewModel
import com.lahzei.app.util.F

object CurrencyList {
    val display: List<CurrencyEntry> = listOf(
        CurrencyEntry("USD", "دلار آمریکا", "🇺🇸"),
        CurrencyEntry("EUR", "یورو", "🇪🇺"),
        CurrencyEntry("GBP", "پوند انگلیس", "🇬🇧"),
        CurrencyEntry("CHF", "فرانک سوئیس", "🇨🇭"),
        CurrencyEntry("CAD", "دلار کانادا", "🇨🇦"),
        CurrencyEntry("AUD", "دلار استرالیا", "🇦🇺"),
        CurrencyEntry("NZD", "دلار نیوزیلند", "🇳🇿"),
        CurrencyEntry("JPY", "ین ژاپن", "🇯🇵"),
        CurrencyEntry("CNY", "یوان چین", "🇨🇳"),
        CurrencyEntry("KRW", "وون کره جنوبی", "🇰🇷"),
        CurrencyEntry("TRY", "لیر ترکیه", "🇹🇷"),
        CurrencyEntry("AED", "درهم امارات", "🇦🇪"),
        CurrencyEntry("SAR", "ریال عربستان", "🇸🇦"),
        CurrencyEntry("QAR", "ریال قطر", "🇶🇦"),
        CurrencyEntry("KWD", "دینار کویت", "🇰🇼"),
        CurrencyEntry("OMR", "ریال عمان", "🇴🇲"),
        CurrencyEntry("BHD", "دینار بحرین", "🇧🇭"),
        CurrencyEntry("IQD", "دینار عراق", "🇮🇶"),
        CurrencyEntry("RUB", "روبل روسیه", "🇷🇺"),
        CurrencyEntry("AZN", "منات آذربایجان", "🇦🇿"),
        CurrencyEntry("AMD", "درام ارمنستان", "🇦🇲"),
        CurrencyEntry("GEL", "لاری گرجستان", "🇬🇪"),
        CurrencyEntry("INR", "روپیه هند", "🇮🇳"),
        CurrencyEntry("PKR", "روپیه پاکستان", "🇵🇰"),
        CurrencyEntry("MYR", "رینگیت مالزی", "🇲🇾"),
        CurrencyEntry("SGD", "دلار سنگاپور", "🇸🇬"),
        CurrencyEntry("HKD", "دلار هنگ‌کنگ", "🇭🇰"),
        CurrencyEntry("THB", "بات تایلند", "🇹🇭"),
        CurrencyEntry("SEK", "کرون سوئد", "🇸🇪"),
        CurrencyEntry("NOK", "کرون نروژ", "🇳🇴"),
        CurrencyEntry("DKK", "کرون دانمارک", "🇩🇰"),
        CurrencyEntry("BRL", "رئال برزیل", "🇧🇷"),
        CurrencyEntry("MXN", "پزو مکزیک", "🇲🇽"),
        CurrencyEntry("ZAR", "رند آفریقای جنوبی", "🇿🇦"),
        CurrencyEntry("EGP", "پوند مصر", "🇪🇬")
    )
}

class CurrencyFragment : Fragment() {

    private val vm: AppViewModel by lazy {
        ViewModelProvider(requireActivity())[AppViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_currency, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rv = view.findViewById<RecyclerView>(R.id.rv_list)
        val progress = view.findViewById<ProgressBar>(R.id.progress)
        val errorText = view.findViewById<TextView>(R.id.error_text)
        val retry = view.findViewById<Button>(R.id.btn_retry)
        val tvDollar = view.findViewById<TextView>(R.id.tv_dollar_value)
        val btnRefresh = view.findViewById<ExtendedFloatingActionButton>(R.id.btn_refresh)

        rv.layoutManager = LinearLayoutManager(requireContext())
        val adapter = CurrencyAdapter(
            rates = { vm.erRates.value ?: emptyMap() },
            dollar = { vm.dollarToman ?: 0.0 }
        )
        rv.adapter = adapter

        vm.erLoading.observe(viewLifecycleOwner) {
            progress.visibility = if (it) View.VISIBLE else View.GONE
        }
        vm.erError.observe(viewLifecycleOwner) { err ->
            if (err != null && vm.erRates.value == null) {
                errorText.text = err
                errorText.visibility = View.VISIBLE
                retry.visibility = View.VISIBLE
            } else {
                errorText.visibility = View.GONE
                retry.visibility = View.GONE
            }
        }

        fun refreshRender() {
            adapter.notifyDataSetChanged()
            val d = vm.dollarToman
            tvDollar.text = if (d != null) "دلار بازار آزاد: ${F.money(d)} تومان" else "—"
        }

        vm.erRates.observe(viewLifecycleOwner) { refreshRender() }
        vm.tgju.observe(viewLifecycleOwner) { refreshRender() }

        retry.setOnClickListener { vm.refreshEr() }
        btnRefresh.setOnClickListener {
            vm.refreshEr()
            if (vm.tgju.value == null) vm.refreshTgju()
        }

        if (vm.erRates.value == null) vm.refreshEr()
        if (vm.tgju.value == null) vm.refreshTgju()
        adapter.notifyDataSetChanged()
    }
}

/* ---------------- adapter ---------------- */

private class CurrencyAdapter(
    val rates: () -> Map<String, Double>,
    val dollar: () -> Double
) : RecyclerView.Adapter<CurrencyAdapter.VH>() {

    private var rows: List<CurrencyEntry> = CurrencyList.display

    class VH(val b: com.lahzei.app.databinding.ItemCurrencyBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        com.lahzei.app.databinding.ItemCurrencyBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    )

    override fun getItemCount() = rows.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val e = rows[pos]
        h.b.tvFlag.text = e.flag
        h.b.tvCode.text = e.code
        h.b.tvName.text = e.name

        val d = dollar()
        if (e.code == "USD") {
            h.b.tvToman.text = if (d > 0) F.money(d) + " تومان" else "—"
            h.b.tvRate.text = "نرخ بازار آزاد ایران"
            return
        }

        val perUsd = rates()[e.code]
        if (perUsd == null || perUsd <= 0 || d <= 0) {
            h.b.tvToman.text = "—"
            h.b.tvRate.text = "در انتظار داده…"
            return
        }
        // er-api rates: units of `code` per 1 USD
        h.b.tvToman.text = F.money(d / perUsd, if (d / perUsd < 100) 2 else 0) + " تومان"
        h.b.tvRate.text = "1 دلار = ${F.rate(perUsd)} ${e.code}"
    }
}
