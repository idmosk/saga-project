package io.github.idmosk.saga.api.sagas.suspendable

interface TwoStepsNokRolledBackSaga : TwoStepsSaga {
    override suspend fun forward2(id: String): Boolean {
        return false
    }

    override suspend fun backward1(id: String) {
        throw Exception("backward1 exception")
    }
}

class TwoStepsNokRolledBackSagaImpl : TwoStepsNokRolledBackSaga
