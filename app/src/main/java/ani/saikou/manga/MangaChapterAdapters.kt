package ani.saikou.manga

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ani.saikou.databinding.ItemChapterListBinding
import ani.saikou.databinding.ItemEpisodeCompactBinding
import ani.saikou.media.Media

fun mangaChapterAdapter(media: Media, fragment: MangaSourceFragment, style:Int, reversed:Boolean=false, start:Int=0, e:Int?=null): RecyclerView.Adapter<*> {
    val end = e?:(media.manga!!.chapters!!.size-1)
    var arr = media.manga!!.chapters!!.values.toList().slice(start..end)
    arr = if (reversed) arr.reversed() else arr
    return when (style){
        0 -> ChapterListAdapter(media, fragment,arr)
        1 ->  ChapterCompactAdapter(media, fragment,arr)
        else -> ChapterListAdapter(media, fragment,arr)
    }
}

class ChapterCompactAdapter(
    private val media: Media,
    private val fragment: MangaSourceFragment,
    private val arr: List<MangaChapter>,
): RecyclerView.Adapter<ChapterCompactAdapter.ChapterCompactViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterCompactViewHolder {
        val binding = ItemEpisodeCompactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChapterCompactViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ChapterCompactViewHolder, position: Int) {
        val binding = holder.binding

        val ep = arr[position]
        binding.itemEpisodeNumber.text = ep.number
        if (media.userProgress!=null) {
            if (ep.number.toFloatOrNull()?:9999f<=media.userProgress!!.toFloat()) binding.root.alpha = 0.66f
        }
    }

    override fun getItemCount(): Int = arr.size

    inner class ChapterCompactViewHolder(val binding: ItemEpisodeCompactBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                fragment.onMangaChapterClick(media,arr[bindingAdapterPosition].number)
            }
        }
    }
}

class ChapterListAdapter(
    private val media: Media,
    private val fragment: MangaSourceFragment,
    private val arr: List<MangaChapter>,
): RecyclerView.Adapter<ChapterListAdapter.ChapterCompactViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterCompactViewHolder {
        val binding = ItemChapterListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChapterCompactViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ChapterCompactViewHolder, position: Int) {
        val binding = holder.binding
        val ep = arr[position]
        binding.itemChapterNumber.text = ep.number
        binding.itemChapterTitle.text = ep.title
        if (media.userProgress!=null) {
            if (ep.number.toFloatOrNull()?:9999f<=media.userProgress!!.toFloat()) binding.root.alpha = 0.66f
        }
    }

    override fun getItemCount(): Int = arr.size

    inner class ChapterCompactViewHolder(val binding: ItemChapterListBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                fragment.onMangaChapterClick(media,arr[bindingAdapterPosition].number)
            }
        }
    }
}