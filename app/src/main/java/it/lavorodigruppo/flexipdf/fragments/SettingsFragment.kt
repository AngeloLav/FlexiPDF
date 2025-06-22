/**
 * @file SettingsFragment.kt
 *
 * @brief Fragment che rappresenta la schermata delle impostazioni dell'applicazione.
 *
 * Questo Fragment visualizza una lista di opzioni di impostazione tramite una RecyclerView.
 * Carica le opzioni da un SettingsDatasource e le presenta all'utente.
 * Implementa una gestione specifica degli WindowInsets per assicurare che sia il banner
 * che la RecyclerView si adattino correttamente alle barre di sistema (status bar e navigation bar),
 * evitando sovrapposizioni.
 *
 */
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

/**
 * `SettingsFragment` è il frammento che gestisce e visualizza la schermata delle impostazioni dell'applicazione.
 * Popola una `RecyclerView` con le opzioni di impostazione definite nel `SettingsDatasource`
 * e assicura che il layout si adatti correttamente agli `WindowInsets` del sistema.
 */
class SettingsFragment : Fragment() {

    /**
     * Riferimento alla `RecyclerView` che visualizza le opzioni delle impostazioni.
     */
    private lateinit var settingsRecyclerView: RecyclerView

    /**
     * L'adapter per la `RecyclerView` che collega i dati delle impostazioni alle viste.
     */
    private lateinit var settingsAdapter: SettingsAdapter

    /**
     * La lista di oggetti `SettingsItem` che rappresentano le opzioni di impostazione disponibili.
     */
    private lateinit var settingsList: List<SettingsItem>

    /**
     * Memorizza il padding superiore originale del layout del banner per calcoli successivi
     * legati agli `WindowInsets`.
     */
    private var originalBannerPaddingTop = 0

    /**
     * Metodo chiamato per creare e restituire la gerarchia di viste associata al Fragment.
     * In questo metodo, il layout del Fragment viene gonfiato, la `RecyclerView` e il suo `Adapter`
     * vengono inizializzati con i dati provenienti dal `SettingsDatasource`.
     * Inoltre, viene impostato un listener per gli `WindowInsets` sulla vista radice
     * per adattare dinamicamente il padding del banner superiore e della `RecyclerView` inferiore
     * in base alla presenza e dimensione delle barre di sistema (status bar e navigation bar).
     *
     * @param inflater L'oggetto `LayoutInflater` che può essere usato per gonfiare qualsiasi vista nel contesto corrente.
     * @param container Se non nullo, questo è il `ViewGroup` padre a cui la UI del Fragment dovrebbe essere allegata.
     * @param savedInstanceState Se non nullo, questo Fragment viene ricostruito da uno stato precedentemente salvato.
     * @return La vista radice (View) del Fragment.
     */
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