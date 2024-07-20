package io.github.idmosk.saga.storage.provider

import io.github.idmosk.saga.storage.model.CreateModel
import io.github.idmosk.saga.storage.model.GetModel
import io.github.idmosk.saga.storage.model.UpdateModel
import java.util.UUID

class BrokenStorageProvider : SimpleProvider() {
    var brokenCreate = true
    var brokenUpdate = true
    var brokenGet = true

    override suspend fun create(meta: CreateModel): GetModel {
        if (brokenCreate) {
            throw Exception("Broken create")
        }
        return super.create(meta)
    }

    override suspend fun update(meta: UpdateModel): GetModel {
        if (brokenUpdate) {
            throw Exception("Broken update")
        }
        return super.update(meta)
    }

    override suspend fun get(uuid: UUID): GetModel {
        if (brokenGet) {
            throw Exception("Broken get")
        }
        return super.get(uuid)
    }
}
