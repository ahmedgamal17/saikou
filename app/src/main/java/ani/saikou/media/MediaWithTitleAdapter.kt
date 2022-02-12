package ani.saikou.media

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ani.saikou.databinding.ItemMediasWithTitleBinding

class MediasWithTitleAdapter(private val medias: MutableMap<String,ArrayList<Media>>, private val activity: FragmentActivity) : RecyclerView.Adapter<MediasWithTitleAdapter.MediaGridViewHolder>() {
    private var keys = medias.keys.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaGridViewHolder {
        val binding = ItemMediasWithTitleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaGridViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaGridViewHolder, position: Int) {
        val binding = holder.binding
        binding.itemTitle.text = keys[position]
        binding.itemRecyclerView.adapter = MediaAdaptor(medias[keys[position]]!!,activity)
        binding.itemRecyclerView.layoutManager = LinearLayoutManager(activity,RecyclerView.HORIZONTAL,false)
    }

    override fun getItemCount(): Int = medias.size
    inner class MediaGridViewHolder(val binding: ItemMediasWithTitleBinding) : RecyclerView.ViewHolder(binding.root)
}