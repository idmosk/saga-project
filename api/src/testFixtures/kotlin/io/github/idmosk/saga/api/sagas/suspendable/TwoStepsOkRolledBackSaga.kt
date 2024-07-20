package io.github.idmosk.saga.api.sagas.suspendable

import kotlinx.coroutines.delay

interface TwoStepsOkRolledBackSaga : TwoStepsSaga {
    override suspend fun forward2(id: String): Boolean {
        delay(20)
        return false
    }
}

class TwoStepsOkRolledBackSagaImpl : TwoStepsOkRolledBackSaga
