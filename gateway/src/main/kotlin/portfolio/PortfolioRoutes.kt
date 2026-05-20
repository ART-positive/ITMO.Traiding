package com.trading.portfolio

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.http.*
import java.math.BigDecimal

fun Application.configurePortfolioRoutes() {
    routing {
        // GET /portfolio - отображение портфеля пользователя
        get("/portfolio") {
            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Мой портфель</title>
                    <style>
                        body { font-family: Arial; margin: 50px; }
                        table { border-collapse: collapse; width: 100%; }
                        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
                        th { background-color: #4CAF50; color: white; }
                        tr:nth-child(even) { background-color: #f2f2f2; }
                        .error { color: red; }
                        .success { color: green; }
                        input { margin: 5px; padding: 8px; }
                        button { padding: 8px 20px; margin-top: 10px; cursor: pointer; }
                    </style>
                </head>
                <body>
                    <h2>Мой портфель</h2>
                    <div id="portfolio">Загрузка...</div>
                    
                    <h3>Добавить/Изменить позицию</h3>
                    <form id="addForm">
                        <input type="text" id="quoteName" placeholder="Название котировки" required><br>
                        <input type="number" id="quantity" placeholder="Количество" required><br>
                        <input type="number" id="price" placeholder="Цена" step="0.01" required><br>
                        <button type="submit">Добавить/Обновить</button>
                    </form>
                    <div id="message"></div>
                    
                    <script>
                        async function loadPortfolio() {
                            const response = await fetch('/api/portfolio');
                            if (response.ok) {
                                const html = await response.text();
                                document.getElementById('portfolio').innerHTML = html;
                            } else {
                                document.getElementById('portfolio').innerHTML = '<p class="error">Ошибка загрузки портфеля</p>';
                            }
                        }
                        
                        document.getElementById('addForm').onsubmit = async (e) => {
                            e.preventDefault();
                            const quoteName = document.getElementById('quoteName').value;
                            const quantity = document.getElementById('quantity').value;
                            const price = document.getElementById('price').value;
                            const msgDiv = document.getElementById('message');
                            
                            const response = await fetch('/api/portfolio', {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                                body: 'quoteName=' + encodeURIComponent(quoteName) + 
                                      '&quantity=' + encodeURIComponent(quantity) + 
                                      '&price=' + encodeURIComponent(price)
                            });
                            
                            const message = await response.text();
                            
                            if (response.ok) {
                                msgDiv.className = 'success';
                                msgDiv.innerHTML = message;
                                loadPortfolio();
                                document.getElementById('addForm').reset();
                            } else {
                                msgDiv.className = 'error';
                                msgDiv.innerHTML = message;
                            }
                        };
                        
                        // Загрузить портфель при открытии страницы
                        loadPortfolio();
                    </script>
                </body>
                </html>
            """.trimIndent()
            call.respondText(html, ContentType.Text.Html)
        }

        // GET /api/portfolio - API для получения портфеля (возвращает HTML таблицу)
        get("/api/portfolio") {
            // Временная заглушка - в реальности нужно получать userId из сессии
            val userId = 1 // TODO: получить из сессии/аутентификации
            
            val items = PortfolioRepository.getPortfolio(userId)
            
            if (items.isEmpty()) {
                call.respondText("<p>Портфель пуст</p>", ContentType.Text.Html)
            } else {
                val html = buildString {
                    append("<table>")
                    append("<tr><th>Название</th><th>Количество</th><th>Средняя цена</th><th>Стоимость</th></tr>")
                    var totalValue = 0.0
                    for (item in items) {
                        val itemValue = item.averagePrice.toDouble() * item.quantity
                        totalValue += itemValue
                        append("<tr>")
                        append("<td>${item.quoteName}</td>")
                        append("<td>${item.quantity}</td>")
                        append("<td>${String.format("%.2f", item.averagePrice.toDouble())}</td>")
                        append("<td>${String.format("%.2f", itemValue)}</td>")
                        append("</tr>")
                    }
                    append("<tr><td colspan='3'><strong>Итого:</strong></td><td><strong>${String.format("%.2f", totalValue)}</strong></td></tr>")
                    append("</table>")
                }
                call.respondText(html, ContentType.Text.Html)
            }
        }

        // POST /api/portfolio - добавить/обновить позицию в портфеле
        post("/api/portfolio") {
            val params = call.receiveParameters()
            val quoteName = params["quoteName"]
            val quantityStr = params["quantity"]
            val priceStr = params["price"]
            
            if (quoteName.isNullOrBlank() || quantityStr.isNullOrBlank() || priceStr.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Missing quoteName, quantity or price")
                return@post
            }
            
            val quantity = quantityStr.toIntOrNull()
            val price = priceStr.toDoubleOrNull()?.toBigDecimal()
            
            if (quantity == null || price == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid quantity or price format")
                return@post
            }
            
            // Временная заглушка - в реальности нужно получать userId из сессии
            val userId = 1 // TODO: получить из сессии/аутентификации
            
            val result = PortfolioRepository.addOrUpdatePortfolioItem(userId, quoteName, quantity, price)
            
            if (result) {
                call.respond(HttpStatusCode.OK, "Позиция добавлена/обновлена")
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Ошибка при обновлении портфеля")
            }
        }

        // DELETE /api/portfolio/{quoteName} - удалить позицию из портфеля
        delete("/api/portfolio/{quoteName}") {
            val quoteName = call.parameters["quoteName"]
            
            if (quoteName.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Missing quoteName")
                return@delete
            }
            
            // Временная заглушка - в реальности нужно получать userId из сессии
            val userId = 1 // TODO: получить из сессии/аутентификации
            
            val result = PortfolioRepository.removePortfolioItem(userId, quoteName)
            
            if (result) {
                call.respond(HttpStatusCode.OK, "Позиция удалена")
            } else {
                call.respond(HttpStatusCode.InternalServerError, "Ошибка при удалении позиции")
            }
        }
    }
}
