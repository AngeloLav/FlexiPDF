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


class SettingsFragment : Fragment() {

    // Dichiarazioni delle variabili lateinit per i componenti della UI e i dati.
    // Verranno inizializzate in onCreateView.
    private lateinit var settingsRecyclerView: RecyclerView
    private lateinit var settingsAdapter: SettingsAdapter
    private lateinit var settingsList: List<SettingsItem>

    // Memorizza il padding superiore originale del layout del banner.
    private var originalBannerPaddingTop = 0

    /**
     * Metodo chiamato per creare e restituire la View associata al Fragment.
     * Questo è il punto in cui il layout XML del Fragment viene gonfiato e i componenti UI vengono inizializzati.
     *
     * @param inflater L'LayoutInflater che può essere utilizzato per gonfiare qualsiasi View nel Fragment.
     * @param container Se non nullo, questo è il ViewGroup genitore a cui la UI del Fragment dovrebbe essere attaccata.
     * @param savedInstanceState Se non nullo, questo Fragment sta venendo ricreato da un precedente stato salvato.
     * @return La View radice del layout del Fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Gonfia il layout del Fragment dal file XML 'fragment_settings.xml'.
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        // Inizializzazione della RecyclerView per le impostazioni.
        settingsRecyclerView = view.findViewById(R.id.settingsRecyclerView)
        // Ottimizzazione: indica che la dimensione del RecyclerView non cambierà, migliorando le performance.
        settingsRecyclerView.setHasFixedSize(true)
        // Imposta un LinearLayoutManager per visualizzare gli elementi in una lista verticale.
        settingsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Inizializzazione del datasource per ottenere le opzioni delle impostazioni.
        val dataSource = SettingsDatasource(requireContext())
        settingsList = dataSource.getSettingsOptions()

        // Inizializzazione e impostazione dell'adapter per la RecyclerView.
        settingsAdapter = SettingsAdapter(settingsList)
        settingsRecyclerView.adapter = settingsAdapter

        // --- Gestione degli WindowInsets ---

        // Trova il layout del banner all'interno della View del Fragment.
        val bannerContentLayout = view.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.bannerContentLayout)

        // Memorizza il padding superiore originale del layout del banner prima di applicare gli insets.
        originalBannerPaddingTop = bannerContentLayout.paddingTop

        // Imposta un listener per gli WindowInsets sulla View radice del Fragment.
        // Questo listener viene chiamato ogni volta che le barre di sistema cambiano posizione o dimensione.
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            // Ottiene gli insets specifici per le barre di sistema (status bar, navigation bar).
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Applica il padding superiore al layout del banner.
            // Il nuovo padding superiore è la somma del padding originale e l'altezza della status bar.
            // Questo spinge il contenuto del banner sotto la status bar, evitando sovrapposizioni.
            bannerContentLayout.setPadding(
                bannerContentLayout.paddingLeft,
                originalBannerPaddingTop + systemBarsInsets.top,
                bannerContentLayout.paddingRight,
                bannerContentLayout.paddingBottom
            )

            // Applica il padding inferiore alla RecyclerView.
            // Questo assicura che il contenuto della lista non venga coperto dalla navigation bar di sistema
            // (o dalla BottomNavigationView se il suo layout lo permette).
            settingsRecyclerView.setPadding(
                settingsRecyclerView.paddingLeft,
                settingsRecyclerView.paddingTop,
                settingsRecyclerView.paddingRight,
                systemBarsInsets.bottom
            )
            // Restituisce gli insets per permettere che vengano propagati ad altre viste se necessario.
            insets
        }

        // --- Fine della gestione degli WindowInsets ---

        return view
    }
}