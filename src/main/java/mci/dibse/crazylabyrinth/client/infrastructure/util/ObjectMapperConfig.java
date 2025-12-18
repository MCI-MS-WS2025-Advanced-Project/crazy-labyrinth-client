package mci.dibse.crazylabyrinth.client.infrastructure.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class ObjectMapperConfig {

    public static ObjectMapper createCustomObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    public static ObjectMapper createEventObjectMapper() {
        ObjectMapper mapper = createCustomObjectMapper();

        SimpleModule eventModule = new SimpleModule();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.addHandler(new DeserializationProblemLogger());
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        eventModule.addDeserializer(Object.class, new EventDeserializer());
        mapper.registerModule(eventModule);

        return mapper;
    }
}
