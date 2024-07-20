package io.github.idmosk.saga.autoconfigure.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "io.github.idmosk.saga.api.router")
class RouterProperties {
    /**
     * How many coroutines will be started.
     */
    var concurrency: Int = 1
}
