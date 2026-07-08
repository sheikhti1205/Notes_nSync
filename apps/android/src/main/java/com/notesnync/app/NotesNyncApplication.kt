package com.notesnync.app

import android.app.Application
import com.notesnync.app.data.DiagnosticsStore

class NotesNyncApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DiagnosticsStore.install(this)
    }
}
