package it.lavorodigruppo.flexipdf.adapters

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import it.lavorodigruppo.flexipdf.R
import it.lavorodigruppo.flexipdf.databinding.PdfFileHorizontalItemBinding
import it.lavorodigruppo.flexipdf.items.PdfFileItem


class PdfHorizontalAdapter(
    private val listener: OnPdfFileClickListener
) : ListAdapter<PdfFileItem, PdfHorizontalAdapter.PdfHorizontalViewHolder>(PdfFileDiffCallback()) {

    class PdfHorizontalViewHolder(private val binding: PdfFileHorizontalItemBinding) : RecyclerView.ViewHolder(binding.root) {

        private var shakeAnimator: ObjectAnimator? = null

        fun bind(pdfFile: PdfFileItem, listener: OnPdfFileClickListener) {
            binding.horizontalTitleTextView.text = pdfFile.displayName
            binding.horizontalIconImageView.setImageResource(R.drawable.pdf_svgrepo_com)

            // Imposta l'icona della stella in base allo stato isFavorite
            binding.horizontalFavoriteIcon.setImageResource(
                if (pdfFile.isFavorite) R.drawable.star_24dp_f19e39_fill0_wght400_grad0_opsz24__1_
                else R.drawable.star_24dp_999999_fill0_wght400_grad0_opsz24
            )

            // --- Logica di visualizzazione per HomeFragment ---
            // Nelle liste Home, di solito non c'è una "modalità di selezione" con cestino visibile.
            // Il cestino è sempre nascosto.


            // La CardView cambia colore e l'animazione di shake solo se l'item è selezionato (es. per long press)
            if (pdfFile.isSelected) {
                // Utilizza un colore per lo sfondo dell'item selezionato (definisci in colors.xml)

                // Potresti decidere di nascondere la stella e mostrare il cestino anche qui se il long click attiva una "selezione leggera"
                // binding.horizontalFavoriteIcon.visibility = View.GONE
                // binding.horizontalDeleteIcon.visibility = View.VISIBLE
                startShakeAnimation(binding.root)
            } else {

                // Assicurati che la stella sia visibile quando l'item non è selezionato
                // binding.horizontalFavoriteIcon.visibility = View.VISIBLE
                stopShakeAnimation()
            }


            // --- Impostazione dei Listener ---
            binding.root.setOnClickListener {
                listener.onPdfFileClick(pdfFile)
            }

            // Per la HomeFragment, un long click può essere utilizzato per togglare il preferito
            // o attivare una selezione se lo desideri.
            binding.root.setOnLongClickListener {
                listener.onPdfFileLongClick(pdfFile) // Chiamerà il long click handler in HomeFragment
                true
            }

            binding.horizontalFavoriteIcon.setOnClickListener {
                listener.onFavoriteIconClick(pdfFile)
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
        holder.bind(pdfFile, listener)
    }

    override fun onViewRecycled(holder: PdfHorizontalViewHolder) {
        super.onViewRecycled(holder)
        holder.stopShakeAnimation()
    }

}


