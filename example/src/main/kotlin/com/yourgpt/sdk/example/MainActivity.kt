package com.yourgpt.sdk.example

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Redirect to HomeScreenActivity
        val intent = Intent(this, HomeScreenActivity::class.java)
        startActivity(intent)
        finish()
    }
}