package com.nichudict

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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

@Composable
fun DictScreen(viewModel: DictViewModel) {
    var text by remember { mutableStateOf("") }
    
    // 键盘数据定义：50音 + 功能键
    // 逻辑：将功能键插入到指定位置
    val gojuon = listOf(
        "あ", "い", "う", "え", "お", "か", "き", "く", "け", "こ",
        "さ", "し", "す", "せ", "そ", "た", "ち", "つ", "て", "と",
        "な", "に", "ぬ", "ね", "の", "は", "ひ", "ふ", "へ", "ほ",
        "ま", "み", "む", "め", "も", "や", "ゆ", "よ", "ら", "り",
        "る", "れ", "ろ", "わ", "を", "ん"
    )

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        // 1. 搜索框 (置顶)
        OutlinedTextField(
            value = text,
            onValueChange = { 
                text = it
                viewModel.search(it)
            },
            label = { Text("搜索日语单词") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                if (text.isNotEmpty()) {
                    IconButton(onClick = { text = ""; viewModel.search("") }) {
                        Text("✕")
                    }
                }
            }
        )

        // 2. 搜索结果列表 (占据剩余空间)
        LazyColumn(
            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(viewModel.results) { entry ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(entry.word, style = MaterialTheme.typography.titleMedium)
                        Text(entry.meaning, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        // 3. 五十音键盘 (底部固定)
        val keyboardItems = remember {
            val list = gojuon.toMutableList()
            // 插入逻辑: 5列一行，第8行是 index 35-39, 第10行是 index 45-49
            // 这里我们手动添加占位符和功能键
            list.add(35, "←") // 第8行功能键
            list.add(47, "清空") // 第10行功能键
            list
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier.height(300.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(keyboardItems) { item ->
                Button(
                    onClick = {
                        when (item) {
                            "←" -> if (text.isNotEmpty()) { text = text.dropLast(1); viewModel.search(text) }
                            "清空" -> { text = ""; viewModel.search("") }
                            else -> { text += item; viewModel.search(text) }
                        }
                    },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(45.dp)
                ) {
                    Text(item)
                }
            }
        }
    }
}
