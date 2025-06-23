/**
 * @file SettingsItem.kt
 * @brief Definizione della data class per un singolo elemento di impostazione.
 *
 * Questo file contiene la data class `SettingsItem`, che modella un'opzione visualizzabile
 * nella schermata delle impostazioni dell'applicazione. Incapsula le informazioni essenziali
 * per rappresentare un'impostazione nell'interfaccia utente.
 *
 */
package it.lavorodigruppo.flexipdf.items

/**
 * Rappresenta un singolo elemento di impostazione visualizzabile nell'interfaccia utente.
 * Questa `data class` è utilizzata per definire le proprietà di ciascuna opzione
 * presente nella schermata delle impostazioni.
 *
 * @property title Il testo principale o il titolo dell'opzione di impostazione,
 * che sarà visualizzato all'utente (es. "Lingua", "Tema").
 * @property iconResId L'ID della risorsa drawable (es. `R.drawable.my_icon`)
 * che rappresenta l'icona associata a questa opzione di impostazione.
 * Questa icona fornisce un'indicazione visiva rapida dello scopo dell'impostazione.
 * @property id L'identificazione del tipo di impostazione (es. "ID_LANGUAGE")
 */
data class SettingsItem(
    val title: String,
    val id: String,
    val iconResId: Int,
)