package com.trading

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.ContentType

import com.trading.quotes.quotesAsTable 
import com.trading.quotes.Quote
import com.trading.quotes.QuoteRepository

fun Application.configureRouting() {
    routing {
        get("/quotes") {
            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Котировки</title>
                    <style>
                        body { font-family: Arial; margin: 50px; }
                        table { border-collapse: collapse; width: 100%; }
                        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
                        th { background-color: #4CAF50; color: white; }
                        tr:nth-child(even) { background-color: #f2f2f2; }
                        nav { margin-bottom: 20px; }
                        nav a { margin-right: 15px; }
                        button { padding: 5px 10px; margin: 2px; cursor: pointer; }
                        .buy { background-color: #4CAF50; color: white; border: none; }
                        .sell { background-color: #f44336; color: white; border: none; }
                        .modal { display: none; position: fixed; z-index: 1; left: 0; top: 0; width: 100%; height: 100%; background-color: rgba(0,0,0,0.4); }
                        .modal-content { background-color: #fefefe; margin: 15% auto; padding: 20px; border: 1px solid #888; width: 300px; }
                        .close { color: #aaa; float: right; font-size: 28px; font-weight: bold; cursor: pointer; }
                        input { margin: 5px; padding: 8px; width: 100%; box-sizing: border-box; }
                    </style>
                </head>
                <body>
                    <nav>
                        <a href="/register">Регистрация</a>
                        <a href="/authorization">Вход</a>
                        <a href="/quotes">Котировки</a>
                        <a href="/portfolio">Портфель</a>
                    </nav>
                    <h2>Котировки акций</h2>
                    <div id="quotes">Загрузка...</div>
                    
                    <!-- Модальное окно для покупки/продажи -->
                    <div id="tradeModal" class="modal">
                        <div class="modal-content">
                            <span class="close">&times;</span>
                            <h3 id="modalTitle">Покупка/Продажа</h3>
                            <input type="hidden" id="quoteName">
                            <input type="hidden" id="actionType">
                            <label>Количество:</label>
                            <input type="number" id="quantity" placeholder="Введите количество" required><br>
                            <button id="confirmBtn" style="width: 100%; margin-top: 10px;">Подтвердить</button>
                        </div>
                    </div>
                    
                    <script>
                        const userId = sessionStorage.getItem('userId');
                        
                        async function loadQuotes() {
                            const response = await fetch('/quotes');
                            if (response.ok) {
                                const text = await response.text();
                                // Добавляем кнопки к таблице
                                const parser = new DOMParser();
                                const doc = parser.parseFromString(text, 'text/html');
                                const table = doc.querySelector('table');
                                if (table) {
                                    const rows = table.querySelectorAll('tr');
                                    for (let i = 1; i < rows.length; i++) {
                                        const row = rows[i];
                                        const cells = row.querySelectorAll('td');
                                        if (cells.length >= 2) {
                                            const quoteName = cells[0].textContent;
                                            const price = cells[1].textContent;
                                            
                                            const actionCell = document.createElement('td');
                                            actionCell.innerHTML = \`
                                                <button class="buy" onclick="openTradeModal('\${quoteName}', \${price}, 'buy')">Купить</button>
                                                <button class="sell" onclick="openTradeModal('\${quoteName}', \${price}, 'sell')">Продать</button>
                                            \`;
                                            row.appendChild(actionCell);
                                        }
                                    }
                                    // Добавляем заголовок для нового столбца
                                    const headerRow = rows[0];
                                    const th = document.createElement('th');
                                    th.textContent = 'Действия';
                                    headerRow.appendChild(th);
                                    
                                    document.getElementById('quotes').innerHTML = table.outerHTML;
                                } else {
                                    document.getElementById('quotes').innerHTML = text;
                                }
                            } else {
                                document.getElementById('quotes').innerHTML = '<p class="error">Ошибка загрузки котировок</p>';
                            }
                        }
                        
                        function openTradeModal(quoteName, price, action) {
                            const modal = document.getElementById('tradeModal');
                            const title = document.getElementById('modalTitle');
                            document.getElementById('quoteName').value = quoteName;
                            document.getElementById('actionType').value = action;
                            
                            title.textContent = (action === 'buy' ? 'Покупка' : 'Продажа') + ' ' + quoteName;
                            title.style.color = action === 'buy' ? 'green' : 'red';
                            
                            modal.style.display = 'block';
                        }
                        
                        // Закрытие модального окна
                        document.querySelector('.close').onclick = function() {
                            document.getElementById('tradeModal').style.display = 'none';
                        }
                        window.onclick = function(event) {
                            const modal = document.getElementById('tradeModal');
                            if (event.target == modal) {
                                modal.style.display = 'none';
                            }
                        }
                        
                        // Подтверждение сделки
                        document.getElementById('confirmBtn').onclick = async function() {
                            const quoteName = document.getElementById('quoteName').value;
                            const action = document.getElementById('actionType').value;
                            const quantity = parseInt(document.getElementById('quantity').value);
                            
                            if (!quantity || quantity <= 0) {
                                alert('Введите корректное количество');
                                return;
                            }
                            
                            if (!userId) {
                                alert('Требуется авторизация для совершения сделок');
                                window.location.href = '/authorization';
                                return;
                            }
                            
                            // Получаем текущую цену из таблицы (упрощенно - берем из API)
                            const response = await fetch('/quotes');
                            const text = await response.text();
                            const parser = new DOMParser();
                            const doc = parser.parseFromString(text, 'text/html');
                            const rows = doc.querySelectorAll('tr');
                            let price = 0;
                            for (let i = 1; i < rows.length; i++) {
                                const cells = rows[i].querySelectorAll('td');
                                if (cells.length > 0 && cells[0].textContent === quoteName) {
                                    price = parseFloat(cells[1].textContent);
                                    break;
                                }
                            }
                            
                            // Отправляем запрос на обновление портфеля
                            const qty = action === 'buy' ? quantity : -quantity;
                            const body = 'quoteName=' + encodeURIComponent(quoteName) + 
                                        '&quantity=' + encodeURIComponent(qty) + 
                                        '&price=' + encodeURIComponent(price) +
                                        '&userId=' + encodeURIComponent(userId);
                            
                            const result = await fetch('/api/portfolio', {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                                body: body
                            });
                            
                            const message = await result.text();
                            
                            if (result.ok) {
                                alert((action === 'buy' ? 'Покупка' : 'Продажа') + ' выполнена успешно!');
                                document.getElementById('tradeModal').style.display = 'none';
                                document.getElementById('quantity').value = '';
                            } else {
                                alert('Ошибка: ' + message);
                            }
                        }
                        
                        loadQuotes();
                    </script>
                </body>
                </html>
            """.trimIndent()
            call.respondText(html, ContentType.Text.Html)
        }
    }
}