/**
 * @file HomeFragment.kt
 *
 * @brief Fragment che rappresenta la schermata principale dell'applicazione (Home).
 *
 * Questo Fragment è responsabile di visualizzare il contenuto della schermata Home.
 * Implementa una gestione specifica degli WindowInsets per assicurare che il layout
 * del banner si adatti correttamente alle barre di sistema, evitando che il contenuto venga coperto.
 *
 */

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

    /*
     * Memorizza il padding superiore originale del layout del banner.
     * Utilizzato per aggiungere il padding degli insets senza sovrascrivere il padding esistente.
     */
    private var originalBannerPaddingTop = 0

    /**
     * Metodo chiamato alla creazione del Fragment.
     * In questo caso, viene utilizzato solo per recuperare gli argomenti se presenti,
     * ma attualmente non ne vengono passati.
     *
     * @param savedInstanceState Se il Fragment viene ricreato dopo essere stato terminato,
     * questo Bundle contiene i dati forniti più recentemente in onSaveInstanceState.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // Logica per recuperare gli argomenti se presenti.
            // Attualmente, non ci sono argomenti definiti per HomeFragment.
        }
    }

    /**
     * Metodo chiamato per creare e restituire la View gerarchica associata al Fragment.
     * Questo è il punto in cui il layout XML del Fragment viene gonfiato.
     *
     * @param inflater L'LayoutInflater che può essere utilizzato per gonfiare qualsiasi View nel Fragment.
     * @param container Se non nullo, questo è il [ViewGroup] genitore a cui la UI del Fragment dovrebbe essere attaccata.
     * @param savedInstanceState Se non nullo, questo Fragment sta venendo ricreato da un precedente stato salvato.
     * @return La View radice del layout del Fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Gonfia il layout del Fragment dal file XML 'fragment_home.xml'.
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // --- Gestione degli WindowInsets ---
        // Trova il layout del banner all'interno della View del Fragment.
        // 'bannerContentLayout' è un ConstraintLayout con ID 'bannerContentLayout' nel layout 'fragment_home.xml'.
        val bannerContentLayout = view?.findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.bannerContentLayout)

        // Se il layout del banner è stato trovato, memorizza il suo padding superiore originale.
        if (bannerContentLayout != null) {
            originalBannerPaddingTop = bannerContentLayout.paddingTop
        }

        // Imposta un listener per gli WindowInsets sulla View radice del Fragment.
        // Questo listener viene chiamato ogni volta che le barre di sistema cambiano posizione o dimensione.
        view?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { _, insets ->

                val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

                // Applica il padding superiore al layout del banner.
                // Il nuovo padding superiore è la somma del padding originale e l'altezza della status bar.
                // Questo spinge il contenuto del banner sotto la status bar, evitando sovrapposizioni.
                bannerContentLayout?.setPadding(
                    bannerContentLayout.paddingLeft,
                    originalBannerPaddingTop + systemBarsInsets.top,
                    bannerContentLayout.paddingRight,
                    bannerContentLayout.paddingBottom
                )
                // Restituisce gli insets per permettere che vengano propagati ad altre viste se necessario.
                insets
            }
        }
        // --- Fine della gestione degli WindowInsets ---

        return view
    }

}