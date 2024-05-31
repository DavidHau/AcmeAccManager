package com.acmebank.acmeaccountmanager.rest.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Error Response entity")
public record ErrorResponse(
    @Schema(description = "Error message", example = "Something bad happened!")
    String error
) {
}
