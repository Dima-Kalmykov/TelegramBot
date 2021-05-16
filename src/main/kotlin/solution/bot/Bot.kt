package solution.bot

import solution.models.Message
import solution.models.TimerWrapper
import solution.utils.States
import solution.utils.Utils
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


class Bot(
    private var updateId: Int = -1,
    private val timeout: Int = 30,
    private val filePathToUpdateId: String = "data.ser",
    private var timers: MutableList<TimerWrapper> = mutableListOf()
) : BotApi() {

    /**
     * Start bot.
     */
    fun run() {
        dao.createTable()
        loadLastHandledUpdateId()

        while (true) {
            try {
                val updates = getUpdates(updateId, timeout)
                for (update in updates) {
                    updateId = update.update_id + 1
                    val message = update.message
                    val username = message.from.username

                    if (dao.isExistUser(username)) {
                        replyMessage(message)
                    } else {
                        dao.addUser(username)
                        Thread {
                            replyMessage(message)
                        }.start()
                    }
                }
            } catch (exception: Exception) {
                System.err.println("Something went wrong. Reason: ${exception.message}")
                saveLastHandledUpdateId()
            }
        }
    }

    /**
     * Extract period from message.
     */
    private fun extractPeriod(text: String): String {
        val flagIndex = text.lastIndexOf(BotInfo.PERIOD_FLAG)

        if (flagIndex == -1) {
            return ""
        }

        return text.substring(flagIndex + BotInfo.PERIOD_FLAG.length + 1)
    }

    /**
     * Reply to user message.
     */
    private fun replyMessage(message: Message) {
        val text = message.text
        val chatId = message.chat.id
        val username = message.from.username
        val state = dao.getState(username)
        if (text != null) {

            if (text == "Set" && state == States.EMPTY) {
                processSetCommand(username, chatId)
                return
            }

            if (text == "Cancel" && state == States.EMPTY) {
                processCancelCommand(username, chatId)
                return
            }

            if (text == "List" && state == States.EMPTY) {
                processListCommand(message, chatId)
                return
            }

            if (state == States.WAIT_DATE_CANCELING) {
                processWaitDateCancellingState(text, username, chatId)
                return
            }

            if (state == States.WAIT_DATE_SETTING) {
                processWaitDateSettingState(text, username, chatId)
                return
            }

            if (state == States.WAIT_MESSAGE_SETTING) {
                processWaitMessageSettingState(text, username, chatId)
                return
            }

            processDefault(chatId)
        }
    }

    private fun processWaitDateSettingState(text: String, username: String, chatId: Long) {
        if (text == "Cancel") {
            dao.setState(username, States.EMPTY)
            sendMessage(chatId, "Notification canceled")
            return
        }
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.ENGLISH)
        try {
            val date = LocalDateTime.parse(text, formatter)
            val timer = Timer()
            if (timers.any {
                    it.username == username && Utils.dateAreEqual(date, it.date)
                }) {
                sendMessage(chatId, "Time has already taken, enter again")
                return
            }
            val timerWrapper = TimerWrapper(
                timer, username, date,
                canceled = false, isWorked = false, ""
            )
            timers.add(timerWrapper)
            dao.setState(username, States.WAIT_MESSAGE_SETTING)
            sendMessage(chatId, "Enter message for notification")
        } catch (ex: Exception) {
            sendMessage(chatId, "Invalid date, try again. Format: dd.mm.yyyy hh:mm")
        }
    }

    private fun processListCommand(message: Message, chatId: Long) {
        val filtered = getTimersForUser(message.from.username)
        var resultMessage = "List of your notifications:\n\n"
        val notifications = mutableListOf<String>()

        for (suitTimer in filtered) {
            val formatter = DateTimeFormatter.ISO_DATE_TIME
            val date = suitTimer.date
            notifications.add(
                date.format(formatter).replace('T', ' ')
                        + ", with message \"${suitTimer.message}\""
            )
        }

        notifications.sort()

        for (word in notifications) {
            resultMessage += "$word\n"
        }

        sendMessage(chatId, resultMessage)
    }

    /**
     * Stop and delete timer from list.
     */
    private fun cancelTimer(timerWrapper: TimerWrapper) {
        timerWrapper.timer.cancel()
        timerWrapper.canceled = true
        timerWrapper.isWorked = false
        timerWrapper.timer.purge()
        timers.removeAll { it.canceled }
    }

    private fun processWaitDateCancellingState(text: String, username: String, chatId: Long) {
        if (text == "Stop") {
            dao.setState(username, States.EMPTY)
            sendMessage(chatId, "Cancelling stopped")
            return
        }

        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale.ENGLISH)
        try {
            val date = LocalDateTime.parse(text, formatter)

            var canceled = false
            for (timerWrapper in timers) {
                if (Utils.dateAreEqual(timerWrapper.date, date) &&
                    timerWrapper.username == username
                ) {
                    cancelTimer(timerWrapper)
                    sendMessage(chatId, "Notification canceled")
                    canceled = true
                    break
                }
            }

            if (!canceled) {
                sendMessage(chatId, "There are no notification with given date, try again")
            } else {
                dao.setState(username, States.EMPTY)
            }
        } catch (ex: Exception) {
            sendMessage(chatId, "Invalid date, try again. Format: dd.mm.yyyy hh:mm")
        }
    }

    private fun processWaitMessageSettingState(text: String, username: String, chatId: Long) {
        for (timerWrapper in timers) {
            if (timerWrapper.username == username &&
                !timerWrapper.canceled &&
                !timerWrapper.isWorked
            ) {
                val date = timerWrapper.date
                val calendar = Calendar.getInstance()
                calendar.set(
                    date.year, date.monthValue - 1, date.dayOfMonth,
                    date.hour, date.minute, 0
                )
                val period = extractPeriod(text)
                if (period != "") {
                    setNotificationWithPeriod(timerWrapper, calendar, chatId, text, username, period)
                } else {
                    setNotificationWithoutPeriod(timerWrapper, calendar, chatId, text, username)
                }
                return
            }
        }
    }

    /**
     * Set notification for user without period.
     */
    private fun setNotificationWithoutPeriod(
        timerWrapper: TimerWrapper, calendar: Calendar, chatId: Long,
        text: String, username: String
    ) {
        timerWrapper.timer.schedule(object : TimerTask() {
            override fun run() {
                sendMessage(chatId, text)
                cancelTimer(timerWrapper)
            }
        }, calendar.time)
        timerWrapper.message = text
        timerWrapper.isWorked = true
        dao.setState(username, States.EMPTY)
        sendMessage(chatId, "Notification was set without period!")
    }

    /**
     * Set notification for user with period.
     */
    private fun setNotificationWithPeriod(
        timerWrapper: TimerWrapper, calendar: Calendar,
        chatId: Long, text: String, username: String, period: String
    ) {
        if (!Utils.isValidPeriod(period)) {
            sendMessage(chatId, "Invalid period, try again. Format: mm dd")
            return
        }

        val seconds = Utils.getSeconds(period)
        val textWithoutPeriod = text.substring(0, text.lastIndexOf(BotInfo.PERIOD_FLAG)).trim()
        timerWrapper.timer.schedule(object : TimerTask() {
            override fun run() {
                sendMessage(chatId, textWithoutPeriod)
                timerWrapper.date = timerWrapper.date.plusSeconds(seconds)
            }
        }, calendar.time, seconds)
        timerWrapper.isWorked = true
        timerWrapper.message = textWithoutPeriod
        dao.setState(username, States.EMPTY)
        sendMessage(chatId, "Notification was set with period!")
    }

    private fun processSetCommand(username: String, chatId: Long) {
        dao.setState(username, States.WAIT_DATE_SETTING)
        sendMessage(chatId, "Enter date to set notification")
    }

    private fun processCancelCommand(username: String, chatId: Long) {
        dao.setState(username, States.WAIT_DATE_CANCELING)
        sendMessage(chatId, "Enter date to cancel notification")
    }

    // Todo change menu
    private fun processDefault(chatId: Long) {
        sendMessage(
            chatId, """You have next options:
            |
            |'Set' - set notification (you can cancel it at any moment by command 'Cancel')
            |
            |'Cancel' - cancel action
            |
            |'List' - get list of your notifications
            |
            |To cancel existing notification:
            |   1) Type 'Cancel' (you can stop cancelling at any moment by command 'Stop')
            |   2) Type date of notification in format "dd.mm.yy hh:mm"
            |   
            |To set notification:
            |   1) Type 'Set'
            |   2) Type date of notification in format (dd.mm.yy hh:mm)
            |   3) Type message for notification (if you want period, 
            |   then add flag "${BotInfo.PERIOD_FLAG}" to the end of message 
            |   and type period in format "mm dd" (months, days) 
        """.trimMargin()
        )
    }

    /**
     * Load last updated id after fail.
     */
    private fun loadLastHandledUpdateId() {
        updateId = Integer.parseInt(File(filePathToUpdateId).readText())
    }

    /**
     * Save last updated id after fail.
     */
    private fun saveLastHandledUpdateId() {
        File(filePathToUpdateId).writeText(updateId.toString())
    }

    /**
     * Get list of timers for user.
     */
    private fun getTimersForUser(username: String): List<TimerWrapper> {
        return timers.filter { it.username == username }
    }
}