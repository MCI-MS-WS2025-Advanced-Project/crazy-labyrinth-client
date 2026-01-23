package mci.dibse.crazylabyrinth.client.infrastructure.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeserializationProblemLogger extends DeserializationProblemHandler {

    /**
     * Log unknown properties during deserialization and mark them as handled.
     *
     * @param ctx deserialization context
     * @param p JSON parser
     * @param deserializer active deserializer
     * @param o target object
     * @param propertyName unknown property name
     * @return true to indicate the property was handled
     */
    @Override
    public boolean handleUnknownProperty(
            DeserializationContext ctx,
            JsonParser p,
            JsonDeserializer<?> deserializer,
            Object o,
            String propertyName) {

        log.warn("Unknown property {} in {}", propertyName, o.getClass().getSimpleName());
        return true;
    }
}
