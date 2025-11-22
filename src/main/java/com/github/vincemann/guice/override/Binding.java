package com.github.vincemann.guice.override;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @see EnvOverrideBindingsModule
 */
public class Binding {
    private Class intf;
    private Class impl;
    private Object implInstance;
    private boolean injectOriginal;
    private String name;
    private List<String> args;

    Binding() {
    }

    private <IF, IMPL extends IF> Binding(Class<IF> intf, Class<IMPL> impl, List<String> args,
                                          String name, boolean injectOriginal, IMPL implInstance) {
        Preconditions.checkNotNull(intf, "Interface class must be set");
        Preconditions.checkNotNull(impl, "Implementation class must be set");
        this.implInstance = implInstance;
        this.intf = intf;
        this.impl = impl;
        this.args = Objects.requireNonNullElseGet(args, ArrayList::new);
        this.name = name;
        this.injectOriginal = injectOriginal;
    }

    public <IF, IMPL extends IF> Binding(Class<IF> intf, Class<IMPL> impl) {
        this(intf, impl, null, null, false, null);
    }

    public <IF, IMPL extends IF> Binding(Class<IF> intf, Class<IMPL> impl, String... args) {
        this(intf, impl, Lists.newArrayList(args), null, false, null);
    }

    @Override
    public String toString() {
        return "Binding{" +
                "intf=" + intf.getSimpleName() +
                ", impl=" + impl.getSimpleName() +
                ", implInstance=" + implInstance +
                ", args=" + args +
                ", name=" + name +
                ", injectOriginal=" + injectOriginal +
                '}';
    }

    public static class Builder<IF, IMPL extends IF> {
        private Class<IF> intf;
        private Class<? extends IF> impl;
        private IMPL implInstance;
        private boolean injectOriginal = false;
        private String name;
        private List<String> args = new ArrayList<>();

        private Builder(Class<IF> intf) {
            this.intf = intf;
        }

        public static <IF, IMPL extends IF> Builder<IF, IMPL> bind(Class<IF> intf) {
            return new Builder<>(intf);
        }

        public Builder<IF, IMPL> to(Class<? extends IF> impl) {
            this.impl = impl;
            return this;
        }

        public Builder<IF, IMPL> toInstance(IMPL instance) {
            this.implInstance = instance;
            return this;
        }

        public Builder<IF, IMPL> injectOriginal() {
            this.injectOriginal = true;
            return this;
        }

        public Builder<IF, IMPL> withName(String name) {
            this.name = name;
            return this;
        }

        public Builder<IF, IMPL> withArgs(String... args) {
            this.args.addAll(Arrays.asList(args));
            return this;
        }

        public Builder<IF, IMPL> withArgs(List<String> args) {
            this.args.addAll(args);
            return this;
        }

        public Binding build() {
            Binding binding = new Binding();
            binding.setIntf(intf);
            binding.setImpl(impl);
            binding.setInjectOriginal(injectOriginal);
            binding.setName(name);
            binding.setArgs(new ArrayList<>(args));
            binding.setImplInstance(implInstance);
            return binding;
        }
    }

    public Class getIntf() {
        return intf;
    }

    public void setIntf(Class intf) {
        this.intf = intf;
    }

    public Class getImpl() {
        return impl;
    }

    public void setImpl(Class impl) {
        this.impl = impl;
    }

    public Object getImplInstance() {
        return implInstance;
    }

    public void setImplInstance(Object implInstance) {
        this.implInstance = implInstance;
    }

    public boolean isInjectOriginal() {
        return injectOriginal;
    }

    public void setInjectOriginal(boolean injectOriginal) {
        this.injectOriginal = injectOriginal;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getArgs() {
        return args;
    }

    public void setArgs(List<String> args) {
        this.args = args;
    }
}
