// Publishes the JSpecify null-safety of the API records in their OpenAPI schemas.
package io.github.alexbosch3000.familyhub.shared.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Iterator;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Packages are {@code @NullMarked}: a record component is never null unless annotated
 * {@code @Nullable}. This converter marks the others as required and the nullable ones as accepting
 * null, so that the generated TypeScript types say exactly which fields may be missing.
 */
@Component
class NullabilityModelConverter implements ModelConverter {

    private static final String SCHEMA_REF_PREFIX = "#/components/schemas/";

    @Override
    public @Nullable Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        if (!chain.hasNext()) {
            return null;
        }
        Schema resolved = chain.next().resolve(type, context, chain);
        Class<?> raw = Json.mapper().constructType(type.getType()).getRawClass();
        if (resolved == null || !raw.isRecord()) {
            return resolved;
        }
        Schema model = resolved.get$ref() == null ? resolved
                : context.getDefinedModels().get(resolved.get$ref().substring(SCHEMA_REF_PREFIX.length()));
        if (model == null || model.getProperties() == null) {
            return resolved;
        }
        for (RecordComponent component : raw.getRecordComponents()) {
            mark(model, component.getName(), component.getAnnotatedType());
        }
        for (Method method : raw.getDeclaredMethods()) {
            if (method.isAnnotationPresent(JsonProperty.class) && method.getParameterCount() == 0) {
                mark(model, method.getName(), method.getAnnotatedReturnType());
            }
        }
        return resolved;
    }

    @SuppressWarnings("unchecked")
    private static void mark(Schema model, String name, java.lang.reflect.AnnotatedType javaType) {
        Schema property = (Schema) model.getProperties().get(name);
        if (property == null) {
            return;
        }
        if (!javaType.isAnnotationPresent(Nullable.class)) {
            if (model.getRequired() == null || !model.getRequired().contains(name)) {
                model.addRequiredItem(name);
            }
        } else if (property.get$ref() != null) {
            Schema absent = new Schema();
            absent.addType("null");
            Schema union = new Schema();
            union.setAnyOf(List.<Schema>of(property, absent));
            model.getProperties().put(name, union);
        } else if (property.getTypes() != null) {
            property.addType("null");
        }
    }
}
