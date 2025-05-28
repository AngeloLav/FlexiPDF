package it.lavorodigruppo.flexipdf.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import it.lavorodigruppo.flexipdf.R
import it.lavorodigruppo.flexipdf.databinding.PdfFileItemBinding
import it.lavorodigruppo.flexipdf.items.PdfFileItem

interface OnPdfFileClickListener {
    fun onPdfFileClick(pdfFile: PdfFileItem)
}

class PdfFileAdapter(private val listener: OnPdfFileClickListener) : RecyclerView.Adapter<PdfFileAdapter.PdfFileViewHolder>() {

    private var pdfFiles: List<PdfFileItem> = emptyList()

    class PdfFileViewHolder(private val binding: PdfFileItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pdfFile: PdfFileItem) {

            binding.titleTextView.text = pdfFile.displayName
            binding.iconImageView.setImageResource(R.drawable.pdf_svgrepo_com)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfFileViewHolder {
        val binding = PdfFileItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PdfFileViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PdfFileViewHolder, position: Int) {
        val pdfFile = pdfFiles[position]
        holder.bind(pdfFile)

        holder.itemView.setOnClickListener {
            listener.onPdfFileClick(pdfFile)
        }
    }

    override fun getItemCount(): Int {
        return pdfFiles.size
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newList: List<PdfFileItem>) {
        pdfFiles = newList
        notifyDataSetChanged()
    }
}