package com.ioszhuyin.keyboard

import android.content.Intent
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val enableBtn = findViewById<Button>(R.id.btn_enable_keyboard)
        val switchBtn = findViewById<Button>(R.id.btn_switch_keyboard)
        val infoText = findViewById<TextView>(R.id.tv_info)

        enableBtn.setOnClickListener {
            val intent = Intent(android.provider.Settings.ACTION_INPUT_METHOD_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }

        switchBtn.setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }

        infoText.text = buildString {
            appendLine("動態注音鍵盤")
            appendLine()
            appendLine("使用方法：")
            appendLine("1. 點擊「開啟鍵盤設定」")
            appendLine("2. 在 Android 的輸入法清單中啟用本鍵盤")
            appendLine("3. 返回後點擊「切換鍵盤」")
            appendLine("4. 選擇「動態注音鍵盤」")
            appendLine()
            appendLine("Android 會對所有第三方鍵盤顯示標準安全提醒。")
            appendLine("本鍵盤目前不需要網路權限，輸入內容只在裝置上處理，不會上傳。")
        }
    }
}
