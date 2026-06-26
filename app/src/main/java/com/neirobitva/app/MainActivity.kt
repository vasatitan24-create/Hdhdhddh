package com.neirobitva.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// === МОДЕЛИ ДАННЫХ ===
data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val status: String?, val username: String?, val role: String?, val error: String?)
data class ChatItem(val contact_name: String?, val message: String?, val created_at: String?, val is_read: Int?)

// === ИНТЕРФЕЙС API ===
interface ApiService {
    @POST("api/login.php")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @GET("api/get_chats.php")
    fun getChats(): Call<List<ChatItem>>
}

// === СЕТЕВОЙ КЛИЕНТ С ПОДДЕРЖКОЙ СЕССИЙ (COOKIES) ===
object NetworkClient {
    private val cookieJar = object : CookieJar {
        private val cookieStore = HashMap<String, List<Cookie>>()
        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url.host] = cookies
        }
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: ArrayList()
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .build()

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://neiro-bitva.ru/") // Домен вашего сайта
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    var isLoggedIn by remember { mutableStateOf(false) }

    if (!isLoggedIn) {
        LoginScreen(onLoginSuccess = { isLoggedIn = true })
    } else {
        MainAppScreen(onLogout = { isLoggedIn = false })
    }
}

// === ЭКРАН ВХОДА ===
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Neiro-Bitva",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    if (email.isEmpty() || password.isEmpty()) {
                        errorMessage = "Пожалуйста, заполните все поля"
                        return@Button
                    }
                    isLoading = true
                    errorMessage = ""
                    
                    NetworkClient.apiService.login(LoginRequest(email, password))
                        .enqueue(object : Callback<LoginResponse> {
                            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                                isLoading = false
                                if (response.isSuccessful && response.body()?.status == "success") {
                                    Toast.makeText(context, "Вход успешен!", Toast.LENGTH_SHORT).show()
                                    onLoginSuccess()
                                } else {
                                    errorMessage = response.body()?.error ?: "Неверный логин или пароль"
                                }
                            }
                            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                                isLoading = false
                                errorMessage = "Ошибка сети: ${t.localizedMessage}"
                            }
                        })
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Войти")
            }
        }
    }
}

// === ЭКРАН НАЗВАНИЙ ЧАТОВ (НАТИВНЫЙ СПИСОК) ===
@Composable
fun MainAppScreen(onLogout: () -> Unit) {
    var chats by remember { mutableStateOf<List<ChatItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    val context = LocalContext.current

    // Автоматический запрос списка чатов после успешного входа
    LaunchedEffect(Unit) {
        NetworkClient.apiService.getChats().enqueue(object : Callback<List<ChatItem>> {
            override fun onResponse(call: Call<List<ChatItem>>, response: Response<List<ChatItem>>) {
                isLoading = false
                if (response.isSuccessful) {
                    chats = response.body() ?: emptyList()
                } else {
                    errorMessage = "Не удалось загрузить чаты"
                }
            }
            override fun onFailure(call: Call<List<ChatItem>>, t: Throwable) {
                isLoading = false
                errorMessage = "Ошибка подключения к сети"
            }
        })
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Простая верхняя панель приложения
        SmallTopAppBar(
            title = { Text("Чаты (Нативный Android)") },
            actions = {
                Button(onClick = onLogout) {
                    Text("Выход")
                }
            }
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (errorMessage.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
            }
        } else if (chats.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Нет активных диалогов")
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(chats) { chat ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Toast.makeText(context, "Открытие чата с ${chat.contact_name}", Toast.LENGTH_SHORT).show()
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Символическая аватарка в нативном стиле
                        Box(
                            modifier = Modifier.size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👤", style = MaterialTheme.typography.titleLarge)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = chat.contact_name ?: "Неизвестный",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = chat.message ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Text(
                            text = if (chat.created_at != null) chat.created_at.takeLast(8).substring(0, 5) else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }
}
