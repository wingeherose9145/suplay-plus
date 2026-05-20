package com.system.helper

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
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
        "ワ", "ヲ", "ン", "ー", "号/促"
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
        
        // 触摸控制台：短按退格，屏幕空白时切换平片假名
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
                    val firstChar = if (trimmed.startsWith("{") && trimmed.length > 1) {
                        trimmed.substring(1, 2)
                    } else {
                        trimmed.first().toString()
                    }
                    
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
            button.setOnClickListener { handleButtonClick(textValue) }
        }
    }

    private fun setupSpecialLongClick() {
        for (button in buttonList) {
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

    private fun handleButtonClick(value: String) {
        when (value) {
            "◀" -> {
                if (filteredTexts.isNotEmpty()) {
                    filteredIndex = if (filteredIndex - 1 < 0) filteredTexts.size - 1 else filteredIndex - 1
                    updateDisplayResult()
                }
            }
            "▶" -> {
                if (filteredTexts.isNotEmpty()) {
                    filteredIndex = (filteredIndex + 1) % filteredTexts.size
                    updateDisplayResult()
                }
            }
            "假名" -> {
                isHiragana = !isHiragana
                refreshButtonLabels()
            }
            "号/促" -> {
                if (currentInput.isNotEmpty()) {
                    val lastChar = currentInput.last().toString()
                    val converted = convertToTransformChar(lastChar)
                    currentInput = currentInput.substring(0, currentInput.length - 1) + converted
                    matchAndFilter()
                }
            }
            else -> {
                if (value.isNotEmpty()) {
                    currentInput += value
                    
                    inputSequence.add(value)
                    if (inputSequence.size > 5) inputSequence.removeAt(0)
                    if (inputSequence == secretSequence) unlocked = true

                    matchAndFilter()
                }
            }
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

        val matchedList = subList.filter { line ->
            val cleanLine = line.replace("{", "").replace("}", "")
            cleanLine.startsWith(currentInput)
        }

        val maxAllowedSize = if (currentInput.length == 1) 3 else 4
        filteredTexts = matchedList.take(maxAllowedSize)
        
        filteredIndex = 0 
        updateDisplayResult()
    }

    private fun convertToTransformChar(char: String): String {
        return when (char) {
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
            "ェ" -> "电"
            "オ" -> "ォ"
            "ォ" -> "オ"
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
            "ゾ" -> "苏"
            "た" -> "だ"
            "だ" -> "た"
            "ち" -> "ぢ"
            "ぢ" -> "ち"
            "て" -> "で"
            "で" -> "て"
            "と" -> "ど"
            "ど" -> "と"
            "タ" -> "ダ"
            "ダ" -> "单"
            "チ" -> "ヂ"
            "ヂ" -> "チ"
            "テ" -> "デ"
            "デ" -> "テ"
            "ト" -> "ド"
            "ド" -> "ト"
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
            "フ" -> "布"
            "布" -> "プ"
            "プ" -> "フ"
            "へ" -> "ベ"
            "ベ" -> "ペ"
            "ペ" -> "ヘ"
            "ホ" -> "ボ"
            "ボ" -> "ポ"
            "ポ" -> "ホ"
            else -> char
        }
    }

    /**
     * 🌟 动态高亮逻辑：
     * 去除大括号 {} 符号本体，高亮长度严格匹配用户当前已输入字符的长度
     */
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

        // 擦除隐藏标识符 {}
        val cleanDisplayStr = matchText.replace("{", "").replace("}", "")

        val spannable = SpannableString(cleanDisplayStr)
        
        // 动态根据当前已输入的假名长度进行高亮染色
        val highlightLength = currentInput.length

        if (highlightLength <= cleanDisplayStr.length) {
            val goldColor = 0xFFFFD700.toInt() // 璀璨亮金色
            
            spannable.setSpan(
                ForegroundColorSpan(goldColor),
                0,
                highlightLength,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        display.text = spannable
    }
}
