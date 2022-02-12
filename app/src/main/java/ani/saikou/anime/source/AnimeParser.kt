package ani.saikou.anime.source

import androidx.lifecycle.MutableLiveData
import ani.saikou.anime.Episode
import ani.saikou.media.Media
import ani.saikou.media.Source

abstract class AnimeParser {
    abstract val name : String
    var referer : String?=null
    val live: MutableLiveData<String> = MutableLiveData()
    abstract fun getStream(episode: Episode,server:String):Episode
    abstract fun getStreams(episode: Episode):Episode
    abstract fun getEpisodes(media: Media):MutableMap<String,Episode>
    abstract fun search(string: String):ArrayList<Source>
    abstract fun getSlugEpisodes(slug: String): MutableMap<String, Episode>
    open fun saveSource(source: Source, id:Int, selected:Boolean=true){
        live.postValue("${if(selected) "Selected" else "Found"} : ${source.name}")
    }
}