package io.github.idmosk.saga.queue.provider

import io.github.idmosk.saga.queue.model.NextStep
import io.github.idmosk.saga.queue.model.SagasResult
import io.github.idmosk.saga.queue.model.Task
import io.github.idmosk.saga.queue.model.TasksResult

class BrokenPutsQueueProvider : SimpleProvider() {
    var brokenPutNextStep = true
    var brokenPutTask = true
    var brokenPutTasksResult = true
    var brokenPutSagasResult = true

    override fun putNextStep(meta: NextStep) {
        if (brokenPutNextStep) {
            throw Exception("Broken putNextStep")
        }
        super.putNextStep(meta)
    }

    override fun putTask(
        key: String,
        task: Task,
    ) {
        if (brokenPutTask) {
            throw Exception("Broken putTask")
        }
        super.putTask(key, task)
    }

    override fun putTasksResult(result: TasksResult) {
        if (brokenPutTasksResult) {
            throw Exception("Broken putTasksResult")
        }
        super.putTasksResult(result)
    }

    override fun putSagasResult(
        key: String,
        sagasResult: SagasResult,
    ) {
        if (brokenPutSagasResult) {
            throw Exception("Broken putSagasResult")
        }
        super.putSagasResult(key, sagasResult)
    }
}
