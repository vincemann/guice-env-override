package com.github.vincemann.guice.override;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * @author vincemann
 
 */
public class DefaultOverrideBindingsGsonFactory implements OverrideBindingsGsonFactory {


    protected GsonBuilder gsonBuilder(){
        return new GsonBuilder()
                .registerTypeAdapter(Class.class, new GsonClassTypeAdapter())
                .registerTypeAdapter(Binding.class, new BindingTypeAdapter());
    }

    @Override
    public Gson create() {
        return gsonBuilder().create();
    }
}
