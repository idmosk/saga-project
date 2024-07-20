package io.github.idmosk.saga.api.sagas.suspendable

import kotlinx.coroutines.delay

interface TwoStepsWithBackwardExceptionSaga : TwoStepsSaga

class TwoStepsWithBackwardExceptionSagaImpl : TwoStepsWithBackwardExceptionSaga {
    private var ids: MutableSet<String> = mutableSetOf()

    override suspend fun forward2(id: String): Boolean {
        return false
    }

    override suspend fun backward1(id: String) {
        if (!ids.contains(id)) {
            ids.add(id)
            throw Exception("backward1 exception")
        }
        delay(20)
    }
}
