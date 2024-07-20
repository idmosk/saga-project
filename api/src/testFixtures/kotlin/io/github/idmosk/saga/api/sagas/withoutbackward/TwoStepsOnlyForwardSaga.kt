package io.github.idmosk.saga.api.sagas.withoutbackward

import kotlinx.coroutines.delay

interface TwoStepsOnlyForwardSaga : io.github.idmosk.saga.api.ISaga {
    suspend fun forward1(id: String): Boolean {
        delay(20)
        return true
    }

    suspend fun forward2(id: String): Boolean {
        delay(20)
        return true
    }
}
