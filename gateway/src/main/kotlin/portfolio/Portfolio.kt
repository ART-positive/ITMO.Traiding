package com.trading.portfolio

import com.trading.database.DataBaseManager
import java.math.BigDecimal

data class PortfolioItem(
    val portfolioId: Int,
    val userId: Int,
    val quoteName: String,
    val quantity: Int,
    val averagePrice: BigDecimal,
    val createdAt: java.sql.Timestamp,
    val updatedAt: java.sql.Timestamp
)

object PortfolioRepository {
    private val db = DataBaseManager()

    init {
        db.connect()
    }

    fun getPortfolio(userId: Int): List<PortfolioItem> {
        val sql = """
            SELECT 
                portfolio_id,
                user_id,
                quote_name,
                quantity,
                average_price,
                created_at,
                updated_at
            FROM portfolio
            WHERE user_id = ?
            ORDER BY quote_name
        """.trimIndent()

        return try {
            val rs = db.query(sql, userId)
            val items = mutableListOf<PortfolioItem>()
            while (rs.next()) {
                items.add(
                    PortfolioItem(
                        portfolioId = rs.getInt("portfolio_id"),
                        userId = rs.getInt("user_id"),
                        quoteName = rs.getString("quote_name"),
                        quantity = rs.getInt("quantity"),
                        averagePrice = rs.getBigDecimal("average_price"),
                        createdAt = rs.getTimestamp("created_at"),
                        updatedAt = rs.getTimestamp("updated_at")
                    )
                )
            }
            rs.close()
            items
        } catch (e: Exception) {
            println("Ошибка при получении портфеля: ${e.message}")
            emptyList()
        }
    }

    fun addOrUpdatePortfolioItem(userId: Int, quoteName: String, quantity: Int, price: BigDecimal): Boolean {
        return try {
            val existingItem = getPortfolioItem(userId, quoteName)
            
            if (existingItem != null) {
                val newQuantity = existingItem.quantity + quantity
                val totalValue = existingItem.averagePrice.multiply(BigDecimal.valueOf(existingItem.quantity.toLong()))
                    .add(price.multiply(BigDecimal.valueOf(quantity.toLong())))
                val newAveragePrice = if (newQuantity > 0) totalValue.divide(BigDecimal.valueOf(newQuantity.toLong()), 2, BigDecimal.ROUND_HALF_UP) else BigDecimal.ZERO
                
                val updateSql = """
                    UPDATE portfolio 
                    SET quantity = ?, average_price = ?, updated_at = CURRENT_TIMESTAMP
                    WHERE user_id = ? AND quote_name = ?
                """.trimIndent()
                
                db.execute(updateSql, newQuantity, newAveragePrice, userId, quoteName)
            } else {
                val insertSql = """
                    INSERT INTO portfolio (user_id, quote_name, quantity, average_price)
                    VALUES (?, ?, ?, ?)
                """.trimIndent()
                
                db.execute(insertSql, userId, quoteName, quantity, price)
            }
            
            true
        } catch (e: Exception) {
            println("Ошибка при обновлении портфеля: ${e.message}")
            false
        }
    }

    fun removePortfolioItem(userId: Int, quoteName: String): Boolean {
        return try {
            val sql = "DELETE FROM portfolio WHERE user_id = ? AND quote_name = ?"
            db.execute(sql, userId, quoteName)
            true
        } catch (e: Exception) {
            println("Ошибка при удалении элемента портфеля: ${e.message}")
            false
        }
    }

    fun getUserIdByUsername(username: String): Int? {
        return try {
            val rs = db.query("SELECT user_id FROM users WHERE username = ?", username)
            val userId = if (rs.next()) rs.getInt("user_id") else null
            rs.close()
            userId
        } catch (e: Exception) {
            println("Ошибка при получении ID пользователя: ${e.message}")
            null
        }
    }
    
    private fun getPortfolioItem(userId: Int, quoteName: String): PortfolioItem? {
        val sql = """
            SELECT 
                portfolio_id,
                user_id,
                quote_name,
                quantity,
                average_price,
                created_at,
                updated_at
            FROM portfolio
            WHERE user_id = ? AND quote_name = ?
        """.trimIndent()

        return try {
            val rs = db.query(sql, userId, quoteName)
            val item = if (rs.next()) {
                PortfolioItem(
                    portfolioId = rs.getInt("portfolio_id"),
                    userId = rs.getInt("user_id"),
                    quoteName = rs.getString("quote_name"),
                    quantity = rs.getInt("quantity"),
                    averagePrice = rs.getBigDecimal("average_price"),
                    createdAt = rs.getTimestamp("created_at"),
                    updatedAt = rs.getTimestamp("updated_at")
                )
            } else null
            rs.close()
            item
        } catch (e: Exception) {
            println("Ошибка при получении элемента портфеля: ${e.message}")
            null
        }
    }
}
