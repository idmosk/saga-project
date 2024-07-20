package io.github.idmosk.saga.api.sagas.suspendable

import io.github.idmosk.saga.api.ISaga
import kotlinx.coroutines.delay

interface TwoStepsSaga : io.github.idmosk.saga.api.ISaga {
    suspend fun forward1(id: String): Boolean {
        delay(20)
        return true
    }

    suspend fun backward1(id: String) {
        delay(20)
    }

    suspend fun forward2(id: String): Boolean {
        delay(20)
        return true
    }

    suspend fun backward2(id: String) {
        delay(20)
    }
}
