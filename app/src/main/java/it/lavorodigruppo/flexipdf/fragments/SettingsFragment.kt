package it.lavorodigruppo.flexipdf.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import it.lavorodigruppo.flexipdf.R
import it.lavorodigruppo.flexipdf.adapters.SettingsAdapter
import it.lavorodigruppo.flexipdf.items.SettingsItem
import it.lavorodigruppo.flexipdf.data.SettingsDatasource


class SettingsFragment : Fragment() {

    private lateinit var settingsRecyclerView: RecyclerView
    private lateinit var settingsAdapter: SettingsAdapter
    private lateinit var settingsList: List<SettingsItem>

    private var originalBannerPaddingTop = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        settingsRecyclerView = view.findViewById(R.id.settingsRecyclerView)
        settingsRecyclerView.setHasFixedSize(true)
        settingsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        val dataSource = SettingsDatasource(requireContext())
        settingsList = dataSource.getSettingsOptions()

        settingsAdapter = SettingsAdapter(settingsList)
        settingsRecyclerView.adapter = settingsAdapter

        // --- WindowInsets management ---
        val bannerContentLayout = view.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.bannerContentLayout)

        originalBannerPaddingTop = bannerContentLayout.paddingTop

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            bannerContentLayout.setPadding(
                bannerContentLayout.paddingLeft,
                originalBannerPaddingTop + systemBarsInsets.top,
                bannerContentLayout.paddingRight,
                bannerContentLayout.paddingBottom
            )

            settingsRecyclerView.setPadding(
                settingsRecyclerView.paddingLeft,
                settingsRecyclerView.paddingTop,
                settingsRecyclerView.paddingRight,
                systemBarsInsets.bottom
            )

            insets
        }

        return view
    }
}