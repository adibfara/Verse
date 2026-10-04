package com.github.adibfara.verse.app.di

import com.github.adibfara.verse.app.ExternalOpenRequests
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

fun initializeDependencies() {
    if (GlobalContext.getOrNull() != null) return
    startKoin { modules(appModule) }
}

fun externalOpenRequests(): ExternalOpenRequests = GlobalContext.get().get()
