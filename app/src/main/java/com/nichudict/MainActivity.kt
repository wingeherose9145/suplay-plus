package com.nichudict

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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

// 五十音循环变换逻辑
fun cycleKana(current: String): String {
    if (current.isEmpty()) return ""
    val lastChar = current.last()
    val fullText = current.dropLast(1)
    
    // 定义循环表 (平假名 & 片假名)
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
        listOf('塔', 'ダ'), listOf('チ', 'ヂ'), listOf('ツ', 'ッ', 'ヅ'), listOf('テ', 'デ'), listOf('ト', 'ド'),
        listOf('ハ', 'バ', 'パ'), listOf('ヒ', 'ビ', 'ピ'), listOf('フ', 'ブ', 'プ'), listOf('ヘ', 'ベ', 'ペ'), listOf('ホ', 'ボ', 'ポ'),
        listOf('ヤ', 'ャ'), listOf('ユ', 'ュ'), listOf('ヨ', 'ョ'), listOf('ワ', 'ヮ')
    )

    val targetList = cycles.find { it.contains(lastChar) }
    return if (targetList != null) {
        val nextIndex = (targetList.indexOf(lastChar) + 1) % targetList.size
        fullText + targetList[nextIndex]
    } else {
        current // 如果没找到匹配项（如“ん”），保持不变
    }
}

// 平片假名转换
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

    // 50音基础数据 (10行5列)
    val baseKeys = listOf(
        "あ", "い", "う", "え", "お",
        "か", "き", "く", "け", "开",
        "さ", "し", "す", "せ", "そ",
        "た", "ち", "つ", "て", "と",
        "な", "に", "ぬ", "ね", "の",
        "は", "ひ", "ふ", "へ", "ほ",
        "ま", "み", "む", "め", "も",
        "や", "ー", "ゆ", "DEL", "よ", // 第8行插槽: index 36=长音, 38=删除/清空
        "ら", "り", "る", "れ", "ろ",
        "わ", "を", "ん", "KANA", "CYC" // 第10行插槽: index 48=切换, 49=循环
    )

    Column(modifier = Modifier.fillMaxSize().padding(4.dp)) {
        // 1. 搜索框 (无标题)
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; viewModel.search(it) },
            placeholder = { Text("输入日语或中文检索...") },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )

        // 2. 搜索结果区 (缩小显示，占据剩余空间)
        LazyColumn(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
            items(viewModel.results) { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(entry.word, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Text(entry.content, style = MaterialTheme.typography.bodySmall, fontSize = 13.sp)
                    }
                }
            }
        }

        // 3. 紧致五十音键盘 (固定高度，无需滑动)
        Box(modifier = Modifier.height(340.dp).fillMaxWidth()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = false, // 禁止滚动，确保全部显示
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(baseKeys.size) { index ->
                    val rawKey = baseKeys[index]
                    val displayKey = if (rawKey.length == 1) convertKana(rawKey, isKatakana) else rawKey
                    
                    when (rawKey) {
                        "DEL" -> KeyButton("←/CLR", isSpecial = true, 
                            onClick = { if(text.isNotEmpty()) { text = text.dropLast(1); viewModel.search(text) } },
                            onDoubleTap = { text = ""; viewModel.search("") })
                        "KANA" -> KeyButton(if(isKatakana) "片" else "平", true, onClick = { isKatakana = !isKatakana })
                        "CYC" -> KeyButton("促/浊", true, onClick = { text = cycleKana(text); viewModel.search(text) })
                        "ー" -> KeyButton("ー", onClick = { text += "ー"; viewModel.search(text) })
                        else -> if (rawKey.isNotEmpty()) {
                            KeyButton(displayKey, onClick = { 
                                text += displayKey
                                viewModel.search(text) 
                            })
                        }
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
            .height(32.dp) // 降低高度实现紧凑布局
            .border(0.5.dp, Color.LightGray, RoundedCornerShape(4.dp))
            .background(if (isSpecial) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onDoubleTap = { onDoubleTap() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 14.sp, style = MaterialTheme.typography.labelSmall)
    }
}
