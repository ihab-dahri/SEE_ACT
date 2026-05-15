import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class DetectionAdapter(private val detections: MutableList<String>) :
    RecyclerView.Adapter<DetectionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Utilise l'ID par défaut d'Android pour un texte simple
        val textView: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = detections[position]
    }

    override fun getItemCount() = detections.size

    // La fonction qui ajoute le panneau en haut de la liste
    fun addDetection(name: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        val fullText = "$name ($timestamp)"

        // 1. IL FAUT AJOUTER À LA LISTE
        detections.add(0, fullText)

        // 2. IL FAUT PRÉVENIR ANDROID (Sinon il ne redessine rien)
        notifyItemInserted(0)
    }
}