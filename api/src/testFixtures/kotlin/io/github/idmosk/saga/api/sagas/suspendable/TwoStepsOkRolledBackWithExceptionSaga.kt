package io.github.idmosk.saga.api.sagas.suspendable

import kotlinx.coroutines.delay

interface TwoStepsOkRolledBackWithExceptionSaga : TwoStepsSaga {
    override suspend fun forward2(id: String): Boolean {
        delay(20)
        throw Exception("forward2 exception")
    }
}

class TwoStepsOkRolledBackWithExceptionSagaImpl : TwoStepsOkRolledBackWithExceptionSaga
