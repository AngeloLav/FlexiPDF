package it.lavorodigruppo.flexipdf.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment

import it.lavorodigruppo.flexipdf.R

class HomeFragment : Fragment() {

    private var originalBannerPaddingTop = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

    val view = inflater.inflate(R.layout.fragment_home, container, false)


    // --- WindowInsets management ---
        val bannerContentLayout = view?.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.bannerContentLayout)

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
        // --- End of WindowInsets management ---

    return view
    }

}