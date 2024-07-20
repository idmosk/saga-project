package io.github.idmosk.saga.queue

import io.github.idmosk.saga.queue.provider.Provider
import io.github.idmosk.saga.queue.provider.SimpleProvider

class SimpleProviderTest : SpiTest() {
    private val provider = SimpleProvider()

    override fun getProvider(): Provider {
        return provider
    }
}
