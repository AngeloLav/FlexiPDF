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

class SettingsDatasource(private val context: Context) {

    /**
     * Recupera e restituisce una lista di SettingsItem che rappresentano le opzioni di impostazione.
     * Le opzioni di testo e le icone vengono caricate da risorse predefinite dell'applicazione.
     *
     * @return Una List di SettingsItem pronta per essere visualizzata nell'interfaccia utente delle impostazioni.
     */
    fun getSettingsOptions(): List<SettingsItem> {
        val settingsList = mutableListOf<SettingsItem>()
        // Carica le opzioni di testo dal file delle risorse stringhe.
        val options = context.resources.getStringArray(R.array.settings_options)

        // Definisce una lista di ID di risorse per le icone corrispondenti a ciascuna opzione.
        val icons = listOf(
            R.drawable.baseline_language_24,
            R.drawable.contrast_24dp_ffffff_fill0_wght400_grad0_opsz24,
            R.drawable.info_24dp_ffffff_fill0_wght400_grad0_opsz24,
            R.drawable.description_24dp_ffffff_fill0_wght400_grad0_opsz24,
            R.drawable.help_24dp_ffffff_fill0_wght400_grad0_opsz24,
            R.drawable.share_24dp_ffffff_fill0_wght400_grad0_opsz24
        )

        for (i in options.indices)
            settingsList.add(SettingsItem(options[i], icons[i]))

        return settingsList
    }
}