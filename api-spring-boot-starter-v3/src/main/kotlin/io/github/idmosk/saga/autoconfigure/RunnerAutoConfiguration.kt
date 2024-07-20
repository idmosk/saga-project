package io.github.idmosk.saga.autoconfigure

import io.github.idmosk.saga.api.ISaga
import io.github.idmosk.saga.api.runner.Runner
import io.github.idmosk.saga.autoconfigure.properties.RunnerProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import io.github.idmosk.saga.queue.provider.Provider as QueueProvider

@Configuration
@ConditionalOnProperty(value = ["io.github.idmosk.saga.api.runner.enabled"], havingValue = "true")
@EnableConfigurationProperties(RunnerProperties::class)
@ConditionalOnBean(QueueProvider::class, io.github.idmosk.saga.api.ISaga::class)
open class RunnerAutoConfiguration(
    private val runnerProperties: RunnerProperties,
) {
    @Bean
    @ConditionalOnMissingBean
    open fun runner(
        queueProvider: QueueProvider,
        sagasImplementations: Set<io.github.idmosk.saga.api.ISaga>,
    ): Runner {
        return Runner(
            queueProvider,
            sagasImplementations,
            runnerProperties.enabledMethods,
            runnerProperties.allMethodsAreEnabled,
            runnerProperties.concurrencyForAllMethods,
        )
    }
}
