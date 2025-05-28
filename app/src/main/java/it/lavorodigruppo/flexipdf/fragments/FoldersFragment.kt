package it.lavorodigruppo.flexipdf.fragments

//Standard
import android.os.Bundle
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import it.lavorodigruppo.flexipdf.databinding.FragmentFoldersBinding

//Custom popupWindow
import android.widget.PopupWindow
import android.widget.Toast
import it.lavorodigruppo.flexipdf.databinding.CustomPopupMenuBinding

//Animations
import android.animation.ObjectAnimator
import android.content.Context
import android.view.animation.OvershootInterpolator
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import it.lavorodigruppo.flexipdf.adapters.OnPdfFileClickListener
import it.lavorodigruppo.flexipdf.adapters.PdfFileAdapter
import it.lavorodigruppo.flexipdf.items.PdfFileItem
import it.lavorodigruppo.flexipdf.viewmodels.PdfListViewModel
import it.lavorodigruppo.flexipdf.activities.PDFViewerActivity
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri


interface OnPdfPickerListener {
    fun launchPdfPicker()
}

class FoldersFragment : Fragment(), OnPdfFileClickListener {

    private var _binding: FragmentFoldersBinding? = null
    private val binding get() = _binding!!
    private var listener: OnPdfPickerListener? = null

    private lateinit var pdfListViewModel: PdfListViewModel
    private lateinit var pdfFileAdapter: PdfFileAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFoldersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pdfListViewModel = ViewModelProvider(requireActivity())[PdfListViewModel::class.java]

        pdfFileAdapter = PdfFileAdapter(this)

        binding.pdfRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = pdfFileAdapter
            setHasFixedSize(true)
        }

        pdfListViewModel.pdfFiles.observe(viewLifecycleOwner) { pdfFiles ->
            pdfFileAdapter.submitList(pdfFiles)
        }

        // --- WindowInsets management ---
        val bannerContentLayout = binding.bannerContentLayout

        ViewCompat.setOnApplyWindowInsetsListener(bannerContentLayout) { v, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                v.paddingLeft,
                systemBarsInsets.top,
                v.paddingRight,
                v.paddingBottom
            )
            insets
        }
        // --- End WindowInsets manager ---

        binding.floatingActionButton.setOnClickListener {
            rotateFabForward()
            showPopupMenu()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // --- Other functions ---

    override fun onPdfFileClick(pdfFile: PdfFileItem) {
        Log.d("PDF_CLICK", "Attempting to open PDF: ${pdfFile.displayName}, URI String: ${pdfFile.uriString}")
        val intent = Intent(requireContext(), PDFViewerActivity::class.java).apply {

            putExtra("pdf_uri", pdfFile.uriString.toUri())
            putExtra("pdf_display_name", pdfFile.displayName)
        }
        startActivity(intent)
        Toast.makeText(context, "Opening ${pdfFile.displayName}", Toast.LENGTH_SHORT).show()
    }

    private fun showPopupMenu() {

        val popupBinding = CustomPopupMenuBinding.inflate(LayoutInflater.from(requireContext()))
        val popupView = popupBinding.root

        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        val location = IntArray(2)
        binding.floatingActionButton.getLocationOnScreen(location)

        val fabX = location[0]
        val fabY = location[1]
        val popupWidth = popupView.measuredWidth
        val fabWidth = binding.floatingActionButton.width

        val xOffset = fabX - popupWidth + fabWidth/3
        val yOffset = fabY - popupView.measuredHeight

        val margin = (15 * resources.displayMetrics.density).toInt()
        val finalYOffset = yOffset - margin

        popupWindow.showAtLocation(
            binding.root,
            Gravity.NO_GRAVITY,
            xOffset,
            finalYOffset
        )

        // --- Listeners ---
        popupBinding.optionImportPdf.setOnClickListener {
            listener?.launchPdfPicker()
            popupWindow.dismiss()

        }

        popupBinding.optionCreateFolder.setOnClickListener {
            Toast.makeText(context, "Create new folder clicked!", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }

    }

    // --- PDF Picker ---
    // Methods necessary for a safe attachment of PDFs during the lifecycle of the fragment
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnPdfPickerListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnPdfPickerListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
    // --- End of PDF Picker ---

    private fun rotateFabForward() {
        ObjectAnimator.ofFloat(binding.floatingActionButton, "rotation", 0f, 90f).apply {
            //in milliseconds
            duration = 500
            interpolator = OvershootInterpolator()
            start()
        }
    }

}