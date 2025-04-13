package com.example.ifbest

import android.content.Context

class SharedPrefsRepository(application: IfBestApplication) {
    val tus = application.getSharedPreferences(
        "tus",
        Context.MODE_PRIVATE
    )
}