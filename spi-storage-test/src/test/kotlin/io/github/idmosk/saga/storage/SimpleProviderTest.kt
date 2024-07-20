package io.github.idmosk.saga.storage

import io.github.idmosk.saga.storage.provider.Provider
import io.github.idmosk.saga.storage.provider.SimpleProvider

class SimpleProviderTest : SpiTest() {
    private val provider = SimpleProvider()

    override fun getProvider(): Provider {
        return provider
    }
}
