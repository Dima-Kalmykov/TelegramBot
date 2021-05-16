package solution.utils

/**
 * States of user chat.
 */
enum class States {
    EMPTY,
    INVALID,
    WAIT_DATE_SETTING,
    WAIT_MESSAGE_SETTING,
    WAIT_DATE_CANCELING,
}