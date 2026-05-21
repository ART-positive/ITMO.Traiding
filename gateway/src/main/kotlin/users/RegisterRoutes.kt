package com.trading.users

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.server.sessions.*

fun Application.registerRoutes() {
    val register = Register()
    val auth = Authorization()
    
    routing {
        get("/register") {
    val html = """
        <!DOCTYPE html>
        <html>
        <head>
            <title>Регистрация</title>
            <style>
                body { font-family: Arial; margin: 50px; }
                input { margin: 5px; padding: 8px; width: 200px; }
                button { padding: 8px 20px; margin-top: 10px; }
                .error { color: red; }
                .success { color: green; }
                nav { margin-bottom: 20px; }
                nav a { margin-right: 15px; }
            </style>
        </head>
        <body>
            <nav>
                <a href="/register">Регистрация</a>
                <a href="/authorization">Вход</a>
                <a href="/quotes">Котировки</a>
            </nav>
            <h2>Регистрация</h2>
            <form id="registerForm">
                <input type="text" id="login" placeholder="Логин" required><br>
                <input type="password" id="password" placeholder="Пароль" required><br>
                <button type="submit">Зарегистрироваться</button>
            </form>
            <div id="message"></div>
            
            <script>
                const form = document.getElementById('registerForm');
                form.onsubmit = async (event) => {
                    event.preventDefault();
                    
                    const login = document.getElementById('login').value;
                    const password = document.getElementById('password').value;
                    const msgDiv = document.getElementById('message');
                    
                    const response = await fetch('/api/register', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                        body: 'login=' + encodeURIComponent(login) + '&password=' + encodeURIComponent(password)
                    });
                    
                    const message = await response.text();
                    
                    if (response.ok) {
                        msgDiv.className = 'success';
                        msgDiv.innerHTML = message + '<br>Перенаправление на вход...';
                        setTimeout(() => {
                            window.location.href = '/authorization';
                        }, 1500);
                    } else {
                        msgDiv.className = 'error';
                        msgDiv.innerHTML = message;
                    }
                };
            </script>
        </body>
        </html>
    """.trimIndent()
    call.respondText(html, ContentType.Text.Html)
}
        
        post("/api/register") {
            val params = call.receiveParameters()
            val login = params["login"]
            val password = params["password"]
            
            if (login.isNullOrBlank() || password.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Missing login or password")
                return@post
            }
            
            val result = register.register(login, password)
            
            if (result == "User registered successfully.") {
                call.respond(HttpStatusCode.OK, result)
            } else {
                call.respond(HttpStatusCode.BadRequest, result)
            }
        }
        
        get("/authorization") {
            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Авторизация</title>
                    <style>
                        body { font-family: Arial; margin: 50px; }
                        input { margin: 5px; padding: 8px; width: 200px; }
                        button { padding: 8px 20px; margin-top: 10px; }
                        .error { color: red; }
                        nav { margin-bottom: 20px; }
                        nav a { margin-right: 15px; }
                    </style>
                </head>
                <body>
                    <nav>
                        <a href="/register">Регистрация</a>
                        <a href="/authorization">Вход</a>
                        <a href="/quotes">Котировки</a>
                    </nav>
                    <h2>Авторизация</h2>
                    <form id="loginForm">
                        <input type="text" id="login" placeholder="Логин" required><br>
                        <input type="password" id="password" placeholder="Пароль" required><br>
                        <button type="submit">Войти</button>
                    </form>
                    <div id="message"></div>
                    
                    <script>
                        document.getElementById('loginForm').onsubmit = async (e) => {
                            e.preventDefault();
                            const login = document.getElementById('login').value;
                            const password = document.getElementById('password').value;
                            const msgDiv = document.getElementById('message');
                            
                            const response = await fetch('/api/login', {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                                body: 'login=' + encodeURIComponent(login) + '&password=' + encodeURIComponent(password)
                            });
                            
                            const message = await response.text();
                            
                            if (response.ok) {
                                // Сохраняем userId в sessionStorage
                                sessionStorage.setItem('userId', message);
                                window.location.href = '/portfolio';
                            } else {
                                msgDiv.className = 'error';
                                msgDiv.innerHTML = message;
                            }
                        };
                    </script>
                </body>
                </html>
            """.trimIndent()
            call.respondText(html, ContentType.Text.Html)
        }
        
        post("/api/login") {
            val params = call.receiveParameters()
            val login = params["login"]
            val password = params["password"]
            
            if (login.isNullOrBlank() || password.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Missing login or password")
                return@post
            }
            
            val userId = auth.authorization(login, password)
            
            if (userId != null) {
                // Возвращаем userId клиенту для сохранения в sessionStorage
                call.respond(HttpStatusCode.OK, userId.toString())
            } else {
                call.respond(HttpStatusCode.Unauthorized, "Invalid login or password")
            }
        }
        
        get("/api/logout") {
            call.respond(HttpStatusCode.OK, "Logout successful")
        }
        
        get("/api/me") {
            val userIdStr = call.request.queryParameters["userId"]
            if (userIdStr.isNullOrBlank()) {
                call.respond(HttpStatusCode.Unauthorized, "User not authenticated")
                return@get
            }
            
            val userId = userIdStr.toIntOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, "Invalid user ID")
                return@get
            }
            
            val username = auth.getUserById(userId)
            if (username != null) {
                call.respond(HttpStatusCode.OK, """{"userId": $userId, "username": "$username"}""", ContentType.Application.Json)
            } else {
                call.respond(HttpStatusCode.NotFound, "User not found")
            }
        }
    }
}

fun Application.module() {
    registerRoutes()
}