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

    private val secretSequence = listOf("𝑠θ", "π", "7", "㏒", "√")

    private val randomTexts = listOf(
    "E = mc²",
    "F = ma",
    "PV = nRT",
    "ΔG = ΔH - TΔS",
    "sin²θ + cos²θ = 1",
    "∫ f(x)dx = F(x) + C",
    "lim(x→0) sinx/x = 1",
    "d/dx (xⁿ) = n xⁿ⁻¹",
    "∇·E = ρ/ε₀",
    "∇×E = -∂B/∂t",
    "iħ ∂ψ/∂t = Hψ",
    "E = hf",
    "c = λf",
    "pV = NkT",
    "a² + b² = c²",
    "σ = εE",
    "V = IR",
    "Q = mcΔT",
    "S = k lnΩ",
    "λ = h/p"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        setupButtons()
        setupClear()
        setupUnlock()
    }

    // =========================
    // 统一按钮映射（核心优化）
    // =========================
    private fun setupButtons() {

        val map = mapOf(
            R.id.btnSin to "𝑠θ",
            R.id.btnCos to "𝑐θ",
            R.id.btnTan to "𝑡θ",
            R.id.btnPi to "π",

            R.id.btnLog to "㏒",
            R.id.btnLn to "ℓn",
            R.id.btnE to "ℯ",
            R.id.btnSqrt to "√",

            R.id.btn7 to "7",
            R.id.btn8 to "8",
            R.id.btn9 to "9",
            R.id.btnDivide to "÷",

            R.id.btn4 to "4",
            R.id.btn5 to "5",
            R.id.btn6 to "6",
            R.id.btnMultiply to "×",

            R.id.btn1 to "1",
            R.id.btn2 to "2",
            R.id.btn3 to "3",
            R.id.btnMinus to "−",

            R.id.btn0 to "0",
            R.id.btnDot to ".",
            R.id.btnEqual to "=",
            R.id.btnPlus to "+",

            R.id.btnAbs to "‖x‖",
            R.id.btnPow to "x²",
            R.id.btnFact to "!",
            R.id.btnClear to "C",

            R.id.btnMod to "%",
            R.id.btnRad to "∡r",
            R.id.btnDeg to "°",
            R.id.btnLn2 to "㏑2",

            R.id.btnAsin to "𝑠⁻¹",
            R.id.btnAcos to "𝑐⁻¹",
            R.id.btnAtan to "𝑡⁻¹",
            R.id.btnPhi to "φ",

            R.id.btnPowY to "xʸ",
            R.id.btnTenPow to "10ˣ",
            R.id.btnEPow to "eˣ",
            R.id.btnAbsX to "|x|",

            R.id.btnSigma to "Σ",
            R.id.btnIntegral to "∫",
            R.id.btnDiff to "∂x",
            R.id.btnInf to "∞",

            R.id.btnMatrix to "[M]",
            R.id.btnVector to "[V]",
            R.id.btnRand to "∿",
            R.id.btnClose to "OFF"
        )

        map.forEach { (id, value) ->
            findViewById<Button>(id).setOnClickListener {
                press(value)
            }
        }
    }

    private fun setupClear() {
        findViewById<Button>(R.id.btnClear).setOnClickListener {
            inputSequence.clear()
            display.text = randomTexts.random()
        }
    }

    private fun setupUnlock() {
        findViewById<Button>(R.id.btnEqual).setOnLongClickListener {
            if (unlocked) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            true
        }
    }

    private fun press(value: String) {

        inputSequence.add(value)

        if (inputSequence.size > 5) {
            inputSequence.removeAt(0)
        }

        pressCount++

        if (pressCount % 3 == 0) {
            display.text = randomTexts.random()
        }

        if (inputSequence == secretSequence) {
            unlocked = true
            display.text = "Scientific Mode"
        }
    }
}
