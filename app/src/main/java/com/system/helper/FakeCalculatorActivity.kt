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

    // 按首字母分类的 Map 词库结构
    private var allTextsMap = mutableMapOf<String, MutableList<String>>()
    
    private var currentInput = ""          
    private var filteredTexts = listOf<String>() 
    private var filteredIndex = 0          

    // 50音图标准矩阵定义
    private val hiraganaList = listOf(
        "あ", "い", "う", "え", "お", 
        "か", "き", "く", "け", "こ", 
        "さ", "し", "す", "せ", "そ", 
        "た", "ち", "つ", "て", "と", 
        "な", "に", "ぬ", "ね", "の", 
        "は", "ひ", "ふ", "へ", "ほ", 
        "ま", "み", "む", "め", "も", 
        "や", "ゆ", "よ", "◀", "▶", 
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
        "ヤ", "ユ", "ヨ", "◀", "▶",
        "ラ", "リ", "ル", "レ", "ロ",
        "ワ", "ヲ", "ン", "ー", "号/促" // 🌟 片假名状态下，这里白纸黑字写着 "ー"
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
        
        // 方案一完美运行：触摸控制台
        display.setOnClickListener {
            if (currentInput.isNotEmpty()) {
                currentInput = currentInput.substring(0, currentInput.length - 1)
                matchAndFilter()
            } else {
                isHiragana = !isHiragana
                refreshButtonLabels()
            }
        }

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
        allTextsMap.clear()
        try {
            val inputStream = assets.open("random_texts.txt")
            val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val trimmed = line!!.trim()
                if (!trimmed.startsWith("#") && trimmed.isNotEmpty()) {
                    val firstChar = trimmed.first().toString()
                    if (!allTextsMap.containsKey(firstChar)) {
                        allTextsMap[firstChar] = mutableListOf()
                    }
                    allTextsMap[firstChar]?.add(trimmed)
                }
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
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
            // 🌟 核心修正：不传死板的 index 数字，直接把动态赋予的文本 value 传给点击事件处理
            button.setOnClickListener { handleButtonClick(textValue) }
        }
    }

    // 绑定长按后门进播放器（针对最后一行的变音键）
    private fun setupSpecialLongClick() {
        for (button in buttonList) {
            // 只要按键文字是“号/促”，就给它赋予长按后门，更加安全独立
            if (button.text == "号/促") {
                button.setOnLongClickListener {
                    if (unlocked) {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                    true
                }
            }
        }
    }

    /**
     * 🌟 核心交互逻辑升级：完全采用行为文本(value)作为分支判定，从根本上杜绝错位
     */
    private fun handleButtonClick(value: String) {
        when (value) {
            // 1. 翻页键：上一个
            "◀" -> {
                if (filteredTexts.isNotEmpty()) {
                    filteredIndex = if (filteredIndex - 1 < 0) filteredTexts.size - 1 else filteredIndex - 1
                    updateDisplayResult()
                }
            }
            // 2. 翻页键：下一个
            "▶" -> {
                if (filteredTexts.isNotEmpty()) {
                    filteredIndex = (filteredIndex + 1) % filteredTexts.size
                    updateDisplayResult()
                }
            }
            // 3. 切换假名键：只有按钮文字明确显示为“假名”时，才会触发切换平片状态
            "假名" -> {
                isHiragana = !isHiragana
                refreshButtonLabels()
            }
            // 4. 变音/变换键：短按进行清/浊/促循环变换
            "号/促" -> {
                if (currentInput.isNotEmpty()) {
                    val lastChar = currentInput.last().toString()
                    val converted = convertToTransformChar(lastChar)
                    currentInput = currentInput.substring(0, currentInput.length - 1) + converted
                    matchAndFilter()
                }
            }
            // 5. 普通假名输入分支：包含所有的清音、以及动态变出来的长音符号 "ー"
            else -> {
                // 防御拦截：确保不是遗留的功能字符
                if (value.isNotEmpty()) {
                    currentInput += value
                    
                    // 暗号队列检测
                    inputSequence.add(value)
                    if (inputSequence.size > 5) inputSequence.removeAt(0)
                    if (inputSequence == secretSequence) unlocked = true

                    matchAndFilter()
                }
            }
        }
    }

    private fun convertToTransformChar(char: String): String {
        return when (char) {
            // === 促音、小字、清音的闭环互转 (平假名) ===
            "つ" -> "っ"
            "っ" -> "つ"
            "や" -> "ゃ"
            "ゃ" -> "や"
            "ゆ" -> "ゅ"
            "ゅ" -> "ゆ"
            "よ" -> "ょ"
            "ょ" -> "よ"
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

            // === 促音、小字、清音的闭环互转 (片假名) ===
            "ツ" -> "ッ"
            "ッ" -> "ツ"
            "ヤ" -> "ャ"
            "ャ" -> "ヤ"
            "ユ" -> "ュ"
            "ュ" -> "ユ"
            "ヨ" -> "ョ"
            "ョ" -> "ヨ"
            "ア" -> "ァ"
            "ァ" -> "ア"
            "イ" -> "ィ"
            "ィ" -> "ア"
            "ウ" -> "ゥ"
            "ゥ" -> "ウ"
            "エ" -> "ェ"
            "ェ" -> "エ"
            "オ" -> "ォ"
            "ォ" -> "オ"

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
            "グ" -> "ク"
            "ケ" -> "ゲ"
            "ゲ" -> "ケ"
            "コ" -> "ゴ"
            "ゴ" -> "コ"

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
            
            "サ" -> "ザ"
            "ザ" -> "サ"
            "シ" -> "ジ"
            "ジ" -> "シ"
            "ス" -> "ズ"
            "ズ" -> "ス"
            "セ" -> "ゼ"
            "ゼ" -> "セ"
            "ソ" -> "ゾ"
            "ゾ" -> "ソ"

            // === た行 <-> だ行 闭环 ===
            "た" -> "だ"
            "だ" -> "た"
            "ち" -> "ぢ"
            "ぢ" -> "ち"
            "て" -> "で"
            "で" -> "て"
            "と" -> "ど"
            "ど" -> "と"
            
            "タ" -> "ダ"
            "ダ" -> "タ"
            "チ" -> "ヂ"
            "ヂ" -> "チ"
            "テ" -> "デ"
            "送" -> "テ"
            "ト" -> "ド"
            "ド" -> "ト"

            // === は行 -> ば行 -> ぱ行 -> は行 三向循环 ===
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
            
            "ハ" -> "バ"
            "バ" -> "パ"
            "パ" -> "ハ"
            "ヒ" -> "ビ"
            "ビ" -> "ピ"
            "ピ" -> "ヒ"
            "フ" -> "ブ"
            "ブ" -> "プ"
            "プ" -> "フ"
            "ヘ" -> "ベ"
            "ベ" -> "ベ"
            "ヘ" -> "ヘ"
            "ホ" -> "ボ"
            "ボ" -> "ポ"
            "ポ" -> "ホ"

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

        val firstChar = currentInput.first().toString()
        val subList = allTextsMap[firstChar] ?: listOf<String>()

        val matchedList = subList.filter { it.startsWith(currentInput) }
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
