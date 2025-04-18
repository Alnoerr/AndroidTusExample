package com.example.tus

import android.content.Context
import android.content.SharedPreferences

class SharedPrefsRepository(application: TusApplication) {
    val tus: SharedPreferences? = application.getSharedPreferences(
        "tus",
        Context.MODE_PRIVATE
    )
}
