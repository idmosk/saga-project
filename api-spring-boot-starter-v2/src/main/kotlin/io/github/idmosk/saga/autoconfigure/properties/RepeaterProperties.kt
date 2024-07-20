package io.github.idmosk.saga.autoconfigure.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "io.github.idmosk.saga.api.repeater")
class RepeaterProperties {
    /**
     * How many items will be fetched at a time.
     */
    var fetchSize: Int = 1

    /**
     * How often scan will be started.
     */
    var periodSeconds: Long = 1
}
