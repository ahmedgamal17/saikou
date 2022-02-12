package ani.saikou.anime.source.extractors

import ani.saikou.anime.Episode
import ani.saikou.anime.source.Extractor
import ani.saikou.findBetween
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.jsoup.Jsoup

class RapidCloud : Extractor() {
    override fun getStreamLinks(name: String, url: String): Episode.StreamLinks {
        val jsonLink = "https://rapid-cloud.ru/ajax/embed-6/getSources?id=${ url.findBetween("/embed-6/", "?z=")!! }"
        val json = Json.decodeFromString<JsonObject>(Jsoup.connect(jsonLink).ignoreContentType(true).execute().body())
        val m3u8 = json["sources"]!!.jsonArray[0].jsonObject["file"].toString().trim('"')
        val subtitle = mutableMapOf<String,String>()
        json["tracks"]!!.jsonArray.forEach {
            if(it.jsonObject["kind"].toString().trim('"')=="captions")
                subtitle[it.jsonObject["label"].toString().trim('"')] = it.jsonObject["file"].toString().trim('"')
        }
        return Episode.StreamLinks(
            name,
            listOf(Episode.Quality(m3u8, "Multi Quality", null)),
            null,
            subtitle
        )
    }
}