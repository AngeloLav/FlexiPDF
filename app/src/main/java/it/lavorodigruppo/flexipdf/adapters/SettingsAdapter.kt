/**
 * @file SettingsAdapter.kt
 *
 * @brief Adapter per RecyclerView per visualizzare le opzioni delle impostazioni.
 *
 * Questo file contiene la classe SettingsAdapter, che è un adattatore per RecyclerView
 * responsabile di collegare i dati delle opzioni di impostazione all'interfaccia utente.
 * Gestisce la creazione e il binding delle view per ogni elemento della lista,
 * e implementa la logica di gestione dei click per ciascuna opzione di impostazione.
 * Ad ogni impostazione corrisponde un messaggio particolare definito tramite @fun Toast.make
 * in cui si sono usate le risorse di tipo stringa in modo da poter essere adattabili ad ogni
 * linguaggio sopportato dalla applicazione. (La risorsa di tipo title è quella che definisce
 * l'impostazione ad esempio "Lingua/Language/Sprache", mentre la risorsa message fa visualizzare
 * un breve messaggio sullo schermo sull'impostazione cliccata.)
 *
 */
package it.lavorodigruppo.flexipdf.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.RecyclerView
import it.lavorodigruppo.flexipdf.R
import it.lavorodigruppo.flexipdf.items.SettingsItem
import androidx.core.net.toUri
import android.widget.Toast
import it.lavorodigruppo.flexipdf.databinding.SettingsItemBinding


class SettingsAdapter(private val settingsList: List<SettingsItem>, private val onLanguageSettingClicked: () -> Unit) :
/**
 * Adattatore per `RecyclerView` che visualizza una lista di opzioni di impostazione.
 * Questo adattatore è responsabile di prendere una lista di `SettingsItem` e di visualizzarli
 * in una `RecyclerView`, gestendo anche gli eventi di click su ciascuna opzione.
 *
 * @param settingsList La lista di oggetti `SettingsItem` da visualizzare.
 */
class SettingsAdapter(private val settingsList: List<SettingsItem>, private val onLanguageSettingClicked: () -> Unit) :
    RecyclerView.Adapter<SettingsAdapter.SettingsViewHolder>() {

    /**
     * `SettingsViewHolder` è la classe ViewHolder che detiene i riferimenti alle view
     * per un singolo elemento della lista delle impostazioni. È responsabile di legare
     * i dati di un `SettingsItem` agli elementi grafici nel layout.
     *
     * @param binding Un'istanza di `SettingsItemBinding` generata tramite View Binding,
     * che fornisce un accesso diretto e sicuro alle view del layout dell'item `settings_item.xml`.
     */
    class SettingsViewHolder(private val binding: SettingsItemBinding) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Collega i dati di un `SettingsItem` alle view all'interno del ViewHolder.
         * Imposta il testo del titolo e l'immagine dell'icona basandosi sull'oggetto `item` fornito.
         *
         * @param item L'oggetto `SettingsItem` da visualizzare in questo ViewHolder.
         */
        fun bind(item: SettingsItem) {
            binding.settingTitleTextView.text = item.title
            binding.iconImageView.setImageResource(item.iconResId)
        }
    }

    /**
     * Chiamato quando `RecyclerView` ha bisogno di un nuovo `SettingsViewHolder`.
     * Questo metodo "gonfia" (inflates) il layout di un singolo elemento delle impostazioni
     * (`settings_item.xml`) utilizzando View Binding e crea un nuovo `SettingsViewHolder` che lo contiene.
     * Imposta anche un `OnClickListener` per la view radice di ogni elemento.
     *
     * @param parent Il `ViewGroup` in cui verrà inserita la nuova View.
     * @param viewType Il tipo di view del nuovo ViewHolder (non utilizzato in questo adattatore, in cui c'è un solo tipo di item).
     * @return Una nuova istanza di `SettingsViewHolder`.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingsViewHolder {
        val binding = SettingsItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.root.setOnClickListener(onClickListener)
        return SettingsViewHolder(binding)
    }

    /**
     * Restituisce il numero totale di elementi nella lista delle impostazioni gestita dall'adattatore.
     *
     * @return Il numero di elementi presenti in `settingsList`.
     */
    override fun getItemCount(): Int {
        return settingsList.size
    }

    /**
     * Collega i dati di un `SettingsItem` al `SettingsViewHolder` nella posizione specificata.
     * Questo metodo è responsabile di aggiornare il contenuto del ViewHolder per riflettere
     * i dati dell'elemento corrente e di associare l'oggetto `SettingsItem` alla view
     * per un successivo recupero nel click listener.
     *
     * @param holder Il `SettingsViewHolder` da aggiornare.
     * @param position La posizione dell'elemento all'interno della lista dell'adattatore.
     */
    override fun onBindViewHolder(holder: SettingsViewHolder, position: Int) {
        val currentItem = settingsList[position]
        holder.bind(currentItem)
        holder.itemView.tag = currentItem
    }

    /**
     * `OnClickListener` privato per gestire i click sugli elementi della lista delle impostazioni.
     * Quando un elemento viene cliccato, questo listener recupera l'oggetto `SettingsItem` associato
     * alla vista (tramite il tag) ed esegue un'azione specifica basata sul titolo dell'impostazione.
     * Le azioni includono la visualizzazione di Toast, il cambio del tema dell'app, o l'apertura di un URL esterno.
     */
    private val onClickListener = View.OnClickListener { v ->
        val clickedSettingsItem = v.tag as? SettingsItem

        clickedSettingsItem?.let { item ->
            when (item.title) {

                v.context.getString(R.string.settings_language_title) -> {
                    onLanguageSettingClicked()
                }

                v.context.getString(R.string.settings_theme_title) -> {
                    val currentNightMode = AppCompatDelegate.getDefaultNightMode()
                    if (currentNightMode == AppCompatDelegate.MODE_NIGHT_NO) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                        Toast.makeText(v.context, v.context.getString(R.string.theme_changed_to_dark),
                            Toast.LENGTH_SHORT).show()
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                        Toast.makeText(v.context, v.context.getString(R.string.theme_changed_to_light),
                            Toast.LENGTH_SHORT).show()
                    }
                }

                v.context.getString(R.string.settings_about_title) -> {
                    Toast.makeText(v.context, v.context.getString(R.string.about_us_message),
                        Toast.LENGTH_SHORT).show()
                }

                v.context.getString(R.string.settings_credits_title) -> {
                    Toast.makeText(v.context, v.context.getString(R.string.credits_message),
                        Toast.LENGTH_SHORT).show()
                }

                v.context.getString(R.string.settings_help_title) -> {
                    Toast.makeText(v.context, v.context.getString(R.string.help_message),
                        Toast.LENGTH_SHORT).show()
                }

                v.context.getString(R.string.settings_share_title) -> {
                    Toast.makeText(v.context, "Share clicked", Toast.LENGTH_SHORT).show()
                }


                else -> {
                    Toast.makeText(v.context, "Unknown: ${item.title}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}