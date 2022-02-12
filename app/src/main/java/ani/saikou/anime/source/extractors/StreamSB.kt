package ani.saikou.anime.source.extractors

import ani.saikou.anime.Episode
import ani.saikou.anime.source.Extractor
import ani.saikou.findBetween
import ani.saikou.toastString
import com.google.android.gms.common.util.Hex
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.jsoup.Jsoup

class StreamSB: Extractor(){
    override fun getStreamLinks(name: String, url: String): Episode.StreamLinks {

        //thanks to KR (https://bit.ly/321WIq9)

        return try{
            val jsonLink = "https://sbplay2.com/sourcesx38/7361696b6f757c7c${Hex.bytesToStringLowercase((url.findBetween("/e/",".html")?:url.split("/e/")[1]).encodeToByteArray())}7c7c7361696b6f757c7c73747265616d7362/7361696b6f757c7c363136653639366436343663363136653639366436343663376337633631366536393664363436633631366536393664363436633763376336313665363936643634366336313665363936643634366337633763373337343732363536313664373336327c7c7361696b6f757c7c73747265616d7362"
            val json = Json.decodeFromString<JsonObject>(Jsoup.connect(jsonLink).ignoreContentType(true).header("watchsb","streamsb").execute().body())
            val m3u8 = json["stream_data"]!!.jsonObject["file"].toString().trim('"')
            Episode.StreamLinks(
                name,
                listOf(Episode.Quality(m3u8, "Multi Quality", null)),
                mutableMapOf("referer" to url)
            )
        }catch (e:Exception){
            toastString(e.toString())
            Episode.StreamLinks("", listOf(Episode.Quality("", "", null)),null)
        }
    }
}
