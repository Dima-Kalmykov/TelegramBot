package solution.bot

import com.mashape.unirest.http.Unirest
import org.apache.http.client.utils.URIBuilder
import org.json.JSONArray
import solution.dao.Dao
import solution.models.Update
import solution.utils.Converter
import java.net.URI


open class BotApi {

    private val converter = Converter()
    protected val dao = Dao("users")

    /**
     *  Get updates.
     */
    fun getUpdates(offset: Int? = null, timeout: Int? = null): List<Update> {
        val uri = getUri(offset, timeout)
        val response = Unirest.get(uri.toString()).asJson()
        val jsonContent: JSONArray = response.body.`object`.getJSONArray("result")

        return converter.jsonToUpdateList(jsonContent)
    }

    /**
     * Send message to chat.
     */
    fun sendMessage(chatId: Long, text: String) {
        Unirest.post("${BotInfo.SERVER_URL}${BotInfo.BOT_TOKEN}/sendMessage")
            .field("chat_id", chatId)
            .field("text", text)
            .asJson()
    }

    /**
     * Get uri for getting updates.
     */
    private fun getUri(offset: Int? = null, timeout: Int? = null): URI {
        val uri = URI("${BotInfo.SERVER_URL}${BotInfo.BOT_TOKEN}/getUpdates")

        if (offset != null && timeout != null) {
            return URIBuilder(uri)
                .addParameter("offset", offset.toString())
                .addParameter("timeout", timeout.toString()).build()
        } else if (offset != null && timeout == null) {
            return URIBuilder(uri)
                .addParameter("offset", offset.toString()).build()
        } else if (offset == null && timeout != null) {
            return URIBuilder(uri)
                .addParameter("timeout", timeout.toString()).build()
        }

        return uri
    }
}
