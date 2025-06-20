/**
 * @file FileSystemAdapter.kt
 *
 * @brief Adattatore RecyclerView per la visualizzazione di elementi del file system (PDF e Cartelle).
 *
 * @overview
 * Questo adattatore è responsabile di prendere una lista di `FileSystemItem` (che possono essere `PdfFileItem` o `FolderItem`)
 * e visualizzarli in una `RecyclerView`. Supporta la visualizzazione di due tipi diversi di elementi, gestisce le selezioni
 * multiple con animazioni, e implementa callback per vari eventi di interazione dell'utente come click, long click,
 * toggle di selezione e preferiti, e doppio click sui PDF. Utilizza `ListAdapter` con `DiffUtil` per aggiornamenti efficienti.
 */
package it.lavorodigruppo.flexipdf.adapters

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import it.lavorodigruppo.flexipdf.R
import it.lavorodigruppo.flexipdf.databinding.FolderItemBinding
import it.lavorodigruppo.flexipdf.databinding.PdfFileItemBinding
import it.lavorodigruppo.flexipdf.items.FileSystemItem
import it.lavorodigruppo.flexipdf.items.FolderItem
import it.lavorodigruppo.flexipdf.items.PdfFileItem

/**
 * Type alias per la callback di un click su un elemento generico del file system.
 */
typealias OnItemClick = (FileSystemItem) -> Unit

/**
 * Type alias per la callback di un long click su un elemento generico del file system.
 * Restituisce un booleano che indica se l'evento è stato consumato.
 */
typealias OnItemLongClick = (FileSystemItem) -> Boolean

/**
 * Type alias per la callback di toggle della selezione di un elemento.
 */
typealias OnSelectionToggle = (FileSystemItem) -> Unit

/**
 * Type alias per la callback di toggle dello stato preferito di un file PDF.
 */
typealias OnFavoriteToggle = (PdfFileItem) -> Unit

/**
 * Adattatore per `RecyclerView` che visualizza una lista mista di `PdfFileItem` e `FolderItem`.
 * Gestisce la creazione e il binding dei diversi ViewHolder, la modalità di selezione,
 * e le interazioni dell'utente tramite callback.
 *
 * @param onItemClick Callback invocata al click singolo su un elemento.
 * @param onItemLongClick Callback invocata al long click su un elemento.
 * @param onSelectionToggle Callback invocata per attivare/disattivare la selezione di un elemento.
 * @param onFavoriteToggle Callback invocata per attivare/disattivare lo stato "preferito" di un PDF.
 * @param onItemDoubleClick Callback opzionale invocata al doppio click su un PDF.
 */
class FileSystemAdapter(
    private val onItemClick: OnItemClick,
    private val onItemLongClick: OnItemLongClick,
    private val onSelectionToggle: OnSelectionToggle,
    private val onFavoriteToggle: OnFavoriteToggle,
    private val onItemDoubleClick: ((PdfFileItem) -> Unit)? = null
) : ListAdapter<FileSystemItem, RecyclerView.ViewHolder>(FileSystemDiffCallback()) {

    private val VIEW_TYPE_PDF = 1
    private val VIEW_TYPE_FOLDER = 2

    private var isSelectionMode: Boolean = false

    /**
     * Imposta o disattiva la modalità di selezione dell'adattatore.
     * Quando la modalità di selezione cambia, l'intera lista viene notificata per aggiornare
     * l'interfaccia utente degli elementi (es. visualizzare l'overlay di selezione e l'animazione).
     * @param active `true` per attivare la modalità selezione, `false` per disattivarla.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun setSelectionMode(active: Boolean) {
        if (isSelectionMode != active) {
            isSelectionMode = active
            notifyDataSetChanged()
            Log.d("FileSystemAdapter", "Modalità selezione cambiata a: $active. Chiamato notifyDataSetChanged().")
        }
    }

    /**
     * Restituisce il tipo di vista per l'elemento nella posizione specificata.
     * Questo metodo è utilizzato da `RecyclerView` per determinare quale layout (`PdfFileItemBinding` o `FolderItemBinding`)
     * e quale ViewHolder creare.
     * @param position La posizione dell'elemento nella lista.
     * @return Un intero che rappresenta il tipo di vista (`VIEW_TYPE_PDF` o `VIEW_TYPE_FOLDER`).
     * @throws IllegalArgumentException Se il tipo di elemento in una data posizione non è riconosciuto.
     */
    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is PdfFileItem -> VIEW_TYPE_PDF
            is FolderItem -> VIEW_TYPE_FOLDER
            else -> throw IllegalArgumentException("Tipo di elemento sconosciuto in FileSystemAdapter")
        }
    }

    /**
     * Crea un nuovo ViewHolder in base al tipo di vista specificato.
     * Questo metodo viene chiamato quando `RecyclerView` ha bisogno di un nuovo ViewHolder per visualizzare un elemento.
     * @param parent Il ViewGroup in cui verrà gonfiata la nuova vista.
     * @param viewType Il tipo di vista dell'elemento (determinato da `getItemViewType`).
     * @return Un'istanza di `RecyclerView.ViewHolder` appropriata (`PdfFileViewHolder` o `FolderViewHolder`).
     * @throws IllegalArgumentException Se il tipo di vista non è riconosciuto.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_PDF -> {
                val binding = PdfFileItemBinding.inflate(inflater, parent, false)
                PdfFileViewHolder(binding)
            }
            VIEW_TYPE_FOLDER -> {
                val binding = FolderItemBinding.inflate(inflater, parent, false)
                FolderViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Tipo di vista sconosciuto: $viewType")
        }
    }

    /**
     * Collega i dati di un elemento del file system (PDF o Cartella) al ViewHolder nella posizione specificata.
     * Questo metodo aggiorna il contenuto della vista per riflettere i dati dell'elemento e imposta i listener di interazione.
     * @param holder Il ViewHolder da aggiornare.
     * @param position La posizione dell'elemento nella lista dell'adattatore.
     */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder.itemViewType) {
            VIEW_TYPE_PDF -> {
                (holder as PdfFileViewHolder).bind(
                    item as PdfFileItem,
                    onItemClick,
                    onItemLongClick,
                    onSelectionToggle,
                    onFavoriteToggle,
                    onItemDoubleClick,
                    isSelectionMode
                )
            }
            VIEW_TYPE_FOLDER -> {
                (holder as FolderViewHolder).bind(
                    item as FolderItem,
                    onItemClick,
                    onItemLongClick,
                    onSelectionToggle,
                    isSelectionMode
                )
            }
        }
    }

    /**
     * Metodo chiamato quando un ViewHolder viene riciclato da `RecyclerView`.
     * Utilizzato per interrompere eventuali animazioni in corso sul ViewHolder prima che venga riutilizzato,
     * prevenendo comportamenti visivi indesiderati.
     * @param holder Il ViewHolder che sta per essere riciclato.
     */
    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        when (holder) {
            is PdfFileViewHolder -> holder.stopShakeAnimation()
            is FolderViewHolder -> holder.stopShakeAnimation()
        }
    }

    /**
     * ViewHolder specifico per la visualizzazione di un `PdfFileItem`.
     * Mantiene i riferimenti agli elementi della vista (titolo, icona, icona preferiti)
     * e gestisce l'aggiornamento dei dati e le interazioni dell'utente.
     * Implementa anche un'animazione di "scuotimento" per gli elementi selezionati.
     * @param binding L'oggetto ViewBinding per il layout `pdf_file_item.xml`.
     */
    inner class PdfFileViewHolder(private val binding: PdfFileItemBinding) : RecyclerView.ViewHolder(binding.root) {
        private var shakeAnimator: ObjectAnimator? = null
        private var lastClickTime: Long = 0
        private val DOUBLE_CLICK_TIME_DELTA: Long = 300

        /**
         * Collega i dati di un `PdfFileItem` alla vista del ViewHolder.
         * Imposta il testo, le icone, lo stato di selezione (con animazione) e tutti i listener di click.
         * Include la logica per rilevare click singoli e doppi click.
         * @param pdfFile L'oggetto `PdfFileItem` da visualizzare.
         * @param onItemClick Callback per il click singolo.
         * @param onItemLongClick Callback per il long click.
         * @param onSelectionToggle Callback per il toggle di selezione.
         * @param onFavoriteToggle Callback per il toggle dei preferiti.
         * @param onItemDoubleClick Callback opzionale per il doppio click.
         * @param isSelectionMode Indica se l'adattatore è attualmente in modalità di selezione.
         */
        fun bind(
            pdfFile: PdfFileItem,
            onItemClick: OnItemClick,
            onItemLongClick: OnItemLongClick,
            onSelectionToggle: OnSelectionToggle,
            onFavoriteToggle: OnFavoriteToggle,
            onItemDoubleClick: ((PdfFileItem) -> Unit)? = null,
            isSelectionMode: Boolean
        ) {
            Log.d("PdfFileViewHolder", "Bind per ${pdfFile.displayName}, isSelectionMode: $isSelectionMode, isSelected: ${pdfFile.isSelected}")
            binding.titleTextView.text = pdfFile.displayName
            binding.iconImageView.setImageResource(R.drawable.pdf_svgrepo_com)

            binding.favoriteIcon.setImageResource(
                if (pdfFile.isFavorite) R.drawable.star_24dp_f19e39_fill0_wght400_grad0_opsz24__1_
                else R.drawable.star_24dp_999999_fill0_wght400_grad0_opsz24
            )

            if (isSelectionMode) {
                if (pdfFile.isSelected) {
                    startShakeAnimation(binding.root)
                } else {
                    stopShakeAnimation()
                }
            } else {
                stopShakeAnimation()
            }

            binding.root.setOnClickListener {
                if (isSelectionMode) {
                    onSelectionToggle(pdfFile)
                } else {
                    val clickTime = System.currentTimeMillis()
                    if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA && onItemDoubleClick != null) {
                        onItemDoubleClick.invoke(pdfFile)
                        lastClickTime = 0
                    } else {
                        onItemClick.invoke(pdfFile)
                    }
                    lastClickTime = clickTime
                }
            }

            binding.root.setOnLongClickListener {
                onItemLongClick(pdfFile)
            }

            binding.favoriteIcon.setOnClickListener {
                onFavoriteToggle(pdfFile)
            }
        }

        /**
         * Avvia un'animazione di "scuotimento" (shake) sulla vista radice dell'elemento.
         * Questa animazione viene utilizzata per indicare visivamente che un elemento è selezionato
         * quando la modalità di selezione è attiva.
         * @param view La vista su cui applicare l'animazione.
         */
        private fun startShakeAnimation(view: View) {
            shakeAnimator?.cancel()
            shakeAnimator = ObjectAnimator.ofFloat(view, "translationX", 0f, -5f, 5f, -3f, 3f, -2f, 2f, 0f).apply {
                duration = 500
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.RESTART
                interpolator = LinearInterpolator()
                start()
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
     * ViewHolder specifico per la visualizzazione di un `FolderItem`.
     * Mantiene i riferimenti agli elementi della vista (nome cartella, icona)
     * e gestisce l'aggiornamento dei dati e le interazioni dell'utente.
     * Implementa anche un'animazione di "scuotimento" per gli elementi selezionati.
     * @param binding L'oggetto ViewBinding per il layout `folder_item.xml`.
     */
    inner class FolderViewHolder(private val binding: FolderItemBinding) : RecyclerView.ViewHolder(binding.root) {
        private var shakeAnimator: ObjectAnimator? = null

        /**
         * Collega i dati di un `FolderItem` alla vista del ViewHolder.
         * Imposta il nome della cartella, l'icona (differenziando tra locale e cloud) e i listener di click.
         * Gestisce l'animazione di "scuotimento" se l'elemento è selezionato in modalità selezione.
         * @param folder L'oggetto `FolderItem` da visualizzare.
         * @param onItemClick Callback per il click singolo.
         * @param onItemLongClick Callback per il long click.
         * @param onSelectionToggle Callback per il toggle di selezione.
         * @param isSelectionMode Indica se l'adattatore è attualmente in modalità di selezione.
         */
        fun bind(
            folder: FolderItem,
            onItemClick: OnItemClick,
            onItemLongClick: OnItemLongClick,
            onSelectionToggle: OnSelectionToggle,
            isSelectionMode: Boolean
        ) {
            Log.d("FolderViewHolder", "Bind per ${folder.displayName}, isSelectionMode: $isSelectionMode, isSelected: ${folder.isSelected}")
            binding.folderNameTextView.text = folder.displayName

            if (folder.isCloudFolder) {
                binding.folderIconImageView.setImageResource(R.drawable.folder_svgrepo_cloud_com)
                binding.folderIconImageView.contentDescription = itemView.context.getString(R.string.folders_icon_description, "Cloud Folder")
            } else {
                binding.folderIconImageView.setImageResource(R.drawable.folder_svgrepo_com)
                binding.folderIconImageView.contentDescription = itemView.context.getString(R.string.folders_icon_description, "Local Folder")
            }

            if (isSelectionMode) {
                if (folder.isSelected) {
                    startShakeAnimation(binding.root)
                } else {
                    stopShakeAnimation()
                }
            } else {
                stopShakeAnimation()
            }

            binding.root.setOnClickListener {
                if (isSelectionMode) {
                    onSelectionToggle(folder)
                } else {
                    onItemClick(folder)
                }
            }

            binding.root.setOnLongClickListener {
                onItemLongClick(folder)
            }
        }

        /**
         * Avvia un'animazione di "scuotimento" (shake) sulla vista radice dell'elemento.
         * Questa animazione viene utilizzata per indicare visivamente che una cartella è selezionata
         * quando la modalità di selezione è attiva.
         * @param view La vista su cui applicare l'animazione.
         */
        private fun startShakeAnimation(view: View) {
            shakeAnimator?.cancel()
            shakeAnimator = ObjectAnimator.ofFloat(view, "translationX", 0f, -5f, 5f, -3f, 3f, -2f, 2f, 0f).apply {
                duration = 500
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.RESTART
                interpolator = LinearInterpolator()
                start()
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
     * Implementazione di `DiffUtil.ItemCallback` per `FileSystemItem`.
     * Utilizzata da `ListAdapter` per calcolare in modo efficiente le differenze tra due liste
     * di elementi, ottimizzando gli aggiornamenti della `RecyclerView`.
     */
    class FileSystemDiffCallback : DiffUtil.ItemCallback<FileSystemItem>() {
        /**
         * Determina se due elementi rappresentano lo stesso "item" (entità logica).
         * Il confronto viene fatto basandosi sull'ID univoco di ciascun `FileSystemItem`.
         * @param oldItem L'elemento della vecchia lista.
         * @param newItem L'elemento della nuova lista.
         * @return `true` se gli ID sono uguali, `false` altrimenti.
         */
        override fun areItemsTheSame(oldItem: FileSystemItem, newItem: FileSystemItem): Boolean {
            return oldItem.id == newItem.id
        }

        /**
         * Determina se il contenuto di due elementi è lo stesso.
         * Questo metodo è chiamato solo se `areItemsTheSame` restituisce `true`.
         * Per le data class in Kotlin, l'operatore di uguaglianza (`==`) esegue un confronto strutturale
         * di tutte le proprietà, rendendo questa implementazione efficiente.
         * @param oldItem L'elemento della vecchia lista.
         * @param newItem L'elemento della nuova lista.
         * @return `true` se il contenuto è identico, `false` altrimenti.
         */
        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: FileSystemItem, newItem: FileSystemItem): Boolean {
            return oldItem == newItem
        }
    }
}