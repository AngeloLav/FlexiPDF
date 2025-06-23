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
 * (come titoli, icone e ID) dalle risorse dell'applicazione.
 *
 * @param context Il contesto dell'applicazione, utilizzato per accedere alle risorse.
 */
class SettingsDatasource(private val context: Context) {

    /**
     * Recupera e restituisce una lista di `SettingsItem` che rappresentano le opzioni di impostazione disponibili.
     * Le opzioni di testo (titoli) vengono caricate dalle risorse di tipo stringa, definite nel file strings.xml
     * (es. settings_language_title) e le icone corrispondenti sono predefinite come ID di risorse drawable.
     *
     * @return Una `List` di `SettingsItem` contenente tutte le opzioni di impostazione predefinite,
     * pronta per essere visualizzata in un'interfaccia utente come una `RecyclerView`.
     */
    fun getSettingsOptions(): List<SettingsItem> {

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
            ),

            SettingsItem(
                title = context.getString(R.string.settings_share_title),
                id = ID_SHARE,
                iconResId = R.drawable.share_24dp_ffffff_fill0_wght400_grad0_opsz24
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