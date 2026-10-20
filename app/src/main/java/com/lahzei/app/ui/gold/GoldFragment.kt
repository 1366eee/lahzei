package com.lahzei.app.ui.gold

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
import com.lahzei.app.data.TgjuItem
import com.lahzei.app.ui.AppViewModel
import com.lahzei.app.util.F

class GoldFragment : Fragment() {

    private val vm: AppViewModel by lazy {
        ViewModelProvider(requireActivity())[AppViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_gold, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val rv = view.findViewById<RecyclerView>(R.id.rv_list)
        val progress = view.findViewById<ProgressBar>(R.id.progress)
        val errorText = view.findViewById<TextView>(R.id.error_text)
        val retry = view.findViewById<Button>(R.id.btn_retry)
        val tvDollar = view.findViewById<TextView>(R.id.tv_dollar_value)
        val tvUpdated = view.findViewById<TextView>(R.id.tv_updated)
        val btnRefresh = view.findViewById<ExtendedFloatingActionButton>(R.id.btn_refresh)

        rv.layoutManager = LinearLayoutManager(requireContext())
        val adapter = GoldAdapter()
        rv.adapter = adapter

        vm.tgjuLoading.observe(viewLifecycleOwner) {
            progress.visibility = if (it) View.VISIBLE else View.GONE
        }
        vm.tgjuError.observe(viewLifecycleOwner) { err ->
            if (err != null && vm.tgju.value == null) {
                errorText.text = err
                errorText.visibility = View.VISIBLE
                retry.visibility = View.VISIBLE
            } else {
                errorText.visibility = View.GONE
                retry.visibility = View.GONE
            }
        }
        vm.tgju.observe(viewLifecycleOwner) { data ->
            if (data != null) {
                adapter.submit(data.items)
                tvDollar.text = "${F.money(data.dollarToman)} تومان"
                val t = data.items.firstOrNull { it.key == "price_dollar_rl" }?.time
                tvUpdated.text = if (t != null) "آخرین به‌روزرسانی: ${F.fa(t)}" else ""
            }
        }

        retry.setOnClickListener { vm.refreshTgju() }
        btnRefresh.setOnClickListener { vm.refreshTgju() }

        if (vm.tgju.value == null) vm.refreshTgju()
    }
}

/* ---------------- adapter ---------------- */

private class GoldAdapter : RecyclerView.Adapter<GoldAdapter.VH>() {

    private var rows: List<TgjuItem> = emptyList()

    fun submit(rows: List<TgjuItem>) {
        this.rows = rows
        notifyDataSetChanged()
    }

    class VH(val b: com.lahzei.app.databinding.ItemGoldBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        com.lahzei.app.databinding.ItemGoldBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
    )

    override fun getItemCount() = rows.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = rows[pos]
        h.b.tvTitle.text = item.title
        h.b.tvValue.text = if (item.isUsd) {
            "$" + F.money(item.value)
        } else {
            F.money(item.value) + " تومان"
        }
        val pct = item.changePct
        if (pct != null && item.up != null && pct != 0.0) {
            h.b.tvChange.visibility = View.VISIBLE
            h.b.tvChange.text = F.pct(pct)
            h.b.tvChange.setBackgroundResource(
                if (item.up) R.drawable.bg_chip_up else R.drawable.bg_chip_down
            )
            h.b.tvChange.setTextColor(
                ContextCompat.getColor(
                    h.b.root.context,
                    if (item.up) R.color.chip_up_fg else R.color.chip_down_fg
                )
            )
        } else {
            h.b.tvChange.visibility = View.GONE
        }
        h.b.tvTime.text = item.time?.let { F.fa(it) } ?: ""
    }
}
