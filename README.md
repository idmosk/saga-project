# Description

A library for creating and distributed step-by-step execution of sagas.

The implementation is closest to the pattern
[Orchestration-based saga](https://microservices.io/patterns/data/saga.html#example-orchestration-based-saga).

# Components

- [Service provider interface for storage](spi-storage) [![javadoc](https://javadoc.io/badge2/io.github.idmosk.saga/spi-storage/javadoc.svg)](https://javadoc.io/doc/io.github.idmosk.saga/spi-storage)
- [Service provider interface for-queue](spi-queue) [![javadoc](https://javadoc.io/badge2/io.github.idmosk.saga/spi-queue/javadoc.svg)](https://javadoc.io/doc/io.github.idmosk.saga/spi-queue)
- [Tests to test your storage implementation](spi-storage-test) [![javadoc](https://javadoc.io/badge2/io.github.idmosk.saga/spi-storage-test/javadoc.svg)](https://javadoc.io/doc/io.github.idmosk.saga/spi-storage-test)
- [Tests to test your queue implementation](spi-queue-test) [![javadoc](https://javadoc.io/badge2/io.github.idmosk.saga/spi-queue-test/javadoc.svg)](https://javadoc.io/doc/io.github.idmosk.saga/spi-queue-test)
- [API for use in your application](api) [![javadoc](https://javadoc.io/badge2/io.github.idmosk.saga/api/javadoc.svg)](https://javadoc.io/doc/io.github.idmosk.saga/api)
- [Spring boot starter (v2) for API to use in your spring application](api-spring-boot-starter-v2) [![javadoc](https://javadoc.io/badge2/io.github.idmosk.saga.spring-boot-2/api-spring-boot-starter/javadoc.svg)](https://javadoc.io/doc/io.github.idmosk.saga.spring-boot-2/api-spring-boot-starter)
- [Spring boot starter (v3) for API to use in your spring application](api-spring-boot-starter-v3) [![javadoc](https://javadoc.io/badge2/io.github.idmosk.saga.spring-boot-3/api-spring-boot-starter/javadoc.svg)](https://javadoc.io/doc/io.github.idmosk.saga.spring-boot-3/api-spring-boot-starter)

# Usage

## Kotlin application

maven

```maven
<dependency>
  <groupId>io.github.idmosk.saga</groupId>
  <artifactId>api</artifactId>
  <version>0.1.0</version>
</dependency>
```

gradle

```gradle
implementation("io.github.idmosk.saga:api:0.1.0")
```

## Spring boot v2 application

maven

```maven
<dependency>
  <groupId>io.github.idmosk.saga.spring-boot-2</groupId>
  <artifactId>api-spring-boot-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

gradle

```gradle
implementation("io.github.idmosk.saga.spring-boot-2:api-spring-boot-starter:0.1.0")
```

## Spring boot v3 application

maven

```maven
<dependency>
  <groupId>io.github.idmosk.saga.spring-boot-3</groupId>
  <artifactId>api-spring-boot-starter</artifactId>
  <version>0.1.0</version>
</dependency>
```

gradle

```gradle
implementation("io.github.idmosk.saga.spring-boot-3:api-spring-boot-starter:0.1.0")
```

---

Attention! This is a project written for educational purposes.
