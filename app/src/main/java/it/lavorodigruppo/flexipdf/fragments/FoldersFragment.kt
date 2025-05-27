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
import it.lavorodigruppo.flexipdf.R

interface OnPdfPickerListener {
    fun launchPdfPicker()
}

class FoldersFragment : Fragment() {

    private var _binding: FragmentFoldersBinding? = null
    private val binding get() = _binding!!
    private var listener: OnPdfPickerListener? = null
    private var originalBannerPaddingTop = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFoldersBinding.inflate(inflater, container, false)

        // --- WindowInsets management ---
        val bannerContentLayout = view?.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(
            R.id.bannerContentLayout)

        if (bannerContentLayout != null) {
            originalBannerPaddingTop = bannerContentLayout.paddingTop
        }

        view?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { _, insets ->
                val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

                bannerContentLayout?.setPadding(
                    bannerContentLayout.paddingLeft,
                    originalBannerPaddingTop + systemBarsInsets.top,
                    bannerContentLayout.paddingRight,
                    bannerContentLayout.paddingBottom
                )
                insets
            }
        }
        // --- End WindowInsets manager ---

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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

    private fun showPopupMenu() {

        val popupBinding = CustomPopupMenuBinding.inflate(LayoutInflater.from(requireContext()))
        val popupView = popupBinding.root

        //Measure the popup view
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