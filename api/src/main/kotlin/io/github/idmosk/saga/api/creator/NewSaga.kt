package io.github.idmosk.saga.api.creator

import io.github.idmosk.saga.api.ISaga
import java.time.LocalDateTime
import java.time.Period
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

/**
 * A class for creating new saga
 * @property businessId unique custom format identifier
 * @property stepsForward ordered full-name forward method
 * @property stepsBackward ordered full-name backward method
 * @property deadLine maximum date on which the saga must be completed or the rollback must begin
 * @property retries maximum retries count
 * @property retriesTimeout timeout between retries
 */
class NewSaga private constructor(
    val businessId: String,
    val stepsForward: List<String>,
    val stepsBackward: List<String?>,
    val deadLine: LocalDateTime?,
    val retries: Int?,
    val retriesTimeout: Period?,
) {
    private constructor(builder: Builder) : this(
        builder.businessId,
        builder.stepsForwardStr,
        builder.stepsBackwardStr,
        builder.deadLine,
        builder.retries,
        builder.retriesTimeout,
    )

    /**
     * A class-helper for creating [NewSaga]
     * @property businessId
     * @property clazz class that implemented [ISaga] interface. Itself can be an interface and a class
     */
    data class Builder(val businessId: String, private val clazz: KClass<out io.github.idmosk.saga.api.ISaga>) {
        private var stepsForward: MutableList<KCallable<*>> = mutableListOf()
        var stepsForwardStr: MutableList<String> = mutableListOf()
        private var stepsBackward: MutableList<KCallable<*>?> = mutableListOf()
        var stepsBackwardStr: MutableList<String?> = mutableListOf()
        var deadLine: LocalDateTime? = null
        var retries: Int? = null
        var retriesTimeout: Period? = null

        /**
         * Forward and optional backward methods pair that will be called by [io.github.idmosk.saga.api.runner.Runner] on [clazz] implementation
         */
        fun addStep(
            forward: String,
            backward: String?,
        ) = apply {
            val forwardMethod =
                clazz.members.find { it.name == forward }
                    ?: throw Exception("class does not contain forward method $forward")

            if (forwardMethod !is KFunction<*>) {
                throw Exception("forward is not KFunction")
            }
            if (forwardMethod.returnType.classifier != Boolean::class) {
                throw Exception("forward does not return Boolean")
            }
            if (forwardMethod.parameters.size != 2 ||
                forwardMethod.parameters.get(0).kind != KParameter.Kind.INSTANCE ||
                forwardMethod.parameters.get(1).type.classifier != String::class
            ) {
                throw Exception("forward does not accept only String")
            }

            this.stepsForward.add(forwardMethod)

            backward?.let {
                val backwardMethod =
                    clazz.members.find { it.name == backward }
                        ?: throw Exception("class does not contain backward method $backward")

                if (backwardMethod !is KFunction<*>) {
                    throw Exception("backward is not KFunction")
                }
                if (backwardMethod.returnType.classifier != Unit::class) {
                    throw Exception("backward does not return Boolean")
                }
                if (backwardMethod.parameters.size != 2 ||
                    backwardMethod.parameters.get(0).kind != KParameter.Kind.INSTANCE ||
                    backwardMethod.parameters.get(1).type.classifier != String::class
                ) {
                    throw Exception("backward does not accept only String")
                }

                this.stepsBackward.add(backwardMethod)
            } ?: run {
                this.stepsBackward.add(null)
            }
        }

        fun deadLine(deadLine: LocalDateTime) = apply { this.deadLine = deadLine }

        fun retries(retries: Int) = apply { this.retries = retries }

        fun retriesTimeout(retriesTimeout: Period) = apply { this.retriesTimeout = retriesTimeout }

        fun build(): NewSaga {
            if (stepsForward.size == 0 || stepsBackward.size == 0) {
                throw Exception("must be at least one step")
            }
            var className = clazz.qualifiedName
            if (className?.contains("Impl") == true) {
                className = className.substring(0, className.lastIndexOf("Impl"))
            }
            stepsForward.forEach {
                stepsForwardStr.add(className + "." + it.name)
            }
            stepsBackward.forEach {
                stepsBackwardStr.add(
                    if (it != null) {
                        (className + "." + it.name)
                    } else {
                        null
                    },
                )
            }
            return NewSaga(this)
        }
    }
}
