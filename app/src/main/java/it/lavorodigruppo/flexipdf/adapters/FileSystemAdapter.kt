// it.lavorodigruppo.flexipdf.adapters/FileSystemAdapter.kt
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
import java.text.SimpleDateFormat
import java.util.Locale

// Definizione delle callback per gli eventi di click e selezione
typealias OnItemClick = (FileSystemItem) -> Unit
typealias OnItemLongClick = (FileSystemItem) -> Boolean
typealias OnSelectionToggle = (FileSystemItem) -> Unit
typealias OnFavoriteToggle = (PdfFileItem) -> Unit

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

    @SuppressLint("NotifyDataSetChanged")
    fun setSelectionMode(active: Boolean) {
        if (isSelectionMode != active) {
            isSelectionMode = active
            notifyDataSetChanged()
            Log.d("FileSystemAdapter", "Modalità selezione cambiata a: $active. Chiamato notifyDataSetChanged().")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is PdfFileItem -> VIEW_TYPE_PDF
            is FolderItem -> VIEW_TYPE_FOLDER
            else -> throw IllegalArgumentException("Tipo di elemento sconosciuto in FileSystemAdapter")
        }
    }

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

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder.itemViewType) {
            VIEW_TYPE_PDF -> {
                // Passa tutti i listener necessari e lo stato della modalità selezione al ViewHolder
                (holder as PdfFileViewHolder).bind(
                    item as PdfFileItem,
                    onItemClick,
                    onItemLongClick,
                    onSelectionToggle,
                    onFavoriteToggle,
                    onItemDoubleClick, // Passa il listener di doppio click
                    isSelectionMode
                )
            }
            VIEW_TYPE_FOLDER -> {
                // Passa tutti i listener necessari e lo stato della modalità selezione al ViewHolder
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

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        when (holder) {
            is PdfFileViewHolder -> holder.stopShakeAnimation()
            is FolderViewHolder -> holder.stopShakeAnimation()
        }
    }

    // --- ViewHolder per PDF ---
    inner class PdfFileViewHolder(private val binding: PdfFileItemBinding) : RecyclerView.ViewHolder(binding.root) {
        private var shakeAnimator: ObjectAnimator? = null
        private var lastClickTime: Long = 0 // Spostato qui per essere specifico per ogni elemento PDF
        private val DOUBLE_CLICK_TIME_DELTA: Long = 300 // Millisecondi per il doppio clic

        fun bind(
            pdfFile: PdfFileItem,
            onItemClick: OnItemClick,
            onItemLongClick: OnItemLongClick,
            onSelectionToggle: OnSelectionToggle,
            onFavoriteToggle: OnFavoriteToggle,
            onItemDoubleClick: ((PdfFileItem) -> Unit)? = null, // Riceve il listener di doppio click
            isSelectionMode: Boolean
        ) {
            Log.d("PdfFileViewHolder", "Bind per ${pdfFile.displayName}, isSelectionMode: $isSelectionMode, isSelected: ${pdfFile.isSelected}")
            binding.titleTextView.text = pdfFile.displayName
            binding.iconImageView.setImageResource(R.drawable.pdf_svgrepo_com) // Icona PDF generica

            // Imposta l'icona del preferito in base allo stato
            binding.favoriteIcon.setImageResource(
                if (pdfFile.isFavorite) R.drawable.star_24dp_f19e39_fill0_wght400_grad0_opsz24__1_
                else R.drawable.star_24dp_999999_fill0_wght400_grad0_opsz24
            )

            // Gestione della visibilità dell'overlay di selezione e animazione
            if (isSelectionMode) {
                if (pdfFile.isSelected) {
                    startShakeAnimation(binding.root)
                } else {
                    stopShakeAnimation()
                }
            } else {
                stopShakeAnimation()
            }

            // Centralizza la gestione dei click per il singolo elemento PDF qui
            binding.root.setOnClickListener {
                if (isSelectionMode) {
                    onSelectionToggle(pdfFile) // In modalità selezione, un clic singolo toggla la selezione
                } else {
                    // Logica per il rilevamento del doppio clic
                    val clickTime = System.currentTimeMillis()
                    if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA && onItemDoubleClick != null) {
                        onItemDoubleClick.invoke(pdfFile) // Chiamata al listener del doppio clic
                        lastClickTime = 0 // Resetta per evitare tripli clic accidentali
                    } else {
                        onItemClick.invoke(pdfFile) // Chiamata al listener del clic singolo
                    }
                    lastClickTime = clickTime
                }
            }

            binding.root.setOnLongClickListener {
                onItemLongClick(pdfFile) // Long click sempre attivo
            }

            // Listener per l'icona "Preferito"
            binding.favoriteIcon.setOnClickListener {
                onFavoriteToggle(pdfFile) // Chiama la callback specifica per togglare i preferiti
            }
        }

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

        fun stopShakeAnimation() {
            shakeAnimator?.cancel()
            binding.root.translationX = 0f
            shakeAnimator = null
        }
    }

    // --- ViewHolder per Cartelle ---
    inner class FolderViewHolder(private val binding: FolderItemBinding) : RecyclerView.ViewHolder(binding.root) {
        private var shakeAnimator: ObjectAnimator? = null

        fun bind(
            folder: FolderItem,
            onItemClick: OnItemClick,
            onItemLongClick: OnItemLongClick,
            onSelectionToggle: OnSelectionToggle,
            isSelectionMode: Boolean
        ) {
            Log.d("FolderViewHolder", "Bind per ${folder.displayName}, isSelectionMode: $isSelectionMode, isSelected: ${folder.isSelected}")
            binding.folderNameTextView.text = folder.displayName

            // *** MODIFICA FONDAMENTALE QUI: Imposta l'icona in base a isCloudFolder ***
            if (folder.isCloudFolder) {
                binding.folderIconImageView.setImageResource(R.drawable.folder_svgrepo_cloud_com) // Icona cloud (blu)
                binding.folderIconImageView.contentDescription = itemView.context.getString(R.string.folders_icon_description, "Cloud Folder")
            } else {
                binding.folderIconImageView.setImageResource(R.drawable.folder_svgrepo_com) // Icona locale (gialla)
                binding.folderIconImageView.contentDescription = itemView.context.getString(R.string.folders_icon_description, "Local Folder")
            }
            // ************************************************************************

            // Gestione della visibilità dell'overlay di selezione e animazione
            if (isSelectionMode) {
                if (folder.isSelected) {
                    startShakeAnimation(binding.root)
                } else {
                    stopShakeAnimation()
                }
            } else {
                stopShakeAnimation()
            }

            // Centralizza la gestione dei click per il singolo elemento Cartella qui
            binding.root.setOnClickListener {
                if (isSelectionMode) {
                    onSelectionToggle(folder) // In modalità selezione, un clic singolo toggla la selezione
                } else {
                    onItemClick(folder) // In modalità normale, un clic singolo apre la cartella
                }
            }

            binding.root.setOnLongClickListener {
                onItemLongClick(folder) // Long click sempre attivo
            }
        }

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

        fun stopShakeAnimation() {
            shakeAnimator?.cancel()
            binding.root.translationX = 0f
            shakeAnimator = null
        }
    }

    // --- DiffCallback per FileSystemItem ---
    class FileSystemDiffCallback : DiffUtil.ItemCallback<FileSystemItem>() {
        override fun areItemsTheSame(oldItem: FileSystemItem, newItem: FileSystemItem): Boolean {
            return oldItem.id == newItem.id
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: FileSystemItem, newItem: FileSystemItem): Boolean {
            // Per le data class, il confronto di uguaglianza controlla tutte le proprietà
            return oldItem == newItem
        }
    }
}