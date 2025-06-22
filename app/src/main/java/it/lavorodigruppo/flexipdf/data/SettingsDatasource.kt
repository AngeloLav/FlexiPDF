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
import it.lavorodigruppo.flexipdf.fragments.SettingsFragment
import it.lavorodigruppo.flexipdf.items.SettingsItem

class SettingsDatasource(private val context: Context) {

    /**
     * Recupera e restituisce una lista di SettingsItem che rappresentano le opzioni di impostazione.
     * Le opzioni di testo e le icone vengono caricate da risorse predefinite dell'applicazione.
     *
     * @return Una List di SettingsItem pronta per essere visualizzata nell'interfaccia utente delle impostazioni.
     */
    fun getSettingsOptions(): List<SettingsItem> {

        //Gestione della lingua dell'applicazione
        val languageValues =context.resources.getStringArray(R.array.language_options_values)
        val languagesDisplay =context.resources.getStringArray(R.array.language_options)


        return listOf(
            SettingsItem(
                title = context.getString(R.string.settings_language_title),
                id = ID_LANGUAGE,
                iconResId = R.drawable.baseline_language_24,
            ),

            SettingsItem(
                title = context.getString(R.string.settings_theme_title),
                id = ID_CONTRAST,
                iconResId = R.drawable.contrast_24dp_ffffff_fill0_wght400_grad0_opsz24,
            ),

            SettingsItem(
                title = context.getString(R.string.settings_about_title),
                id = ID_ABOUT,
                iconResId = R.drawable.info_24dp_ffffff_fill0_wght400_grad0_opsz24
            ),

            SettingsItem(
                title = context.getString(R.string.settings_credits_title),
                id = ID_HELP,
                iconResId = R.drawable.description_24dp_ffffff_fill0_wght400_grad0_opsz24
            ),

            SettingsItem(
                title = context.getString(R.string.settings_help_title),
                id = ID_HELP,
                iconResId = R.drawable.help_24dp_ffffff_fill0_wght400_grad0_opsz24
            )
        )
    }

    companion object {
        const val ID_LANGUAGE = "language_setting"
        const val ID_CONTRAST = "contrast_setting"
        const val ID_ABOUT = "about_setting"
        const val ID_HELP = "help_setting"
        const val ID_SHARE = "share_setting"
    }
}