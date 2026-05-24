@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.nichudict

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
            NichuDictApp(viewModel)
        }
    }
}

@Composable
fun NichuDictApp(viewModel: DictViewModel) {
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            DictScreen(viewModel)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DictScreen(viewModel: DictViewModel) {
    var text by remember { mutableStateOf("") }
    
    val gojuon = listOf(
        "あ", "い", "う", "え", "お", "か", "き", "く", "け", "こ",
        "さ", "し", "す", "せ", "そ", "た", "ち", "つ", "て", "と"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "日中离线词典",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "SQLite 预置词典",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = text,
            onValueChange = { 
                text = it
                viewModel.search(it)
            },
            label = { Text("搜索日语单词") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("五十音快速输入", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))

        // 使用 experimental 的 FlowRow
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            gojuon.forEach { kana ->
                Button(
                    onClick = {
                        text += kana
                        viewModel.search(text)
                    }
                ) {
                    Text(kana)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.isLoading.value) {
            CircularProgressIndicator()
        } else if (viewModel.results.isEmpty() && text.isNotBlank()) {
            Text("没有找到匹配的词条", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(viewModel.results) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(entry.word, style = MaterialTheme.typography.headlineSmall)
                            Text("📖 ${entry.reading}", color = MaterialTheme.colorScheme.primary)
                            Text(entry.meaning, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}
