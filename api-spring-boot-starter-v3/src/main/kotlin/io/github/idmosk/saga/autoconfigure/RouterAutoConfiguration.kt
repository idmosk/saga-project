package io.github.idmosk.saga.autoconfigure

import io.github.idmosk.saga.api.router.Router
import io.github.idmosk.saga.autoconfigure.properties.RouterProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import io.github.idmosk.saga.queue.provider.Provider as QueueProvider
import io.github.idmosk.saga.storage.provider.Provider as StorageProvider

@Configuration
@ConditionalOnProperty(value = ["io.github.idmosk.saga.api.router.enabled"], havingValue = "true")
@EnableConfigurationProperties(RouterProperties::class)
@ConditionalOnBean(StorageProvider::class, QueueProvider::class)
open class RouterAutoConfiguration(
    private val routerProperties: RouterProperties,
) {
    @Bean
    @ConditionalOnMissingBean
    open fun router(
        storageProvider: StorageProvider,
        queueProvider: QueueProvider,
    ): Router {
        return Router(storageProvider, queueProvider, routerProperties.concurrency)
    }
}
