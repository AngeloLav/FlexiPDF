package it.lavorodigruppo.flexipdf.data

import android.content.Context
import it.lavorodigruppo.flexipdf.R
import it.lavorodigruppo.flexipdf.items.SettingsItem

class SettingsDatasource(private val context: Context) {

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

        for (i in options.indices)
            settingsList.add(SettingsItem(options[i], icons[i]))

        return settingsList
    }
}