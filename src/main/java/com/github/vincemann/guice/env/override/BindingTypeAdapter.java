package com.github.vincemann.guice.env.override;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.List;

/**
 * @author vincemann
 
 */
public class BindingTypeAdapter implements JsonDeserializer<Binding>, JsonSerializer<Binding>{

    // this must not be created inside the deserialize method!
    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>(){}.getType();


    @Override
    public JsonElement serialize(Binding binding, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject object = new JsonObject();
        object.add("intf", jsonSerializationContext.serialize(binding.getIntf()));
        object.add("impl", jsonSerializationContext.serialize(binding.getImpl()));
        object.add("name", jsonSerializationContext.serialize(binding.getName()));
        object.add("args", jsonSerializationContext.serialize(binding.getArgs()));
        object.add("injectOriginal", jsonSerializationContext.serialize(binding.isInjectOriginal()));
        if (binding.getImplInstance() != null){
            object.add("implInstance",
                    jsonSerializationContext.serialize(binding.getImplInstance(), binding.getImpl())
            );
        }else {
            object.add("implInstance", JsonNull.INSTANCE);
        }
        return object;
    }

    @Override
    public Binding deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        Binding binding = new Binding();
        binding.setIntf(
                jsonDeserializationContext.deserialize(jsonElement.getAsJsonObject().get("intf"), Class.class)
        );
        binding.setImpl(
                jsonDeserializationContext.deserialize(jsonElement.getAsJsonObject().get("impl"), Class.class)
        );
        binding.setName(
                jsonDeserializationContext.deserialize(jsonElement.getAsJsonObject().get("name"), String.class)
        );

        binding.setInjectOriginal(
                jsonDeserializationContext.deserialize(jsonElement.getAsJsonObject().get("injectOriginal"), Boolean.class)
        );

        binding.setArgs(
                jsonDeserializationContext.deserialize(jsonElement.getAsJsonObject().get("args"), STRING_LIST_TYPE)
        );
        binding.setImplInstance(
                jsonDeserializationContext.deserialize(jsonElement.getAsJsonObject().get("implInstance"), binding.getImpl())
        );
        return binding;
    }
}