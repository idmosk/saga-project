[![javadoc](https://javadoc.io/badge2/io.github.idmosk.saga/api/javadoc.svg)](https://javadoc.io/doc/io.github.idmosk.saga/api) ![coverage badge](https://img.shields.io/endpoint?url=https://gist.githubusercontent.com/idmosk/4db605570a25e36c5611e58a07edbb80/raw/saga-project-coverage-api-badge.json)

# Description

API for use in your application.

Contains 4 components, each of which can be initialized separately

All components can be launched competitively (but not with test providers)

## Components

### Creator

API for creating, running and waiting for the created saga to complete.

#### Initialization example:

```kotlin
val newSaga = 
    NewSaga// class-helper for initiate new saga
        .Builder("businessId", TwoStepsOkSaga::class) // Builder with necessary fields
        .addStep("forward1", "backward1")// methods pair 1. Must be at least one pair
        .addStep("forward2", null)// methods pair 2; backward method might be null
        .retriesTimeout(Period.ZERO)// timeout for repeating the method that threw the exception (default null)
        .retries(5)// maximum retries (default null)
        .deadLine(LocalDateTime.now().plusYears(100))// the time when the saga will be considered expired (default null)
        .build()
                    
val manager = 
    creator // previously initialized
        .create(newSaga) // saving created saga
        .listen(this, newSaga.businessId) // subscribe to sagas result channel
        .start() // start saga execution
        .await() // block until the result of the saga appears
```

#### Use cases

With these steps

```kotlin
newSaga
    .addStep("forward1", "backward1")
    .addStep("forward2", null)
    .addStep("forward3", "backward3")
```

- and an error at the last `forward` step, the order of execution will be as follows:
  - forward1
  - forward2
  - forward3
  - backward3
  - backward1
- and the expiration of the deadline before the last `forward` step, the order of execution will be as follows:
  - forward1
  - forward2
  - backward1

#### Recommendations:

The `listen` and `await` methods do not need to be called if you are not interested in the execution result

If the `listen` method has been called, but the `await` method has not, then it is worth calling the `unregister` method to release resources

### Router

Process routing saga steps.

#### Usage example:

```kotlin
router.start(this)
router.stop()
```

#### Recommendations:

To complete the job correctly, you should call `stop`

### Runner

Process for executing saga steps.

#### Usage example:

```kotlin
runner.start(this)
runner.stop()
```

#### Recommendations:

To complete the job correctly, you should call `stop`

#### Requirements:

Each `forward` method must return `bool` and may throw an error

Each `backward` method should not return anything and may throw an error

### Repeater

A process that re-runs previously failed steps in a saga.

#### Usage example:

```kotlin
repeater.start(this)
repeater.stop()
```

#### Recommendations:

To complete the job correctly, you should call `stop`
