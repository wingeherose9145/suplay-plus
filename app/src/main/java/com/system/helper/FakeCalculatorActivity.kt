package com.system.helper

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class FakeCalculatorActivity : AppCompatActivity() {

    private lateinit var display: TextView
    private var inputText = ""

    // 这里改成你的密码
    private val secretCode = "9527"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fake_calculator)

        display = findViewById(R.id.display)

        val buttons = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2,
            R.id.btn3, R.id.btn4, R.id.btn5,
            R.id.btn6, R.id.btn7, R.id.btn8,
            R.id.btn9
        )

        buttons.forEachIndexed { index, id ->
            findViewById<Button>(id).setOnClickListener {
                inputText += index.toString()
                display.text = inputText
            }
        }

        findViewById<Button>(R.id.btnClear).setOnClickListener {
            inputText = ""
            display.text = ""
        }

        findViewById<Button>(R.id.btnEqual).setOnClickListener {

            if (inputText == secretCode) {

                startActivity(
                    Intent(this, MainActivity::class.java)
                )

                finish()

            } else {

                try {
                    val result = inputText.toDoubleOrNull()

                    display.text = result?.toString() ?: "0"

                } catch (e: Exception) {
                    display.text = "0"
                }

                inputText = ""
            }
        }
    }
}
