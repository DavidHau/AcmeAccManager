package com.acmebank.acmeaccountmanager.service.impl;

import java.util.UUID;

class ReferenceCodeGenerator {
    public static final int UUID_RANDOM_LENGTH = 32;

    public String generate(int referenceCodeLength) {
        if (referenceCodeLength < 1 || referenceCodeLength > UUID_RANDOM_LENGTH) {
            throw new IllegalArgumentException(
                "Generator is not able to generate random number with length: %d".formatted(referenceCodeLength));
        }
        return UUID.randomUUID().toString()
            .replaceAll("-", "")
            .toUpperCase()
            .substring(UUID_RANDOM_LENGTH - referenceCodeLength);
    }
}
