[![javadoc](https://javadoc.io/badge2/io.github.idmosk.saga.spring-boot-2/api-spring-boot-starter/javadoc.svg)](https://javadoc.io/doc/io.github.idmosk.saga.spring-boot-2/api-spring-boot-starter) ![coverage badge](https://img.shields.io/endpoint?url=https://gist.githubusercontent.com/idmosk/4db605570a25e36c5611e58a07edbb80/raw/saga-project-coverage-api-spring-boot-starter-v2-badge.json)

# Description

A starter for use in your spring-boot v2 application.

Allows you to initialize [API](../api) components in the Spring context of your application

Components and possible settings:
- Creator
  - `io.github.idmosk.saga.api.creator.enabled` - enables initialization of creator with providers found in the Spring context
    (default: `false`)
- Router
  - `io.github.idmosk.saga.api.router.enabled` - enables initialization of router with providers found in the Spring context
    (default: `false`)
  - `io.github.idmosk.saga.api.router.concurrency` - concurrency with which the router will process queues with task starts
    and task execution results (default: `1`)
- Runner
  - `io.github.idmosk.saga.api.runner.enabled` - enables initialization of runner with queue provider found in the Spring context
     and interface implementations [`ISaga`](../api/src/main/kotlin/io/github/idmosk/saga/api/ISaga.kt) (default: `false`)
  - `io.github.idmosk.saga.api.runner.allMethodsAreEnabled` - whether all public methods need to be considered in the found implementations
    interface [`ISaga`](../api/src/main/kotlin/io/github/idmosk/saga/api/ISaga.kt) as methods for executing saga steps
    (default: `false`)
  - `io.github.idmosk.saga.api.runner.concurrencyForAllMethods` - concurrency for each method of the found implementations
    interface [`ISaga`](../api/src/main/kotlin/io/github/idmosk/saga/api/ISaga.kt), with which the runner will perform steps
    sag (default: `1`)
  - `io.github.idmosk.saga.api.runner.enabledMethods` - method:concurrency pairs separated by commas for fine tuning (default:
    `[]`, example:
    `io.github.idmosk.saga.api.sagas.suspendable.TwoStepsOkSaga.forward1:1,io.github.idmosk.saga.api.sagas.ThreeStepsOkSaga.backward1:1`)
- Repeater:
  - `io.github.idmosk.saga.api.repeater.enabled` - enables initialization of repeater with providers found in the Spring context
    (default: `false`)
  - `io.github.idmosk.saga.api.repeater.fetchSize` - the maximum number of tasks ready to be run again, which
    repeater will fetch from storage at a time (default: `1`)
  - `io.github.idmosk.saga.api.repeater.periodSeconds` - frequency with which the repeater will be launched (default: `1`)

