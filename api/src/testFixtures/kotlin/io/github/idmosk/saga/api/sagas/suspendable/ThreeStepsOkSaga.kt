package io.github.idmosk.saga.api.sagas.suspendable

import kotlinx.coroutines.delay

interface ThreeStepsOkSaga : ThreeStepsSaga {
    override suspend fun forward1(id: String): Boolean {
        delay(1000)
        return true
    }
}

class ThreeStepsOkSagaImpl : ThreeStepsOkSaga
