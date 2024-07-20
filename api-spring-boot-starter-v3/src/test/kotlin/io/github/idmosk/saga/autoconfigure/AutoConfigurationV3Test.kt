package io.github.idmosk.saga.autoconfigure

import io.github.idmosk.saga.api.creator.Creator
import io.github.idmosk.saga.api.repeater.Repeater
import io.github.idmosk.saga.api.router.Router
import io.github.idmosk.saga.api.runner.Runner
import io.github.idmosk.saga.api.sagas.suspendable.TwoStepsOkSagaImpl
import org.assertj.core.api.Assertions.assertThat
import org.springframework.boot.test.context.assertj.AssertableApplicationContext
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import kotlin.test.Test
import io.github.idmosk.saga.queue.provider.SimpleProvider as QueueProvider
import io.github.idmosk.saga.storage.provider.SimpleProvider as StorageProvider

class AutoConfigurationV3Test {
    private val contextRunner: ApplicationContextRunner = ApplicationContextRunner()

    @Test
    fun successCreatorCreation() {
        contextRunner
            .withUserConfiguration(CreatorAutoConfiguration::class.java)
            .withBean("storageProvider", StorageProvider::class.java)
            .withBean("queueProvider", QueueProvider::class.java)
            .withPropertyValues(
                "io.github.idmosk.saga.api.creator.enabled=true",
            )
            .run { context: AssertableApplicationContext ->
                assertThat(context).hasSingleBean(io.github.idmosk.saga.api.creator.Creator::class.java)
                assertThat(context).getBean("creator")
                    .isSameAs(context.getBean(io.github.idmosk.saga.api.creator.Creator::class.java))
            }
    }

    @Test
    fun failCreatorCreation_absenceProvider() {
        contextRunner
            .withUserConfiguration(CreatorAutoConfiguration::class.java)
            .withPropertyValues(
                "io.github.idmosk.saga.api.creator.enabled=true",
            )
            .run { context: AssertableApplicationContext ->
                assertThat(context).doesNotHaveBean(io.github.idmosk.saga.api.creator.Creator::class.java)
            }
    }

    @Test
    fun failCreatorCreation_absenceEnable() {
        contextRunner
            .withUserConfiguration(CreatorAutoConfiguration::class.java)
            .withBean("storageProvider", StorageProvider::class.java)
            .withBean("queueProvider", QueueProvider::class.java)
            .run { context: AssertableApplicationContext ->
                assertThat(context).doesNotHaveBean(io.github.idmosk.saga.api.creator.Creator::class.java)
            }
    }

    @Test
    fun successRepeaterCreation() {
        contextRunner
            .withUserConfiguration(RepeaterAutoConfiguration::class.java)
            .withBean("storageProvider", StorageProvider::class.java)
            .withBean("queueProvider", QueueProvider::class.java)
            .withPropertyValues(
                "io.github.idmosk.saga.api.repeater.enabled=true",
            )
            .run { context: AssertableApplicationContext ->
                assertThat(context).hasSingleBean(Repeater::class.java)
                assertThat(context).getBean("repeater")
                    .isSameAs(context.getBean(Repeater::class.java))
            }
    }

    @Test
    fun failRepeaterCreation_absenceProvider() {
        contextRunner
            .withUserConfiguration(RepeaterAutoConfiguration::class.java)
            .withPropertyValues(
                "io.github.idmosk.saga.api.repeater.enabled=true",
            )
            .run { context: AssertableApplicationContext ->
                assertThat(context).doesNotHaveBean(Repeater::class.java)
            }
    }

    @Test
    fun failRepeaterCreation_absenceEnable() {
        contextRunner
            .withUserConfiguration(RepeaterAutoConfiguration::class.java)
            .withBean("storageProvider", StorageProvider::class.java)
            .withBean("queueProvider", QueueProvider::class.java)
            .run { context: AssertableApplicationContext ->
                assertThat(context).doesNotHaveBean(Repeater::class.java)
            }
    }

    @Test
    fun successRouterCreation() {
        contextRunner
            .withUserConfiguration(RouterAutoConfiguration::class.java)
            .withBean("storageProvider", StorageProvider::class.java)
            .withBean("queueProvider", QueueProvider::class.java)
            .withPropertyValues(
                "io.github.idmosk.saga.api.router.enabled=true",
                "io.github.idmosk.saga.api.router.concurrency=1",
            )
            .run { context: AssertableApplicationContext ->
                assertThat(context).hasSingleBean(Router::class.java)
                assertThat(context).getBean("router")
                    .isSameAs(context.getBean(Router::class.java))
            }
    }

    @Test
    fun failRouterCreation_absenceProvider() {
        contextRunner
            .withUserConfiguration(RouterAutoConfiguration::class.java)
            .withPropertyValues(
                "io.github.idmosk.saga.api.router.enabled=true",
                "io.github.idmosk.saga.api.router.concurrency=1",
            )
            .run { context: AssertableApplicationContext ->
                assertThat(context).doesNotHaveBean(Router::class.java)
            }
    }

    @Test
    fun failRouterCreation_absenceEnable() {
        contextRunner
            .withUserConfiguration(RouterAutoConfiguration::class.java)
            .withBean("storageProvider", StorageProvider::class.java)
            .withBean("queueProvider", QueueProvider::class.java)
            .withPropertyValues(
                "io.github.idmosk.saga.api.router.concurrency=1",
            )
            .run { context: AssertableApplicationContext ->
                assertThat(context).doesNotHaveBean(Router::class.java)
            }
    }

    @Suppress("ktlint:standard:max-line-length")
    @Test
    fun successRunnerCreation() {
        contextRunner
            .withUserConfiguration(RunnerAutoConfiguration::class.java)
            .withBean("queueProvider", QueueProvider::class.java)
            .withBean(TwoStepsOkSagaImpl::class.java)
            .withPropertyValues(
                "io.github.idmosk.saga.api.runner.enabled=true",
                "io.github.idmosk.saga.api.runner.enabled-methods=io.github.idmosk.saga.api.sagas.suspendable.TwoStepsOkSaga.forward1:1,io.github.idmosk.saga.api.sagas.suspendable.TwoStepsOkSaga.backward1:1,io.github.idmosk.saga.api.sagas.suspendable.TwoStepsOkSaga.forward2:1,io.github.idmosk.saga.api.sagas.suspendable.TwoStepsOkSaga.backward2:1",
            )
            .run { context: AssertableApplicationContext ->
                assertThat(context).hasSingleBean(Runner::class.java)
                assertThat(context).getBean("runner")
                    .isSameAs(context.getBean(Runner::class.java))
            }
    }

    @Test
    fun failRunnerCreation_wrongFormatMethodNaming() {
        contextRunner
            .withUserConfiguration(RunnerAutoConfiguration::class.java)
            .withBean("queueProvider", QueueProvider::class.java)
            .withBean(TwoStepsOkSagaImpl::class.java)
            .withPropertyValues(
                "io.github.idmosk.saga.api.runner.enabled=true",
                "io.github.idmosk.saga.api.runner.enabled-methods=wrong_format",
            )
            .run { context: AssertableApplicationContext ->
                assertThat(context).hasFailed()
            }
    }

    @Test
    fun failRunnerCreation_NotNumberMethodNaming() {
        contextRunner
            .withUserConfiguration(RunnerAutoConfiguration::class.java)
            .withBean("queueProvider", QueueProvider::class.java)
            .withBean(TwoStepsOkSagaImpl::class.java)
            .withPropertyValues(
                "io.github.idmosk.saga.api.runner.enabled=true",
                "io.github.idmosk.saga.api.runner.enabled-methods=package.TwoStepsOkSaga.forward1:not_number",
            )
            .run { context: AssertableApplicationContext ->
                assertThat(context).hasFailed()
            }
    }

    @Test
    fun failRunnerCreation_mistakeMethodNaming() {
        contextRunner
            .withUserConfiguration(RunnerAutoConfiguration::class.java)
            .withBean("queueProvider", QueueProvider::class.java)
            .withBean(TwoStepsOkSagaImpl::class.java)
            .withPropertyValues(
                "io.github.idmosk.saga.api.runner.enabled=true",
                @Suppress("ktlint:standard:max-line-length")
                "io.github.idmosk.saga.api.runner.enabled-methods=io.github.idmosk.saga.api.sagas.suspendable.TwoStepsOkSaga.MISTAKE:1,io.github.idmosk.saga.api.sagas.suspendable.TwoStepsOkSaga.backward1:1,io.github.idmosk.saga.api.sagas.suspendable.TwoStepsOkSaga.forward2:1,io.github.idmosk.saga.api.sagas.suspendable.TwoStepsOkSaga.backward2:1",
            )
            .run { context: AssertableApplicationContext ->
                assertThat(context).hasFailed()
            }
    }

    @Test
    fun failRunnerCreation_absenceProvider() {
        contextRunner
            .withUserConfiguration(RunnerAutoConfiguration::class.java)
            .withBean(TwoStepsOkSagaImpl::class.java)
            .withPropertyValues(
                "io.github.idmosk.saga.api.runner.enabled=true",
                "io.github.idmosk.saga.api.runner.enabled-methods=ex.am.ple.SomeSaga.forward1:1,ex.am.ple.SomeSaga.backward1:1",
            )
            .run { context: AssertableApplicationContext ->
                assertThat(context).doesNotHaveBean(Runner::class.java)
            }
    }

    @Test
    fun failRunnerCreation_absenceEnable() {
        contextRunner
            .withUserConfiguration(RunnerAutoConfiguration::class.java)
            .withBean("queueProvider", QueueProvider::class.java)
            .withBean(TwoStepsOkSagaImpl::class.java)
            .withPropertyValues(
                "io.github.idmosk.saga.api.runner.enabled-methods=ex.am.ple.SomeSaga.forward1:1,ex.am.ple.SomeSaga.backward1:1",
            )
            .run { context: AssertableApplicationContext ->
                assertThat(context).doesNotHaveBean(Runner::class.java)
            }
    }

    @Test
    fun failRunnerCreation_absenceSagaImpls() {
        contextRunner
            .withUserConfiguration(RunnerAutoConfiguration::class.java)
            .withBean("queueProvider", QueueProvider::class.java)
            .withPropertyValues(
                "io.github.idmosk.saga.api.runner.enabled=true",
                "io.github.idmosk.saga.api.runner.enabled-methods=ex.am.ple.SomeSaga.forward1:1,ex.am.ple.SomeSaga.backward1:1",
            )
            .run { context: AssertableApplicationContext ->
                assertThat(context).doesNotHaveBean(Runner::class.java)
            }
    }
}
