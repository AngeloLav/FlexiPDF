package it.lavorodigruppo.flexipdf.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import it.lavorodigruppo.flexipdf.R
import it.lavorodigruppo.flexipdf.items.SettingsItem
import androidx.core.net.toUri

class SettingsAdapter(private val settingsList: List<SettingsItem>) :
    RecyclerView.Adapter<SettingsAdapter.SettingsViewHolder>() {

    class SettingsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val iconImageView: ImageView = itemView.findViewById(R.id.iconImageView)
        private val settingTitleTextView: TextView = itemView.findViewById(R.id.settingTitleTextView)

        fun bind(item: SettingsItem) {
            settingTitleTextView.text = item.title
            iconImageView.setImageResource(item.iconResId)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.settings_item, parent, false)

        view.setOnClickListener(onClickListener)

        return SettingsViewHolder(view)
    }

    override fun getItemCount(): Int {
        return settingsList.size
    }

    override fun onBindViewHolder(holder: SettingsViewHolder, position: Int) {
        val currentItem = settingsList[position]
        holder.bind(currentItem)
        holder.itemView.tag = currentItem
    }


    // --- Listeners for Settings ---
    private val onClickListener = View.OnClickListener { v ->
        val clickedSettingsItem = v.tag as? SettingsItem

        clickedSettingsItem?.let { item ->
            when (item.title) {
                "Language" -> {
                    android.widget.Toast.makeText(v.context, "Language clicked", android.widget.Toast.LENGTH_SHORT).show()
                }
                "Theme" -> {
                    android.widget.Toast.makeText(v.context, "Theme clicked", android.widget.Toast.LENGTH_SHORT).show()
                }
                "About Us" -> {
                    android.widget.Toast.makeText(v.context, "About us clicked", android.widget.Toast.LENGTH_SHORT).show()
                }
                "Credits" -> {
                    android.widget.Toast.makeText(v.context, "Credits clicked", android.widget.Toast.LENGTH_SHORT).show()
                }
                "Help" -> {
                    android.widget.Toast.makeText(v.context, "Help clicked", android.widget.Toast.LENGTH_SHORT).show()
                }
                "Share App" -> {
                    val appUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
                    val intent = Intent(Intent.ACTION_VIEW, appUrl.toUri())
                    v.context.startActivity(intent)
                }
                else -> {
                    android.widget.Toast.makeText(v.context, "Unknown: ${item.title}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}