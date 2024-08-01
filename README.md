# Description

A library for creating and distributed step-by-step execution of sagas.

The implementation is closest to the pattern
[Orchestration-based saga](https://microservices.io/patterns/data/saga.html#example-orchestration-based-saga).

# Components

- [Service provider interface for storage](spi-storage) [![javadoc](https://javadoc.io/badge2/io.github.idmosk.saga/spi-storage/javadoc.svg)](https://javadoc.io/doc/io.github.idmosk.saga/spi-storage)
- [Service provider interface for-queue](spi-queue) [![javadoc](https://javadoc.io/badge2/io.github.idmosk.saga/spi-queue/javadoc.svg)](https://javadoc.io/doc/io.github.idmosk.saga/spi-queue)
- [Tests to test your storage implementation](spi-storage-test) [![javadoc](https://javadoc.io/badge2/io.github.idmosk.saga/spi-storage-test/javadoc.svg)](https://javadoc.io/doc/io.github.idmosk.saga/spi-storage-test) ![coverage badge](https://img.shields.io/endpoint?url=https://gist.githubusercontent.com/idmosk/4db605570a25e36c5611e58a07edbb80/raw/saga-project-coverage-spi-storage-test-badge.json)
- [Tests to test your queue implementation](spi-queue-test) [![javadoc](https://javadoc.io/badge2/io.github.idmosk.saga/spi-queue-test/javadoc.svg)](https://javadoc.io/doc/io.github.idmosk.saga/spi-queue-test) ![coverage badge](https://img.shields.io/endpoint?url=https://gist.githubusercontent.com/idmosk/4db605570a25e36c5611e58a07edbb80/raw/saga-project-coverage-spi-queue-test-badge.json)
- [API for use in your application](api) [![javadoc](https://javadoc.io/badge2/io.github.idmosk.saga/api/javadoc.svg)](https://javadoc.io/doc/io.github.idmosk.saga/api) ![coverage badge](https://img.shields.io/endpoint?url=https://gist.githubusercontent.com/idmosk/4db605570a25e36c5611e58a07edbb80/raw/saga-project-coverage-api-badge.json)
- [Spring boot starter (v2) for API to use in your spring application](api-spring-boot-starter-v2) [![javadoc](https://javadoc.io/badge2/io.github.idmosk.saga.spring-boot-2/api-spring-boot-starter/javadoc.svg)](https://javadoc.io/doc/io.github.idmosk.saga.spring-boot-2/api-spring-boot-starter) ![coverage badge](https://img.shields.io/endpoint?url=https://gist.githubusercontent.com/idmosk/4db605570a25e36c5611e58a07edbb80/raw/saga-project-coverage-api-spring-boot-starter-v2-badge.json)
- [Spring boot starter (v3) for API to use in your spring application](api-spring-boot-starter-v3) [![javadoc](https://javadoc.io/badge2/io.github.idmosk.saga.spring-boot-3/api-spring-boot-starter/javadoc.svg)](https://javadoc.io/doc/io.github.idmosk.saga.spring-boot-3/api-spring-boot-starter) ![coverage badge](https://img.shields.io/endpoint?url=https://gist.githubusercontent.com/idmosk/4db605570a25e36c5611e58a07edbb80/raw/saga-project-coverage-api-spring-boot-starter-v3-badge.json)

# How it works in pictures

<img src="./saga.svg">

# Usage

## Kotlin application

### maven

```maven
<dependency>
  <groupId>io.github.idmosk.saga</groupId>
  <artifactId>api</artifactId>
  <version>0.1.0</version>
</dependency>
```

### gradle

```gradle
implementation("io.github.idmosk.saga:api:0.1.0")
```

## Spring boot v2 application

### maven

```maven
<dependency>
  <groupId>io.github.idmosk.saga.spring-boot-2</groupId>
  <artifactId>api-spring-boot-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

### gradle

```gradle
implementation("io.github.idmosk.saga.spring-boot-2:api-spring-boot-starter:0.1.0")
```

## Spring boot v3 application

### maven

```maven
<dependency>
  <groupId>io.github.idmosk.saga.spring-boot-3</groupId>
  <artifactId>api-spring-boot-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

### gradle

```gradle
implementation("io.github.idmosk.saga.spring-boot-3:api-spring-boot-starter:0.1.0")
```

# Demo stand

[demo-saga-project](http://github.com/idmosk/demo-saga-project/tree/master)

# Roadmap

- More flexible [new saga builder](io.github.idmosk.saga.api.creator.NewSaga.Builder). Flexible retry and timeout policies (per step)
- Declaring sagas, sagas steps, steps execution concurrency and steps retry policies using annotations and annotation parameters instead of implementation of [ISaga](io.github.idmosk.saga.api.ISaga), usage [addStep](io.github.idmosk.saga.api.creator.NewSaga.Builder.addStep), [retries](io.github.idmosk.saga.api.creator.NewSaga.Builder.retries), [retriesTimeout](io.github.idmosk.saga.api.creator.NewSaga.Builder.retriesTimeout) methods and concurrency properties parameters [1](api-spring-boot-starter-v2/README.md:24) [2](api-spring-boot-starter-v2/README.md:27)
- Support a companion object for saga with business payloads and pass them to [NewSaga Builder](io.github.idmosk.saga.api.creator.NewSaga.Builder) instead of the simple `businessId`.

*Serializing and storing the object in storage next to other saga meta information and further deserializing and passing them from the router through the runner to the saga methods as parameters instead of passing a simple `businessId`.*

*This way, the user application will be freed from the need to store the saga payload and retrieve it by `businessId`. All payload will be saved by library and passing to saga step.*

*At each step of the saga, the payload can change depending on the result of the execution, and the changed payload will be stored by the router.*

- Abandoning the concept of steps. One step forward - one step back. Go to actions and counter actions. One action can have 0...n counteractions

---

Attention! This is a project written for educational purposes.
