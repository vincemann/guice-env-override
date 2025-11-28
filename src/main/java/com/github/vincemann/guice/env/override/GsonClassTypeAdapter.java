package com.github.vincemann.guice.env.override;

import com.google.gson.*;

import java.lang.reflect.Type;

/**
 * @author vincemann
 */
public class GsonClassTypeAdapter implements JsonDeserializer<Class>, JsonSerializer<Class> {

    @Override
    public Class deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            return Class.forName(json.getAsString());
        } catch (ClassNotFoundException e) {
            throw new JsonParseException("Could not find json serialized class with name :" + json.getAsString(), e);
        }
    }

    @Override
    public JsonElement serialize(Class src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.getName());
    }
}
