package com.nichudict

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

// 假名辅助工具
fun toggleKana(text: String, isKatakana: Boolean): String {
    return text.map { char ->
        if (isKatakana) {
            // 简单的平转片逻辑 (涵盖大部分常用)
            if (char in 'ぁ'..'ん') char + ('ァ' - 'ぁ') else char
        } else {
            if (char in 'ァ'..'ン') char - ('ァ' - 'ぁ') else char
        }
    }.joinToString("")
}

@Composable
fun DictScreen(viewModel: DictViewModel) {
    var text by remember { mutableStateOf("") }
    var isKatakana by remember { mutableStateOf(false) }
    var modifierMode by remember { mutableStateOf(0) } // 0:正常, 1:促音, 2:浊音

    val baseKeys = listOf(
        "あ", "い", "う", "え", "お", "か", "き", "く", "け", "こ",
        "さ", "し", "す", "せ", "そ", "た", "ち", "つ", "て", "と",
        "な", "に", "ぬ", "ね", "の", "は", "ひ", "ふ", "へ", "ほ",
        "ま", "み", "む", "め", "も", "や", "", "ゆ", "", "よ",
        "ら", "り", "る", "れ", "ろ", "わ", "を", "ん", "", ""
    )

    Column(modifier = Modifier.fillMaxSize().padding(4.dp)) {
        // 搜索框
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; viewModel.search(it) },
            label = { Text("搜索") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // 搜索结果
        LazyColumn(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
            items(viewModel.results) { entry ->
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(entry.word, style = MaterialTheme.typography.titleMedium)
                        Text(entry.meaning, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // 键盘区
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier.height(300.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(50) { index ->
                val key = baseKeys[index]
                
                // 功能键逻辑
                when (index) {
                    36 -> KeyButton("ー", onClick = { text += "ー"; viewModel.search(text) })
                    38 -> KeyButton("←/CLR", isSpecial = true, 
                        onClick = { if(text.isNotEmpty()) { text = text.dropLast(1); viewModel.search(text) } },
                        onDoubleTap = { text = ""; viewModel.search("") })
                    48 -> KeyButton(if(isKatakana) "片" else "平", isSpecial = true, onClick = { isKatakana = !isKatakana })
                    49 -> KeyButton(when(modifierMode){ 0->"促/浊"; 1->"促"; 2->"浊"; else->"促/浊" }, isSpecial = true, 
                        onClick = { modifierMode = (modifierMode + 1) % 3 })
                    else -> if (key.isNotEmpty()) {
                        KeyButton(if(isKatakana) toggleKana(key, true) else key, onClick = {
                            var char = if(isKatakana) toggleKana(key, true) else key
                            if(modifierMode == 1) char = "っ"
                            if(modifierMode == 2) char += "゛"
                            text += char
                            viewModel.search(text)
                        })
                    } else { Box(Modifier) }
                }
            }
        }
    }
}

// 修复后的 KeyButton，使用 Surface 替代 Button，彻底解决点击冲突
@Composable
fun KeyButton(text: String, isSpecial: Boolean = false, onClick: () -> Unit = {}, onDoubleTap: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(1.dp)
            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
            .background(if (isSpecial) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onDoubleTap = { onDoubleTap() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
