package io.github.idmosk.saga.api.sagas.suspendable

import kotlinx.coroutines.delay

interface ThreeStepsSaga : TwoStepsSaga {
    suspend fun forward3(id: String): Boolean {
        delay(20)
        return true
    }

    suspend fun backward3(id: String) {
        delay(20)
    }
}
