package io.github.idmosk.saga.api.sagas.notsuspendable

interface TwoStepsOkRolledBackNotSuspendSaga : TwoStepsNotSuspendSaga {
    override fun forward2(id: String): Boolean {
        return false
    }
}

class TwoStepsOkRolledBackNotSuspendSagaImpl : TwoStepsOkRolledBackNotSuspendSaga
