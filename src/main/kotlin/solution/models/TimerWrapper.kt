package solution.models

import java.time.LocalDateTime
import java.util.*

data class TimerWrapper(
    val timer: Timer,
    val username: String,
    var date: LocalDateTime,
    var canceled: Boolean,
    var isWorked: Boolean,
    var message: String,

)