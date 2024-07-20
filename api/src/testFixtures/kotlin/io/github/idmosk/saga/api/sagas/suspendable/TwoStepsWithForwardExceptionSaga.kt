package io.github.idmosk.saga.api.sagas.suspendable

import kotlinx.coroutines.delay

interface TwoStepsWithForwardExceptionSaga : TwoStepsSaga

class TwoStepsWithForwardExceptionSagaImpl : TwoStepsWithForwardExceptionSaga {
    private var ids: MutableSet<String> = mutableSetOf()

    override suspend fun forward2(id: String): Boolean {
        if (!ids.contains(id)) {
            ids.add(id)
            throw Exception("forward2 exception")
        }
        delay(20)
        return true
    }
}
