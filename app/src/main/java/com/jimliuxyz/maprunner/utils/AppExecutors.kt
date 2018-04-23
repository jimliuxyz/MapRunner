package com.jimliuxyz.maprunner.utils

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor
import java.util.concurrent.Executors

object AppExecutors {
    val diskIO = Executors.newSingleThreadExecutor()
    val networkIO = Executors.newFixedThreadPool(5)

    val mainThread = object : Executor {
        private val mainThreadHandler = Handler(Looper.getMainLooper())

        override fun execute(command: Runnable?) {
            mainThreadHandler.post(command)
        }
    }
}

fun doIO(block: () -> Unit) {
    AppExecutors.diskIO.execute {
        block()
    }
}

fun doNetwork(block: () -> Unit) {
    AppExecutors.networkIO.execute {
        block()
    }
}

fun doMain(block: () -> Unit) {
    AppExecutors.mainThread.execute {
        block()
    }
}