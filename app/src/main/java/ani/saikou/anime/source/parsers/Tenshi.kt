package ani.saikou.anime.source.parsers

import ani.saikou.*
import ani.saikou.anime.Episode
import ani.saikou.anime.source.AnimeParser
import ani.saikou.media.Media
import ani.saikou.media.Source
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

open class Tenshi(override val name: String="tenshi.moe") : AnimeParser() {
    private var cookie:MutableMap<String, String>?=null

    fun getCookies(): MutableMap<String, String> {
        cookie = cookie ?: Jsoup.connect("https://check.ddos-guard.net/check.js").ignoreContentType(true).method(Connection.Method.GET).execute().cookies()
        return cookie!!
    }

    fun getCookieHeaders() : String{
        var a = ""
        cookie = cookie ?: getCookies()
        cookie!!.forEach {
            val s = it.key
            val s2 = it.value
            a = ",$a$s=$s2"
        }
        return a.removePrefix(",")
    }
    override fun getStream(episode: Episode, server: String): Episode {
        try {
            runBlocking{
                val asy = arrayListOf<Deferred<*>>()
                episode.streamLinks = mutableMapOf()
                Jsoup.connect(episode.link!!).cookies(getCookies()).get().select("ul.dropdown-menu > li > a.dropdown-item").forEach {
                    asy.add(async{
                        val a = it.text().replace(" ","").replace("/-","")
                        if(server==a)
                            load(episode,it)
                    })
                }
                asy.awaitAll()
            }
        }catch (e:Exception){
            toastString(e.toString())
        }
        return episode
    }

    override fun getStreams(episode: Episode): Episode {
        try {
            runBlocking{
                val asy = arrayListOf<Deferred<*>>()
                episode.streamLinks = mutableMapOf()
                Jsoup.connect(episode.link!!).cookies(getCookies()).get().select("ul.dropdown-menu > li > a.dropdown-item").forEach {
                    asy.add(async{
                        load(episode,it)
                    })
                }
                asy.awaitAll()
            }
        }catch (e:Exception){
            toastString(e.toString())
        }
        return episode
    }

    open fun load(episode: Episode,it:Element){
        val server = it.text().replace(" ","").replace("/-","")
        val url = "https://$name/embed?v="+("${it.attr("href")}|").findBetween("?v=","|")
        val unSanitized = Jsoup.connect(url).header("Referer",episode.link!!).cookies(getCookies()).get().select("main").toString().findBetween("player.source = ",";")!!
        val json = Json.decodeFromString<JsonObject>(
            Regex("""([a-z0-9A-Z_]+): """,RegexOption.DOT_MATCHES_ALL)
                .replace(unSanitized,"\"$1\" : ")
                .replace('\'','"')
                .replace("\n","").replace(" ","").replace(",}","}").replace(",]","]")
                .also { i-> println(i) })

        val a = arrayListOf<Deferred<*>>()
        val headers = mutableMapOf("cookie" to getCookieHeaders(),"referer" to url)
        val qualities = arrayListOf<Episode.Quality>()
        runBlocking {
            json["sources"]?.jsonArray?.forEach{ i->
                a.add(async {
                    val uri = i.jsonObject["src"]?.toString()?.trim('"')
                    if(uri!=null)
                        qualities.add(
                            Episode.Quality(
                                url = uri,
                                quality = i.jsonObject["size"].toString()+"p",
                                size = getSize(uri,headers)
                            )
                        )
                })
            }
            a.awaitAll()
        }
        episode.streamLinks[server] = Episode.StreamLinks(server,qualities,headers)
    }

    override fun getEpisodes(media: Media): MutableMap<String, Episode> {
        try{
            var slug:Source? = loadData("tenshi_${media.id}")
            if (slug==null) {
                fun s(it:String):Boolean{
                    live.postValue("Searching for $it")
                    logger("Tenshi : Searching for $it")
                    val search = search(it)
                    if (search.isNotEmpty()) {
                        slug = search[0]
                        saveSource(slug!!,media.id,false)
                        return true
                    }
                    return false
                }
                if(!s(media.nameMAL?:media.name))
                    s(media.nameRomaji)
            }
            else {
                live.postValue("Selected : ${slug!!.name}")
            }
            if (slug!=null) return getSlugEpisodes(slug!!.link)
        }catch (e:Exception){
            toastString("$e")
        }
        return mutableMapOf()
    }

    override fun search(string: String): ArrayList<Source> {
        logger("Searching for : $string")
        val responseArray = arrayListOf<Source>()
        try{
            Jsoup.connect("https://$name/anime?q=$string&s=vtt-d").cookies(mutableMapOf("loop-view" to "thumb")).cookies(getCookies()).get().apply {
                select("ul.loop.anime-loop.thumb > li > a").forEach{
                    responseArray.add(
                        Source(
                            link = it.attr("abs:href"),
                            name = it.attr("title"),
                            cover = it.select(".image")[0].attr("src")
                        )
                    )
                }
            }
        }catch (e:Exception){
            toastString(e.toString())
        }
        return responseArray
    }

    override fun getSlugEpisodes(slug: String): MutableMap<String, Episode> {
        val responseArray = mutableMapOf<String,Episode>()
        try {
            (1..Jsoup.connect(slug).cookies(getCookies()).get().select(".entry-episodes > h2 > span.badge.badge-secondary.align-top").text().toInt()).forEach{
                responseArray[it.toString()] = Episode(it.toString(), link = "${slug}/$it")
            }
        }catch (e:Exception){
            toastString(e.toString())
        }
        return responseArray
    }

    override fun saveSource(source: Source, id: Int, selected: Boolean) {
        super.saveSource(source, id, selected)
        saveData("tenshi_$id", source)
    }
}