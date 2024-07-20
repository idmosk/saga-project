package io.github.idmosk.saga.autoconfigure

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import io.github.idmosk.saga.queue.provider.Provider as QueueProvider
import io.github.idmosk.saga.storage.provider.Provider as StorageProvider

@Configuration
@ConditionalOnProperty(value = ["io.github.idmosk.saga.api.creator.enabled"], havingValue = "true")
@ConditionalOnBean(StorageProvider::class, QueueProvider::class)
open class CreatorAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    open fun creator(
        storageProvider: StorageProvider,
        queueProvider: QueueProvider,
    ): io.github.idmosk.saga.api.creator.Creator {
        return io.github.idmosk.saga.api.creator.Creator(storageProvider, queueProvider)
    }
}
