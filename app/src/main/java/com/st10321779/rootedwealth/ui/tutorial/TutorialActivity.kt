package com.st10321779.rootedwealth.ui.tutorial

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.st10321779.rootedwealth.R
import com.st10321779.rootedwealth.theme.ThemeManager

class TutorialActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial)
        ThemeManager.applyTheme(this, ThemeManager.getSelectedTheme(this), findViewById(android.R.id.content))
    }
}