package com.notification.lib.template;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TemplateEngine & MessageTemplate")
class TemplateEngineTest {

    @Test
    @DisplayName("should render template with variables")
    void shouldRenderWithVariables() {
        MessageTemplate template = new MessageTemplate("Hello {{name}}, your order {{orderId}} is ready!");

        String result = template.render(Map.of("name", "John", "orderId", "12345"));

        assertThat(result).isEqualTo("Hello John, your order 12345 is ready!");
    }

    @Test
    @DisplayName("should return template as-is when no variables provided")
    void shouldReturnAsIsWhenNoVariables() {
        MessageTemplate template = new MessageTemplate("No variables here");

        String result = template.render(Map.of());

        assertThat(result).isEqualTo("No variables here");
    }

    @Test
    @DisplayName("should throw when required variable is missing")
    void shouldThrowWhenVariableMissing() {
        MessageTemplate template = new MessageTemplate("Hello {{name}}!");

        assertThatThrownBy(() -> template.render(Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing template variable: 'name'");
    }

    @Test
    @DisplayName("should render safely with missing variables as empty string")
    void shouldRenderSafelyWithMissing() {
        MessageTemplate template = new MessageTemplate("Hello {{name}}!");

        String result = template.renderSafe(Map.of());

        assertThat(result).isEqualTo("Hello !");
    }

    @Test
    @DisplayName("should reject null or blank template content")
    void shouldRejectNullContent() {
        assertThatThrownBy(() -> new MessageTemplate(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MessageTemplate("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("TemplateEngine should register and render templates")
    void engineShouldRegisterAndRender() {
        TemplateEngine engine = new TemplateEngine();
        engine.register("welcome", new MessageTemplate("welcome", "Welcome {{name}} to {{app}}!"));

        String result = engine.render("welcome", Map.of("name", "Alice", "app", "NotifyLib"));

        assertThat(result).isEqualTo("Welcome Alice to NotifyLib!");
        assertThat(engine.hasTemplate("welcome")).isTrue();
        assertThat(engine.getTemplateCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("TemplateEngine should throw for unknown template")
    void engineShouldThrowForUnknown() {
        TemplateEngine engine = new TemplateEngine();

        assertThatThrownBy(() -> engine.render("nonexistent", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Template not found");
    }
}
