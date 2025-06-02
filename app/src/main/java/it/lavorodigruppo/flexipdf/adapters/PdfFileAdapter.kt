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

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import it.lavorodigruppo.flexipdf.R
import it.lavorodigruppo.flexipdf.databinding.PdfFileItemBinding
import it.lavorodigruppo.flexipdf.items.PdfFileItem
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

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
    /**
     * Chiamato quando un elemento PdfFileItem nella lista viene tenuto premuto a lungo.
     * Questo è il punto di ingresso per attivare la modalità di selezione.
     * @param pdfFile L'oggetto PdfFileItem che è stato tenuto premuto.
     */
    fun onPdfFileLongClick(pdfFile: PdfFileItem)
    /**
     * Chiamato quando l'icona del cestino di un PdfFileItem nella lista viene cliccata.
     * @param pdfFile L'oggetto PdfFileItem la cui icona cestino è stata cliccata.
     */
    fun onDeleteIconClick(pdfFile: PdfFileItem)
    fun onFavoriteIconClick(pdfFile: PdfFileItem)
}

class PdfFileAdapter(
    private val listener: OnPdfFileClickListener,
    private var isSelectionModeActive : Boolean = false
) : ListAdapter<PdfFileItem, PdfFileAdapter.PdfFileViewHolder>(PdfFileDiffCallback()) {

    /**
     * Metodo per aggiornare lo stato della modalità di selezione nell'adapter.
     * Chiamato dal Fragment per informare l'adapter quando la modalità di selezione si attiva/disattiva.
     * @param active True se la modalità di selezione è attiva, false altrimenti.
     */
    fun setSelectionModeActive(active: Boolean) {
        if (this.isSelectionModeActive != active) {
            this.isSelectionModeActive = active
        }
    }

    /**
     * PdfFileViewHolder è la classe ViewHolder che detiene i riferimenti alle view
     * per un singolo elemento della lista
     *
     * @param binding Un'istanza di PdfFileItemBinding generata tramite View Binding,
     * che fornisce un accesso diretto e sicuro alle view del layout dell'item.
     */
    class PdfFileViewHolder(private val binding: PdfFileItemBinding) : RecyclerView.ViewHolder(binding.root) {

        private var shakeAnimator: ObjectAnimator? = null

        /**
         * Collega i dati di un PdfFileItem alle view all'interno del ViewHolder
         * e imposta i listener.
         *
         * @param pdfFile L'oggetto PdfFileItem da visualizzare in questo ViewHolder.
         * @param listener Il listener per gli eventi di click e long-click.
         * @param isSelectionModeActive Lo stato corrente della modalità di selezione (passato dall'adapter).
         */
        fun bind(pdfFile: PdfFileItem, listener: OnPdfFileClickListener) {
            binding.titleTextView.text = pdfFile.displayName
            binding.iconImageView.setImageResource(R.drawable.pdf_svgrepo_com)

            //Imposta l'icona della stella in base allo stato isFavorite
            binding.favoriteIcon.setImageResource(
                if (pdfFile.isFavorite) R.drawable.star_24dp_f19e39_fill0_wght400_grad0_opsz24__1_
                else R.drawable.star_24dp_999999_fill0_wght400_grad0_opsz24
            )

            // --- Gestione della visualizzazione in base allo stato di selezione dell'item ---
            if (pdfFile.isSelected) {
                // Se l'elemento è selezionato, cambia il colore di sfondo della CardView
                // e rendi visibile l'icona del cestino.
                binding.deleteIcon.visibility = View.VISIBLE
                binding.favoriteIcon.visibility = View.GONE
                startShakeAnimation(binding.root)
            } else {
                // Se l'elemento non è selezionato, ripristina il colore di sfondo
                // e nascondi l'icona del cestino.
                binding.deleteIcon.visibility = View.GONE
                binding.favoriteIcon.visibility = View.VISIBLE
                stopShakeAnimation()
            }

            // --- Impostazione dei Listener ---

            // Listener per il click normale sull'intero elemento.
            // Il comportamento di questo click dipenderà dalla modalità di selezione.
            binding.root.setOnClickListener {
                listener.onPdfFileClick(pdfFile)
            }

            // Listener per il long-click sull'intero elemento.
            // Questo è il trigger per entrare nella modalità di selezione.
            binding.root.setOnLongClickListener {
                listener.onPdfFileLongClick(pdfFile)
                true
            }

            // Listener per il click sull'icona del cestino.
            // Questa è l'azione per eliminare un elemento specifico.
            binding.deleteIcon.setOnClickListener {
                listener.onDeleteIconClick(pdfFile)
            }

            binding.favoriteIcon.setOnClickListener {
                listener.onFavoriteIconClick(pdfFile)
            }
        }

        // Metodo per avviare l'animazione di tremolio
        private fun startShakeAnimation(view: View) {
            // Se un animatore è già in esecuzione o è stato creato, fermalo e annullalo per evitarne duplicati
            shakeAnimator?.cancel()

            // Crea un ObjectAnimator che anima la proprietà "translationX" (spostamento sull'asse X)
            // L'animazione va da -5f a 5f e poi torna a 0f per creare il tremolio.
            shakeAnimator = ObjectAnimator.ofFloat(view, "translationX", 0f, -5f, 5f, -3f, 3f, -2f, 2f, 0f).apply {
                duration = 500 // Durata dell'intera sequenza di tremolio in millisecondi
                repeatCount = ObjectAnimator.INFINITE // Ripeti l'animazione all'infinito
                repeatMode = ObjectAnimator.RESTART // Ripeti dall'inizio ogni volta
                interpolator = LinearInterpolator() // Interopolatore lineare per un movimento più secco
                start() // Avvia l'animazione
            }
        }

        // Metodo per fermare l'animazione di tremolio
        fun stopShakeAnimation() {
            shakeAnimator?.cancel() // Annulla l'animazione corrente
            binding.root.translationX = 0f // Reimposta la posizione X della vista a 0 per sicurezza
            shakeAnimator = null // Rilascia l'animatore
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
        val pdfFile = getItem(position)
        // Binda i dati del PdfFileItem al ViewHolder.
        holder.bind(pdfFile, listener)
        }
    }

    /**
     * Callback per calcolare le differenze tra due liste di PdfFileItem.
     * Usato da ListAdapter per aggiornare la RecyclerView in modo efficiente.
     */
    class PdfFileDiffCallback : DiffUtil.ItemCallback<PdfFileItem>() {
        override fun areItemsTheSame(oldItem: PdfFileItem, newItem: PdfFileItem): Boolean {
            // Controlla se gli elementi rappresentano lo stesso "oggetto" (stesso URI)
            // Questo è importante per le riorganizzazioni della lista.
            return oldItem.uriString == newItem.uriString
        }

        override fun areContentsTheSame(oldItem: PdfFileItem, newItem: PdfFileItem): Boolean {
            // Controlla se i contenuti degli elementi sono gli stessi.
            // Poiché PdfFileItem è una data class, il confronto '==' verifica tutte le proprietà.
            // Questo è fondamentale per rilevare cambiamenti come 'isSelected'.
            return oldItem == newItem
        }
    }


