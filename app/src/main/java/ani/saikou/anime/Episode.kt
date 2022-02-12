package ani.saikou.anime

import java.io.Serializable

data class Episode (
    val number: String,
    var title : String?=null,
    var desc : String?=null,
    var thumb : String?=null,
    var filler : Boolean = false,
    var link : String? = null,
    var selectedStream : String?=null,
    var selectedQuality: Int = 0,
    var streamLinks : MutableMap<String,StreamLinks?> = mutableMapOf(),
    var watched: Long?=null,
    var maxLength: Long?=null,
):Serializable{
    data class Quality(
        val url: String,
        val quality: String,
        val size: Double?
    ):Serializable

    data class StreamLinks(
        val server: String,
        val quality: List<Quality>,
        val headers: MutableMap<String,String>?=null,
        val subtitles : MutableMap<String,String>?=null
    ):Serializable
}


