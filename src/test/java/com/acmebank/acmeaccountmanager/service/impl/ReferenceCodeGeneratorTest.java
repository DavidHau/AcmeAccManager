package com.acmebank.acmeaccountmanager.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReferenceCodeGeneratorTest {

    ReferenceCodeGenerator generator = new ReferenceCodeGenerator();

    @Test
    void shouldGenerateNumberInCorrectLength() {
        int expectedLength = 20;
        String actualNumber = generator.generate(expectedLength);

        assertThat(actualNumber)
            .hasSize(expectedLength)
            .doesNotContain("-");
    }

    @ParameterizedTest
    @ValueSource(ints = {-10, 0, 33, 100})
    void shouldThrowIllegalArgumentExceptionWhenGenerateNumberInUnsupportedLength(int expectedLength) {
        assertThrows(IllegalArgumentException.class, () -> generator.generate(expectedLength));
    }
}