package ani.saikou.anilist

import android.app.Activity
import ani.saikou.*
import ani.saikou.anime.Anime
import ani.saikou.manga.Manga
import ani.saikou.media.Character
import ani.saikou.media.Media
import ani.saikou.media.Studio
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import org.jsoup.Jsoup
import java.io.Serializable
import java.net.UnknownHostException

fun executeQuery(query:String, variables:String="",force:Boolean=false,useToken:Boolean=true): JsonObject? {
    try {
        val set = Jsoup.connect("https://graphql.anilist.co/")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .requestBody("""{"query":"$query","variables": "$variables"}""")
            .ignoreContentType(true).ignoreHttpErrors(true)
        if (Anilist.token!=null || force) {
            if (Anilist.token!=null && useToken) set.header("Authorization", "Bearer ${Anilist.token}")
            val json = set.post().body().text()
            logger("JSON : $json", false)
            val js = Json.decodeFromString<JsonObject>(json)
            if(js["data"]!=JsonNull)
                return js
        }
    } catch (e:Exception){
        if(e is UnknownHostException) toastString("Network error, please Retry.")
        else toastString("$e")
    }
    return null
}

data class SearchResults(
    val type: String,
    val isAdult: Boolean,
    val perPage:Int?=null,
    val search: String? = null,
    val sort: String? = null,
    val genres: ArrayList<String>? = null,
    var page: Int=1,
    var results:ArrayList<Media>,
    var hasNextPage:Boolean,
):Serializable

class AnilistQueries{
    fun getUserData():Boolean{
        return try{
            val response = executeQuery("""{Viewer {name options{ displayAdultContent } avatar{medium}id statistics{anime{episodesWatched}manga{chaptersRead}}}}""")!!["data"]!!.jsonObject["Viewer"]!!

            Anilist.userid = response.jsonObject["id"].toString().toInt()
            Anilist.username = response.jsonObject["name"].toString().trim('"')
            Anilist.avatar = response.jsonObject["avatar"]!!.jsonObject["medium"].toString().trim('"')
            Anilist.episodesWatched = response.jsonObject["statistics"]!!.jsonObject["anime"]!!.jsonObject["episodesWatched"].toString().toInt()
            Anilist.chapterRead = response.jsonObject["statistics"]!!.jsonObject["manga"]!!.jsonObject["chaptersRead"].toString().toInt()
            Anilist.adult = response.jsonObject["options"]?.jsonObject?.get("displayAdultContent")?.toString() == "true"
            true
        } catch (e: Exception){
            logger(e)
            false
        }
    }

    fun getMedia(id:Int,mal:Boolean=false):Media?{
        val response = executeQuery("""{Media(${if(!mal) "id:" else "idMal:"}$id){id status chapters episodes nextAiringEpisode{episode}type meanScore isAdult isFavourite bannerImage coverImage{large}title{english romaji userPreferred}mediaListEntry{progress score(format:POINT_100)status}}}""", force = true)
        val i = (response?.get("data")?:return null).jsonObject["Media"]?:return null
        if (i!=JsonNull){
            return Media(
                id = i.jsonObject["id"].toString().toInt(),
                name = i.jsonObject["title"]!!.jsonObject["english"].toString().trim('"').replace("\\\"","\""),
                nameRomaji = i.jsonObject["title"]!!.jsonObject["romaji"].toString().trim('"').replace("\\\"","\""),
                userPreferredName = i.jsonObject["title"]!!.jsonObject["userPreferred"].toString().trim('"').replace("\\\"","\""),
                cover = i.jsonObject["coverImage"]!!.jsonObject["large"].toString().trim('"'),
                banner = if(i.jsonObject["bannerImage"]!=JsonNull) i.jsonObject["bannerImage"].toString().trim('"') else null,
                status = i.jsonObject["status"].toString().trim('"'),
                isFav = i.jsonObject["isFavourite"].toString() == "true",
                isAdult = i.jsonObject["isAdult"].toString() == "true",
                userProgress = if (i.jsonObject["mediaListEntry"] != JsonNull) i.jsonObject["mediaListEntry"]!!.jsonObject["progress"].toString().toInt() else null,
                userScore = if (i.jsonObject["mediaListEntry"] != JsonNull) i.jsonObject["mediaListEntry"]!!.jsonObject["score"].toString().toInt() else 0,
                userStatus = if (i.jsonObject["mediaListEntry"] != JsonNull) i.jsonObject["mediaListEntry"]!!.jsonObject["status"].toString().trim('"') else null,
                meanScore = if (i.jsonObject["meanScore"].toString().trim('"') != "null") i.jsonObject["meanScore"].toString().toInt() else null,
                anime = if (i.jsonObject["type"].toString().trim('"') == "ANIME") Anime(totalEpisodes = if (i.jsonObject["episodes"] != JsonNull) i.jsonObject["episodes"].toString().toInt() else null, nextAiringEpisode = if (i.jsonObject["nextAiringEpisode"] != JsonNull) i.jsonObject["nextAiringEpisode"]!!.jsonObject["episode"].toString().toInt() - 1 else null) else null,
                manga = if (i.jsonObject["type"].toString().trim('"') == "MANGA") Manga(totalChapters = if (i.jsonObject["chapters"] != JsonNull) i.jsonObject["chapters"].toString().toInt() else null) else null,
            )
        }
        return null
    }

    fun mediaDetails(media:Media): Media? {
        val query = """{Media(id:${media.id}){mediaListEntry{id status score(format:POINT_100) progress repeat updatedAt startedAt{year month day}completedAt{year month day}}isFavourite siteUrl idMal nextAiringEpisode{episode airingAt}source countryOfOrigin format duration season seasonYear startDate{year month day}endDate{year month day}genres studios(isMain:true){nodes{id name siteUrl}}description characters(sort:[ROLE,FAVOURITES_DESC],perPage:25,page:1){edges{role node{id image{medium}name{userPreferred}}}}relations{edges{relationType(version:2)node{id mediaListEntry{progress score(format:POINT_100) status} episodes chapters nextAiringEpisode{episode}meanScore isAdult isFavourite title{english romaji userPreferred}type status(version:2)bannerImage coverImage{large}}}}recommendations{nodes{mediaRecommendation{id mediaListEntry{progress score(format:POINT_100) status} episodes chapters nextAiringEpisode{episode}meanScore isAdult isFavourite title{english romaji userPreferred}type status(version:2)bannerImage coverImage{large}}}}externalLinks{url site}}}"""
        var response =  executeQuery(query, force = true)
        if (response!=null){
            fun parse():Media{
                val it = response!!["data"]!!.jsonObject["Media"]!!

                media.source = it.jsonObject["source"]!!.toString().trim('"')
                media.countryOfOrigin = it.jsonObject["countryOfOrigin"].toString().trim('"')
                media.format = it.jsonObject["format"]!!.toString().trim('"')

                media.startDate = FuzzyDate(
                    if(it.jsonObject["startDate"]!!.jsonObject["year"]!=JsonNull) it.jsonObject["startDate"]!!.jsonObject["year"].toString().toInt() else null,
                    if(it.jsonObject["startDate"]!!.jsonObject["month"]!=JsonNull) it.jsonObject["startDate"]!!.jsonObject["month"].toString().toInt() else null,
                    if(it.jsonObject["startDate"]!!.jsonObject["day"]!=JsonNull) it.jsonObject["startDate"]!!.jsonObject["day"].toString().toInt() else null
                )
                media.endDate = FuzzyDate(
                    if(it.jsonObject["endDate"]!!.jsonObject["year"]!=JsonNull) it.jsonObject["endDate"]!!.jsonObject["year"].toString().toInt() else null,
                    if(it.jsonObject["endDate"]!!.jsonObject["month"]!=JsonNull) it.jsonObject["endDate"]!!.jsonObject["month"].toString().toInt() else null,
                    if(it.jsonObject["endDate"]!!.jsonObject["day"]!=JsonNull) it.jsonObject["endDate"]!!.jsonObject["day"].toString().toInt() else null
                )
                if (it.jsonObject["genres"]!!!=JsonNull){
                    media.genres = arrayListOf()
                    it.jsonObject["genres"]!!.jsonArray.forEach { i ->
                        media.genres!!.add(i.toString().trim('"'))
                    }
                }
                media.description = it.jsonObject["description"]!!.toString().trim('"').replace("\\\"","\"").replace("\\n","\n")

                if(it.jsonObject["characters"]!=JsonNull){
                    media.characters = arrayListOf()
                    it.jsonObject["characters"]!!.jsonObject["edges"]!!.jsonArray.forEach { i->
                        media.characters!!.add(Character(
                            id = i.jsonObject["node"]!!.jsonObject["id"]!!.toString().toInt(),
                            name = i.jsonObject["node"]!!.jsonObject["name"]!!.jsonObject["userPreferred"].toString().trim('"').replace("\\\"","\""),
                            image = i.jsonObject["node"]!!.jsonObject["image"]!!.jsonObject["medium"].toString().trim('"'),
                            banner = media.banner?:media.cover,
                            role = i.jsonObject["role"]!!.toString().trim('"')
                        ))
                    }
                }
                if(it.jsonObject["relations"]!=JsonNull){
                    media.relations = arrayListOf()
                    it.jsonObject["relations"]!!.jsonObject["edges"]!!.jsonArray.forEach { i->
                        media.relations!!.add(
                            Media(
                                id = i.jsonObject["node"]!!.jsonObject["id"].toString().toInt(),
                                name = i.jsonObject["node"]!!.jsonObject["title"]!!.jsonObject["english"].toString().trim('"').replace("\\\"","\""),
                                nameRomaji = i.jsonObject["node"]!!.jsonObject["title"]!!.jsonObject["romaji"].toString().trim('"').replace("\\\"","\""),
                                userPreferredName = i.jsonObject["node"]!!.jsonObject["title"]!!.jsonObject["userPreferred"].toString().trim('"').replace("\\\"","\""),
                                cover = i.jsonObject["node"]!!.jsonObject["coverImage"]!!.jsonObject["large"].toString().trim('"'),
                                banner = if(i.jsonObject["node"]!!.jsonObject["bannerImage"]!=JsonNull) i.jsonObject["node"]!!.jsonObject["bannerImage"].toString().trim('"') else null,
                                status = i.jsonObject["node"]!!.jsonObject["status"].toString().trim('"'),
                                isFav = i.jsonObject["node"]!!.jsonObject["isFavourite"].toString()=="true",
                                isAdult = i.jsonObject["node"]!!.jsonObject["isAdult"].toString()=="true",
                                userProgress = if (i.jsonObject["node"]!!.jsonObject["mediaListEntry"]!=JsonNull) i.jsonObject["node"]!!.jsonObject["mediaListEntry"]!!.jsonObject["progress"].toString().toInt() else null,
                                userScore = if (i.jsonObject["node"]!!.jsonObject["mediaListEntry"]!=JsonNull) i.jsonObject["node"]!!.jsonObject["mediaListEntry"]!!.jsonObject["score"].toString().toInt() else 0,
                                userStatus = if (i.jsonObject["node"]!!.jsonObject["mediaListEntry"]!=JsonNull) i.jsonObject["node"]!!.jsonObject["mediaListEntry"]!!.jsonObject["status"].toString().trim('"') else null,
                                meanScore = if (i.jsonObject["node"]!!.jsonObject["meanScore"].toString().trim('"')!="null") i.jsonObject["node"]!!.jsonObject["meanScore"].toString().toInt() else null,
                                relation = i.jsonObject["relationType"].toString().trim('"'),
                                anime = if (i.jsonObject["node"]!!.jsonObject["type"].toString().trim('"')=="ANIME") Anime(totalEpisodes = if (i.jsonObject["node"]!!.jsonObject["episodes"] != JsonNull) i.jsonObject["node"]!!.jsonObject["episodes"].toString().toInt() else null, nextAiringEpisode = if(i.jsonObject["node"]!!.jsonObject["nextAiringEpisode"] != JsonNull) i.jsonObject["node"]!!.jsonObject["nextAiringEpisode"]!!.jsonObject["episode"].toString().toInt()-1 else null) else null,
                                manga = if (i.jsonObject["node"]!!.jsonObject["type"].toString().trim('"')=="MANGA") Manga(totalChapters = if (i.jsonObject["node"]!!.jsonObject["chapters"] != JsonNull) i.jsonObject["node"]!!.jsonObject["chapters"].toString().toInt() else null) else null,
                            )
                        )
                    }
                }
                if(it.jsonObject["recommendations"]!=JsonNull) {
                    media.recommendations = arrayListOf()
                    it.jsonObject["recommendations"]!!.jsonObject["nodes"]!!.jsonArray.forEach { i ->
                        if (i.jsonObject["mediaRecommendation"]!=JsonNull){
                            media.recommendations!!.add(
                                Media(
                                    id = i.jsonObject["mediaRecommendation"]!!.jsonObject["id"].toString().toInt(),
                                    name = i.jsonObject["mediaRecommendation"]!!.jsonObject["title"]!!.jsonObject["english"].toString().trim('"').replace("\\\"","\""),
                                    nameRomaji = i.jsonObject["mediaRecommendation"]!!.jsonObject["title"]!!.jsonObject["romaji"].toString().trim('"').replace("\\\"","\""),
                                    userPreferredName = i.jsonObject["mediaRecommendation"]!!.jsonObject["title"]!!.jsonObject["userPreferred"].toString().trim('"').replace("\\\"","\""),
                                    status = i.jsonObject["mediaRecommendation"]!!.jsonObject["status"].toString().trim('"'),
                                    cover = i.jsonObject["mediaRecommendation"]!!.jsonObject["coverImage"]!!.jsonObject["large"].toString().trim('"'),
                                    banner = if(i.jsonObject["mediaRecommendation"]!!.jsonObject["bannerImage"]!=JsonNull) i.jsonObject["mediaRecommendation"]!!.jsonObject["bannerImage"].toString().trim('"') else null,
                                    meanScore = if (i.jsonObject["mediaRecommendation"]!!.jsonObject["meanScore"].toString().trim('"')!="null") i.jsonObject["mediaRecommendation"]!!.jsonObject["meanScore"].toString().toInt() else null,
                                    isFav = i.jsonObject["mediaRecommendation"]!!.jsonObject["isFavourite"].toString()=="true",
                                    isAdult = i.jsonObject["mediaRecommendation"]!!.jsonObject["isAdult"].toString() == "true",
                                    userProgress = if (i.jsonObject["mediaRecommendation"]!!.jsonObject["mediaListEntry"]!=JsonNull) i.jsonObject["mediaRecommendation"]!!.jsonObject["mediaListEntry"]!!.jsonObject["progress"].toString().toInt() else null,
                                    userScore = if (i.jsonObject["mediaRecommendation"]!!.jsonObject["mediaListEntry"]!=JsonNull) i.jsonObject["mediaRecommendation"]!!.jsonObject["mediaListEntry"]!!.jsonObject["score"].toString().toInt() else 0,
                                    userStatus = if (i.jsonObject["mediaRecommendation"]!!.jsonObject["mediaListEntry"]!=JsonNull) i.jsonObject["mediaRecommendation"]!!.jsonObject["mediaListEntry"]!!.jsonObject["status"].toString().trim('"') else null,
                                    anime = if (i.jsonObject["mediaRecommendation"]!!.jsonObject["type"].toString().trim('"') == "ANIME") Anime(totalEpisodes = if (i.jsonObject["mediaRecommendation"]!!.jsonObject["episodes"] != JsonNull) i.jsonObject["mediaRecommendation"]!!.jsonObject["episodes"].toString().toInt() else null, nextAiringEpisode = if (i.jsonObject["mediaRecommendation"]!!.jsonObject["nextAiringEpisode"] != JsonNull) i.jsonObject["mediaRecommendation"]!!.jsonObject["nextAiringEpisode"]!!.jsonObject["episode"].toString().toInt() - 1 else null) else null,
                                    manga = if (i.jsonObject["mediaRecommendation"]!!.jsonObject["type"].toString().trim('"') == "MANGA") Manga(totalChapters = if (i.jsonObject["mediaRecommendation"]!!.jsonObject["chapters"] != JsonNull) i.jsonObject["mediaRecommendation"]!!.jsonObject["chapters"].toString().toInt() else null) else null
                                )
                            )
                        }
                    }
                }

                if (it.jsonObject["mediaListEntry"]!=JsonNull) {
                    val i = it.jsonObject["mediaListEntry"]!!
                    media.userProgress = i.jsonObject["progress"].toString().toInt()
                    media.userListId = i.jsonObject["id"].toString().toInt()
                    media.userScore = i.jsonObject["score"].toString().toInt()
                    media.userStatus = i.jsonObject["status"].toString().trim('"')
                    media.userRepeat = if (i.jsonObject["repeat"].toString() == "null") i.jsonObject["repeat"]!!.toString().toInt() else 0
                    media.userUpdatedAt = i.jsonObject["updatedAt"]!!.toString().toLong()*1000
                    media.userCompletedAt = FuzzyDate(
                        if (i.jsonObject["completedAt"]!!.jsonObject["year"] != JsonNull) i.jsonObject["completedAt"]!!.jsonObject["year"].toString().toInt() else null,
                        if (i.jsonObject["completedAt"]!!.jsonObject["month"] != JsonNull) i.jsonObject["completedAt"]!!.jsonObject["month"].toString().toInt() else null,
                        if (i.jsonObject["completedAt"]!!.jsonObject["day"] != JsonNull) i.jsonObject["completedAt"]!!.jsonObject["day"].toString().toInt() else null
                    )
                    media.userStartedAt = FuzzyDate(
                        if (i.jsonObject["startedAt"]!!.jsonObject["year"] != JsonNull) i.jsonObject["startedAt"]!!.jsonObject["year"].toString().toInt() else null,
                        if (i.jsonObject["startedAt"]!!.jsonObject["month"] != JsonNull) i.jsonObject["startedAt"]!!.jsonObject["month"].toString().toInt() else null,
                        if (i.jsonObject["startedAt"]!!.jsonObject["day"] != JsonNull) i.jsonObject["startedAt"]!!.jsonObject["day"].toString().toInt() else null
                    )
                }else{
                    media.userStatus = null
                    media.userListId = null
                    media.userProgress = null
                    media.userScore = 0
                    media.userRepeat = 0
                    media.userUpdatedAt = null
                    media.userCompletedAt = FuzzyDate()
                    media.userStartedAt = FuzzyDate()
                }

                if (media.anime != null) {
                    media.anime.episodeDuration = if (it.jsonObject["duration"]!!!=JsonNull) it.jsonObject["duration"]!!.toString().toInt() else null
                    media.anime.season = if (it.jsonObject["season"]!!!=JsonNull) it.jsonObject["season"]!!.toString().trim('"') else null
                    media.anime.seasonYear = if (it.jsonObject["seasonYear"]!!!=JsonNull) it.jsonObject["seasonYear"]!!.toString().toInt() else null

                    if (it.jsonObject["studios"]!!.jsonObject["nodes"]!!.jsonArray.isNotEmpty()) {
                        media.anime.mainStudio = Studio(it.jsonObject["studios"]!!.jsonObject["nodes"]!!.jsonArray[0].jsonObject["id"].toString(),it.jsonObject["studios"]!!.jsonObject["nodes"]!!.jsonArray[0].jsonObject["name"].toString().trim('"'))
                    }
                    media.anime.nextAiringEpisodeTime = if(it.jsonObject["nextAiringEpisode"] != JsonNull) it.jsonObject["nextAiringEpisode"]!!.jsonObject["airingAt"].toString().toLong() else null

                    it.jsonObject["externalLinks"]!!.jsonArray.forEach{ i->
                        if(i.jsonObject["site"].toString().trim('"')=="Youtube"){
                            media.anime.youtube = i.jsonObject["url"].toString().trim('"')
                        }
                    }
                }
                else if (media.manga != null) {
                    logger ("Nothing Here lmao",false)
                }
                media.shareLink = it.jsonObject["siteUrl"]!!.toString().trim('"')
                if (it.jsonObject["idMal"]!!!=JsonNull) {
                    media.idMAL = it.jsonObject["idMal"]!!.toString().toInt()
                    return getMalMedia(media)
                }
                return media
            }
            return if (response["data"]?.jsonObject?.get("Media")!=JsonNull) parse() else {
                toastString("Adult Stuff? ( ͡° ͜ʖ ͡°)")
                response = executeQuery(query, force = true, useToken = false)
                parse()
            }
        }
        return null
    }

    fun continueMedia(type:String): ArrayList<Media> {
        val returnArray = arrayListOf<Media>()
        val map = mutableMapOf<Int, Media>()
        val statuses = arrayOf("CURRENT","REPEATING")
        fun repeat(status:String) {
            val response = executeQuery(""" { MediaListCollection(userId: ${Anilist.userid}, type: $type, status: $status , sort: UPDATED_TIME ) { lists { entries { progress score(format:POINT_100) status media { id isAdult status chapters episodes nextAiringEpisode {episode} meanScore isFavourite bannerImage coverImage{large} title { english romaji userPreferred } } } } } } """)
            val data = if(response?.get("data")!=null && response["data"] !=JsonNull) response["data"] else null
            val a = if(data?.jsonObject?.get("MediaListCollection")!=null && data.jsonObject["MediaListCollection"] !=JsonNull) data.jsonObject["MediaListCollection"] else null
            val list = a?.jsonObject?.get("lists")?.jsonArray
            if (list != null && list.isNotEmpty()) {
                list[0].jsonObject["entries"]!!.jsonArray.reversed().forEach {
                    map[it.jsonObject["media"]!!.jsonObject["id"].toString().toInt()] =
                        Media(
                            id = it.jsonObject["media"]!!.jsonObject["id"].toString().toInt(),
                            name = it.jsonObject["media"]!!.jsonObject["title"]!!.jsonObject["english"].toString().trim('"').replace("\\\"", "\""),
                            nameRomaji = it.jsonObject["media"]!!.jsonObject["title"]!!.jsonObject["romaji"].toString().trim('"').replace("\\\"", "\""),
                            userPreferredName = it.jsonObject["media"]!!.jsonObject["title"]!!.jsonObject["userPreferred"].toString().trim('"').replace("\\\"", "\""),
                            cover = it.jsonObject["media"]!!.jsonObject["coverImage"]!!.jsonObject["large"].toString().trim('"'),
                            banner = if (it.jsonObject["media"]!!.jsonObject["bannerImage"] != JsonNull) it.jsonObject["media"]!!.jsonObject["bannerImage"].toString().trim('"') else null,
                            status = it.jsonObject["media"]!!.jsonObject["status"].toString().trim('"'),
                            meanScore = if (it.jsonObject["media"]!!.jsonObject["meanScore"] != JsonNull) it.jsonObject["media"]!!.jsonObject["meanScore"].toString().toInt() else null,
                            isFav = it.jsonObject["media"]!!.jsonObject["isFavourite"].toString() == "true",
                            isAdult = it.jsonObject["media"]!!.jsonObject["isAdult"].toString() == "true",
                            userProgress = it.jsonObject["progress"].toString().toInt(),
                            userScore = it.jsonObject["score"].toString().toInt(),
                            userStatus = it.jsonObject["status"].toString().trim('"'),
                            cameFromContinue = true,
                            anime = if (type == "ANIME") Anime(
                                totalEpisodes = if (it.jsonObject["media"]!!.jsonObject["episodes"] != JsonNull) it.jsonObject["media"]!!.jsonObject["episodes"].toString()
                                    .toInt() else null,
                                nextAiringEpisode = if (it.jsonObject["media"]!!.jsonObject["nextAiringEpisode"] != JsonNull)
                                    it.jsonObject["media"]!!.jsonObject["nextAiringEpisode"]!!.jsonObject["episode"].toString().toInt() - 1 else null
                            ) else null,
                            manga = if (type == "MANGA") Manga(
                                totalChapters = if (it.jsonObject["media"]!!.jsonObject["chapters"] != JsonNull) it.jsonObject["media"]!!.jsonObject["chapters"].toString()
                                    .toInt() else null
                            ) else null,
                        )
                }
            }
        }
        statuses.forEach { repeat(it) }
        val set = loadData<MutableSet<Int>>("continue_$type")
        if (set != null) {
            set.reversed().forEach {
                if (map.containsKey(it)) returnArray.add(map[it]!!)
            }
            for (i in map) {
                if (i.value !in returnArray) returnArray.add(i.value)
            }
        } else returnArray.addAll(map.values)
        return returnArray
    }

    fun recommendations(): ArrayList<Media> {
        val response = executeQuery(""" { Page(page: 1, perPage:30) { pageInfo { total currentPage hasNextPage } recommendations(sort: RATING_DESC, onList: true) { rating userRating mediaRecommendation { id isAdult mediaListEntry {progress score(format:POINT_100) status} chapters isFavourite episodes nextAiringEpisode {episode} meanScore isFavourite title {english romaji userPreferred } type status(version: 2) bannerImage coverImage { large } } } } } """)
        val responseArray = arrayListOf<Media>()
        val ids = arrayListOf<Int>()
        if (response?.get("data")!=null && response["data"] !=JsonNull) response["data"]!!.jsonObject["Page"]!!.jsonObject["recommendations"]!!.jsonArray.reversed().forEach{
            val json = it.jsonObject["mediaRecommendation"]
            if(json!=null && json!=JsonNull){
                val id =  json.jsonObject["id"]?.toString()?.toInt()?:return responseArray
                if (id !in ids) {
                    ids.add(id)
                    responseArray.add(
                        Media(
                            id = id,
                            name = json.jsonObject["title"]!!.jsonObject["english"].toString().trim('"').replace("\\\"","\""),
                            nameRomaji = json.jsonObject["title"]!!.jsonObject["romaji"].toString().trim('"').replace("\\\"","\""),
                            userPreferredName = json.jsonObject["title"]!!.jsonObject["userPreferred"].toString().trim('"').replace("\\\"","\""),
                            status = json.jsonObject["status"].toString().trim('"'),
                            cover = json.jsonObject["coverImage"]!!.jsonObject["large"].toString().trim('"'),
                            banner = if(json.jsonObject["bannerImage"]!=JsonNull) json.jsonObject["bannerImage"].toString().trim('"') else null,
                            meanScore = if(json.jsonObject["meanScore"]!=JsonNull) json.jsonObject["meanScore"].toString().toInt() else null,
                            isFav = json.jsonObject["isFavourite"].toString()=="true",
                            isAdult = json.jsonObject["isAdult"].toString() == "true",
                            userProgress = if (json.jsonObject["mediaListEntry"]!=JsonNull) json.jsonObject["mediaListEntry"]!!.jsonObject["progress"].toString().toInt() else null,
                            userScore = if (json.jsonObject["mediaListEntry"]!=JsonNull) json.jsonObject["mediaListEntry"]!!.jsonObject["score"].toString().toInt() else 0,
                            userStatus = if (json.jsonObject["mediaListEntry"]!=JsonNull) json.jsonObject["mediaListEntry"]!!.jsonObject["status"].toString().trim('"') else null,
                            anime = if(json.jsonObject["type"].toString().trim('"') == "ANIME") Anime(totalEpisodes = if (json.jsonObject["episodes"] != JsonNull) json.jsonObject["episodes"].toString().toInt() else null, nextAiringEpisode = if (json.jsonObject["nextAiringEpisode"] != JsonNull) json.jsonObject["nextAiringEpisode"]!!.jsonObject["episode"].toString().toInt() - 1 else null) else null,
                            manga = if(json.jsonObject["type"].toString().trim('"') == "MANGA") Manga(totalChapters = if (json.jsonObject["chapters"] != JsonNull) json.jsonObject["chapters"].toString().toInt() else null) else null,
                        )
                    )
                }
            }
        }
        return responseArray
    }

    private fun bannerImage(type: String): String? {
        val response = executeQuery("""{ MediaListCollection(userId: ${Anilist.userid}, type: $type, sort:[SCORE_DESC,UPDATED_TIME_DESC],chunk:1,perChunk:1) { lists { entries{ media { bannerImage } } } } } """)
        val data = if (response!=null) response["data"] else null
        if(data!=null && data!=JsonNull) {
            val b = if(data.jsonObject["MediaListCollection"]!=JsonNull) data.jsonObject["MediaListCollection"] else null
            val list = b?.jsonObject?.get("lists")?.jsonArray
            if (list != null && list.isNotEmpty()) {
                val a = list[0].jsonObject["entries"]?.jsonArray?.get(0)?.jsonObject?.get("media")?.jsonObject?.get("bannerImage")?.toString()?.trim('"')
                return if (a != null && a != "null") a else null
            }
        }
        return null
    }

    fun getBannerImages(): ArrayList<String?> {
        val default = arrayListOf<String?>(null,null)
        default[0]=bannerImage("ANIME")
        default[1]=bannerImage("MANGA")
        return default
    }

    fun getMediaLists(anime:Boolean,userId:Int): MutableMap<String,ArrayList<Media>> {
        val response = executeQuery("""{ MediaListCollection(userId: $userId, type: ${if(anime) "ANIME" else "MANGA"}) { lists { name entries { status progress score(format:POINT_100) media { id isAdult status chapters episodes nextAiringEpisode {episode} bannerImage meanScore isFavourite coverImage{large} title {english romaji userPreferred } } } } user { mediaListOptions { rowOrder animeList { sectionOrder } mangaList { sectionOrder } } } } }""")
        val sorted = mutableMapOf<String,ArrayList<Media>>()
        val unsorted = mutableMapOf<String,ArrayList<Media>>()
        val all = arrayListOf<Media>()
        val collection = ((response?:return unsorted)["data"]?:return unsorted).jsonObject["MediaListCollection"]?:return unsorted
        collection.jsonObject["lists"]?.jsonArray?.forEach { i ->
            val name = i.jsonObject["name"].toString().trim('"')
            unsorted[name] = arrayListOf()
            i.jsonObject["entries"]!!.jsonArray.forEach {
                val a = Media(
                    id = it.jsonObject["media"]!!.jsonObject["id"].toString().toInt(),
                    name = it.jsonObject["media"]!!.jsonObject["title"]!!.jsonObject["english"].toString().trim('"'),
                    nameRomaji = it.jsonObject["media"]!!.jsonObject["title"]!!.jsonObject["romaji"].toString().trim('"'),
                    userPreferredName = it.jsonObject["media"]!!.jsonObject["title"]!!.jsonObject["userPreferred"].toString().trim('"'),
                    cover = it.jsonObject["media"]!!.jsonObject["coverImage"]!!.jsonObject["large"].toString().trim('"'),
                    banner = if(it.jsonObject["media"]!!.jsonObject["bannerImage"]!=JsonNull) it.jsonObject["media"]!!.jsonObject["bannerImage"].toString().trim('"') else null,
                    status = it.jsonObject["media"]!!.jsonObject["status"].toString().trim('"'),
                    meanScore = if(it.jsonObject["media"]!!.jsonObject["meanScore"] != JsonNull) it.jsonObject["media"]!!.jsonObject["meanScore"].toString().toInt() else null,
                    isAdult = it.jsonObject["media"]!!.jsonObject["isAdult"].toString() == "true",
                    isFav = it.jsonObject["media"]!!.jsonObject["isFavourite"].toString()=="true",
                    userScore = it.jsonObject["score"].toString().toInt(),
                    userStatus = it.jsonObject["status"].toString().trim('"'),
                    userProgress = it.jsonObject["progress"].toString().toInt(),
                    manga = if(!anime) Manga(totalChapters = if (it.jsonObject["media"]!!.jsonObject["chapters"] != JsonNull) it.jsonObject["media"]!!.jsonObject["chapters"].toString().toInt() else null) else null,
                    anime = if(anime) Anime(totalEpisodes = if(it.jsonObject["media"]!!.jsonObject["episodes"] != JsonNull) it.jsonObject["media"]!!.jsonObject["episodes"].toString().toInt() else null, nextAiringEpisode = if (it.jsonObject["media"]!!.jsonObject["nextAiringEpisode"] != JsonNull) it.jsonObject["media"]!!.jsonObject["nextAiringEpisode"]!!.jsonObject["episode"].toString().toInt()-1 else null) else null
                )
                unsorted[name]!!.add(a)
                all.add(a)
            }
        }

        val options = collection.jsonObject["user"]!!.jsonObject["mediaListOptions"]!!
        options.jsonObject[if(anime) "animeList" else "mangaList"]!!.jsonObject["sectionOrder"]!!.jsonArray.forEach {
            val list = it.toString().trim('"')
            if(unsorted.containsKey(list))
                sorted[list] = unsorted[list]!!
        }
        val favResponse = executeQuery("""{User(id:$userId){favourites{${if(anime) "anime" else "manga"}(page:0){edges{favouriteOrder node{id isAdult mediaListEntry{progress score(format:POINT_100)status}chapters isFavourite episodes nextAiringEpisode{episode}meanScore isFavourite title{english romaji userPreferred}type status(version:2)bannerImage coverImage{large}}}}}}}""")
        sorted["Favourites"] = arrayListOf()
        favResponse?.jsonObject?.get("data").apply {
            if(this!=null && this!=JsonNull) this.jsonObject["User"]!!.jsonObject["favourites"]!!.jsonObject[if(anime) "anime" else "manga"]!!.jsonObject["edges"]!!.jsonArray.forEach {
                val json = it.jsonObject["node"]!!
                sorted["Favourites"]!!.add(
                    Media(
                        id = json.jsonObject["id"].toString().toInt(),
                        name = json.jsonObject["title"]!!.jsonObject["english"].toString().trim('"').replace("\\\"","\""),
                        nameRomaji = json.jsonObject["title"]!!.jsonObject["romaji"].toString().trim('"').replace("\\\"","\""),
                        userPreferredName = json.jsonObject["title"]!!.jsonObject["userPreferred"].toString().trim('"').replace("\\\"","\""),
                        status = json.jsonObject["status"].toString().trim('"'),
                        cover = json.jsonObject["coverImage"]!!.jsonObject["large"].toString().trim('"'),
                        banner = if(json.jsonObject["bannerImage"]!=JsonNull) json.jsonObject["bannerImage"].toString().trim('"') else null,
                        meanScore = if(json.jsonObject["meanScore"]!=JsonNull) json.jsonObject["meanScore"].toString().toInt() else null,
                        isAdult = json.jsonObject["isAdult"].toString() == "true",
                        isFav = true,
                        userProgress = if (json.jsonObject["mediaListEntry"]!=JsonNull) json.jsonObject["mediaListEntry"]!!.jsonObject["progress"].toString().toInt() else null,
                        userScore = if (json.jsonObject["mediaListEntry"]!=JsonNull) json.jsonObject["mediaListEntry"]!!.jsonObject["score"].toString().toInt() else 0,
                        userStatus = if (json.jsonObject["mediaListEntry"]!=JsonNull) json.jsonObject["mediaListEntry"]!!.jsonObject["status"].toString().trim('"') else null,
                        userFavOrder = it.jsonObject["favouriteOrder"].toString().toIntOrNull(),
                        anime = if(json.jsonObject["type"].toString().trim('"') == "ANIME") Anime(totalEpisodes = if (json.jsonObject["episodes"] != JsonNull) json.jsonObject["episodes"].toString().toInt() else null, nextAiringEpisode = if (json.jsonObject["nextAiringEpisode"] != JsonNull) json.jsonObject["nextAiringEpisode"]!!.jsonObject["episode"].toString().toInt() - 1 else null) else null,
                        manga = if(json.jsonObject["type"].toString().trim('"') == "MANGA") Manga(totalChapters = if (json.jsonObject["chapters"] != JsonNull) json.jsonObject["chapters"].toString().toInt() else null) else null,
                    )
                )
            }
        }
        sorted["Favourites"]!!.sortWith(compareBy { it.userFavOrder })

        sorted["All"] = all

        val sort = options.jsonObject["rowOrder"]!!.toString().trim('"')
        for(i in sorted.keys) {
            when(sort) {
                "score" -> sorted[i]!!.sortWith { b, a -> compareValuesBy(a, b, { it.userScore }, { it.meanScore }) }
                "title" -> sorted[i]!!.sortWith(compareBy { it.userPreferredName })
                "updatedAt" -> sorted[i]!!.sortWith(compareBy { it.userUpdatedAt })
                "id" -> sorted[i]!!.sortWith(compareBy { it.id })
            }
        }
        return sorted
    }


    suspend fun genreCollection(activity:Activity):Boolean{
        var success = false
        logger("GenreCollection started")
        val genres:MutableMap<String,String>? = loadData("genres",activity)
        var tags:ArrayList<String>? = loadData("tags",activity)
        val time :Long?= loadData("genresTime",activity)
        fun snack(string:String){
            activity.runOnUiThread {
                val snackBar = Snackbar.make(activity.window.decorView.findViewById(android.R.id.content), string, Snackbar.LENGTH_LONG)
                snackBar.view.translationY = -navBarHeight.dp
                snackBar.show()
            }
        }
        suspend fun get(){

            snack("Updating Genres, Please wait...")
            val returnMap = mutableMapOf<String,String>()
            val query = "{GenreCollection}"
            val ids = arrayListOf<String>()
            try {
                val executedQuery = executeQuery(query, force = true)!!
                if (executedQuery["data"] != JsonNull) {
                    executedQuery["data"]!!.jsonObject["GenreCollection"]!!.jsonArray.forEach { genre ->
                        val genreQuery = """{ Page(perPage: 10){media(genre:${
                            genre.toString().replace("\"", "\\\"")
                        }, sort: TRENDING_DESC, type: ANIME, countryOfOrigin:\"JP\") {id bannerImage } } }"""
                        delay(200)
                        snack("Getting $genre")
                        val response = executeQuery(genreQuery, force = true)!!["data"]!!.jsonObject["Page"]!!
                        if (response.jsonObject["media"] != JsonNull) {
                            run next@{
                                response.jsonObject["media"]!!.jsonArray.forEach {
                                    if (it.jsonObject["id"].toString() !in ids && it.jsonObject["bannerImage"] != JsonNull) {
                                        ids.add(it.jsonObject["id"].toString())
                                        returnMap[genre.toString().trim('"')] =
                                            it.jsonObject["bannerImage"].toString().trim('"')
                                        return@next
                                    }
                                }
                            }
                        }
                    }
                }

                saveData("genres", returnMap, activity)
                saveData("genresTime", System.currentTimeMillis(), activity)

                success = true
                Anilist.genres = returnMap
                logger("$returnMap \nfinished")
                snack("Genres Updated! Now Loading...")
            }catch (e:Exception){
                toastString(e.toString())
            }
        }
        if (genres==null) {
            get()
        }
        else{
            if(time!=null)
                if ((System.currentTimeMillis()-time).also { println(it) } < (1000*60*60*24*7)) {
                    logger("Loaded Genres from Save.")
                    success = true
                    Anilist.genres = genres
                } else get()
            else get()
        }
        if (tags==null){
            snack("Tags not Found, Loading Tags")
            success=false
            executeQuery("""{ MediaTagCollection { name isAdult } }""", force = true)?.get("data")?.apply {
                if(this!=JsonNull){
                    tags = arrayListOf()
                    this.jsonObject["MediaTagCollection"]!!.jsonArray.forEach{
                        if(it.jsonObject["isAdult"].toString()=="true")
                            tags!!.add(it.jsonObject["name"]!!.toString().trim('"'))
                    }
                    saveData("tags",tags!!)
                    success=true
                }
            }
        }
        Anilist.tags = tags
        return success
    }

    fun search(
        type: String,
        page: Int? = null,
        perPage:Int?=null,
        search: String? = null,
        sort: String? = null,
        genres: ArrayList<String>? = null,
        tags: ArrayList<String>? = null,
        format:String?=null,
        isAdult:Boolean=false,
        id: Int?=null
    ): SearchResults? {
        val query = """
query (${"$"}page: Int = 1, ${"$"}id: Int, ${"$"}type: MediaType, ${"$"}isAdult: Boolean = false, ${"$"}search: String, ${"$"}format: [MediaFormat], ${"$"}status: MediaStatus, ${"$"}countryOfOrigin: CountryCode, ${"$"}source: MediaSource, ${"$"}season: MediaSeason, ${"$"}seasonYear: Int, ${"$"}year: String, ${"$"}onList: Boolean, ${"$"}yearLesser: FuzzyDateInt, ${"$"}yearGreater: FuzzyDateInt, ${"$"}episodeLesser: Int, ${"$"}episodeGreater: Int, ${"$"}durationLesser: Int, ${"$"}durationGreater: Int, ${"$"}chapterLesser: Int, ${"$"}chapterGreater: Int, ${"$"}volumeLesser: Int, ${"$"}volumeGreater: Int, ${"$"}licensedBy: [String], ${"$"}isLicensed: Boolean, ${"$"}genres: [String], ${"$"}excludedGenres: [String], ${"$"}tags: [String], ${"$"}excludedTags: [String], ${"$"}minimumTagRank: Int, ${"$"}sort: [MediaSort] = [POPULARITY_DESC, SCORE_DESC]) {
  Page(page: ${"$"}page, perPage: ${perPage?:50}) {
    pageInfo {
      total
      perPage
      currentPage
      lastPage
      hasNextPage
    }
    media(id: ${"$"}id, type: ${"$"}type, season: ${"$"}season, format_in: ${"$"}format, status: ${"$"}status, countryOfOrigin: ${"$"}countryOfOrigin, source: ${"$"}source, search: ${"$"}search, onList: ${"$"}onList, seasonYear: ${"$"}seasonYear, startDate_like: ${"$"}year, startDate_lesser: ${"$"}yearLesser, startDate_greater: ${"$"}yearGreater, episodes_lesser: ${"$"}episodeLesser, episodes_greater: ${"$"}episodeGreater, duration_lesser: ${"$"}durationLesser, duration_greater: ${"$"}durationGreater, chapters_lesser: ${"$"}chapterLesser, chapters_greater: ${"$"}chapterGreater, volumes_lesser: ${"$"}volumeLesser, volumes_greater: ${"$"}volumeGreater, licensedBy_in: ${"$"}licensedBy, isLicensed: ${"$"}isLicensed, genre_in: ${"$"}genres, genre_not_in: ${"$"}excludedGenres, tag_in: ${"$"}tags, tag_not_in: ${"$"}excludedTags, minimumTagRank: ${"$"}minimumTagRank, sort: ${"$"}sort, isAdult: ${"$"}isAdult) {
      id
      isAdult
      status
      chapters
      episodes
      nextAiringEpisode {
        episode
      }
      type
      meanScore
      isFavourite
      bannerImage
      coverImage {
        large
      }
      title {
        english
        romaji
        userPreferred
      }
      mediaListEntry {
        progress
        score(format: POINT_100)
        status
      }
    }
  }
}
        """.replace("\n", " ").replace("""  """, "")
        val variables = """{\"type\":\"$type\",\"isAdult\":$isAdult
            ${if (page != null) """,\"page\":\"$page\"""" else ""}
            ${if (id != null) """,\"id\":\"$id\"""" else ""}
            ${if (search != null) """,\"search\":\"$search\"""" else ""}
            ${if (sort != null) """,\"sort\":\"$sort\"""" else ""}
            ${if (format != null) """,\"format\":\"$format\"""" else ""}
            ${if (genres?.isNotEmpty() == true) """,\"genres\":\"${genres[0]}\"""" else ""}
            ${if (tags?.isNotEmpty() == true) """,\"tags\":\"${tags[0]}\"""" else ""}
            }""".replace("\n", " ").replace("""  """, "")
        val response = executeQuery(query, variables, true)
        if(response!=null){
            val a = if(response["data"]!=JsonNull) response["data"] else null
            val pag = a?.jsonObject?.get("Page") ?:return null
            val responseArray = arrayListOf<Media>()
            pag.jsonObject["media"]!!.jsonArray.forEach { i ->
                responseArray.add(
                    Media(
                        id = i.jsonObject["id"].toString().toInt(),
                        name = i.jsonObject["title"]!!.jsonObject["english"].toString().trim('"').replace("\\\"","\""),
                        nameRomaji = i.jsonObject["title"]!!.jsonObject["romaji"].toString().trim('"').replace("\\\"","\""),
                        userPreferredName = i.jsonObject["title"]!!.jsonObject["userPreferred"].toString().trim('"').replace("\\\"","\""),
                        cover = i.jsonObject["coverImage"]!!.jsonObject["large"].toString().trim('"'),
                        banner = if(i.jsonObject["bannerImage"]!=JsonNull) i.jsonObject["bannerImage"].toString().trim('"') else null,
                        status = i.jsonObject["status"].toString().trim('"'),
                        isAdult = i.jsonObject["isAdult"].toString() == "true",
                        isFav = i.jsonObject["isFavourite"].toString() == "true",
                        userProgress = if (i.jsonObject["mediaListEntry"] != JsonNull) i.jsonObject["mediaListEntry"]!!.jsonObject["progress"].toString().toInt() else null,
                        userScore = if (i.jsonObject["mediaListEntry"] != JsonNull) i.jsonObject["mediaListEntry"]!!.jsonObject["score"].toString().toInt() else 0,
                        userStatus = if (i.jsonObject["mediaListEntry"] != JsonNull) i.jsonObject["mediaListEntry"]!!.jsonObject["status"].toString().trim('"') else null,
                        meanScore = if (i.jsonObject["meanScore"].toString().trim('"') != "null") i.jsonObject["meanScore"].toString().toInt() else null,
                        anime = if (i.jsonObject["type"].toString().trim('"') == "ANIME") Anime(totalEpisodes = if (i.jsonObject["episodes"] != JsonNull) i.jsonObject["episodes"].toString().toInt() else null, nextAiringEpisode = if (i.jsonObject["nextAiringEpisode"] != JsonNull) i.jsonObject["nextAiringEpisode"]!!.jsonObject["episode"].toString().toInt() - 1 else null) else null,
                        manga = if (i.jsonObject["type"].toString().trim('"') == "MANGA") Manga(totalChapters = if (i.jsonObject["chapters"] != JsonNull) i.jsonObject["chapters"].toString().toInt() else null) else null,
                    )
                )
            }
            return SearchResults(
                type = type,
                perPage = perPage,
                search = search,
                sort = sort,
                isAdult = isAdult,
                genres = genres,
                results = responseArray,
                page = pag.jsonObject["pageInfo"]!!.jsonObject["currentPage"].toString().toInt(),
                hasNextPage = pag.jsonObject["pageInfo"]!!.jsonObject["hasNextPage"].toString()=="true",
            )
        } else{
            toastString("Empty Response, Does your internet perhaps suck?")
        }
        return null
    }

    fun recentlyUpdated(): ArrayList<Media>? {
        val query="""{
Page(page:1,perPage:50) {
    pageInfo {
        hasNextPage
        total
    }
    airingSchedules(
        airingAt_greater: 0
        airingAt_lesser: ${System.currentTimeMillis()/1000-10000}
        sort:TIME_DESC
    ) {
        media {
            id
            status
            chapters
            episodes
            nextAiringEpisode { episode }
            isAdult
            type
            meanScore
            isFavourite
            bannerImage
            countryOfOrigin
            coverImage { large }
            title {
                english
                romaji
                userPreferred
            }
            mediaListEntry {
                progress
                score(format: POINT_100)
                status
            }
        }
    }
}
        }""".replace("\n", " ").replace("""  """, "")
        val response = executeQuery(query, force = true)?:return null
        val a = ((response["data"]?:return null).jsonObject["Page"]?:return null).jsonObject["airingSchedules"]?:return null
        val responseArray = arrayListOf<Media>()
        a.jsonArray.forEach {
            val i = it.jsonObject["media"]!!
            if (i.jsonObject["countryOfOrigin"].toString().trim('"')=="JP")
            responseArray.add(
                Media(
                    id = i.jsonObject["id"].toString().toInt(),
                    name = i.jsonObject["title"]!!.jsonObject["english"].toString().trim('"').replace("\\\"","\""),
                    nameRomaji = i.jsonObject["title"]!!.jsonObject["romaji"].toString().trim('"').replace("\\\"","\""),
                    userPreferredName = i.jsonObject["title"]!!.jsonObject["userPreferred"].toString().trim('"').replace("\\\"","\""),
                    cover = i.jsonObject["coverImage"]!!.jsonObject["large"].toString().trim('"'),
                    banner = if(i.jsonObject["bannerImage"]!=JsonNull) i.jsonObject["bannerImage"].toString().trim('"') else null,
                    status = i.jsonObject["status"].toString().trim('"'),
                    isAdult = i.jsonObject["isAdult"].toString() == "true",
                    isFav = i.jsonObject["isFavourite"].toString() == "true",
                    userProgress = if (i.jsonObject["mediaListEntry"] != JsonNull) i.jsonObject["mediaListEntry"]!!.jsonObject["progress"].toString().toInt() else null,
                    userScore = if (i.jsonObject["mediaListEntry"] != JsonNull) i.jsonObject["mediaListEntry"]!!.jsonObject["score"].toString().toInt() else 0,
                    userStatus = if (i.jsonObject["mediaListEntry"] != JsonNull) i.jsonObject["mediaListEntry"]!!.jsonObject["status"].toString().trim('"') else null,
                    meanScore = if (i.jsonObject["meanScore"].toString().trim('"') != "null") i.jsonObject["meanScore"].toString().toInt() else null,
                    anime = Anime(totalEpisodes = if (i.jsonObject["episodes"] != JsonNull) i.jsonObject["episodes"].toString().toInt() else null, nextAiringEpisode = if (i.jsonObject["nextAiringEpisode"] != JsonNull) i.jsonObject["nextAiringEpisode"]!!.jsonObject["episode"].toString().toInt() - 1 else null)
                )
            )
        }
        return responseArray
    }

    fun getCharacterDetails(character: Character):Character{
        val query=""" {
  Character(id: ${character.id}) {
    id
    age
    gender
    description
    dateOfBirth {
      year
      month
      day
    }
    media(page: 0,sort:[POPULARITY_DESC,SCORE_DESC]) {
      pageInfo {
        total
        perPage
        currentPage
        lastPage
        hasNextPage
      }
      edges {
        id
        characterRole
        node {
          id
          isAdult
          status
          chapters
          episodes
          nextAiringEpisode { episode }
          type
          meanScore
          isFavourite
          bannerImage
          countryOfOrigin
          coverImage { large }
          title {
              english
              romaji
              userPreferred
          }
          mediaListEntry {
              progress
              score(format: POINT_100)
              status
          }
        }
      }
    }
  }
}""".replace("\n", " ").replace("""  """, "")
        val response = executeQuery(query, force = true)?:return character
        val char = response["data"]?.jsonObject?.get("Character")?:return character
        character.age = char.jsonObject["age"].toString().trim('"')
        character.gender = char.jsonObject["gender"].toString().trim('"')
        character.description = char.jsonObject["description"].toString().trim('"').replace("\\n","\n").replace("\\\"","\"")
        character.dateOfBirth = FuzzyDate(
            if(char.jsonObject["dateOfBirth"]?.jsonObject?.get("year") !=JsonNull) char.jsonObject["dateOfBirth"]!!.jsonObject["year"].toString().toInt() else null,
            if(char.jsonObject["dateOfBirth"]?.jsonObject?.get("month") !=JsonNull) char.jsonObject["dateOfBirth"]!!.jsonObject["month"].toString().toInt() else null,
            if(char.jsonObject["dateOfBirth"]?.jsonObject?.get("day") !=JsonNull) char.jsonObject["dateOfBirth"]!!.jsonObject["day"].toString().toInt() else null
        )
        character.roles = arrayListOf()
        char.jsonObject["media"]?.jsonObject?.get("edges")?.jsonArray?.forEach { i->
            character.roles!!.add(
                Media(
                    id = i.jsonObject["node"]!!.jsonObject["id"].toString().toInt(),
                    name = i.jsonObject["node"]!!.jsonObject["title"]!!.jsonObject["english"].toString().trim('"').replace("\\\"","\""),
                    nameRomaji = i.jsonObject["node"]!!.jsonObject["title"]!!.jsonObject["romaji"].toString().trim('"').replace("\\\"","\""),
                    userPreferredName = i.jsonObject["node"]!!.jsonObject["title"]!!.jsonObject["userPreferred"].toString().trim('"').replace("\\\"","\""),
                    cover = i.jsonObject["node"]!!.jsonObject["coverImage"]!!.jsonObject["large"].toString().trim('"'),
                    banner = if(i.jsonObject["node"]!!.jsonObject["bannerImage"]!=JsonNull) i.jsonObject["node"]!!.jsonObject["bannerImage"].toString().trim('"') else null,
                    status = i.jsonObject["node"]!!.jsonObject["status"].toString().trim('"'),
                    isAdult = i.jsonObject["node"]!!.jsonObject["isAdult"].toString() == "true",
                    isFav = i.jsonObject["node"]!!.jsonObject["isFavourite"].toString()=="true",
                    userProgress = if (i.jsonObject["node"]!!.jsonObject["mediaListEntry"]!=JsonNull) i.jsonObject["node"]!!.jsonObject["mediaListEntry"]!!.jsonObject["progress"].toString().toInt() else null,
                    userScore = if (i.jsonObject["node"]!!.jsonObject["mediaListEntry"]!=JsonNull) i.jsonObject["node"]!!.jsonObject["mediaListEntry"]!!.jsonObject["score"].toString().toInt() else 0,
                    userStatus = if (i.jsonObject["node"]!!.jsonObject["mediaListEntry"]!=JsonNull) i.jsonObject["node"]!!.jsonObject["mediaListEntry"]!!.jsonObject["status"].toString().trim('"') else null,
                    meanScore = if (i.jsonObject["node"]!!.jsonObject["meanScore"].toString().trim('"')!="null") i.jsonObject["node"]!!.jsonObject["meanScore"].toString().toInt() else null,
                    relation = i.jsonObject["characterRole"].toString().trim('"'),
                    anime = if (i.jsonObject["node"]!!.jsonObject["type"].toString().trim('"')=="ANIME") Anime(totalEpisodes = if (i.jsonObject["node"]!!.jsonObject["episodes"] != JsonNull) i.jsonObject["node"]!!.jsonObject["episodes"].toString().toInt() else null, nextAiringEpisode = if(i.jsonObject["node"]!!.jsonObject["nextAiringEpisode"] != JsonNull) i.jsonObject["node"]!!.jsonObject["nextAiringEpisode"]!!.jsonObject["episode"].toString().toInt()-1 else null) else null,
                    manga = if (i.jsonObject["node"]!!.jsonObject["type"].toString().trim('"')=="MANGA") Manga(totalChapters = if (i.jsonObject["node"]!!.jsonObject["chapters"] != JsonNull) i.jsonObject["node"]!!.jsonObject["chapters"].toString().toInt() else null) else null,
                )
            )
        }
        return character
    }

    fun getStudioDetails(studio: Studio): Studio {
        fun query(page:Int=0) =""" {
  Studio(id: ${studio.id}) {
    media(page: $page,sort:START_DATE_DESC) {
      pageInfo{
        hasNextPage
      }
      edges {
        id
        node {
          id
          isAdult
          status
          chapters
          episodes
          nextAiringEpisode { episode }
          type
          meanScore
          startDate{ year }
          isFavourite
          bannerImage
          countryOfOrigin
          coverImage { large }
          title {
              english
              romaji
              userPreferred
          }
          mediaListEntry {
              progress
              score(format: POINT_100)
              status
          }
        }
      }
    }
  }
}""".replace("\n", " ").replace("""  """, "")
        var hasNextPage=true
        studio.yearMedia = mutableMapOf()
        var page = 0
        while(hasNextPage){
            page++
            val response = executeQuery(query(page), force = true)?:return studio
            val data = response["data"]?.jsonObject?.get("Studio")?.jsonObject?.get("media")?:return studio
            hasNextPage = data.jsonObject["pageInfo"]?.jsonObject?.get("hasNextPage")?.toString() == "true"
            data.jsonObject["edges"]?.jsonArray?.forEach { i->
                val id = i.jsonObject["node"]!!.jsonObject["id"].toString().toInt()
                val status = i.jsonObject["node"]!!.jsonObject["status"].toString().trim('"')
                val year = i.jsonObject["node"]!!.jsonObject["startDate"]!!.jsonObject["year"].toString()
                val title = if(status!="CANCELLED") (if(year!="null") year else "TBA") else status
                if(!studio.yearMedia!!.containsKey(title))
                    studio.yearMedia!![title] = arrayListOf()
                studio.yearMedia!![title]!!.add(
                    Media(
                        id = id,
                        name = i.jsonObject["node"]!!.jsonObject["title"]!!.jsonObject["english"].toString().trim('"').replace("\\\"","\""),
                        nameRomaji = i.jsonObject["node"]!!.jsonObject["title"]!!.jsonObject["romaji"].toString().trim('"').replace("\\\"","\""),
                        userPreferredName = i.jsonObject["node"]!!.jsonObject["title"]!!.jsonObject["userPreferred"].toString().trim('"').replace("\\\"","\""),
                        cover = i.jsonObject["node"]!!.jsonObject["coverImage"]!!.jsonObject["large"].toString().trim('"'),
                        banner = if(i.jsonObject["node"]!!.jsonObject["bannerImage"]!=JsonNull) i.jsonObject["node"]!!.jsonObject["bannerImage"].toString().trim('"') else null,
                        status = i.jsonObject["node"]!!.jsonObject["status"].toString().trim('"'),
                        isAdult = i.jsonObject["node"]!!.jsonObject["isAdult"].toString() == "true",
                        isFav = i.jsonObject["node"]!!.jsonObject["isFavourite"].toString()=="true",
                        userProgress = if (i.jsonObject["node"]!!.jsonObject["mediaListEntry"]!=JsonNull) i.jsonObject["node"]!!.jsonObject["mediaListEntry"]!!.jsonObject["progress"].toString().toInt() else null,
                        userScore = if (i.jsonObject["node"]!!.jsonObject["mediaListEntry"]!=JsonNull) i.jsonObject["node"]!!.jsonObject["mediaListEntry"]!!.jsonObject["score"].toString().toInt() else 0,
                        userStatus = if (i.jsonObject["node"]!!.jsonObject["mediaListEntry"]!=JsonNull) i.jsonObject["node"]!!.jsonObject["mediaListEntry"]!!.jsonObject["status"].toString().trim('"') else null,
                        meanScore = if (i.jsonObject["node"]!!.jsonObject["meanScore"].toString().trim('"')!="null") i.jsonObject["node"]!!.jsonObject["meanScore"].toString().toInt() else null,
                        anime = if (i.jsonObject["node"]!!.jsonObject["type"].toString().trim('"')=="ANIME") Anime(totalEpisodes = if (i.jsonObject["node"]!!.jsonObject["episodes"] != JsonNull) i.jsonObject["node"]!!.jsonObject["episodes"].toString().toInt() else null, nextAiringEpisode = if(i.jsonObject["node"]!!.jsonObject["nextAiringEpisode"] != JsonNull) i.jsonObject["node"]!!.jsonObject["nextAiringEpisode"]!!.jsonObject["episode"].toString().toInt()-1 else null) else null,
                        manga = if (i.jsonObject["node"]!!.jsonObject["type"].toString().trim('"')=="MANGA") Manga(totalChapters = if (i.jsonObject["node"]!!.jsonObject["chapters"] != JsonNull) i.jsonObject["node"]!!.jsonObject["chapters"].toString().toInt() else null) else null,
                    )
                )
            }
        }
        if(studio.yearMedia!!.contains("CANCELLED")){
            val a =  studio.yearMedia!!["CANCELLED"]!!
            studio.yearMedia!!.remove("CANCELLED")
            studio.yearMedia!!["CANCELLED"] = a
        }
        return studio
    }
}

