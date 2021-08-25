package dev.putra.app.webview

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.putra.dev.webview.R
import kotlinx.android.synthetic.main.activity_form.*

class FormActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_form)
        et_url.setText("")

        button_first.setOnClickListener {
            val url = et_url.text.toString()
            if (url.isNotEmpty())
                Intent(this,MainActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_URL, url)
                }.also { startActivity(it) }
            else
                Toast.makeText(this, "URL TIDAK BOLEH KOSONG!", Toast.LENGTH_SHORT).show()
        }
    }

}