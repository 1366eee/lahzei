package com.lahzei.app.ui.crypto

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.lahzei.app.R
import com.lahzei.app.data.Coin
import com.lahzei.app.ui.AppViewModel
import com.lahzei.app.util.F

class CryptoFragment : Fragment() {

    private val vm: AppViewModel by lazy {
        ViewModelProvider(requireActivity())[AppViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_crypto, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rv = view.findViewById<RecyclerView>(R.id.rv_list)
        val progress = view.findViewById<ProgressBar>(R.id.progress)
        val errorText = view.findViewById<TextView>(R.id.error_text)
        val retry = view.findViewById<Button>(R.id.btn_retry)
        val tvDollar = view.findViewById<TextView>(R.id.tv_dollar_value)
        val btnRefresh = view.findViewById<ExtendedFloatingActionButton>(R.id.btn_refresh)

        rv.layoutManager = LinearLayoutManager(requireContext())
        val adapter = CryptoAdapter { vm.dollarToman ?: 0.0 }
        rv.adapter = adapter

        vm.cryptoLoading.observe(viewLifecycleOwner) {
            progress.visibility = if (it) View.VISIBLE else View.GONE
        }
        vm.cryptoError.observe(viewLifecycleOwner) { err ->
            if (err != null && vm.crypto.value == null) {
                errorText.text = err
                errorText.visibility = View.VISIBLE
                retry.visibility = View.VISIBLE
            } else {
                errorText.visibility = View.GONE
                retry.visibility = View.GONE
            }
        }
        vm.crypto.observe(viewLifecycleOwner) { list ->
            if (list != null) adapter.submit(list)
        }
        vm.tgju.observe(viewLifecycleOwner) { data ->
            if (data != null) {
                adapter.submit(vm.crypto.value ?: emptyList())
                tvDollar.text = "دلار مبنا: ${F.money(data.dollarToman)} تومان"
            }
        }

        retry.setOnClickListener { vm.refreshCrypto() }
        btnRefresh.setOnClickListener {
            vm.refreshCrypto()
            if (vm.tgju.value == null) vm.refreshTgju()
        }

        if (vm.crypto.value == null) vm.refreshCrypto()
        if (vm.tgju.value == null) vm.refreshTgju()
    }
}

/* ---------------- adapter ---------------- */

private class CryptoAdapter(val dollar: () -> Double) :
    RecyclerView.Adapter<CryptoAdapter.VH>() {

    private var rows: List<Coin> = emptyList()

    fun submit(rows: List<Coin>) {
        this.rows = rows
        notifyDataSetChanged()
    }

    class VH(val b: com.lahzei.app.databinding.ItemCryptoBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        com.lahzei.app.databinding.ItemCryptoBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    )

    override fun getItemCount() = rows.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val c = rows[pos]
        h.b.tvSymbol.text = c.symbol
        h.b.tvName.text = c.name
        h.b.tvRank.text = if (c.rank != null) F.fa(c.rank.toString()) else "•"
        h.b.tvUsd.text = "$" + F.money(c.usd, if (c.usd < 10) 4 else 2)
        val d = dollar()
        h.b.tvToman.text = if (d > 0) F.money(c.usd * d) + " تومان" else "—"
        val ch = c.change24h
        if (ch != null && ch != 0.0) {
            h.b.tvChange.visibility = View.VISIBLE
            h.b.tvChange.text = F.pct(ch)
            h.b.tvChange.setBackgroundResource(if (ch > 0) R.drawable.bg_chip_up else R.drawable.bg_chip_down)
            h.b.tvChange.setTextColor(
                ContextCompat.getColor(
                    h.b.root.context,
                    if (ch > 0) R.color.chip_up_fg else R.color.chip_down_fg
                )
            )
        } else {
            h.b.tvChange.visibility = View.GONE
        }
    }
}
