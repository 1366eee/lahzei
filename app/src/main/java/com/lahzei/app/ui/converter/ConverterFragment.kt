package com.lahzei.app.ui.converter

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.lahzei.app.R
import com.lahzei.app.ui.AppViewModel
import com.lahzei.app.ui.currency.CurrencyList
import com.lahzei.app.util.F

data class ConvUnit(val id: String, val label: String, val toman: Double)

class ConverterFragment : Fragment() {

    private val vm: AppViewModel by lazy {
        ViewModelProvider(requireActivity())[AppViewModel::class.java]
    }

    private var units: List<ConvUnit> = emptyList()
    private var labels: List<String> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_converter, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val spinFrom = view.findViewById<Spinner>(R.id.spin_from)
        val spinTo = view.findViewById<Spinner>(R.id.spin_to)
        val input = view.findViewById<EditText>(R.id.input_amount)
        val btnSwap = view.findViewById<ImageButton>(R.id.btn_swap)
        val btnRefresh = view.findViewById<ExtendedFloatingActionButton>(R.id.btn_refresh)
        val tvResult = view.findViewById<TextView>(R.id.tv_result)
        val tvRate = view.findViewById<TextView>(R.id.tv_rate)
        val tvStatus = view.findViewById<TextView>(R.id.tv_status)

        input.setText("1")

        fun convert() {
            val fromIdx = spinFrom.selectedItemPosition
            val toIdx = spinTo.selectedItemPosition
            if (fromIdx < 0 || toIdx < 0 || fromIdx >= units.size || toIdx >= units.size) {
                tvResult.text = "—"
                tvRate.text = ""
                return
            }
            val amount = input.text.toString().replace(",", "").toDoubleOrNull() ?: 0.0
            val from = units[fromIdx]
            val to = units[toIdx]
            val result = amount * from.toman / to.toman
            tvResult.text = F.money(result, if (result < 1000) 2 else 0) + " " + to.label.substringBefore(" (")
            tvRate.text = "۱ ${from.label} = ${F.money(from.toman / to.toman, if (from.toman / to.toman < 1000) 4 else 2)} ${to.label}"
        }

        fun rebuild(preserve: Boolean) {
            val prevFrom = if (preserve && spinFrom.selectedItemPosition in units.indices)
                units[spinFrom.selectedItemPosition].id else null
            val prevTo = if (preserve && spinTo.selectedItemPosition in units.indices)
                units[spinTo.selectedItemPosition].id else null

            val list = mutableListOf<ConvUnit>()
            list.add(ConvUnit("IRT", "تومان ایران", 1.0))
            list.add(ConvUnit("IRR", "ریال ایران", 0.1))

            val dollar = vm.dollarToman
            vm.tgju.value?.let { t ->
                t.items.filter { !it.isUsd }.forEach {
                    list.add(ConvUnit(it.key, it.title, it.value))
                }
            }

            vm.erRates.value?.let { rates ->
                if (dollar != null && dollar > 0) {
                    CurrencyList.display.forEach { e ->
                        if (e.code == "USD") return@forEach // already covered by TGJU dollar
                        if (e.code == "EUR" && vm.tgju.value?.items?.any { it.key == "price_eur" } == true) return@forEach
                        val perUsd = rates[e.code] ?: return@forEach
                        if (perUsd > 0) list.add(ConvUnit("F_${e.code}", "${e.name} (${e.code})", dollar / perUsd))
                    }
                }
            }

            if (dollar != null && dollar > 0) {
                vm.crypto.value?.forEach { c ->
                    list.add(ConvUnit("C_${c.symbol}", "${c.name} (${c.symbol})", c.usd * dollar))
                }
            }

            units = list.filter { it.toman > 0 }
            labels = units.map { it.label }

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                labels
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinFrom.adapter = adapter
            spinTo.adapter = adapter

            if (preserve) {
                val fi = units.indexOfFirst { it.id == prevFrom }
                val ti = units.indexOfFirst { it.id == prevTo }
                if (fi >= 0) spinFrom.setSelection(fi, false)
                if (ti >= 0) spinTo.setSelection(ti, false)
            } else {
                // default: dollar -> toman (or first available)
                val di = units.indexOfFirst { it.id == "price_dollar_rl" }
                if (di >= 0) {
                    spinFrom.setSelection(di, false)
                    spinTo.setSelection(0, false)
                }
            }

            tvStatus.text = "واحدهای فعال: ${F.fa(units.size.toString())}"
            convert()
        }

        // observe every source; rebuild keeps current selection
        vm.tgju.observe(viewLifecycleOwner) { rebuild(true) }
        vm.erRates.observe(viewLifecycleOwner) { rebuild(true) }
        vm.crypto.observe(viewLifecycleOwner) { rebuild(true) }

        input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = convert()
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        spinFrom.onItemSelectedListener = spinnerListener { convert() }
        spinTo.onItemSelectedListener = spinnerListener { convert() }

        btnSwap.setOnClickListener {
            val f = spinFrom.selectedItemPosition
            spinFrom.setSelection(spinTo.selectedItemPosition)
            spinTo.setSelection(f)
            convert()
        }

        btnRefresh.setOnClickListener { vm.ensureAll() }

        vm.ensureAll()
        rebuild(false)
    }

    private fun spinnerListener(action: () -> Unit) = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) = action()
        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }
}
