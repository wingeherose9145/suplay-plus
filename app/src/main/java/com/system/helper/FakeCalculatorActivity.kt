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

    private val secretSequence = listOf("π", "π", "π", "π", "π")

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
            R.id.btnSin to "∞",
            R.id.btnCos to "π",
            R.id.btnTan to "∑",
            R.id.btnPi to "∏",

            R.id.btnLog to "∫",
            R.id.btnLn to "∂",
            R.id.btnE to "√",
            R.id.btnSqrt to "Δ",

            R.id.btn7 to "∇",
            R.id.btn8 to "∀",
            R.id.btn9 to "∃",
            R.id.btnDivide to "Ω",

            R.id.btn4 to "λ",
            R.id.btn5 to "Φ",
            R.id.btn6 to "ρ",
            R.id.btnMultiply to "ω",

            R.id.btn1 to "ν",
            R.id.btn2 to "ℏ",
            R.id.btn3 to "Ψ",
            R.id.btnMinus to "α",

            R.id.btn0 to "β",
            R.id.btnDot to "γ",
            R.id.btnEqual to "μ",
            R.id.btnPlus to "⇌",

            R.id.btnAbs to "→",
            R.id.btnPow to "↑",
            R.id.btnFact to "↓",
            R.id.btnClear to "♂",

            R.id.btnMod to "♀",
            R.id.btnRad to "☉",
            R.id.btnDeg to "⊕",
            R.id.btnLn2 to "☾",

            R.id.btnAsin to "♃",
            R.id.btnAcos to "♄",
            R.id.btnAtan to "☄",
            R.id.btnPhi to "Å",

            R.id.btnPowY to "℃",
            R.id.btnTenPow to "‰",
            R.id.btnEPow to "∅",
            R.id.btnAbsX to "∈",

            R.id.btnSigma to "∉",
            R.id.btnIntegral to "⊂",
            R.id.btnDiff to "∩",
            R.id.btnInf to "∪",

            R.id.btnMatrix to "∧",
            R.id.btnVector to "∨",
            R.id.btnRand to "⇔",
            R.id.btnClose to "σ"
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
