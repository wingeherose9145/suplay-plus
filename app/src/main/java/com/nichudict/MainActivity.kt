package com.nichudict

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private val viewModel: DictViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    DictScreen(viewModel)
                }
            }
        }
    }
}

// (此处保留原本的 cycleKana 和 convertKana 函数，无需更改)
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
        listOf('ハ', 'バ', 'パ'), listOf('ヒ', 'ビ', 'ピ'), listOf('フ', 'ブ', 'プ'), listOf('ヘ', 'ベ', 'ペ'), listOf('ホ', 'ボ', 'ポ'),
        listOf('ヤ', 'ャ'), listOf('ユ', 'ュ'), listOf('ヨ', 'ョ'), listOf('ワ', 'ヮ')
    )
    val targetList = cycles.find { it.contains(lastChar) }
    return if (targetList != null) {
        val nextIndex = (targetList.indexOf(lastChar) + 1) % targetList.size
        fullText + targetList[nextIndex]
    } else current
}

fun convertKana(text: String, toKatakana: Boolean): String {
    return text.map { char ->
        if (toKatakana && char in 'ぁ'..'ん') char + ('ァ' - 'ぁ')
        else if (!toKatakana && char in 'ァ'..'ヶ') char - ('ァ' - 'ぁ')
        else char
    }.joinToString("")
}

@Composable
fun DictScreen(viewModel: DictViewModel) {
    var text by remember { mutableStateOf("") }
    var isKatakana by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val baseKeys = listOf(
        "あ", "い", "う", "え", "お", "か", "き", "く", "け", "こ", 
        "さ", "し", "す", "せ", "そ", "た", "ち", "つ", "て", "と",
        "な", "に", "ぬ", "ね", "の", "は", "ひ", "ふ", "へ", "ほ",
        "ま", "み", "む", "め", "も", "や", "ー", "ゆ", "DEL", "よ",
        "ら", "り", "る", "れ", "ろ", "わ", "を", "ん", "KANA", "CYC"
    )

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; viewModel.search(it) },
            placeholder = { Text("输入关键词...") },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            singleLine = true
        )

        LazyColumn(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
            items(viewModel.results) { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(onLongPress = {
                                clipboardManager.setText(AnnotatedString(entry.word + ": " + entry.content))
                                Toast.makeText(context, "已复制", Toast.LENGTH_SHORT).show()
                            })
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(entry.word, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(entry.content, style = MaterialTheme.typography.bodyMedium, fontSize = 16.sp)
                    }
                }
            }
        }

        // 紧凑键盘区：400.dp 总高，无间距，每个键 40.dp 高度
        Box(modifier = Modifier.height(400.dp).fillMaxWidth()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false,
                verticalArrangement = Arrangement.spacedBy(0.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(baseKeys.size) { index ->
                    val rawKey = baseKeys[index]
                    val displayKey = if (rawKey.length == 1) convertKana(rawKey, isKatakana) else rawKey
                    
                    when (rawKey) {
                        "DEL" -> KeyButton("←/CLR", isSpecial = true, onClick = { if(text.isNotEmpty()) { text = text.dropLast(1); viewModel.search(text) } }, onDoubleTap = { text = ""; viewModel.search("") })
                        "KANA" -> KeyButton(if(isKatakana) "片" else "平", true, onClick = { isKatakana = !isKatakana })
                        "CYC" -> KeyButton("促/浊", true, onClick = { text = cycleKana(text); viewModel.search(text) })
                        "ー" -> KeyButton("ー", onClick = { text += "ー"; viewModel.search(text) })
                        else -> KeyButton(displayKey, onClick = { text += displayKey; viewModel.search(text) })
                    }
                }
            }
        }
    }
}

@Composable
fun KeyButton(label: String, isSpecial: Boolean = false, onClick: () -> Unit = {}, onDoubleTap: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp) // 降低高度至40，确保护航10行
            .border(0.5.dp, Color.Gray, RoundedCornerShape(0.dp)) // 设为0.dp让布局更紧凑
            .background(if (isSpecial) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() }, onDoubleTap = { onDoubleTap() })
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 16.sp, style = MaterialTheme.typography.labelLarge)
    }
}
