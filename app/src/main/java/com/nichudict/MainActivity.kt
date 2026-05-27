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
import androidx.compose.foundation.lazy.LazyColumn
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

// 1. 浊音/半浊音/促音循环：像素级校对，绝对无平片混淆
fun cycleKana(current: String): String {
    if (current.isEmpty()) return ""
    val lastChar = current.last()
    val fullText = current.dropLast(1)

    val cycles = listOf(
        // 平假名循环
        listOf('あ', 'ぁ'),
        listOf('い', 'ぃ'),
        listOf('う', 'ぅ', 'ゔ'),
        listOf('え', 'ぇ'),
        listOf('お', 'ぉ'),

        listOf('か', 'が'),
        listOf('き', 'ぎ'),
        listOf('く', 'ぐ'),
        listOf('け', 'げ'),
        listOf('こ', 'ご'),

        listOf('さ', 'ざ'),
        listOf('し', 'じ'),
        listOf('す', 'ず'),
        listOf('せ', 'ぜ'),
        listOf('そ', 'ぞ'),

        listOf('た', 'だ'),
        listOf('ち', 'ぢ'),
        listOf('つ', 'っ', 'づ'),
        listOf('て', 'で'),
        listOf('と', 'ど'),

        listOf('は', 'ば', 'ぱ'),
        listOf('ひ', 'び', 'ぴ'),
        listOf('ふ', 'ぶ', 'ぷ'),
        listOf('へ', 'べ', 'ぺ'),
        listOf('ほ', 'ぼ', 'ぽ'),

        listOf('や', 'ゃ'),
        listOf('ゆ', 'ゅ'),
        listOf('よ', 'ょ'),

        listOf('わ', 'ゎ'),

        // 片假名循环
        listOf('ア', 'ァ'),
        listOf('イ', 'ィ'),
        listOf('ウ', 'ゥ', 'ヴ'),
        listOf('エ', 'ェ'),
        listOf('オ', 'ォ'),

        listOf('カ', 'ガ'),
        listOf('キ', 'ギ'),
        listOf('ク', 'グ'),
        listOf('ケ', 'ゲ'),
        listOf('コ', 'ゴ'),

        listOf('サ', 'ザ'),
        listOf('シ', 'ジ'),
        listOf('ス', 'ズ'),
        listOf('セ', 'ゼ'),
        listOf('ソ', 'ゾ'),

        listOf('タ', 'ダ'),
        listOf('チ', 'ヂ'),
        listOf('ツ', 'ッ', 'ヅ'),
        listOf('テ', 'デ'),
        listOf('ト', 'ド'),

        listOf('ハ', 'バ', 'パ'),
        listOf('ヒ', 'ビ', 'ピ'),
        listOf('フ', 'ブ', 'プ'),
        listOf('ヘ', 'ベ', 'ペ'),
        listOf('ホ', 'ボ', 'ポ'),

        listOf('ヤ', 'ャ'),
        listOf('ユ', 'ュ'),
        listOf('ヨ', 'ョ'),

        listOf('ワ', 'ヮ')
    )

    val targetList = cycles.find { it.contains(lastChar) }

    return if (targetList != null) {
        val nextIndex = (targetList.indexOf(lastChar) + 1) % targetList.size
        fullText + targetList[nextIndex]
    } else {
        current
    }
}

// 2. 平片假名互转：弃用不稳定的数学偏移量算账法，改用最稳固的明文查表映射法
fun convertKana(text: String, toKatakana: Boolean): String {

    val hira =
        "ぁあぃいぅうぇえぉおゔ" +
        "かがきぎくぐけげこご" +
        "さざしじすずせぜそぞ" +
        "ただちぢっつづてでとど" +
        "なにぬねの" +
        "はばぱひびぴふぶぷへべぺほぼぽ" +
        "まみむめも" +
        "ゃやゅゆょよ" +
        "らりるれろ" +
        "ゎわをん" +
        "ゕゖ"

    val kata =
        "ァアィイゥウェエォオヴ" +
        "カガキギクグケゲコゴ" +
        "サザシジスズセゼソゾ" +
        "タダチヂッツヅテデトド" +
        "ナニヌネノ" +
        "ハバパヒビピフブプヘベペホボポ" +
        "マミムメモ" +
        "ャヤュユョヨ" +
        "ラリルレロ" +
        "ヮワヲン" +
        "ヵヶ"

    return text.map { char ->
        if (toKatakana) {
            val idx = hira.indexOf(char)
            if (idx != -1) kata[idx] else char
        } else {
            val idx = kata.indexOf(char)
            if (idx != -1) hira[idx] else char
        }
    }.joinToString("")
}
@Composable
fun HtmlWebView(htmlContent: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                setBackgroundColor(0) 
                settings.textZoom = 100 
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(
                "file:///android_asset/",
                htmlContent,
                "text/html",
                "UTF-8",
                null
            )
        }
    )
}

@Composable
fun DictScreen(viewModel: DictViewModel) {
    var text by remember { mutableStateOf("") }
    var isKatakana by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // 基础键盘定义（全部初始化为平假名清音）
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
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                viewModel.search(it)
            },
            placeholder = { Text("搜索...", fontSize = 16.sp) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            textStyle = TextStyle(fontSize = 22.sp, textAlign = TextAlign.Center),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = OutlinedTextFieldDefaults.colors()
        )

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 4.dp)
        ) {
            items(viewModel.results) { entry ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    clipboardManager.setText(AnnotatedString(entry.word + ": " + entry.content))
                                    Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = entry.word,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        HtmlWebView(
                            htmlContent = entry.content,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                        )
                    }
                }
            }
        }

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
                    val displayKey = if (rawKey.length == 1) {
                        convertKana(rawKey, isKatakana)
                    } else {
                        rawKey
                    }

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
