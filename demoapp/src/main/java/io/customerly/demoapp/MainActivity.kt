package io.customerly.demoapp

import android.os.Bundle
import io.customerly.Customerly
import io.customerly.sxdependencies.SXAppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : SXAppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.button_support.setOnClickListener {
            Customerly.openSupport(this@MainActivity)
        }
    }
}
