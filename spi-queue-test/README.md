[![javadoc](https://javadoc.io/badge2/io.github.idmosk.saga/spi-queue-test/javadoc.svg)](https://javadoc.io/doc/io.github.idmosk.saga/spi-queue-test)

# Description

Tests for your implementation [SPI queu

es](../spi-queue) and a bench implementation of SPI with its testing.

When testing your implementation, you should also inherit
[`abstract class SpiTest`](../spi-queue-test/src/main/kotlin/io/github/idmosk/saga/queue/SpiTest.kt) to check the correctness
work.

---

Attention! The bench implementation does not support distributed execution of [API](../api) and is for demonstration purposes only
operation and testing of functionality.
