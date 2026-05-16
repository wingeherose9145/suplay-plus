package com.system.helper

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class FakeCalculatorActivity : AppCompatActivity() {

    private lateinit var display: TextView

    private val inputSequence = mutableListOf<String>()

    // 隐藏密码组合
    private val secretSequence =
        listOf("sin", "π", "7", "log", "√")

    private var pressCount = 0
    private var unlocked = false
    private val randomTexts = listOf(

        "E = mc²",
        "F = ma",
        "PV = nRT",
        "ΔG = ΔH - TΔS",
        "sin²θ + cos²θ = 1",
        "Reality is merely an illusion.",
        "Knowledge is power.",
        "Time is relative.",
        "The universe is under no obligation to make sense.",
        "∫(a→b) f(x)dx"

    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fake_calculator)

        display = findViewById(R.id.display)

        display.text = randomTexts.random()

        setupButton(R.id.btnSin, "sin")
        setupButton(R.id.btnPi, "π")
        setupButton(R.id.btn7, "7")
        setupButton(R.id.btnLog, "log")
        setupButton(R.id.btnSqrt, "√")

        setupButton(R.id.btn1, "1")
        setupButton(R.id.btn2, "2")
        setupButton(R.id.btn3, "3")
        setupButton(R.id.btnPlus, "+")

        setupButton(R.id.btn4, "4")
        setupButton(R.id.btn5, "5")
        setupButton(R.id.btn6, "6")
        setupButton(R.id.btnMinus, "-")

        setupButton(R.id.btn8, "8")
        setupButton(R.id.btn9, "9")
        setupButton(R.id.btn0, "0")
        setupButton(R.id.btnMultiply, "×")

        setupButton(R.id.btnDot, ".")
        setupButton(R.id.btnDivide, "÷")
        findViewById<Button>(R.id.btnEqual).setOnClickListener {

            pressCount++

            if (pressCount % 3 == 0) {
               display.text = randomTexts.random()
            }
        }

        findViewById<Button>(R.id.btnEqual).setOnLongClickListener {

            if (unlocked) {

                startActivity(
                    Intent(this, MainActivity::class.java)
                )

               finish()

            }

            true
        }
        findViewById<Button>(R.id.btnClear).setOnClickListener {

            inputSequence.clear()

            display.text = randomTexts.random()
        }
    }

    private fun setupButton(buttonId: Int, value: String) {

        findViewById<Button>(buttonId).setOnClickListener {

            inputSequence.add(value)

            if (inputSequence.size > 5) {
                inputSequence.removeAt(0)
            }

            pressCount++

            // 每3次切换随机内容
            if (pressCount % 3 == 0) {
                display.text = randomTexts.random()
            }

            // 密码正确
            if (inputSequence == secretSequence) {

                unlocked = true

                display.text = "Scientific Mode"

            }
        }
    }
}
