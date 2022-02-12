package ani.saikou.media

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import ani.saikou.SearchActivity
import ani.saikou.anilist.Anilist
import ani.saikou.databinding.ItemGenreBinding
import ani.saikou.loadImage
import ani.saikou.px

class GenreAdapter(
    private val genres: ArrayList<String>,
    private val type: String,
    private val activity: Activity,
    private val big:Boolean = false
): RecyclerView.Adapter<GenreAdapter.GenreViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenreViewHolder {
        val binding = ItemGenreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        if (big) binding.genreCard.updateLayoutParams { height=72f.px }
        return GenreViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GenreViewHolder, position: Int) {
        val binding = holder.binding
        val genre = genres[position]
        binding.genreTitle.text = genre
        loadImage(Anilist.genres?.get(genre),binding.genreImage)
    }

    override fun getItemCount(): Int = genres.size
    inner class GenreViewHolder(val binding: ItemGenreBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                ContextCompat.startActivity(activity, Intent(activity, SearchActivity::class.java).putExtra("type",type).putExtra("genre",genres[bindingAdapterPosition]).putExtra("sortBy","Trending").also {
                    if(genres[bindingAdapterPosition].lowercase()=="hentai")
                        it.putExtra("hentai",true)
                },null)
            }
        }
    }
}