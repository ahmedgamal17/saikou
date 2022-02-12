package ani.saikou.anime

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ani.saikou.databinding.ItemEpisodesRecyclerviewBinding
import ani.saikou.media.Media

class EpisodesGridAdapter(
private val style : Int,
private val media: Media,
private val fragment: AnimeWatchFragment,
private val a: List<Episode>
) : RecyclerView.Adapter<EpisodesGridAdapter.MediaGridViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaGridViewHolder {
        val binding = ItemEpisodesRecyclerviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaGridViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaGridViewHolder, position: Int) {
        val binding = holder.binding

        binding.root.adapter = when (style){
            0 -> EpisodeListAdapter(media, fragment,a)
            1 -> EpisodeGridAdapter(media, fragment,a)
            2 -> EpisodeCompactAdapter(media, fragment,a)
            else -> EpisodeListAdapter(media, fragment,a)
        }
        binding.root.layoutManager = GridLayoutManager(fragment.requireContext(),a.size)
    }

    override fun getItemCount(): Int = 1
    inner class MediaGridViewHolder(val binding: ItemEpisodesRecyclerviewBinding) : RecyclerView.ViewHolder(binding.root)
}