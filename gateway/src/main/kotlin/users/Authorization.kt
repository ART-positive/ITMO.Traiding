package com.trading.users

import com.trading.database.DataBaseManager
import org.mindrot.jbcrypt.BCrypt


class Authorization {
    private val db: DataBaseManager = DataBaseManager()

    init {
        db.connect()
    }

    fun authorization(login: String, password: String): Int? {
        // Получаем пользователя из БД
        return try {
            val rs = db.query("SELECT user_id, password_hash FROM users WHERE username = ? LIMIT 1", login)
            if (rs.next()) {
                val userId = rs.getInt("user_id")
                val storedHash = rs.getString("password_hash")
                rs.close()
                
                // Сравниваем введенный пароль с хешем в БД
                if (BCrypt.checkpw(password, storedHash)) {
                    println("Пользователь $login успешно авторизован, ID: $userId")
                    return userId
                }
            }
            rs.close()
            println("Неверный логин или пароль для: $login")
            null
        } catch (e: Exception) {
            println("Ошибка при проверке пользователя: ${e.message}")
            null
        }
    }
    
    fun getUserById(userId: Int): String? {
        return try {
            val rs = db.query("SELECT username FROM users WHERE user_id = ? LIMIT 1", userId)
            val username = if (rs.next()) rs.getString("username") else null
            rs.close()
            username
        } catch (e: Exception) {
            println("Ошибка при получении пользователя по ID: ${e.message}")
            null
        }
    }
}