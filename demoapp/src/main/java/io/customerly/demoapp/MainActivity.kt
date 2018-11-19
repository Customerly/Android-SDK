package io.customerly.demoapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.customerly.Customerly
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.button_support.setOnClickListener {
            Customerly.openSupport(this@MainActivity)
        }
    }
}
