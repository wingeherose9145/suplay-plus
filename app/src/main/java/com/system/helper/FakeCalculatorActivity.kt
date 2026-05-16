package com.system.helper

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class FakeCalculatorActivity : AppCompatActivity() {

    private lateinit var display: TextView

    private val inputSequence = mutableListOf<String>()
    private var pressCount = 0
    private var unlocked = false

    // 隐藏进入序列（科学按键组合）
    private val secretSequence =
        listOf("sin", "π", "7", "log", "√")

    // 顶部随机显示内容（伪科学/名言）
    private val randomTexts = listOf(
        "E = mc²",
        "F = ma",
        "PV = nRT",
        "ΔG = ΔH - TΔS",
        "sin²θ + cos²θ = 1",
        "∫ f(x)dx",
        "Reality is merely an illusion.",
        "Time is relative.",
        "Energy is conserved.",
        "The universe is under no obligation to make sense.",
        "lim(x→∞) (1 + 1/x)^x = e"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ===== 全屏隐藏状态栏 =====
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        window.decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        setContentView(R.layout.activity_fake_calculator)

        display = findViewById(R.id.display)
        display.text = randomTexts.random()

        // ===== 统一按钮绑定 =====
        fun bind(id: Int, value: String) {
            findViewById<Button>(id).setOnClickListener {
                press(value)
            }
        }

        // ===== 第一行 =====
        bind(R.id.btnSin, "sin")
        bind(R.id.btnCos, "cos")
        bind(R.id.btnTan, "tan")
        bind(R.id.btnPi, "π")

        // ===== 第二行 =====
        bind(R.id.btnLog, "log")
        bind(R.id.btnLn, "ln")
        bind(R.id.btnE, "e")
        bind(R.id.btnSqrt, "√")

        // ===== 第三行 =====
        bind(R.id.btn7, "7")
        bind(R.id.btn8, "8")
        bind(R.id.btn9, "9")
        bind(R.id.btnDivide, "÷")

        // ===== 第四行 =====
        bind(R.id.btn4, "4")
        bind(R.id.btn5, "5")
        bind(R.id.btn6, "6")
        bind(R.id.btnMultiply, "×")

        // ===== 第五行 =====
        bind(R.id.btn1, "1")
        bind(R.id.btn2, "2")
        bind(R.id.btn3, "3")
        bind(R.id.btnMinus, "-")

        // ===== 第六行 =====
        bind(R.id.btn0, "0")
        bind(R.id.btnDot, ".")
        bind(R.id.btnEqual, "=")
        bind(R.id.btnPlus, "+")

        // ===== 第七行 =====
        bind(R.id.btnAbs, "abs")
        bind(R.id.btnPow, "x²")
        bind(R.id.btnFact, "!")
        bind(R.id.btnClear, "C")

        // ===== 第八行 =====
        bind(R.id.btnMod, "mod")
        bind(R.id.btnRad, "rad")
        bind(R.id.btnDeg, "deg")
        bind(R.id.btnLn2, "ln2")

        // ===== 第九行 =====
        bind(R.id.btnAsin, "sin⁻¹")
        bind(R.id.btnAcos, "cos⁻¹")
        bind(R.id.btnAtan, "tan⁻¹")
        bind(R.id.btnPhi, "φ")

        // ===== 第十行 =====
        bind(R.id.btnPowY, "xʸ")
        bind(R.id.btnTenPow, "10ˣ")
        bind(R.id.btnEPow, "eˣ")
        bind(R.id.btnAbsX, "|x|")

        // ===== 第十一行 =====
        bind(R.id.btnSigma, "Σ")
        bind(R.id.btnIntegral, "∫")
        bind(R.id.btnDiff, "d/dx")
        bind(R.id.btnInf, "∞")

        // ===== 第十二行 =====
        bind(R.id.btnMatrix, "matrix")
        bind(R.id.btnVector, "vector")
        bind(R.id.btnRand, "rand")
        bind(R.id.btnClose, "CLOSE")

        // ===== 长按 "=" 才进入播放器 =====
        findViewById<Button>(R.id.btnEqual).setOnLongClickListener {
            if (unlocked) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            true
        }

        // 清空
        findViewById<Button>(R.id.btnClear).setOnClickListener {
            inputSequence.clear()
            display.text = randomTexts.random()
        }
    }

    // ===== 输入处理核心 =====
    private fun press(value: String) {

        inputSequence.add(value)

        if (inputSequence.size > 5) {
            inputSequence.removeAt(0)
        }

        pressCount++

        // 每3次输入刷新显示
        if (pressCount % 3 == 0) {
            display.text = randomTexts.random()
        }

        // 判断隐藏序列
        if (inputSequence == secretSequence) {
            unlocked = true
            display.text = "Scientific Mode"
        }
    }
}
