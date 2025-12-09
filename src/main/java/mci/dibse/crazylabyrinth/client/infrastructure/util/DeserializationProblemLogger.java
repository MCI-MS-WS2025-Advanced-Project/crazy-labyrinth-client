package mci.dibse.crazylabyrinth.client.infrastructure.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class DeserializationProblemLogger  extends DeserializationProblemHandler {


    @Override
    public boolean handleUnknownProperty(DeserializationContext ctx, JsonParser p,
                                        JsonDeserializer<?> deserializer, Object o, String propertyName) throws IOException {

        log.warn("Unknown property {} in {}", propertyName, o.getClass().getSimpleName());
        return true;
    }
}
