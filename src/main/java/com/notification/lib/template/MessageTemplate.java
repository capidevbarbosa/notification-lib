package com.notification.lib.template;

import lombok.Getter;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple template engine for notification messages.
 * Supports variable substitution using {{variableName}} syntax.
 *
 * <p>Example:</p>
 * <pre>
 * MessageTemplate template = new MessageTemplate("Hello {{name}}, your order {{orderId}} is ready!");
 * String result = template.render(Map.of("name", "John", "orderId", "12345"));
 * // Result: "Hello John, your order 12345 is ready!"
 * </pre>
 */
@Getter
public class MessageTemplate {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    private final String templateName;
    private final String templateContent;

    public MessageTemplate(String templateContent) {
        this(null, templateContent);
    }

    public MessageTemplate(String templateName, String templateContent) {
        if (templateContent == null || templateContent.isBlank()) {
            throw new IllegalArgumentException("Template content must not be null or blank");
        }
        this.templateName = templateName;
        this.templateContent = templateContent;
    }

    /**
     * Renders the template by replacing all {{variable}} placeholders
     * with values from the provided variables map.
     *
     * @param variables map of variable names to their values
     * @return the rendered string with all variables substituted
     * @throws IllegalArgumentException if a required variable is missing
     */
    public String render(Map<String, String> variables) {
        if (variables == null) {
            return templateContent;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(templateContent);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String variableName = matcher.group(1);
            String value = variables.get(variableName);

            if (value == null) {
                throw new IllegalArgumentException(
                        "Missing template variable: '" + variableName + "' in template" +
                                (templateName != null ? " '" + templateName + "'" : ""));
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Renders the template, using empty string for missing variables.
     */
    public String renderSafe(Map<String, String> variables) {
        if (variables == null) {
            return templateContent;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(templateContent);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String variableName = matcher.group(1);
            String value = variables.getOrDefault(variableName, "");
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);

        return result.toString();
    }
}
