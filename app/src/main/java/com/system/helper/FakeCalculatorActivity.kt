package com.system.helper

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import java.io.BufferedReader
import java.io.InputStreamReader

class FakeCalculatorActivity : AppCompatActivity() {

    private lateinit var display: TextView
    private val inputSequence = mutableListOf<String>()
    private var unlocked = false

    // 解锁暗号序列（顺序输入这5个音解锁）
    private val secretSequence = listOf("あ", "い", "う", "え", "お") 

    // 全量内容库
    private var allTexts = listOf<String>()
    
    // 匹配检索相关变量
    private var currentInput = ""          
    private var filteredTexts = listOf<String>() 
    private var filteredIndex = 0          

    // 50音图字符定义
    private val hiraganaList = listOf(
        "あ", "い", "う", "え", "お", 
        "か", "き", "く", "け", "こ", 
        "さ", "し", "す", "せ", "そ", 
        "た", "ち", "つ", "て", "と", 
        "な", "に", "ぬ", "ね", "の", 
        "は", "ひ", "ふ", "へ", "ほ", 
        "ま", "み", "む", "め", "も", 
        "や", "◀", "よ", "▶", "よ", 
        "ら", "り", "る", "れ", "ろ", 
        "わ", "を", "ん", "假名", "号/促" // 右下角定义为 浊音/促音/小字 变换键
    )

    private val katakanaList = listOf(
        "ア", "イ", "ウ", "电", "オ",
        "カ", "キ", "ク", "ケ", "コ",
        "サ", "シ", "ス", "セ", "ソ",
        "タ", "チ", "ツ", "テ", "ト",
        "ナ", "ニ", "ヌ", "ネ", "ノ",
        "ハ", "ヒ", "フ", "ヘ", "ホ",
        "マ", "ミ", "ム", "メ", "モ",
        "ヤ", "◀", "ヨ", "▶", "ヨ",
        "ラ", "リ", "ル", "レ", "ロ",
        "ワ", "ヲ", "ン", "假名", "号/促"
    )

    private var isHiragana = true
    private val buttonList = mutableListOf<MaterialButton>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN 
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        setContentView(R.layout.activity_fake_calculator)
        display = findViewById(R.id.display)
        
        // 【解决问题2】：长按显示屏区域，直接一键清空输入，进入下一个输入状态
        display.setOnLongClickListener {
            currentInput = ""
            matchAndFilter()
            true
        }

        loadTextLibrary()
        display.text = ""

        scanAllButtons(window.decorView.findViewById(android.R.id.content))
        refreshButtonLabels()
        setupSpecialLongClick() // 初始化长按后门
    }

    private fun loadTextLibrary() {
        try {
            val inputStream = assets.open("random_texts.txt")
            val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
            val list = mutableListOf<String>()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val trimmed = line!!.trim()
                if (!trimmed.startsWith("#") && trimmed.isNotEmpty()) {
                    list.add(trimmed)
                }
            }
            reader.close()
            allTexts = list
        } catch (e: Exception) {
            e.printStackTrace()
            allTexts = listOf("がっこう", "いっぱい", "きょう", "あさ")
        }
    }

    private fun scanAllButtons(view: View) {
        if (view is MaterialButton) {
            buttonList.add(view)
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                scanAllButtons(view.getChildAt(i))
            }
        }
    }

    private fun refreshButtonLabels() {
        val currentAlphabet = if (isHiragana) hiraganaList else katakanaList
        val maxIndex = minOf(buttonList.size, currentAlphabet.size)

        for (i in 0 until maxIndex) {
            val button = buttonList[i]
            val textValue = currentAlphabet[i]
            button.text = textValue
            button.setOnClickListener { handleButtonClick(textValue, i) }
        }
    }

    // 单独为右下角第50个按钮（索引49）绑定长按进播放器的后门
    private fun setupSpecialLongClick() {
        if (buttonList.size > 49) {
            buttonList[49].setOnLongClickListener {
                if (unlocked) {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                true
            }
        }
    }

    private fun handleButtonClick(value: String, index: Int) {
        when (index) {
            // 第8行第2位 -> 上一个切换
            36 -> {
                if (filteredTexts.isNotEmpty()) {
                    filteredIndex = if (filteredIndex - 1 < 0) filteredTexts.size - 1 else filteredIndex - 1
                    updateDisplayResult()
                }
            }
            // 第8行第4位 -> 下一个切换
            38 -> {
                if (filteredTexts.isNotEmpty()) {
                    filteredIndex = (filteredIndex + 1) % filteredTexts.size
                    updateDisplayResult()
                }
            }
            // 第10行第4位 -> 切换假名
            48 -> {
                isHiragana = !isHiragana
                refreshButtonLabels()
            }
            // 【解决问题1】：第10行第5位 (索引 49) 短按触发“浊音/促音/小字”变换
            49 -> {
                if (currentInput.isNotEmpty()) {
                    val lastChar = currentInput.last().toString()
                    val converted = convertToTransformChar(lastChar)
                    // 用变换后的字符替换掉最后一个字
                    currentInput = currentInput.substring(0, currentInput.length - 1) + converted
                    matchAndFilter()
                }
            }
            // 普通50音输入键
            else -> {
                if (value != "◀" && value != "▶" && value != "假名" && value != "号/促") {
                    currentInput += value
                    
                    inputSequence.add(value)
                    if (inputSequence.size > 5) inputSequence.removeAt(0)
                    if (inputSequence == secretSequence) unlocked = true

                    matchAndFilter()
                }
            }
        }
    }

    /**
     * 💡 浊音、半浊音、促音、小字转换核心算法表
     */
    private fun convertToTransformChar(char: String): String {
        return when (char) {
            // 促音与小字变换
            "つ" -> "っ"
            "っ" -> "つ"
            "ツ" -> "ッ"
            "ッ" -> "ツ"
            "や" -> "ゃ"
            "ゆ" -> "ゅ"
            "よ" -> "ょ"
            "ヤ" -> "ャ"
            "ユ" -> "ュ"
            "ヨ" -> "ョ"
            "あ" -> "ぁ"
            "い" -> "ぃ"
            "う" -> "ぅ"
            "え" -> "ぇ"
            "お" -> "ぉ"

            // 假名浊音化 (か行 -> が行)
            "か" -> "が"
            "き" -> "ぎ"
            "く" -> "ぐ"
            "け" -> "げ"
            "こ" -> "ご"
            "カ" -> "ガ"
            "キ" -> "ギ"
            "ク" -> "グ"
            "ケ" -> "ゲ"
            "コ" -> "ゴ"

            // さ行 -> ざ行
            "さ" -> "ざ"
            "し" -> "じ"
            "す" -> "ず"
            "せ" -> "ぜ"
            "そ" -> "ぞ"

            // た行 -> だ行
            "た" -> "だ"
            "ち" -> "ぢ"
            "つ" -> "づ"
            "て" -> "で"
            "と" -> "ど"

            // は行 -> ば行 -> ぱ行 循环
            "は" -> "ば"
            "ば" -> "ぱ"
            "ぱ" -> "は"
            "ひ" -> "び"
            "び" -> "ぴ"
            "ぴ" -> "ひ"
            "ふ" -> "ぶ"
            "ぶ" -> "ぷ"
            "ぷ" -> "ふ"
            "へ" -> "べ"
            "べ" -> "ぺ"
            "ぺ" -> "he"
            "ほ" -> "ぼ"
            "ぼ" -> "ぽ"
            "ぽ" -> "ほ"

            // 如果没有匹配的，保持原样
            else -> char
        }
    }

    private fun matchAndFilter() {
        if (currentInput.isEmpty()) {
            filteredTexts = listOf()
            filteredIndex = 0
            display.text = ""
            return
        }

        val matchedList = allTexts.filter { it.startsWith(currentInput) }
        val maxAllowedSize = if (currentInput.length == 1) 3 else 4
        filteredTexts = matchedList.take(maxAllowedSize)
        
        filteredIndex = 0 
        updateDisplayResult()
    }

    private fun updateDisplayResult() {
        if (currentInput.isEmpty()) {
            display.text = ""
            return
        }

        if (filteredTexts.isEmpty()) {
            display.text = currentInput
            return
        }

        val matchText = filteredTexts[filteredIndex]
        display.text = "$currentInput → $matchText"
    }
}
