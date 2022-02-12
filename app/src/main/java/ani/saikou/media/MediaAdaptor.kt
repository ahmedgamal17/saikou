package ani.saikou.media

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import ani.saikou.R
import ani.saikou.databinding.ItemMediaCompactBinding
import ani.saikou.loadImage
import ani.saikou.setAnimation
import java.io.Serializable


class MediaAdaptor(
    private val mediaList: ArrayList<Media>?, private val activity: FragmentActivity,private val matchParent:Boolean=false,
    ) : RecyclerView.Adapter<MediaAdaptor.MediaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaCompactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaViewHolder(binding)
    }
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val b = holder.binding
        setAnimation(activity,holder.binding.root)
        val media = mediaList?.get(position)
        if(media!=null) {
            loadImage(media.cover, b.itemCompactImage)
            b.itemCompactOngoing.visibility =
                if (media.status == "RELEASING") View.VISIBLE else View.GONE
            b.itemCompactTitle.text = media.userPreferredName
            b.itemCompactScore.text = ((if (media.userScore == 0) (media.meanScore
                ?: 0) else media.userScore) / 10.0).toString()
            b.itemCompactScoreBG.background = ContextCompat.getDrawable(
                b.root.context,
                (if (media.userScore != 0) R.drawable.item_user_score else R.drawable.item_score)
            )
            b.itemCompactUserProgress.text = (media.userProgress ?: "~").toString()
            if (media.relation != null) {
                b.itemCompactRelation.text = "${media.relation}  "
                b.itemCompactType.visibility = View.VISIBLE
            }
            if (media.anime != null) {
                if (media.relation != null) b.itemCompactTypeImage.setImageDrawable(
                    AppCompatResources.getDrawable(activity, R.drawable.ic_round_movie_filter_24)
                )
                b.itemCompactTotal.text =
                    " | ${if (media.anime.nextAiringEpisode != null) (media.anime.nextAiringEpisode.toString() + " | " + (media.anime.totalEpisodes ?: "~").toString()) else (media.anime.totalEpisodes ?: "~").toString()}"
            } else if (media.manga != null) {
                if (media.relation != null) b.itemCompactTypeImage.setImageDrawable(
                    AppCompatResources.getDrawable(activity, R.drawable.ic_round_import_contacts_24)
                )
                b.itemCompactTotal.text = " | ${media.manga.totalChapters ?: "~"}"
            }
        }
    }

    override fun getItemCount() = mediaList!!.size

    inner class MediaViewHolder(val binding: ItemMediaCompactBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            if (matchParent) itemView.updateLayoutParams { width=-1 }

            itemView.setOnClickListener {
                val media = mediaList?.get(bindingAdapterPosition)
                if(media!=null){
                    ContextCompat.startActivity(
                        activity,
                        Intent(activity, MediaDetailsActivity::class.java).putExtra("media", media as Serializable),
                        ActivityOptionsCompat.makeSceneTransitionAnimation(activity, Pair.create(binding.itemCompactImage, ViewCompat.getTransitionName(binding.itemCompactImage)!!)).toBundle()
                    )
                }
            }
            itemView.setOnLongClickListener {
                val media = mediaList?.get(bindingAdapterPosition)
                if(media!=null){
                    MediaListDialogSmallFragment.newInstance(media).show(activity.supportFragmentManager, "list")
                }
                true
            }
        }
    }
}