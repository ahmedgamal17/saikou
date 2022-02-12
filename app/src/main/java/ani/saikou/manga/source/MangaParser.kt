package ani.saikou.manga.source

import androidx.lifecycle.MutableLiveData
import ani.saikou.manga.MangaChapter
import ani.saikou.media.Media
import ani.saikou.media.Source

abstract class MangaParser {
    abstract val name : String
    var referer : String? = null
    val live: MutableLiveData<String> = MutableLiveData()
    abstract fun getLinkChapters(link:String):MutableMap<String, MangaChapter>
    abstract fun getChapter(chapter: MangaChapter): MangaChapter
    abstract fun getChapters(media: Media):MutableMap<String, MangaChapter>
    abstract fun search(string: String):ArrayList<Source>
    abstract fun saveSource(source: Source,id:Int,selected:Boolean=true)
}