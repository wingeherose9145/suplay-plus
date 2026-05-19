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
    private var currentInput = ""          // 当前输入的假名
    private var filteredTexts = listOf<String>() // 动态限制数量后的匹配子集
    private var filteredIndex = 0          // 当前翻页指针

    // 50音图字符定义（10行5列）
    private val hiraganaList = listOf(
        "あ", "い", "う", "え", "お", 
        "か", "き", "く", "け", "こ", 
        "さ", "し", "す", "せ", "そ", 
        "た", "ち", "つ", "て", "と", 
        "な", "に", "ぬ", "ね", "の", 
        "は", "ひ", "ふ", "へ", "ほ", 
        "ま", "み", "む", "め", "も", 
        "や", "◀", "よ", "▶", "よ", // 第2位 ◀，第4位 ▶
        "ら", "り", "る", "れ", "ろ", 
        "わ", "を", "ん", "假名", "预留"
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
        "ワ", "ヲ", "ン", "假名", "预留"
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
        
        loadTextLibrary()
        
        // 初始状态完全空白，没有任何提示
        display.text = ""

        scanAllButtons(window.decorView.findViewById(android.R.id.content))
        refreshButtonLabels()
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
            // 保底模拟数据
            allTexts = listOf("あさ", "あおい", "ありがとう", "いぬ", "うえ")
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

    private fun handleButtonClick(value: String, index: Int) {
        when (index) {
            // 第8行第2位 (索引 36) -> 上一个切换
            36 -> {
                if (filteredTexts.isNotEmpty()) {
                    filteredIndex = if (filteredIndex - 1 < 0) filteredTexts.size - 1 else filteredIndex - 1
                    updateDisplayResult()
                }
            }
            // 第8行第4位 (索引 38) -> 下一个切换
            38 -> {
                if (filteredTexts.isNotEmpty()) {
                    filteredIndex = (filteredIndex + 1) % filteredTexts.size
                    updateDisplayResult()
                }
            }
            // 第10行第4位 (索引 48) -> 切换假名
            48 -> {
                isHiragana = !isHiragana
                refreshButtonLabels()
            }
            // 第10行第5位 (索引 49) -> 预留/退格/解锁
            49 -> {
                if (unlocked) {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    // 退格功能
                    if (currentInput.isNotEmpty()) {
                        currentInput = currentInput.substring(0, currentInput.length - 1)
                        matchAndFilter()
                    }
                }
            }
            // 普通50音输入键
            else -> {
                if (value != "◀" && value != "▶" && value != "假名" && value != "预留") {
                    currentInput += value
                    
                    // 暗号检测
                    inputSequence.add(value)
                    if (inputSequence.size > 5) inputSequence.removeAt(0)
                    if (inputSequence == secretSequence) unlocked = true

                    matchAndFilter()
                }
            }
        }
    }

    /**
     * 核心筛选算法（已按照最新要求调优）：
     * 1. 使用 startsWith 严格限制必须以输入开头
     * 2. 1个字限制3个，2个字以上限制4个结果
     */
    private fun matchAndFilter() {
        if (currentInput.isEmpty()) {
            filteredTexts = listOf()
            filteredIndex = 0
            display.text = ""
            return
        }

        // 🛠️ 调整：改用 startsWith 进行严格的开头匹配。
        // 这能保证如果 TXT 里顺序是 あさ、あおい... 那筛选出来排第一的永远是 あさ
        val matchedList = allTexts.filter { it.startsWith(currentInput) }

        // 动态判定最大允许数量限制
        val maxAllowedSize = if (currentInput.length == 1) 3 else 4
        
        // 截取前 N 个匹配项，后面的直接丢弃，不加入切换循环
        filteredTexts = matchedList.take(maxAllowedSize)
        
        filteredIndex = 0 
        updateDisplayResult()
    }

    /**
     * 单行最大化精简渲染
     */
    private fun updateDisplayResult() {
        if (currentInput.isEmpty()) {
            display.text = ""
            return
        }

        // 🛠️ 调整：如果库里完全没有以 currentInput 开头的单词，
        // 则直接干净显示用户打出的内容本身，没有任何额外修饰
        if (filteredTexts.isEmpty()) {
            display.text = currentInput
            return
        }

        // 匹配成功时：极简单行合并显示
        val matchText = filteredTexts[filteredIndex]
        display.text = "$currentInput → $matchText"
    }
}
