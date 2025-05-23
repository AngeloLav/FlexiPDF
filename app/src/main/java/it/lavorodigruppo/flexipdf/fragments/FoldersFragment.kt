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
import android.view.animation.OvershootInterpolator

class FoldersFragment : Fragment() {

    private var _binding: FragmentFoldersBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFoldersBinding.inflate(inflater, container, false)
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
            Toast.makeText(context, "Import PDF clicked!", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }

        popupBinding.optionCreateFolder.setOnClickListener {
            Toast.makeText(context, "Create new folder clicked!", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }

    }

    private fun rotateFabForward() {
        ObjectAnimator.ofFloat(binding.floatingActionButton, "rotation", 0f, 90f).apply {
            //in milliseconds
            duration = 500
            interpolator = OvershootInterpolator()
            start()
        }
    }

}