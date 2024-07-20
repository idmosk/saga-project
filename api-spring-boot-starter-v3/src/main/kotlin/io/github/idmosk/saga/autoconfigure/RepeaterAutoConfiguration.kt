package io.github.idmosk.saga.autoconfigure

import io.github.idmosk.saga.api.repeater.Repeater
import io.github.idmosk.saga.autoconfigure.properties.RepeaterProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import io.github.idmosk.saga.queue.provider.Provider as QueueProvider
import io.github.idmosk.saga.storage.provider.Provider as StorageProvider

@Configuration
@ConditionalOnProperty(value = ["io.github.idmosk.saga.api.repeater.enabled"], havingValue = "true")
@EnableConfigurationProperties(RepeaterProperties::class)
@ConditionalOnBean(StorageProvider::class, QueueProvider::class)
open class RepeaterAutoConfiguration(
    private val repeaterProperties: RepeaterProperties,
) {
    @Bean
    @ConditionalOnMissingBean
    open fun repeater(
        storageProvider: StorageProvider,
        queueProvider: QueueProvider,
    ): Repeater {
        return Repeater(
            storageProvider,
            queueProvider,
            repeaterProperties.periodSeconds,
            repeaterProperties.fetchSize,
        )
    }
}
