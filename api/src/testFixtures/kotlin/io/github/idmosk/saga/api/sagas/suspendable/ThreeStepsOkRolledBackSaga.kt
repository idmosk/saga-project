package io.github.idmosk.saga.api.sagas.suspendable

import kotlinx.coroutines.delay

interface ThreeStepsOkRolledBackSaga : ThreeStepsSaga {
    override suspend fun forward3(id: String): Boolean {
        delay(20)
        return false
    }
}

class ThreeStepsOkRolledBackSagaImpl : ThreeStepsOkRolledBackSaga
