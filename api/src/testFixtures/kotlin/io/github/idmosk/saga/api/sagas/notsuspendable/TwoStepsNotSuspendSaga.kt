package io.github.idmosk.saga.api.sagas.notsuspendable

import io.github.idmosk.saga.api.ISaga

interface TwoStepsNotSuspendSaga : io.github.idmosk.saga.api.ISaga {
    fun forward1(id: String): Boolean {
        return true
    }

    fun backward1(id: String) {
    }

    fun forward2(id: String): Boolean {
        return true
    }

    fun backward2(id: String) {
    }
}
