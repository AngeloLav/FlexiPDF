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
 * Inoltre, implementa le funzioni necessarie per permettere all'utente di scegliere il linguaggio
 * preferito all'interno dell'applicazione. Inizialmente il linguaggio è impostato uguale a quello
 * predefinito del dispositivo, poi si può cambiare tramite l'interfaccia di dialogo che compare
 * appena cliccata l'impostazione Lingua/Language/Sprache.
 */
package it.lavorodigruppo.flexipdf.fragments

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import it.lavorodigruppo.flexipdf.R
import it.lavorodigruppo.flexipdf.activities.MainActivity
import it.lavorodigruppo.flexipdf.adapters.SettingsAdapter
import it.lavorodigruppo.flexipdf.items.SettingsItem
import it.lavorodigruppo.flexipdf.data.SettingsDatasource
import java.util.Locale

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

    companion object {
        private const val PREFS_NAME = "FlexiAPPPrefs"
        private const val KEY_SELECTED_LANGUAGE = "selectedLanguage"
        private const val TAG = "SettingsFragment"

        fun applyLocale(context: Context) {
            val languageCode = getSelectedLanguageCode(context)
            if (languageCode.isNotEmpty()) {
                val targetLocale = Locale(languageCode)
                Locale.setDefault(targetLocale)

                val resources: Resources = context.resources
                val config: Configuration = resources.configuration

                val appLocaleListCompat: LocaleListCompat = LocaleListCompat.create(targetLocale)
                AppCompatDelegate.setApplicationLocales(appLocaleListCompat)

                config.setLocale(targetLocale)
                config.setLayoutDirection(targetLocale)
                Log.d(
                    TAG,
                    "Locale ${targetLocale.toLanguageTag()} applied to current resources config."
                )
            } else {
                Log.d(TAG, "No language code selected or empty, using system default.")
            }
        }

        private fun getSelectedLanguageCode(context: Context): String {
            val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return sharedPreferences.getString(KEY_SELECTED_LANGUAGE, "") ?: ""
        }
    }

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

        // Ottimizzazione: indica che la dimensione del RecyclerView non cambierà, migliorando le performance.
        settingsRecyclerView.setHasFixedSize(true)

        // Imposta un LinearLayoutManager per visualizzare gli elementi in una lista verticale.
        settingsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        val dataSource = SettingsDatasource(requireContext())
        settingsList = dataSource.getSettingsOptions()

        settingsAdapter = SettingsAdapter(settingsList) { showLanguageSelectionDialog() }
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


    //Funzioni ausiliari per la gestione della lingua selizionata in app
    private fun showLanguageSelectionDialog() {
        val languageDisplay = resources.getStringArray(R.array.language_options)
        val languageValues = resources.getStringArray(R.array.language_options_values)
        val currentLanguageCode = getSelectedLanguageCode(requireContext())

        var checkedItem = languageValues.indexOf(currentLanguageCode)
        //Fa il controllo se la lingue con indice emmessa è supportata dalla app
        if(checkedItem == -1) { checkedItem = 0  }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.settings_language_title))
            .setSingleChoiceItems(languageDisplay, checkedItem){
                dialog, which -> val selectedLanguageCode = languageValues[which]
                if (selectedLanguageCode != currentLanguageCode) {
                    persistLanguage(selectedLanguageCode)
                    applyLocaleAndRecreateActivity()
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
        }

    //Questa funzione fa si che dopo che il linguaggio sia stato cambiato l'activity venga riavviata
    private fun applyLocaleAndRecreateActivity() {
        Log.d(TAG, "Calling Companion.applyLocale to update application context.")
            applyLocale(requireContext().applicationContext)

        Log.d(TAG, "Recreating activity to apply language changes.")
        activity?.let { currentActivity ->
            val intent = Intent(currentActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            currentActivity.startActivity(intent)
            currentActivity.finish() } ?: run {
            Log.e(TAG, "Activity is null, cannot recreate.")
            Toast.makeText(requireContext(), requireContext().getString(R.string.language_missing_message),
                Toast.LENGTH_SHORT).show()
        }
    }

    private fun persistLanguage(selectedLanguageCode: String?) {
        val sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(KEY_SELECTED_LANGUAGE, selectedLanguageCode).apply()
    }
}