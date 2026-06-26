package com.neirobitva.app

import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppDrawerNavigation()
                }
            }
        }
    }
}

// Модель для пунктов меню
data class MenuItem(val title: String, val url: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawerNavigation() {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var webViewInstance: WebView? by remember { mutableStateOf(null) }

    // Нативные пункты меню, при нажатии на которые WebView перейдет на нужную страницу
    val menuItems = listOf(
        MenuItem("💬 Чаты", "https://neiro-bitva.ru/messages.php"),
        MenuItem("🔍 Поиск контактов", "https://neiro-bitva.ru/users.php"),
        MenuItem("👥 Мои контакты", "https://neiro-bitva.ru/contacts.php"),
        MenuItem("🎮 Игротека", "https://neiro-bitva.ru/games.php"),
        MenuItem("🤖 AI Помощник", "https://neiro-bitva.ru/ai/ai.php"),
        MenuItem("⚙️ Настройки", "https://neiro-bitva.ru/settings.php"),
        MenuItem("🚪 Выйти из аккаунта", "https://neiro-bitva.ru/logout.php")
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Neiro-Bitva",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                menuItems.forEach { item ->
                    NavigationDrawerItem(
                        label = { Text(item.title, style = MaterialTheme.typography.titleMedium) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            webViewInstance?.loadUrl(item.url)
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                SmallTopAppBar(
                    title = { Text("Neiro-Bitva") },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Default.Menu, contentDescription = "Открыть меню")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            webViewInstance = this
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                                    if (url.startsWith("gonative://") || url.startsWith("median://")) {
                                        return true
                                    }
                                    view.loadUrl(url)
                                    return true
                                }
                            }
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                cacheMode = WebSettings.LOAD_DEFAULT
                            }
                            // Старт со стандартной страницы входа на сайт
                            loadUrl("https://neiro-bitva.ru/")
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
