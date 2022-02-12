package ani.saikou.anilist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ani.saikou.media.Media

class AnilistHomeViewModel : ViewModel() {
    private val listImages : MutableLiveData<ArrayList<String?>> = MutableLiveData<ArrayList<String?>>(arrayListOf())
    fun getListImages(): LiveData<ArrayList<String?>> = listImages
    fun setListImages() = listImages.postValue(Anilist.query.getBannerImages())

    private val animeContinue: MutableLiveData<ArrayList<Media>> = MutableLiveData<ArrayList<Media>>(null)
    fun getAnimeContinue(): LiveData<ArrayList<Media>> = animeContinue
    fun setAnimeContinue() = animeContinue.postValue(Anilist.query.continueMedia("ANIME"))

    private val mangaContinue: MutableLiveData<ArrayList<Media>> = MutableLiveData<ArrayList<Media>>(null)
    fun getMangaContinue(): LiveData<ArrayList<Media>> = mangaContinue
    fun setMangaContinue() = mangaContinue.postValue(Anilist.query.continueMedia("MANGA"))

    private val recommendation: MutableLiveData<ArrayList<Media>> = MutableLiveData<ArrayList<Media>>(null)
    fun getRecommendation(): LiveData<ArrayList<Media>> = recommendation
    fun setRecommendation() = recommendation.postValue(Anilist.query.recommendations())

    var loaded : Boolean = false
    val genres : MutableLiveData<Boolean> = MutableLiveData(false)
}

class AnilistAnimeViewModel : ViewModel() {
    private val type = "ANIME"
    private val trending: MutableLiveData<ArrayList<Media>> = MutableLiveData<ArrayList<Media>>(null)
    fun getTrending(): LiveData<ArrayList<Media>> = trending
    fun loadTrending() = trending.postValue(Anilist.query.search(type, perPage = 10, sort="TRENDING_DESC")?.results)

    private val updated: MutableLiveData<ArrayList<Media>> = MutableLiveData<ArrayList<Media>>(null)
    fun getUpdated(): LiveData<ArrayList<Media>> = updated
    fun loadUpdated() = updated.postValue(Anilist.query.recentlyUpdated())

    private val animePopular = MutableLiveData<SearchResults?>(null)
    fun getPopular(): LiveData<SearchResults?> = animePopular
    fun loadPopular(type:String,search_val:String?=null,genres:ArrayList<String>?=null,sort:String="SEARCH_MATCH") = animePopular.postValue(Anilist.query.search(type, search=search_val, sort=sort, genres = genres))
    fun loadNextPage(r:SearchResults) = Anilist.query.search(r.type,r.page+1,r.perPage,r.search,r.sort,r.genres)

    var loaded : Boolean = false
}

class AnilistMangaViewModel : ViewModel() {
    private val type = "MANGA"
    private val trending: MutableLiveData<ArrayList<Media>> = MutableLiveData<ArrayList<Media>>(null)
    fun getTrending(): LiveData<ArrayList<Media>> = trending
    fun loadTrending() = trending.postValue(Anilist.query.search(type, perPage = 10, sort="TRENDING_DESC")?.results)

    private val updated: MutableLiveData<ArrayList<Media>> = MutableLiveData<ArrayList<Media>>(null)
    fun getTrendingNovel(): LiveData<ArrayList<Media>> = updated
    fun loadTrendingNovel() = updated.postValue(Anilist.query.search(type, perPage = 10, sort="TRENDING_DESC",format="NOVEL")?.results)

    private val mangaPopular = MutableLiveData<SearchResults?>(null)
    fun getPopular(): LiveData<SearchResults?> = mangaPopular
    fun loadPopular(type:String,search_val:String?=null,genres:ArrayList<String>?=null,sort:String="SEARCH_MATCH") = mangaPopular.postValue(Anilist.query.search(type, search=search_val, sort=sort, genres = genres))
    fun loadNextPage(r:SearchResults) = Anilist.query.search(r.type,r.page+1,r.perPage,r.search,r.sort,r.genres)

    var loaded : Boolean = false
}

class AnilistSearch : ViewModel(){
    private val search: MutableLiveData<SearchResults?> = MutableLiveData<SearchResults?>(null)

    fun getSearch(): LiveData<SearchResults?> = search
    fun loadSearch(type:String,search_val:String?=null,genres:ArrayList<String>?=null,tags:ArrayList<String>?=null,sort:String="SEARCH_MATCH",adult:Boolean=false) = search.postValue(Anilist.query.search(type, search=search_val, sort=sort, genres = genres, tags = tags, isAdult = adult))

    fun loadNextPage(r:SearchResults) = Anilist.query.search(r.type,r.page+1,r.perPage,r.search,r.sort,r.genres)
}