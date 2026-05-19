package com.system.helper

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.google.android.material.button.MaterialButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class FakeCalculatorActivity : AppCompatActivity() {

    private lateinit var display: TextView
    private val inputSequence = mutableListOf<String>()
    private var pressCount = 0
    private var unlocked = false

    // 解锁暗号序列
    private val secretSequence = listOf("∞", "π", "∇", "∫", "Δ")

    private val randomTexts = listOf(
        "E = mc²", "F = ma", "PV = nRT", "ΔG = ΔH - TΔS",
        "sin²θ + cos²θ = 1", "∫ f(x)dx = F(x) + C", "lim(x→0) sinx/x = 1",
        "d/dx (xⁿ) = n xⁿ⁻¹", "∇·E = ρ/ε₀", "∇×E = -∂B/∂t",
        "iħ ∂ψ/∂t = Hψ", "E = hf", "c = λf", "pV = NkT",
        "a² + b² = c²", "σ = εE", "V = IR", "Q = mcΔT",
        "S = k lnΩ", "λ = h/p"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 全屏沉浸式无状态栏遮挡
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN 
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        setContentView(R.layout.activity_fake_calculator)

        display = findViewById(R.id.display)
        display.text = randomTexts.random()

        // 🌟【方案A核心】：自动遍历界面上的所有组件，动态绑定点击事件
        autoBindAllButtons(window.decorView.findViewById(android.R.id.content))

        setupSpecialActions()
    }

    /**
     * 递归扫描整个布局树，只要发现是 MaterialButton，就自动接管它的点击事件
     * 彻底干掉硬编码映射表！
     */
    private fun autoBindAllButtons(view: View) {
        if (view is MaterialButton) {
            view.setOnClickListener {
                // 动态获取 XML 里面配置的文字，直接丢给处理函数
                val buttonText = view.text.toString()
                if (buttonText != "⌫" && buttonText != "μ") { // 排除退格和等号
                    press(buttonText)
                }
            }
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                autoBindAllButtons(view.getChildAt(i))
            }
        }
    }

    private fun setupSpecialActions() {
        // 特殊处理退格键
        findViewById<MaterialButton>(R.id.btnClear).setOnClickListener {
            inputSequence.clear()
            display.text = randomTexts.random()
        }

        // 特殊处理等号键（μ）：单击触发逻辑，长按解锁
        val equalBtn = findViewById<MaterialButton>(R.id.btnEqual)
        equalBtn.setOnClickListener {
            press("μ")
        }
        equalBtn.setOnLongClickListener {
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
        
        // 没解锁时，每按3次随机刷新一下极客公式
        if (pressCount % 3 == 0) {
            display.text = randomTexts.random()
        }
        
        // 检测暗号
        if (inputSequence == secretSequence) {
            unlocked = true
            display.text = "Scientific Mode"
        }
    }
}
