package com.notification.lib.exception;

import java.util.Collections;
import java.util.List;

/**
 * Thrown when a notification message fails validation before sending.
 * Contains a list of all validation errors found, enabling
 * consumers to display or handle multiple issues at once.
 *
 * <p>Distinct from {@link SendException} to allow consumers to
 * differentiate between "bad input" and "send failure".</p>
 */
public class ValidationException extends NotificationException {

    private final List<String> violations;

    public ValidationException(String message, List<String> violations) {
        super(message + ": " + String.join(", ", violations));
        this.violations = Collections.unmodifiableList(violations);
    }

    public ValidationException(String message) {
        super(message);
        this.violations = Collections.emptyList();
    }

    /**
     * Returns the list of specific validation violations.
     */
    public List<String> getViolations() {
        return violations;
    }
}
