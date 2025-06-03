/**
 * @file SettingsAdapter.kt
 *
 * @brief Adapter per RecyclerView per visualizzare le opzioni delle impostazioni.
 *
 * Questo file contiene la classe SettingsAdapter, che è un adattatore per RecyclerView
 * responsabile di collegare i dati delle opzioni di impostazione all'interfaccia utente.
 * Gestisce la creazione e il binding delle view per ogni elemento della lista,
 * e implementa la logica di gestione dei click per ciascuna opzione di impostazione.
 *
 */
package it.lavorodigruppo.flexipdf.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.recyclerview.widget.RecyclerView
import it.lavorodigruppo.flexipdf.R
import it.lavorodigruppo.flexipdf.items.SettingsItem
import androidx.core.net.toUri
import android.widget.Toast
import it.lavorodigruppo.flexipdf.databinding.SettingsItemBinding


class SettingsAdapter(private val settingsList: List<SettingsItem>) :
    RecyclerView.Adapter<SettingsAdapter.SettingsViewHolder>() {

    /**
     * SettingsViewHolder è la classe ViewHolder che detiene i riferimenti alle view
     * per un singolo elemento della lista delle impostazioni.
     *
     * @param binding Un'istanza di SettingsItemBinding generata tramite View Binding,
     * che fornisce un accesso diretto e sicuro alle view del layout dell'item.
     */
    class SettingsViewHolder(private val binding: SettingsItemBinding) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Collega i dati di un SettingsItem alle view all'interno del ViewHolder.
         *
         * @param item L'oggetto [SettingsItem] da visualizzare in questo ViewHolder.
         */
        fun bind(item: SettingsItem) {
            // Imposta il testo del TextView con il titolo dell'impostazione, accedendo tramite binding.
            binding.settingTitleTextView.text = item.title
            // Imposta l'immagine dell'ImageView con l'icona dell'impostazione, accedendo tramite binding.
            binding.iconImageView.setImageResource(item.iconResId)
        }
    }

    /**
     * Chiamato quando RecyclerView ha bisogno di un nuovo SettingsViewHolder.
     * Questo metodo "gonfia" (inflates) il layout di un singolo elemento delle impostazioni
     * utilizzando View Binding e crea un nuovo SettingsViewHolder che lo contiene.
     * Imposta anche un OnClickListener per l'intera view dell'elemento.
     *
     * @param parent Il ViewGroup in cui verrà inserita la nuova View.
     * @param viewType Il tipo di view del nuovo ViewHolder (non utilizzato in questo adattatore, in cui c'è un solo tipo di item).
     * @return Una nuova istanza di SettingsViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingsViewHolder {
        // Gonfia il layout 'settings_item.xml' utilizzando la classe di binding.
        val binding = SettingsItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        // Imposta il click listener per la view radice dell'elemento (binding.root).
        // Questo listener è definito come una proprietà privata dell'adapter.
        binding.root.setOnClickListener(onClickListener)

        // Passa l'oggetto 'binding' al ViewHolder.
        return SettingsViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return settingsList.size
    }

    override fun onBindViewHolder(holder: SettingsViewHolder, position: Int) {
        val currentItem = settingsList[position]
        holder.bind(currentItem)
        // L'oggetto viene associato al tag della view radice del ViewHolder.
        holder.itemView.tag = currentItem
    }


    // --- Listeners for Settings ---
    private val onClickListener = View.OnClickListener { v ->
        val clickedSettingsItem = v.tag as? SettingsItem

        clickedSettingsItem?.let { item ->
            when (item.title) {
                "Language" -> {
                    Toast.makeText(v.context, "Language clicked", Toast.LENGTH_SHORT).show()
                }
                "Theme" -> {
                    val currentNightMode = AppCompatDelegate.getDefaultNightMode()
                    if (currentNightMode == AppCompatDelegate.MODE_NIGHT_NO) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    }
                }
                "About Us" -> {
                    Toast.makeText(v.context, "About us clicked", Toast.LENGTH_SHORT).show()
                }
                "Credits" -> {
                    Toast.makeText(v.context, "Credits clicked", Toast.LENGTH_SHORT).show()
                }
                "Help" -> {
                    Toast.makeText(v.context, "Help clicked", Toast.LENGTH_SHORT).show()
                }
                "Share App" -> {
                    val appUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
                    val intent = Intent(Intent.ACTION_VIEW, appUrl.toUri())
                    v.context.startActivity(intent)
                }
                else -> {
                    Toast.makeText(v.context, "Unknown: ${item.title}", Toast.LENGTH_SHORT).show()
                }

            }
        }
    }
}