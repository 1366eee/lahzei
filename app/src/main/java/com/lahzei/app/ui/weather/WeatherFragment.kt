package com.lahzei.app.ui.weather

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lahzei.app.R
import com.lahzei.app.data.Cities
import com.lahzei.app.data.City
import com.lahzei.app.data.WeatherData
import com.lahzei.app.util.F
import com.lahzei.app.util.Wx

class WeatherFragment : Fragment() {

    private val vm: WeatherViewModel by lazy {
        ViewModelProvider(this)[WeatherViewModel::class.java]
    }

    private val majorAdapter = MajorAdapter()
    private val searchAdapter = SearchAdapter { startDetail(it) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_weather, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val swipe = view.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipe)
        val progress = view.findViewById<ProgressBar>(R.id.progress)
        val errorText = view.findViewById<TextView>(R.id.error_text)
        val rvMajor = view.findViewById<RecyclerView>(R.id.rv_major)
        val rvSearch = view.findViewById<RecyclerView>(R.id.rv_search)
        val tvSearchTitle = view.findViewById<TextView>(R.id.tv_search_title)
        val searchInput = view.findViewById<EditText>(R.id.search_input)

        rvMajor.layoutManager = GridLayoutManager(requireContext(), 2)
        rvMajor.adapter = majorAdapter

        rvSearch.layoutManager = LinearLayoutManager(requireContext())
        rvSearch.adapter = searchAdapter

        vm.loading.observe(viewLifecycleOwner) {
            progress.visibility = if (it) View.VISIBLE else View.GONE
            if (!it) swipe.isRefreshing = false
        }
        swipe.setOnRefreshListener { vm.load(true) }
        vm.error.observe(viewLifecycleOwner) {
            if (it != null) { errorText.text = it; errorText.visibility = View.VISIBLE } else errorText.visibility = View.GONE
        }
        vm.major.observe(viewLifecycleOwner) { map ->
            majorAdapter.submit(
                Cities.majorCities.map { c -> Triple(c, map[c.name], map[c.name]?.let { w -> w.temp }) },
                ::startDetail
            )
        }

        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val q = (s?.toString() ?: "").trim()
                if (q.isEmpty()) {
                    tvSearchTitle.visibility = View.GONE
                    rvSearch.visibility = View.GONE
                } else {
                    val hits = Cities.all.filter { it.name.contains(q) }.take(10)
                    tvSearchTitle.visibility = if (hits.isEmpty()) View.GONE else View.VISIBLE
                    rvSearch.visibility = if (hits.isEmpty()) View.GONE else View.VISIBLE
                    searchAdapter.submit(hits)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })

        vm.load(false)
    }

    private fun startDetail(city: City) {
        val i = Intent(requireContext(), WeatherDetailActivity::class.java)
        i.putExtra("name", city.name)
        i.putExtra("province", city.province)
        i.putExtra("lat", city.lat)
        i.putExtra("lon", city.lon)
        startActivity(i)
    }
}

/* ---------------- majors grid ---------------- */

private class MajorAdapter : RecyclerView.Adapter<MajorAdapter.VH>() {

    private var rows: List<Triple<City, WeatherData?, Double?>> = emptyList()
    private var onClick: ((City) -> Unit)? = null

    fun submit(rows: List<Triple<City, WeatherData?, Double?>>, onClick: (City) -> Unit) {
        this.rows = rows
        this.onClick = onClick
        notifyDataSetChanged()
    }

    class VH(val b: com.lahzei.app.databinding.ItemMajorCityBinding) :
        RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        com.lahzei.app.databinding.ItemMajorCityBinding.inflate(
            android.view.LayoutInflater.from(parent.context), parent, false
        )
    )

    override fun getItemCount() = rows.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val (city, w, _) = rows[pos]
        h.b.tvCity.text = city.name
        if (w == null) {
            h.b.tvEmoji.text = "⏳"
            h.b.tvTemp.text = "--"
            h.b.tvDesc.text = "در حال دریافت"
        } else {
            h.b.tvEmoji.text = Wx.emoji(w.code, w.isDay)
            h.b.tvTemp.text = F.temp(w.temp)
            h.b.tvDesc.text = Wx.desc(w.code)
        }
        h.b.root.setOnClickListener { onClick?.invoke(city) }
    }
}

/* ---------------- search rows ---------------- */

private class SearchAdapter(val onClick: (City) -> Unit) :
    RecyclerView.Adapter<SearchAdapter.VH>() {

    private var rows: List<City> = emptyList()

    fun submit(rows: List<City>) {
        this.rows = rows
        notifyDataSetChanged()
    }

    class VH(val b: com.lahzei.app.databinding.ItemCityRowBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        com.lahzei.app.databinding.ItemCityRowBinding.inflate(
            android.view.LayoutInflater.from(parent.context), parent, false
        )
    )

    override fun getItemCount() = rows.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val c = rows[pos]
        h.b.tvName.text = c.name
        h.b.tvProvince.text = c.province
        h.b.root.setOnClickListener { onClick(c) }
    }
}
