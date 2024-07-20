[![javadoc](https://javadoc.io/badge2/io.github.idmosk.saga/spi-storage-test/javadoc.svg)](https://javadoc.io/doc/io.github.idmosk.saga/spi-storage-test)

# Description

Tests for your implementation [SPI storages](../spi-storage) and a bench implementation of SPI with its testing.

When testing your implementation, you should also inherit
[`abstract class SpiTest`](src/main/kotlin/io/github/idmosk/saga/storage/SpiTest.kt) to check the correctness
work.

---

Attention! The bench implementation does not support distributed execution of [API](../api) and is for demonstration purposes only
operation and testing of functionality.
