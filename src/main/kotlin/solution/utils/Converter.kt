package solution.utils

import org.json.JSONArray
import org.json.JSONObject
import solution.models.Chat
import solution.models.Message
import solution.models.Update
import solution.models.User

class Converter {

    /**
     * Convert json to list of updates.
     */
    fun jsonToUpdateList(json: JSONArray): List<Update> {
        val result: MutableList<Update> = mutableListOf()

        for (i in 0 until json.length()) {
            val upd = json.getJSONObject(i)
            result += jsonToUpdate(upd)
        }

        return result
    }

    /**
     * Convert string to state enum.
     */
    fun stringToState(state: String): States {
        return when (state) {
            "Wait date for setting" -> States.WAIT_DATE_SETTING
            "Wait message for setting" -> States.WAIT_MESSAGE_SETTING
            "Wait date for cancelling" -> States.WAIT_DATE_CANCELING
            "" -> States.EMPTY
            else -> States.INVALID
        }
    }

    /**
     * Convert state to string.
     */
    fun stateToString(state: States): String {
        return when (state) {
            States.WAIT_DATE_SETTING -> "Wait date for setting"
            States.WAIT_MESSAGE_SETTING -> "Wait message for setting"
            States.WAIT_DATE_CANCELING -> "Wait date for cancelling"
            else -> ""
        }
    }

    /**
     * Convert json to update.
     */
    private fun jsonToUpdate(json: JSONObject): Update {
        val updateId = json.getInt("update_id")
        val message: Message = jsonToMessage(json.getJSONObject("message"))

        return Update(updateId, message)
    }

    /**
     * Convert json to chat.
     */
    private fun jsonToChat(json: JSONObject): Chat {
        val chatId = json.getLong("id")
        return Chat(chatId)
    }

    /**
     * Convert json to message.
     */
    private fun jsonToMessage(json: JSONObject): Message {
        val messageId = json.getInt("message_id")
        val chat = jsonToChat(json.getJSONObject("chat"))
        val text = json.getString("text")
        val user = jsonToUser(json.getJSONObject("from"))

        return Message(messageId, user, chat, text)
    }

    /**
     * Convert json to user.
     */
    private fun jsonToUser(json: JSONObject): User {
        val firstName = json.getString("first_name")
        val id = json.getInt("id")
        val username = try {
            json.getString("username")
        } catch (ex: Exception) {
            "$id"
        }
        return User(id, firstName, username)
    }
}