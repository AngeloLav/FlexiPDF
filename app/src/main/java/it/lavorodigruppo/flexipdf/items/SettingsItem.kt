/**
 * @file SettingsItem.kt
 * @brief Definizione della data class per un singolo elemento di impostazione.
 *
 * Questo file contiene la modella un'opzione visualizzabile nella schermata delle impostazioni.
 * Incapsula il titolo testuale dell'opzione e l'ID della risorsa drawable
 * per l'icona associata.
 *
 * @author Angelo
 * @date 31/05/25
 */
package it.lavorodigruppo.flexipdf.items

/**
 * @property title Il testo o il titolo dell'opzione di impostazione.
 * @property iconResId L'ID della risorsa drawable che rappresenta l'icona
 * associata a questa opzione di impostazione.
 * @property id L'identificazione del tipo di impostazione (e.g "language")
 */
data class SettingsItem(
    val title: String,
    val id: String,
    val iconResId: Int,
)