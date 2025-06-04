// it.lavorodigruppo.flexipdf.adapters/PdfHorizontalAdapter.kt
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

// Definiamo delle typealias per le callback specifiche di questo adapter
typealias OnPdfItemClick = (PdfFileItem) -> Unit
typealias OnPdfItemLongClick = (PdfFileItem) -> Boolean // Restituisce Boolean per consumare l'evento

class PdfHorizontalAdapter(
    private val onPdfItemClick: OnPdfItemClick,
    private val onPdfItemLongClick: OnPdfItemLongClick
) : ListAdapter<PdfFileItem, PdfHorizontalAdapter.PdfHorizontalViewHolder>(PdfHorizontalDiffCallback()) {

    class PdfHorizontalViewHolder(private val binding: PdfFileHorizontalItemBinding) : RecyclerView.ViewHolder(binding.root) {

        private var shakeAnimator: ObjectAnimator? = null

        fun bind(
            pdfFile: PdfFileItem,
            onPdfItemClick: OnPdfItemClick,
            onPdfItemLongClick: OnPdfItemLongClick
        ) {
            binding.horizontalTitleTextView.text = pdfFile.displayName
            binding.horizontalIconImageView.setImageResource(R.drawable.pdf_svgrepo_com)

            // Imposta l'icona della stella in base allo stato isFavorite
            binding.horizontalFavoriteIcon.setImageResource(
                if (pdfFile.isFavorite) R.drawable.star_24dp_f19e39_fill0_wght400_grad0_opsz24__1_
                else R.drawable.star_24dp_999999_fill0_wght400_grad0_opsz24
            )

            // La CardView cambia colore e l'animazione di shake solo se l'item è selezionato (es. per long press)
            // In HomeFragment, la selezione non è per la modalità CAB, ma per indicare un long click
            if (pdfFile.isSelected) {
                // Puoi aggiungere un colore di sfondo qui se vuoi evidenziare l'elemento selezionato
                // binding.cardView.setCardBackgroundColor(itemView.context.getColor(R.color.selected_item_background))
                startShakeAnimation(binding.root)
            } else {
                // binding.cardView.setCardBackgroundColor(itemView.context.getColor(android.R.color.transparent))
                stopShakeAnimation()
            }

            // --- Impostazione dei Listener ---
            binding.root.setOnClickListener {
                onPdfItemClick(pdfFile)
            }

            binding.root.setOnLongClickListener {
                onPdfItemLongClick(pdfFile)
            }

            // Il pulsante preferiti qui toggla direttamente lo stato
            binding.horizontalFavoriteIcon.setOnClickListener {
                onPdfItemLongClick(pdfFile) // Riutilizziamo la long click callback per togglare il preferito
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfHorizontalViewHolder {
        val binding = PdfFileHorizontalItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PdfHorizontalViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PdfHorizontalViewHolder, position: Int) {
        val pdfFile = getItem(position)
        holder.bind(pdfFile, onPdfItemClick, onPdfItemLongClick)
    }

    override fun onViewRecycled(holder: PdfHorizontalViewHolder) {
        super.onViewRecycled(holder)
        holder.stopShakeAnimation()
    }

    /**
     * Callback per calcolare le differenze tra due liste di PdfFileItem.
     * Usato da ListAdapter per aggiornare la RecyclerView in modo efficiente.
     */
    class PdfHorizontalDiffCallback : DiffUtil.ItemCallback<PdfFileItem>() {
        override fun areItemsTheSame(oldItem: PdfFileItem, newItem: PdfFileItem): Boolean {
            // Ora confrontiamo per ID, che è univoco per ogni PdfFileItem
            return oldItem.id == newItem.id
        }

        @Suppress("SuspiciousEquals") // Sopprimi il warning, poiché PdfFileItem è una data class
        override fun areContentsTheSame(oldItem: PdfFileItem, newItem: PdfFileItem): Boolean {
            // Poiché PdfFileItem è una data class, il confronto '==' verifica tutte le proprietà.
            return oldItem == newItem
        }
    }
}


