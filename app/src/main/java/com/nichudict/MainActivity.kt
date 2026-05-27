package com.nichudict

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {

    private val viewModel: DictViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DictionaryMainScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(Material3Api::class)
@Composable
fun DictionaryMainScreen(viewModel: DictViewModel) {
    var text by remember { mutableStateOf("") }
    var selectedEntry by remember { mutableStateOf<DictionaryEntry?>(null) }

    // 监听搜索结果变化，默认选中搜索到的第一个词条
    LaunchedEffect(viewModel.results.size) {
        if (viewModel.results.isNotEmpty()) {
            selectedEntry = viewModel.results.first()
        } else {
            selectedEntry = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // 1. 顶部自定义搜索框显示区
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (text.isEmpty()) "输入词汇开始查词..." else text,
                    fontSize = 18.sp,
                    color = if (text.isEmpty()) Color.Gray else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (text.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            text = ""
                            viewModel.search("")
                            selectedEntry = null
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Text("✕", fontSize = 16.sp, color = Color.Gray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. 中间多功能内容排版渲染区 (权重 Weight = 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (viewModel.isLoading.value) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (viewModel.results.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (text.isBlank()) "日韩双解大词典\n请使用下方键盘输入查词" else "未找到相关词条",
                        textAlign = TextAlign.Center,
                        color = Color.Gray,
                        fontSize = 16.sp,
                        lineHeight = 24.sp
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 候选词横向快捷切词栏（极其精美且节省空间）
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(viewModel.results) { entry ->
                            val isSelected = entry == selectedEntry
                            SuggestionChip(
                                onClick = { selectedEntry = entry },
                                label = { Text(text = entry.word, fontSize = 14.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    labelColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }

                    // 分割线
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.LightGray.copy(alpha = 0.5f))
                    )

                    // 核心黑科技：WebView HTML 渲染引擎，完美解析内嵌图、排版、样式
                    selectedEntry?.let { entry ->
                        DictionaryHtmlViewer(
                            htmlContent = entry.content,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. 底部自定义软键盘区（10列标准网格）
        // 💡 提示：你可以自由修改下面数组里字符的排列顺序，以完全还原你本来的键盘布局！
        val keys = listOf(
            "あ", "い", "う", "え", "お", "か", "き", "く", "け", "こ",
            "さ", "し", "す", "せ", "そ", "た", "ち", "つ", "て", "と",
            "な", "に", "ぬ", "ね", "之", "は", "ひ", "ふ", "へ", "ほ",
            "ま", "み", "む", "め", "も", "や", "ゆ", "よ", "ら", "り",
            "る", "れ", "ろ", "わ", "を", "ん", "っ", "ー", "清空", "退格"
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(10),
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(keys) { displayKey ->
                val isSpecial = displayKey == "清空" || displayKey == "退格"
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    KeyButton(
                        label = displayKey,
                        isSpecial = isSpecial,
                        onClick = {
                            when (displayKey) {
                                "清空" -> {
                                    text = ""
                                }
                                "退格" -> {
                                    if (text.isNotEmpty()) {
                                        text = text.dropLast(1)
                                    }
                                }
                                else -> {
                                    text += displayKey
                                }
                            }
                            viewModel.search(text)
                        }
                    )
                }
            }
        }
    }
}

/**
 * 专门用于完美加载并渲染词典复杂 HTML 文本的本地浏览器组件
 */
@Composable
fun DictionaryHtmlViewer(htmlContent: String, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.allowFileAccess = true        // 开启本地文件访问权限
                settings.domStorageEnabled = true       // 开启DOM缓存机制
                settings.javaScriptEnabled = false      // 词典数据一般不含复杂JS，关闭更安全流畅
                
                setBackgroundColor(0)                  // 强制背景完全透明，完美融入你的App背景色
                
                webViewClient = WebViewClient()
            }
        },
        update = { webView ->
            // 核心黑科技：以本地 assets 文件夹作为根路径加载 HTML
            // 这能让网页内的 href="xinrihanshuangjie.css" 自动、准确地找到并使用你内置的样式文件！
            webView.loadDataWithBaseURL(
                "file:///android_asset/",
                htmlContent,
                "text/html",
                "utf-8",
                null
            )
        },
        modifier = modifier
    )
}

@Composable
fun KeyButton(
    label: String,
    isSpecial: Boolean = false,
    onClick: () -> Unit = {},
    onDoubleTap: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .height(38.dp) // 精准微调高度，完美适配底部网格
            .background(
                color = if (isSpecial) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
                shape = RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                Color.LightGray.copy(alpha = 0.7f),
                RoundedCornerShape(8.dp)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onDoubleTap = { onDoubleTap() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = if (label.length > 1) 14.sp else 18.sp, // 自动缩小“清空/退格”等长文本字体
            style = MaterialTheme.typography.labelLarge
        )
    }
}
