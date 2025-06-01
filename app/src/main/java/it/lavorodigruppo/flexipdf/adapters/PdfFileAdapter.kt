/**
 * @file PdfFileAdapter.kt
 *
 * @brief Adapter per RecyclerView per visualizzare gli elementi PdfFileItem.
 *
 * Questo file contiene l'interfaccia OnPdfFileClickListener per la gestione dei click sugli elementi,
 * e la classe PdfFileAdapter che è un adattatore per RecyclerView.
 * Si occupa di collegare i dati della lista di PdfFileItem all'interfaccia utente,
 * creando e gestendo le view per ogni elemento della lista.
 * Utilizza View Binding per un accesso efficiente ai componenti delle view di ogni singolo item.
 *
 */

package it.lavorodigruppo.flexipdf.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import it.lavorodigruppo.flexipdf.R
import it.lavorodigruppo.flexipdf.databinding.PdfFileItemBinding
import it.lavorodigruppo.flexipdf.items.PdfFileItem

/**
 * Interfaccia di callback per gestire gli eventi di click sugli elementi della lista di PDF.
 * Qualsiasi componente che voglia ricevere notifiche sui click degli elementi
 * deve implementare questa interfaccia.
 */

interface OnPdfFileClickListener {
    /**
     * Chiamato quando un elemento PdfFileItem nella lista viene cliccato.
     * @param pdfFile L'oggetto PdfFileItem che è stato cliccato.
     */
    fun onPdfFileClick(pdfFile: PdfFileItem)
}

class PdfFileAdapter(private val listener: OnPdfFileClickListener) : RecyclerView.Adapter<PdfFileAdapter.PdfFileViewHolder>() {

    private var pdfFiles: List<PdfFileItem> = emptyList()

    /**
     * PdfFileViewHolder è la classe ViewHolder che detiene i riferimenti alle view
     * per un singolo elemento della lista
     *
     * @param binding Un'istanza di PdfFileItemBinding generata tramite View Binding,
     * che fornisce un accesso diretto e sicuro alle view del layout dell'item.
     */
    class PdfFileViewHolder(private val binding: PdfFileItemBinding) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Collega i dati di un PdfFileItem alle view all'interno del ViewHolder.
         *
         * @param pdfFile L'oggetto PdfFileItem da visualizzare in questo ViewHolder.
         */
        fun bind(pdfFile: PdfFileItem) {
            // Imposta il testo del TextView con il nome visualizzato del PDF.
            binding.titleTextView.text = pdfFile.displayName
            // Imposta l'immagine dell'ImageView con l'icona predefinita del PDF.
            binding.iconImageView.setImageResource(R.drawable.pdf_svgrepo_com)
        }
    }

    /**
     * Chiamato quando RecyclerView ha bisogno di un nuovo PdfFileViewHolder.
     * Questo metodo "gonfia" (inflates) il layout di un singolo elemento della lista
     * utilizzando View Binding e crea un nuovo PdfFileViewHolder che lo contiene.
     *
     * @param parent Il ViewGroup in cui verrà inserita la nuova View dopo essere stata associata a un adapter position.
     * @param viewType Il tipo di view del nuovo ViewHolder; al momento ce un solo tipo di item, ma poi aggiungeremo anche le cartelle
     * @return Una nuova istanza di PdfFileViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfFileViewHolder {
        // Gonfia il layout 'pdf_file_item.xml' utilizzando LayoutInflater e View Binding.
        val binding = PdfFileItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PdfFileViewHolder(binding)
    }

    /**
     * Chiamato da RecyclerView per visualizzare i dati in una posizione specificata.
     * Questo metodo aggiorna i contenuti di un PdfFileViewHolder esistente con i dati
     * della posizione corrente e imposta un OnClickListener per l'elemento.
     *
     * @param holder Il PdfFileViewHolder che deve essere aggiornato.
     * @param position La posizione dell'elemento nel set di dati dell'adapter.
     */
    override fun onBindViewHolder(holder: PdfFileViewHolder, position: Int) {
        // Recupera l'oggetto PdfFileItem dalla lista dei dati in base alla posizione.
        val pdfFile = pdfFiles[position]
        // Binda i dati del PdfFileItem al ViewHolder.
        holder.bind(pdfFile)

        // Imposta un click listener per l'intera view dell'elemento (itemView).
        // Quando l'elemento viene cliccato, viene invocato il callback onPdfFileClick
        // sull'interfaccia del listener fornita al costruttore dell'adapter.
        holder.itemView.setOnClickListener {
            listener.onPdfFileClick(pdfFile)
        }
    }

    /**
     * Restituisce il numero totale di elementi nel set di dati tenuto dall'adapter.
     * Obbligatorio da implemetare per le recyclerView
     * @return Il numero di elementi nella lista pdfFiles.
     */
    override fun getItemCount(): Int {
        return pdfFiles.size
    }

    /**
     * Aggiorna la lista di PdfFileItem visualizzata dall'adapter e notifica il RecyclerView
     * che il set di dati è cambiato.
     *
     * `@SuppressLint("NotifyDataSetChanged")`: Questa annotazione sopprime il warning
     * che sconsiglia l'uso di `notifyDataSetChanged()` per motivi di performance e animazioni.
     *
     * @param newList La nuova [List] di [PdfFileItem] da visualizzare.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newList: List<PdfFileItem>) {
        // Aggiorna la lista interna dell'adapter.
        pdfFiles = newList

        // Notifica al RecyclerView che l'intero set di dati è cambiato,
        // forzando un ridisegno completo della lista.
        notifyDataSetChanged()
    }
}