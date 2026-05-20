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

    // 解锁暗号序列
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
        "わ", "を", "ん", "假名", "号/促" 
    )

    private val katakanaList = listOf(
        "ア", "イ", "ウ", "エ", "オ",
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
        
        // 🛠️【优化：触摸控制台】
        // 1. 短按显示屏区域 -> 触发【退格键】（删掉最后一个输入的字）
        display.setOnClickListener {
            if (currentInput.isNotEmpty()) {
                currentInput = currentInput.substring(0, currentInput.length - 1)
                matchAndFilter()
            }
        }

        // 2. 长按显示屏区域 -> 触发【完全清空】（进入下一个全新输入状态）
        display.setOnLongClickListener {
            currentInput = ""
            matchAndFilter()
            true
        }

        loadTextLibrary()
        display.text = ""

        scanAllButtons(window.decorView.findViewById(android.R.id.content))
        refreshButtonLabels()
        setupSpecialLongClick() 
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
            // 第10行第5位 -> 短按变音键
            49 -> {
                if (currentInput.isNotEmpty()) {
                    val lastChar = currentInput.last().toString()
                    val converted = convertToTransformChar(lastChar)
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
     * 🛠️【优化：完美闭环互转模式】
     * 实现了：清音 <-> 浊音/半浊音 的无限双向循环，以及 正常字 <-> 小字 的无缝循环。
     */
    private fun convertToTransformChar(char: String): String {
        return when (char) {
            // === 促音、小字、清音的闭环互转 ===
            "つ" -> "っ"
            "っ" -> "つ"
            "ツ" -> "ッ"
            "ッ" -> "ツ"
            "や" -> "ゃ"
            "ゃ" -> "や"
            "ゆ" -> "ゅ"
            "ゅ" -> "ゆ"
            "よ" -> "ょ"
            "ょ" -> "よ"
            "ヤ" -> "ャ"
            "ャ" -> "ヤ"
            "ユ" -> "ュ"
            "ュ" -> "ユ"
            "ヨ" -> "ョ"
            "ョ" -> "ヨ"
            "あ" -> "ぁ"
            "ぁ" -> "あ"
            "い" -> "ぃ"
            "ぃ" -> "い"
            "う" -> "ぅ"
            "ぅ" -> "う"
            "え" -> "ぇ"
            "ぇ" -> "え"
            "お" -> "ぉ"
            "ぉ" -> "お"

            // === か行 <-> が行 闭环 ===
            "か" -> "が"
            "が" -> "か"
            "き" -> "ぎ"
            "ぎ" -> "き"
            "く" -> "ぐ"
            "ぐ" -> "く"
            "け" -> "げ"
            "げ" -> "け"
            "こ" -> "ご"
            "ご" -> "こ"
            
            "カ" -> "ガ"
            "ガ" -> "カ"
            "キ" -> "ギ"
            "ギ" -> "キ"
            "ク" -> "グ"
            "ぐ" -> "ク"
            "ケ" -> "ゲ"
            "ゲ" -> "ケ"
            "コ" -> "ゴ"
            "ゴ" -> "打"

            // === さ行 <-> ざ行 闭环 ===
            "さ" -> "ざ"
            "ざ" -> "さ"
            "し" -> "じ"
            "じ" -> "し"
            "す" -> "ず"
            "ず" -> "す"
            "せ" -> "ぜ"
            "ぜ" -> "せ"
            "そ" -> "ぞ"
            "ぞ" -> "そ"

            // === た行 <-> だ行 闭环 ===
            "ta" -> "だ"
            "だ" -> "た"
            "ち" -> "ぢ"
            "ぢ" -> "ち"
            "つ" -> "づ"
            "づ" -> "つ"
            "て" -> "で"
            "で" -> "て"
            "と" -> "ど"
            "ど" -> "と"

            // === は行 -> ば行 -> ぱ行 -> は行 三向完美大循环 ===
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
            "ぺ" -> "へ"
            "ほ" -> "ぼ"
            "ぼ" -> "ぽ"
            "ぽ" -> "ほ"

            // 如果没有配置的，保持原样
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
