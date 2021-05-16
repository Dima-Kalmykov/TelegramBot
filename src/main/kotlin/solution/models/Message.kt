package solution.models

data class Message(
    val message_id: Int,
    val from: User,
    val chat: Chat,
    val text: String?
)