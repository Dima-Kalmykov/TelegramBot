package solution.dao

import solution.utils.Converter
import solution.utils.States
import java.sql.DriverManager
import java.sql.SQLException

class Dao(
    private var tableName: String,
    private val converter: Converter = Converter()
) {

    private val connection = DriverManager.getConnection(
        System.getenv("DB_URL"),
        System.getenv("DB_USER"),
        System.getenv("DB_PASS")
    )

    /**
     * Create initial table.
     */
    fun createTable() {
        try {
            val statement = connection.createStatement()
            statement.execute(
                """CREATE TABLE IF NOT EXISTS $tableName(
                    | id SERIAL PRIMARY KEY,
                    | name VARCHAR(50),
                    | state VARCHAR(50)
                    |);""".trimMargin()
            )
            println("Table \"$tableName\" successfully created!")
        } catch (exception: SQLException) {
            System.err.println(
                "Can't create \"$tableName\" table." +
                        " Reason: ${exception.message}"
            )
        }
    }

    /**
     * Close connection
     */
    fun close() {
        try {
            connection.close()
            println("Connection successfully closed!")
        } catch (exception: SQLException) {
            System.err.println(
                "Can't close connection." +
                        " Reason: ${exception.message}"
            )
        }
    }

    /**
     * Get current state for user.
     */
    fun getState(username: String): States {
        try {
            val statement = connection.prepareStatement(
                "SELECT state FROM $tableName WHERE name = ?"
            )
            statement.setString(1, username)
            val result = statement.executeQuery()
            result.next()

            val state = result.getString("state")

            return converter.stringToState(state)
        } catch (ex: SQLException) {
            System.err.println(
                "Can't get state for user \"$username\"." +
                        " Reason: ${ex.message}"
            )
        }

        return States.INVALID
    }

    /**
     * Set state for user.
     */
    fun setState(username: String, state: States) {
        try {
            val statement = connection.prepareStatement(
                "UPDATE $tableName SET state = ? WHERE name = ?"
            )
            val userState = converter.stateToString(state)

            statement.setString(1, userState)
            statement.setString(2, username)
            statement.executeUpdate()

        } catch (exception: SQLException) {
            System.err.println(
                "Can't set state for user \"$username\"." +
                        " Reason: ${exception.message}"
            )
        }
    }

    /**
     * Check if user with given name is exist.
     */
    fun isExistUser(username: String): Boolean {
        return try {
            val statement = connection.prepareStatement(
                "SELECT name FROM $tableName WHERE name = ?"
            )
            statement.setString(1, username)
            val result = statement.executeQuery()

            result.next()
        } catch (exception: SQLException) {
            System.err.println(
                "Can't check user \"$username\" for existing." +
                        " Reason: ${exception.message}"
            )
            false
        }
    }

    /**
     * Add user to database.
     */
    fun addUser(username: String) {
        try {
            val statement = connection.prepareStatement(
                "INSERT INTO $tableName (name, state) VALUES (?, '')"
            )
            statement.setString(1, username)
            statement.executeUpdate()

            println("User \"$username\" successfully added!")
        } catch (exception: SQLException) {
            System.err.println(
                "Can't insert user into \"$tableName\"." +
                        " Reason: ${exception.message}"
            )
        }
    }
}