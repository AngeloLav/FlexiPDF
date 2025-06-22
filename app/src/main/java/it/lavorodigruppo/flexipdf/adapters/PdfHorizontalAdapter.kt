/**
 * @file PdfHorizontalAdapter.kt
 *
 * @brief Adattatore RecyclerView per la visualizzazione orizzontale di file PDF.
 *
 * @overview
 * Questo adattatore è progettato per visualizzare una lista di `PdfFileItem` in una `RecyclerView` con orientamento orizzontale.
 * Gestisce l'aggiornamento dei dati degli elementi, l'impostazione delle icone (inclusa l'icona dei preferiti),
 * e fornisce feedback visivo tramite un'animazione di "scuotimento" per gli elementi selezionati.
 * Implementa le callback per il click singolo e il long click, riutilizzando il long click anche per il toggle dei preferiti.
 * Utilizza `ListAdapter` con `DiffUtil` per aggiornamenti efficienti della lista.
 */
package it.lavorodigruppo.flexipdf.adapters

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import it.lavorodigruppo.flexipdf.R
import it.lavorodigruppo.flexipdf.databinding.PdfFileHorizontalItemBinding
import it.lavorodigruppo.flexipdf.items.PdfFileItem

/**
 * Type alias per la callback invocata al click su un `PdfFileItem`.
 */
typealias OnPdfItemClick = (PdfFileItem) -> Unit

/**
 * Type alias per la callback invocata al long click su un `PdfFileItem`.
 * Restituisce un booleano che indica se l'evento è stato consumato.
 */
typealias OnPdfItemLongClick = (PdfFileItem) -> Boolean

/**
 * Adattatore per `RecyclerView` specializzato nella visualizzazione orizzontale di `PdfFileItem`.
 * Gestisce la creazione e il binding dei ViewHolder, e le interazioni dell'utente tramite callback.
 *
 * @param onPdfItemClick Callback invocata al click singolo su un `PdfFileItem`.
 * @param onPdfItemLongClick Callback invocata al long click su un `PdfFileItem`.
 */
class PdfHorizontalAdapter(
    private val onPdfItemClick: OnPdfItemClick,
    private val onPdfItemLongClick: OnPdfItemLongClick
) : ListAdapter<PdfFileItem, PdfHorizontalAdapter.PdfHorizontalViewHolder>(PdfHorizontalDiffCallback()) {

    /**
     * ViewHolder specifico per la visualizzazione di un singolo `PdfFileItem` nell'elenco orizzontale.
     * Gestisce il binding dei dati, l'aggiornamento dell'interfaccia utente (inclusa l'icona dei preferiti e lo stato di selezione)
     * e l'animazione di "scuotimento".
     *
     * @param binding L'oggetto ViewBinding per il layout `pdf_file_horizontal_item.xml`.
     */
    class PdfHorizontalViewHolder(private val binding: PdfFileHorizontalItemBinding) : RecyclerView.ViewHolder(binding.root) {

        private var shakeAnimator: ObjectAnimator? = null

        /**
         * Collega i dati di un `PdfFileItem` alla vista del ViewHolder.
         * Imposta il nome del file, l'icona PDF generica, l'icona preferiti in base allo stato,
         * gestisce l'animazione di "scuotimento" se l'elemento è selezionato, e imposta i listener di click e long click.
         * Il click sull'icona dei preferiti riutilizza la callback del long click.
         *
         * @param pdfFile L'oggetto `PdfFileItem` da visualizzare.
         * @param onPdfItemClick Callback per il click singolo.
         * @param onPdfItemLongClick Callback per il long click e il toggle dei preferiti.
         */
        fun bind(
            pdfFile: PdfFileItem,
            onPdfItemClick: OnPdfItemClick,
            onPdfItemLongClick: OnPdfItemLongClick
        ) {
            binding.horizontalTitleTextView.text = pdfFile.displayName
            binding.horizontalIconImageView.setImageResource(R.drawable.pdf_svgrepo_com)

            binding.horizontalFavoriteIcon.setImageResource(
                if (pdfFile.isFavorite) R.drawable.star_24dp_f19e39_fill0_wght400_grad0_opsz24__1_
                else R.drawable.star_24dp_999999_fill0_wght400_grad0_opsz24
            )

            binding.root.setOnClickListener {
                onPdfItemClick(pdfFile)
            }

            binding.root.setOnLongClickListener {
                onPdfItemLongClick(pdfFile)
            }

            binding.horizontalFavoriteIcon.setOnClickListener {
                onPdfItemLongClick(pdfFile)
            }
        }

        /**
         * Interrompe l'animazione di "scuotimento" in corso sulla vista radice dell'elemento
         * e resetta la posizione della vista alla sua origine.
         */
        fun stopShakeAnimation() {
            shakeAnimator?.cancel()
            binding.root.translationX = 0f
            shakeAnimator = null
        }
    }

    /**
     * Crea un nuovo ViewHolder per un `PdfFileItem`.
     * Questo metodo viene chiamato quando la `RecyclerView` ha bisogno di un nuovo ViewHolder
     * per visualizzare un elemento.
     * @param parent Il ViewGroup in cui verrà gonfiata la nuova vista.
     * @param viewType Il tipo di vista (sempre lo stesso per questo adattatore).
     * @return Un'istanza di `PdfHorizontalViewHolder`.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfHorizontalViewHolder {
        val binding = PdfFileHorizontalItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PdfHorizontalViewHolder(binding)
    }

    /**
     * Collega i dati di un `PdfFileItem` al ViewHolder nella posizione specificata.
     * Questo metodo aggiorna il contenuto della vista per riflettere i dati dell'elemento e imposta i listener.
     * @param holder Il ViewHolder da aggiornare.
     * @param position La posizione dell'elemento nella lista dell'adattatore.
     */
    override fun onBindViewHolder(holder: PdfHorizontalViewHolder, position: Int) {
        val pdfFile = getItem(position)
        holder.bind(pdfFile, onPdfItemClick, onPdfItemLongClick)
    }

    /**
     * Metodo chiamato quando un ViewHolder viene riciclato da `RecyclerView`.
     * Utilizzato per interrompere eventuali animazioni in corso sul ViewHolder prima che venga riutilizzato,
     * prevenendo comportamenti visivi indesiderati.
     * @param holder Il ViewHolder che sta per essere riciclato.
     */
    override fun onViewRecycled(holder: PdfHorizontalViewHolder) {
        super.onViewRecycled(holder)
        holder.stopShakeAnimation()
    }

    /**
     * Implementazione di `DiffUtil.ItemCallback` per `PdfFileItem`.
     * Utilizzata da `ListAdapter` per calcolare in modo efficiente le differenze tra due liste
     * di elementi, ottimizzando gli aggiornamenti della `RecyclerView`.
     */
    class PdfHorizontalDiffCallback : DiffUtil.ItemCallback<PdfFileItem>() {
        /**
         * Determina se due elementi rappresentano lo stesso "item" (entità logica).
         * Il confronto viene fatto basandosi sull'ID univoco di ciascun `PdfFileItem`.
         * @param oldItem L'elemento della vecchia lista.
         * @param newItem L'elemento della nuova lista.
         * @return `true` se gli ID sono uguali, `false` altrimenti.
         */
        override fun areItemsTheSame(oldItem: PdfFileItem, newItem: PdfFileItem): Boolean {
            return oldItem.id == newItem.id
        }

        /**
         * Determina se il contenuto di due elementi è lo stesso.
         * Questo metodo è chiamato solo se `areItemsTheSame` restituisce `true`.
         * Poiché `PdfFileItem` è una data class, l'operatore di uguaglianza (`==`) esegue un confronto strutturale
         * di tutte le proprietà, rendendo questa implementazione efficiente.
         * @param oldItem L'elemento della vecchia lista.
         * @param newItem L'elemento della nuova lista.
         * @return `true` se il contenuto è identico, `false` altrimenti.
         */
        @Suppress("SuspiciousEquals")
        override fun areContentsTheSame(oldItem: PdfFileItem, newItem: PdfFileItem): Boolean {
            return oldItem == newItem
        }
    }
}


