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

    // 🌟 核心升级：将原 List<String> 替换为按首字符分类的 Map 结构
    private var allTextsMap = mutableMapOf<String, MutableList<String>>()
    
    // 匹配检索相关状态变量
    private var currentInput = ""          
    private var filteredTexts = listOf<String>() 
    private var filteredIndex = 0          

    // 50音图标准矩阵定义（10行5列，保证经典50音打字肌肉记忆）
    private val hiraganaList = listOf(
        "あ", "い", "う", "え", "お", // 1: あ行
        "か", "き", "く", "け", "こ", // 2: か行
        "さ", "し", "す", "せ", "そ", // 3: さ行
        "た", "ち", "つ", "て", "と", // 4: た行
        "な", "に", "ぬ", "ね", "の", // 5: な行
        "は", "ひ", "ふ", "へ", "ほ", // 6: は行
        "ま", "み", "む", "め", "も", // 7: ま行
        "や", "ゆ", "よ", "◀", "▶", // 8: や行（后两格留给翻页键 ◀ 和 ▶）
        "ら", "り", "る", "れ", "ろ", // 9: ら行
        "わ", "を", "ん", "假名", "号/促" // 10: わ行与功能键
    )

    private val katakanaList = listOf(
        "ア", "イ", "ウ", "电", "オ",
        "カ", "キ", "ク", "ケ", "コ",
        "サ", "シ", "ス", "セ", "ソ",
        "タ", "チ", "ツ", "テ", "ト",
        "ナ", "ニ", "ヌ", "ネ", "ノ",
        "ハ", "ヒ", "フ", "ヘ", "ホ",
        "マ", "ミ", "ム", "メ", "モ",
        "ヤ", "ユ", "ヨ", "◀", "▶",
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
        
        // 触摸控制台：短按退格，长按完全清空
        display.setOnClickListener {
            if (currentInput.isNotEmpty()) {
                currentInput = currentInput.substring(0, currentInput.length - 1)
                matchAndFilter()
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

    /**
     * 🌟 核心升级：按首字母散列读取词典库
     */
    private fun loadTextLibrary() {
        allTextsMap.clear()
        try {
            val inputStream = assets.open("random_texts.txt")
            val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val trimmed = line!!.trim()
                // 排除注释和空行
                if (!trimmed.startsWith("#") && trimmed.isNotEmpty()) {
                    // 获取该行文本的首字假名作为散列键
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
            // 兜底模拟数据
            val backupData = listOf("がっこう", "いっぱい", "きょう", "あさ")
            for (word in backupData) {
                val firstChar = word.first().toString()
                if (!allTextsMap.containsKey(firstChar)) {
                    allTextsMap[firstChar] = mutableListOf()
                }
                allTextsMap[firstChar]?.add(word)
            }
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
            // 第8行第4位 -> 上一个切换
            37 -> {
                if (filteredTexts.isNotEmpty()) {
                    filteredIndex = if (filteredIndex - 1 < 0) filteredTexts.size - 1 else filteredIndex - 1
                    updateDisplayResult()
                }
            }
            // 第8行第5位 -> 下一个切换
            38 -> {
                if (filteredTexts.isNotEmpty()) {
                    filteredIndex = (filteredIndex + 1) % filteredTexts.size
                    updateDisplayResult()
                }
            }
            // 第10行第4位 -> 切换平/片假名
            48 -> {
                isHiragana = !isHiragana
                refreshButtonLabels()
            }
            // 第10行第5位 -> 短按变音/变换键
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
     * 🌟 核心升级：利用首字母分类进行极速过滤，消除大词库卡顿现象
     */
    private fun matchAndFilter() {
        if (currentInput.isEmpty()) {
            filteredTexts = listOf()
            filteredIndex = 0
            display.text = ""
            return
        }

        // 直接根据输入内容的第一个字去 Map 中定位分类子集
        val firstChar = currentInput.first().toString()
        val subList = allTextsMap[firstChar] ?: listOf<String>()

        // 严格以输入文字开头的词汇过滤
        val matchedList = subList.filter { it.startsWith(currentInput) }

        // 动态数量限制逻辑：1个字限3结果，2个字以上限4结果
        val maxAllowedSize = if (currentInput.length == 1) 3 else 4
        filteredTexts = matchedList.take(maxAllowedSize)
        
        filteredIndex = 0 
        updateDisplayResult()
    }

    /**
     * 清音 <-> 浊音/半浊音，普通字 <-> 小字 完美闭环互转映射表（已做细致校对排错）
     */
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
            "意" -> "ィ"
            "ィ" -> "意"
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
            "デ" -> "テ"
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
            "ベ" -> "ペ"
            "ペ" -> "ヘ"
            "ホ" -> "ボ"
            "ボ" -> "ポ"
            "ポ" -> "ホ"

            else -> char
        }
    }

    private fun updateDisplayResult() {
        if (currentInput.isEmpty()) {
            display.text = ""
            return
        }

        // 内容库无匹配时，直接干净地回显输入的元音本身
        if (filteredTexts.isEmpty()) {
            display.text = currentInput
            return
        }

        // 单行最大化精简显示
        val matchText = filteredTexts[filteredIndex]
        display.text = "$currentInput → $matchText"
    }
}
