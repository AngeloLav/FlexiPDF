/**
 * @file SharedFragment.kt
 *
 * @brief Fragment che rappresenta la schermata dei file condivisi.
 *
 * Questo Fragment è un segnaposto per la futura implementazione della funzionalità
 * di gestione dei file PDF condivisi. Attualmente, carica solo il layout di base.
 *
 */
package it.lavorodigruppo.flexipdf.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import it.lavorodigruppo.flexipdf.R


class SharedFragment : Fragment() {

    /**
     * Metodo chiamato alla creazione del Fragment.
     * Attualmente non recupera o elabora argomenti.
     *
     * @param savedInstanceState Se il Fragment viene ricreato da un precedente stato salvato.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // Logica per recuperare gli argomenti se presenti.
            // Attualmente, non ci sono argomenti definiti per SharedFragment.
        }
    }

    /**
     * Metodo chiamato per creare e restituire la View gerarchica associata al Fragment.
     * Gonfia semplicemente il layout definito in R.layout.fragment_shared.
     *
     * @param inflater LayoutInflater per gonfiare il layout.
     * @param container Il ViewGroup genitore a cui la UI del Fragment dovrebbe essere attaccata.
     * @param savedInstanceState Se non nullo, questo Fragment sta venendo ricreato da un precedente stato salvato.
     * @return La View radice del layout del Fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Gonfia il layout del Fragment dal file XML 'fragment_shared.xml'.
        return inflater.inflate(R.layout.fragment_shared, container, false)
    }

}