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

    // 解锁暗号序列（保留后门功能：顺序输入这5个音解锁）
    private val secretSequence = listOf("あ", "い", "う", "え", "お") 

    // 内容库全量列表
    private var allTexts = listOf<String>()
    
    // ================= 新增：匹配与检索状态 =================
    private var currentInput = ""          // 当前输入的假名序列
    private var filteredTexts = listOf<String>() // 筛选后的内容子集
    private var filteredIndex = 0          // 当前处于筛选列表的第几页

    // 50音图字符定义（10行5列）
    private val hiraganaList = listOf(
        "あ", "い", "う", "え", "お", // 1
        "か", "き", "く", "け", "こ", // 2
        "さ", "し", "す", "せ", "そ", // 3
        "た", "ち", "つ", "て", "と", // 4
        "な", "に", "ぬ", "ね", "の", // 5
        "は", "ひ", "ふ", "へ", "ほ", // 6
        "ま", "み", "む", "め", "も", // 7
        "や", "◀", "よ", "▶", "よ", // 8 (第2位是◀，第4位是▶) 注：原本的ゆ/よ位置腾出，这里填补占位
        "ら", "り", "る", "れ", "ろ", // 9
        "わ", "を", "ん", "假名", "预留" // 10
    )

    private val katakanaList = listOf(
        "ア", "イ", "乌", "エ", "オ",
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
        
        // 初始状态显示提示或清空
        display.text = "请输入假名检索..."

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
                if (!line!!.trim().startsWith("#") && line!!.isNotEmpty()) {
                    list.add(line!!)
                }
            }
            reader.close()
            allTexts = list
        } catch (e: Exception) {
            e.printStackTrace()
            allTexts = listOf("あさ（朝）", "いぬ（犬）", "うえ（上）", "えき（駅）", "おかし（お菓子）")
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
        switch (index) {
            // 第8行第2位 (索引 36) -> 上一个
            36 -> {
                if (filteredTexts.isNotEmpty()) {
                    filteredIndex = if (filteredIndex - 1 < 0) filteredTexts.size - 1 else filteredIndex - 1
                    updateDisplayResult()
                }
            }
            // 第8行第4位 (索引 38) -> 下一个
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
                    // 预留功能：这里我们先定义为【退格键】，方便输错时回退
                    if (currentInput.isNotEmpty()) {
                        currentInput = currentInput.substring(0, currentInput.length - 1)
                        matchAndFilter()
                    }
                }
            }
            // 普通50音输入键
            else -> {
                // 排除功能键冲突导致的误输入
                if (value != "◀" && value != "▶" && value != "假名" && value != "预留") {
                    currentInput += value
                    
                    // 暗号流检测（保留后门）
                    inputSequence.add(value)
                    if (inputSequence.size > 5) inputSequence.removeAt(0)
                    if (inputSequence == secretSequence) unlocked = true

                    matchAndFilter()
                }
            }
        }
    }

    /**
     * 根据当前输入的 currentInput 去内容库全量筛选
     */
    private fun matchAndFilter() {
        if (currentInput.isEmpty()) {
            filteredTexts = listOf()
            filteredIndex = 0
            display.text = "请输入假名检索..."
            return
        }

        // 核心匹配算法：筛选出内容库里包含当前输入字符，或者以当前输入开头的文本
        // 这里采用 contains(包含)，如果你需要严格的开头匹配，可以改成 startsWith
        filteredTexts = allTexts.filter { it.contains(currentInput) }
        filteredIndex = 0 // 重置到第一条

        updateDisplayResult()
    }

    /**
     * 将当前的输入信息和匹配结果渲染到屏幕上
     */
    private fun updateDisplayResult() {
        val builder = StringBuilder()
        // 第一行：显示用户当前打出来的假名
        builder.append("输入: $currentInput\n")
        
        // 第二、三行：显示匹配到的内容库文本和页码
        if (filteredTexts.isNotEmpty()) {
            val matchText = filteredTexts[filteredIndex]
            builder.append("匹配 [${filteredIndex + 1}/${filteredTexts.size}]:\n$matchText")
        } else {
            builder.append("\n(未找到匹配内容)")
        }

        display.text = builder.toString()
    }
}
