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
import androidx.compose.ui.text.TextStyle
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
                    DictScreen(viewModel)
                }
            }
        }
    }
}

// ----------------------------------------------------
// 核心算法：平假名/片假名、浊音/半浊音/促音循环
// ----------------------------------------------------

fun cycleKana(current: String): String {
    if (current.isEmpty()) return ""
    val lastChar = current.last()
    val fullText = current.dropLast(1)
    val cycles = listOf(
        listOf('あ', 'ぁ'), listOf('い', 'ぃ'), listOf('う', 'ぅ'), listOf('え', 'ぇ'), listOf('お', 'ぉ'),
        listOf('か', 'が'), listOf('き', 'ぎ'), listOf('く', 'ぐ'), listOf('け', 'げ'), listOf('こ', 'ご'),
        listOf('さ', 'ざ'), listOf('し', 'じ'), listOf('す', 'ず'), listOf('せ', 'ぜ'), listOf('そ', 'ぞ'),
        listOf('た', 'だ'), listOf('ち', 'ぢ'), listOf('つ', 'っ', 'づ'), listOf('て', 'で'), listOf('と', 'ど'),
        listOf('は', 'ば', 'ぱ'), listOf('ひ', 'び', 'ぴ'), listOf('ふ', 'ぶ', 'ぷ'), listOf('へ', 'べ', 'ぺ'), listOf('ほ', 'ぼ', 'ぽ'),
        listOf('や', 'ゃ'), listOf('ゆ', 'ゅ'), listOf('よ', 'ょ'), listOf('わ', 'ゎ'),
        listOf('ア', 'ァ'), listOf('イ', 'ィ'), listOf('ウ', 'ゥ'), listOf('エ', 'ェ'), listOf('オ', 'ォ'),
        listOf('カ', 'ガ'), listOf('キ', 'ギ'), listOf('ク', 'グ'), listOf('ケ', 'ゲ'), listOf('コ', 'ゴ'),
        listOf('サ', 'ザ'), listOf('シ', 'ジ'), listOf('ス', 'ズ'), listOf('セ', 'ゼ'), listOf('ソ', 'ゾ'),
        listOf('タ', 'ダ'), listOf('チ', 'ヂ'), listOf('ツ', 'ッ', 'ヅ'), listOf('テ', 'デ'), listOf('ト', 'ド'),
        listOf('ハ', 'ば', 'ぱ'), listOf('ヒ', 'び', 'ぴ'), listOf('フ', 'ぶ', 'ぷ'), listOf('ヘ', 'べ', 'ぺ'), listOf('ホ', 'ぼ', 'ぽ'),
        listOf('ヤ', 'ャ'), listOf('ユ', 'ュ'), listOf('ヨ', 'ョ'), listOf('ワ', 'ヮ')
    )

    val targetList = cycles.find { it.contains(lastChar) }
    return if (targetList != null) {
        val nextIndex = (targetList.indexOf(lastChar) + 1) % targetList.size
        fullText + targetList[nextIndex]
    } else {
        current
    }
}

fun convertKana(text: String, toKatakana: Boolean): String {
    return text.map { char ->
        if (toKatakana && char in 'ぁ'..'ん') {
            char + ('ァ' - 'ぁ')
        } else if (!toKatakana && char in 'ァ'..'ヶ') {
            char - ('ァ' - 'ぁ')
        } else {
            char
        }
    }.joinToString("")
}

// ----------------------------------------------------
// UI 主界面
// ----------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DictScreen(viewModel: DictViewModel) {

    var text by remember { mutableStateOf("") }
    var isKatakana by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<DictionaryEntry?>(null) }

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // 监听搜索结果变化，默认选中查到的第一个词条
    LaunchedEffect(viewModel.results.size) {
        selectedEntry = if (viewModel.results.isNotEmpty()) viewModel.results.first() else null
    }

    val baseKeys = listOf(
        "あ", "い", "う", "え", "お",
        "か", "き", "く", "け", "こ",
        "さ", "し", "す", "せ", "そ",
        "た", "ち", "つ", "て", "と",
        "な", "に", "ぬ", "ね", "の",
        "は", "ひ", "ふ", "へ", "ほ",
        "ま", "み", "む", "め", "も",
        "や", "ー", "ゆ", "DEL", "よ",
        "ら", "り", "る", "れ", "ろ",
        "わ", "を", "ん", "KANA", "CYC"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(6.dp)
    ) {
        // 1. 搜索输入框优化：去掉闪烁光标，杜绝系统键盘弹窗
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                viewModel.search(it)
            },
            placeholder = { Text("搜索...", fontSize = 16.sp) },
            singleLine = true,
            readOnly = true, // ✨ 核心优化：只读模式，彻底封死手机系统自带键盘的弹窗干扰
            shape = RoundedCornerShape(16.dp),
            textStyle = TextStyle(
                fontSize = 22.sp,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = OutlinedTextFieldDefaults.colors(
                cursorColor = Color.Transparent, // ✨ 核心优化：把光标颜色设为完全透明，彻底去掉闪烁竖线
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Gray
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 2. 内容显示区
        Box(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
            if (viewModel.isLoading.value) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (viewModel.results.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("无查询结果", color = Color.Gray, fontSize = 16.sp)
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 横向滚动的候选词切词栏
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(viewModel.results) { entry ->
                            val isSelected = entry == selectedEntry
                            SuggestionChip(
                                onClick = { selectedEntry = entry },
                                label = { Text(text = entry.word, fontSize = 16.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                )
                            )
                        }
                    }

                    // 网页级 HTML 渲染引擎
                    selectedEntry?.let { entry ->
                        DictionaryHtmlViewer(
                            htmlContent = entry.content,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 3. 5 列多功能日文键盘
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false
            ) {
                items(baseKeys) { rawKey ->
                    val displayKey = if (rawKey.length == 1) convertKana(rawKey, isKatakana) else rawKey

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .padding(2.dp)
                    ) {
                        when (rawKey) {
                            "DEL" -> {
                                KeyButton(
                                    label = "←",
                                    isSpecial = true,
                                    onClick = {
                                        if (text.isNotEmpty()) {
                                            text = text.dropLast(1)
                                            viewModel.search(text)
                                        }
                                    },
                                    onDoubleTap = {
                                        text = ""
                                        viewModel.search("")
                                    }
                                )
                            }
                            "KANA" -> {
                                KeyButton(
                                    label = if (isKatakana) "片" else "平",
                                    isSpecial = true,
                                    onClick = { isKatakana = !isKatakana }
                                )
                            }
                            "CYC" -> {
                                KeyButton(
                                    label = "促/浊",
                                    isSpecial = true,
                                    onClick = {
                                        text = cycleKana(text)
                                        viewModel.search(text)
                                    }
                                )
                            }
                            "ー" -> {
                                KeyButton(
                                    label = "ー",
                                    onClick = {
                                        text += "ー"
                                        viewModel.search(text)
                                    }
                                )
                            }
                            else -> {
                                KeyButton(
                                    label = displayKey,
                                    onClick = {
                                        text += displayKey
                                        viewModel.search(text)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
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
            .background(
                color = if (isSpecial) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
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
            fontSize = 22.sp,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun DictionaryHtmlViewer(htmlContent: String, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.allowFileAccess = true        
                settings.domStorageEnabled = true       
                settings.javaScriptEnabled = false      
                setBackgroundColor(0)                  
                webViewClient = WebViewClient()
            }
        },
        update = { webView ->
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
