package ani.saikou.manga

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ani.saikou.manga.source.MangaSources
import ani.saikou.media.Media

class MangaChapterViewModel : ViewModel() {
    private val mangaChapMedia: MutableLiveData<Media> = MutableLiveData<Media>(null)
    fun getMangaChapMedia(): LiveData<Media> = mangaChapMedia
    fun loadChapMedia(media: Media){
        val chap = media.manga!!.chapters!![media.manga.selectedChapter]!!
        media.manga.chapters!![media.manga.selectedChapter!!] = MangaSources[media.selected!!.source]?.getChapter(chap)?:chap
        mangaChapMedia.postValue(media)
    }
}