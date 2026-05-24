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

fun cycleKana(current: String): String {

    if (current.isEmpty()) return ""

    val lastChar = current.last()
    val fullText = current.dropLast(1)

    val cycles = listOf(

        listOf('あ', 'ぁ'),
        listOf('い', 'ぃ'),
        listOf('う', 'ぅ'),
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

        listOf('ア', 'ァ'),
        listOf('イ', 'ィ'),
        listOf('ウ', 'ゥ'),
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

        val nextIndex =
            (targetList.indexOf(lastChar) + 1) % targetList.size

        fullText + targetList[nextIndex]

    } else {
        current
    }
}

fun convertKana(
    text: String,
    toKatakana: Boolean
): String {

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

@Composable
fun DictScreen(viewModel: DictViewModel) {

    var text by remember { mutableStateOf("") }

    var isKatakana by remember {
        mutableStateOf(false)
    }

    val clipboardManager =
        LocalClipboardManager.current

    val context = LocalContext.current

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

        // 搜索栏
        OutlinedTextField(
            value = text,

            onValueChange = {
                text = it
                viewModel.search(it)
            },

            placeholder = {
                Text(
                    "搜索...",
                    fontSize = 16.sp
                )
            },

            singleLine = true,

            shape = RoundedCornerShape(16.dp),

            textStyle = TextStyle(
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            ),

            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),

            colors = OutlinedTextFieldDefaults.colors()
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 结果区域
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

                                    clipboardManager.setText(
                                        AnnotatedString(
                                            entry.word + ": " + entry.content
                                        )
                                    )

                                    Toast.makeText(
                                        context,
                                        "已复制",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        },

                    colors = CardDefaults.cardColors(
                        containerColor =
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {

                        Text(
                            text = entry.word,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = entry.content,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        // 键盘区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
        ) {

            LazyVerticalGrid(
                columns = GridCells.Fixed(5),

                modifier = Modifier.fillMaxSize(),

                userScrollEnabled = false
            ) {

                items(baseKeys) { rawKey ->

                    val displayKey =

                        if (rawKey.length == 1) {
                            convertKana(rawKey, isKatakana)
                        } else {
                            rawKey
                        }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
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
                                    label =
                                    if (isKatakana) "片" else "平",

                                    isSpecial = true,

                                    onClick = {
                                        isKatakana = !isKatakana
                                    }
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
                color =

                if (isSpecial) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },

                shape = RoundedCornerShape(12.dp)
            )

            .border(
                1.dp,
                Color.LightGray,
                RoundedCornerShape(12.dp)
            )

            .pointerInput(Unit) {

                detectTapGestures(

                    onTap = {
                        onClick()
                    },

                    onDoubleTap = {
                        onDoubleTap()
                    }
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
