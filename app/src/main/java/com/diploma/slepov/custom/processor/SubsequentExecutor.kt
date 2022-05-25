package com.diploma.slepov.custom.processor

import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean

/** Обертка для стандартного экзекьютора, позволяющая отметить запуск части процессов **/
class SubsequentExecutor(private val executor: Executor) : Executor {
    private val shutdown = AtomicBoolean()
    override fun execute(command: Runnable) {
        if (shutdown.get()) {
            return
        }
        executor.execute {

            if (shutdown.get()) {
                return@execute
            }
            command.run()
        }
    }

    fun shutdown() {
        shutdown.set(true)
    }
}