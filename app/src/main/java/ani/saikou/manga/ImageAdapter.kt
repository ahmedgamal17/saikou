package ani.saikou.manga

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ani.saikou.currActivity
import ani.saikou.databinding.ItemImageBinding
import com.squareup.picasso.Callback
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File

class ImageAdapter(
private val arr: ArrayList<String>,
private val referer:String?=null
): RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val binding = holder.binding
        val a = currActivity()
        if (a!=null && !a.isDestroyed) {
            val client = OkHttpClient.Builder()
                .cache(Cache(
                    File(a.cacheDir, "http_cache"),
                    50L * 1024L * 1024L
                ))
                .addInterceptor { chain ->
                    val newRequest = chain.request().newBuilder()
                        .addHeader("referer", referer?:"")
                        .build()
                    chain.proceed(newRequest)
                }
                .build()
            Picasso.Builder(a)
                .downloader(OkHttp3Downloader(client))
                .build()
                .load(arr[position])
                .into(binding.imgProgImage, object : Callback {
                    override fun onSuccess() { binding.imgProgProgress.visibility = View.GONE }
                    override fun onError(e: Exception) {}
                })
        }
    }

    override fun getItemCount(): Int = arr.size

    inner class ImageViewHolder(val binding: ItemImageBinding) : RecyclerView.ViewHolder(binding.root)
}