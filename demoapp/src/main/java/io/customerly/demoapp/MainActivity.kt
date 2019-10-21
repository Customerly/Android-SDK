package io.customerly.demoapp

import android.os.Bundle
import android.util.Log
import io.customerly.Customerly
import io.customerly.sxdependencies.SXAppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : SXAppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.button_support.setOnClickListener {
            Customerly.openSupport(this@MainActivity)
            Log.e("Customerly Android SDK", "OPEN SUPPORT")
        }

        this.button_logout.setOnClickListener {
            Customerly.logoutUser()
            Log.e("Customerly Android SDK", "LOGGED OUT")
        }

        this.button_register.setOnClickListener {
            val email = this@MainActivity.edittext_register.text.toString()
            Customerly.logoutUser {
                Customerly.registerUser(email)
            }
            Log.e("Customerly Android SDK", "REGISTERING $email")
        }
    }
}
