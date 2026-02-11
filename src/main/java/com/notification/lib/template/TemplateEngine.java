package com.notification.lib.template;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry and engine for message templates.
 * Allows registering named templates and rendering them with variables.
 *
 * <p>Thread-safe: uses ConcurrentHashMap for template storage.</p>
 *
 * <p>Example:</p>
 * <pre>
 * TemplateEngine engine = new TemplateEngine();
 * engine.register("welcome", new MessageTemplate("welcome", "Welcome {{name}}!"));
 * String rendered = engine.render("welcome", Map.of("name", "Alice"));
 * </pre>
 */
public class TemplateEngine {

    private final ConcurrentHashMap<String, MessageTemplate> templates = new ConcurrentHashMap<>();

    /**
     * Registers a named template.
     *
     * @param name the template identifier
     * @param template the template instance
     */
    public void register(String name, MessageTemplate template) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Template name must not be null or blank");
        }
        if (template == null) {
            throw new IllegalArgumentException("MessageTemplate must not be null");
        }
        templates.put(name, template);
    }

    /**
     * Renders a registered template with the given variables.
     *
     * @param name the template name
     * @param variables the variables to substitute
     * @return the rendered string
     * @throws IllegalArgumentException if the template is not found
     */
    public String render(String name, Map<String, String> variables) {
        MessageTemplate template = templates.get(name);
        if (template == null) {
            throw new IllegalArgumentException("Template not found: '" + name + "'");
        }
        return template.render(variables);
    }

    /**
     * Checks if a template with the given name is registered.
     */
    public boolean hasTemplate(String name) {
        return templates.containsKey(name);
    }

    /**
     * Returns the number of registered templates.
     */
    public int getTemplateCount() {
        return templates.size();
    }
}
