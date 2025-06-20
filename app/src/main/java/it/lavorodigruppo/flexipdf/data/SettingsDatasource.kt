/**
 * @file SettingsDatasource.kt
 *
 * @brief Datasource per le opzioni delle impostazioni predefinite dell'applicazione.
 *
 * Questa classe fornisce un accesso ai dati per le opzioni di impostazione fisse,
 * leggendole direttamente dalle risorse dell'applicazione (stringhe e drawable).
 * Non gestisce la persistenza di queste impostazioni, ma solo la loro definizione iniziale.
 *
 */
package it.lavorodigruppo.flexipdf.data

import android.content.Context
import it.lavorodigruppo.flexipdf.R
import it.lavorodigruppo.flexipdf.items.SettingsItem

/**
 * Datasource per la gestione delle opzioni di impostazione fisse dell'applicazione.
 * Questa classe è responsabile di recuperare le definizioni delle impostazioni
 * (come titoli e icone) dalle risorse dell'applicazione.
 *
 * @param context Il contesto dell'applicazione, utilizzato per accedere alle risorse.
 */
class SettingsDatasource(private val context: Context) {

    /**
     * Recupera e restituisce una lista di `SettingsItem` che rappresentano le opzioni di impostazione disponibili.
     * Le opzioni di testo (titoli) vengono caricate da un array di stringhe definito nelle risorse dell'applicazione,
     * e le icone corrispondenti sono predefinite come ID di risorse drawable.
     *
     * @return Una `List` di `SettingsItem` contenente tutte le opzioni di impostazione predefinite,
     * pronta per essere visualizzata in un'interfaccia utente come una `RecyclerView`.
     */
    fun getSettingsOptions(): List<SettingsItem> {
        val settingsList = mutableListOf<SettingsItem>()
        val options = context.resources.getStringArray(R.array.settings_options)
        val icons = listOf(
            R.drawable.baseline_language_24,
            R.drawable.contrast_24dp_ffffff_fill0_wght400_grad0_opsz24,
            R.drawable.info_24dp_ffffff_fill0_wght400_grad0_opsz24,
            R.drawable.description_24dp_ffffff_fill0_wght400_grad0_opsz24,
            R.drawable.help_24dp_ffffff_fill0_wght400_grad0_opsz24,
            R.drawable.share_24dp_ffffff_fill0_wght400_grad0_opsz24
        )

        for (i in options.indices) {
            settingsList.add(SettingsItem(options[i], icons[i]))
        }
        return settingsList
    }
}