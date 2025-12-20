package miroshka.aether.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.json.JsonMapper;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.lang.reflect.Type;

public final class JacksonMapper implements JsonMapper {

    private final ObjectMapper mapper = new ObjectMapper();

    @NotNull
    @Override
    public String toJsonString(@NotNull Object obj, @NotNull Type type) {
        try {
            return mapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    @Override
    public <T> T fromJsonString(@NotNull String json, @NotNull Type type) {
        try {
            return mapper.readValue(json, mapper.constructType(type));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    @Override
    public <T> T fromJsonStream(@NotNull InputStream json, @NotNull Type type) {
        try {
            return mapper.readValue(json, mapper.constructType(type));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
