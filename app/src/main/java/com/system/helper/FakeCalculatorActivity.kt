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
    private var pressCount = 0
    private var unlocked = false

    // 解锁暗号序列
    private val secretSequence = listOf("あ", "い", "う", "え", "お") 

    // 内容库列表（从TXT读取）
    private var randomTexts = listOf<String>()

    // 50音图字符定义（10行5列）
    // 为了方便维护，直接定义平假名和片假名两套数组
    private val hiraganaList = listOf(
        "あ", "い", "う", "え", "お", // 行1
        "か", "き", "く", "け", "こ", // 行2
        "さ", "し", "す", "せ", "そ", // 行3
        "た", "ち", "つ", "て", "と", // 行4
        "な", "に", "ぬ", "ね", "の", // 行5
        "は", "ひ", "ふ", "へ", "ほ", // 行6
        "ま", "み", "む", "め", "も", // 行7
        "や", "ゆ", "よ", "◀", "▶", // 行8 (后两格为功能键)
        "ら", "り", "る", "れ", "ろ", // 行9
        "わ", "を", "ん", "假名", "预留" // 行10 (后两格为功能键)
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
        "ラ", "リ", "ル", "レ", "ろ",
        "ワ", "ヲ", "ン", "假名", "预留"
    )

    private var isHiragana = true // 状态锁：当前是否为平假名
    private val buttonList = mutableListOf<MaterialButton>() // 存放扫描到的50个按钮

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 全屏沉浸式无状态栏遮挡
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN 
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        setContentView(R.layout.activity_fake_calculator)

        display = findViewById(R.id.display)
        
        // 初始化TXT内容库
        loadTextLibrary()
        if (randomTexts.isNotEmpty()) {
            display.text = randomTexts.random()
        }

        // 1. 递归扫描并按顺序收集所有的 MaterialButton
        scanAllButtons(window.decorView.findViewById(android.R.id.content))
        
        // 2. 刷新按钮上显示的文字并绑定事件（默认平假名）
        refreshButtonLabels()
    }

    /**
     * 从 assets 目录中读取 txt 文件
     */
    private fun loadTextLibrary() {
        try {
            // 请确保在 app/src/main/assets/ 目录下创建了 random_texts.txt 文件
            val inputStream = assets.open("random_texts.txt")
            val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
            val list = mutableListOf<String>()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (!line!!.trim().startsWith("#") && line!!.isNotEmpty()) { // 过滤注释和空行
                    list.add(line!!)
                }
            }
            reader.close()
            randomTexts = list
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果读取失败，保底数据
            randomTexts = listOf("E = mc²", "こんにちは", "さようなら")
        }
    }

    /**
     * 深度优先遍历：将布局里的所有按钮按【代码/XML中的声明顺序】加入到集合中
     */
    private fun scanAllButtons(view: View) {
        if (view is MaterialButton) {
            buttonList.add(view)
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                scanAllButtons(view.getChildAt(i))
            }
        }
    }

    /**
     * 刷新10行5列按钮的文字，并统一绑定点击逻辑
     */
    private fun refreshButtonLabels() {
        val currentAlphabet = if (isHiragana) hiraganaList else katakanaList

        // 防止XML里按钮数量和50个对不上导致崩溃
        val maxIndex = minOf(buttonList.size, currentAlphabet.size)

        for (i in 0 until maxIndex) {
            val button = buttonList[i]
            val textValue = currentAlphabet[i]
            
            // 设置按钮文字
            button.text = textValue

            // 解绑之前的事件重新绑定
            button.setOnClickListener {
                handleButtonClick(textValue, i)
            }
        }
    }

    /**
     * 统一处理按钮点击事件
     * @param value 按钮当前的文本
     * @param index 按钮在50个格子中的绝对索引（0-49）
     */
    private fun handleButtonClick(value: String, index: Int) {
        // 根据索引或字符判断是否为特殊键
        when {
            // 第8行最后两格 (第39和第40个按键，索引为38, 39)
            index == 38 -> { // ◀ 上一个 键
                // TODO: 以后在此处定义"上一个"的切换功能
                showToast("触发：上一个")
            }
            index == 39 -> { // ▶ 下一个 键
                // TODO: 以后在此处定义"下一个"的切换功能
                showToast("触发：下一个")
            }
            
            // 第10行最后两格 (第49和第50个按键，索引为48, 49)
            index == 48 -> { // 切换键
                isHiragana = !isHiragana
                refreshButtonLabels() // 刷新键盘
            }
            index == 49 -> { // 预留键
                // TODO: 以后在此处定义新增功能
                showToast("触发：预留功能")
                
                // 顺便保留你原本的长按进主页逻辑（可以改成短按或长按，这里先做个后门示范）
                if (unlocked) {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }
            
            // 普通50音字符键
            else -> {
                press(value)
            }
        }
    }

    private fun press(value: String) {
        inputSequence.add(value)
        if (inputSequence.size > 5) {
            inputSequence.removeAt(0)
        }
        pressCount++

        // 没解锁时，每按3次随机从TXT内容库刷新一下文本
        if (pressCount % 3 == 0 && randomTexts.isNotEmpty()) {
            display.text = randomTexts.random()
        }

        // 检测暗号（根据你输入的假名序列）
        if (inputSequence == secretSequence) {
            unlocked = true
            display.text = "Scientific Mode"
        }
    }

    // 辅助提示函数
    private fun showToast(msg: String) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
    }
}
