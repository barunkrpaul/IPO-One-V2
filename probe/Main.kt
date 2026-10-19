import com.example.data.model.*
import org.json.JSONArray
import java.io.File

fun main(args: Array<String>) {
    for (path in args) {
        val text = IpoPageParser.unescape(File(path).readText())
        val lms = IpoPageParser.extractJsonArray(text, "ipoLeadManagersList")?.let { JSONArray(it) }
            ?.let { a -> (0 until a.length()).map { a.getJSONObject(it).getString("comp_name") } }
        val reg = IpoPageParser.extractJsonArray(text, "registrarInfo")?.let { JSONArray(it).optJSONObject(0) }
        val bid = IpoPageParser.extractJsonArray(text, "ipoBiddingData")?.let { JSONArray(it) }?.let { it.getJSONObject(it.length() - 1) }
        val values = bid?.let { o -> o.keys().asSequence().associateWith { o.opt(it).toString() } }.orEmpty()
        val cap = IpoPageParser.extractValue(text, "cap_price")?.toDoubleOrNull()
        val sub = IpoPageParser.subscriptionFromBidding(values, cap)
        val out = listOf(
            "lms=$lms",
            "registrar=${reg?.optString("registrar_name")} site=${IpoPageParser.firstLink(reg?.optString("registrar_basic_info"))}",
            "mm=${IpoPageParser.extractMarketMaker(text)}",
            "symbol=${IpoPageParser.extractValue(text, "nse_symbol")} logo=${IpoPageParser.extractValue(text, "logo_url")} floor=${IpoPageParser.extractValue(text, "floor_price")} cap=$cap",
            "settle=${IpoPageParser.extractIsoDate(text, "timetable_share_credit_dt")} a50=${IpoPageParser.extractIsoDate(text, "timetable_anchor_lockin_end_dt_1")} a100=${IpoPageParser.extractIsoDate(text, "timetable_anchor_lockin_end_dt_2")}",
            "sub=" + sub.entries.joinToString { "${it.key}:${it.value.times}/${"%.1f".format(it.value.sharesOfferedCr ?: -1.0)}/${it.value.sharesAppliedCr}" }
        )
        println("::notice::$path " + out.joinToString(" | "))
    }
}
