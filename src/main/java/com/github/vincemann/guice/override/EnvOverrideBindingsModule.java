package com.github.vincemann.guice.override;

import com.google.common.base.Preconditions;
import com.google.gson.reflect.TypeToken;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.inject.spi.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Accepts custom bindings ({@link Binding}) via given env var.
 * Just serialize a List of Bindings into json using gson object created via {@link GsonFactory#create()}.
 * Bindings also accept a List of string args that can be injected via guice with specific qualifier.
 * <p>
 * Example:
 * <pre>{@code
 * Binding binding = Binding.Builder.bind(AlertDisplayer.class)
 *     .to(TestAlertDisplayer.class)
 *     .withArgs("hello", "world")
 *     .build();
 *
 * class TestAlertDisplayer implements AlertDisplayer {
 *     private final List<String> args;
 *
 *     public TestAlertDisplayer(@Named("TestAlertDisplayerArgs") List<String> args) {
 *         this.args = args; // = ["hello", "world"]
 *     }
 * }
 * }</pre>
 * <p>
 * You can also inject the original implementation if you set {@link Binding#isInjectOriginal()}.
 * <p>
 * Example:
 * <pre>{@code
 * Binding binding = Binding.Builder.bind(AlertDisplayer.class)
 *     .to(TestAlertDisplayer.class)
 *     .injectOriginal()
 *     .build();
 *
 * class TestAlertDisplayer implements AlertDisplayer {
 *     private final AlertDisplayer decorated;
 *
 *     public TestAlertDisplayer(@Named("original") AlertDisplayer decorated) {
 *         this.decorated = decorated;
 *     }
 *
 *     public void showAlert(String msg) {
 *         // write alert msg to File for test
 *         // ...
 *         // show alert for short duration then auto close
 *         decorated.showAlert(msg);
 *         Thread.sleep(100);
 *         decorated.hide();
 *     }
 * }
 * }</pre>
 * <p>
 * You can also bind to an Instance instead of a class.
 * <p>
 * Example:
 * <pre>{@code
 * Binding binding = Binding.Builder.bind(String.class)
 *     .to(String.class)
 *     .withName("configFile")
 *     .toInstance("config-test.properties")
 *     .build();
 * }</pre>
 * Note that you still need to provide the impl class.
 * Also note that the instance needs to be serializable/deserializable by the
 * {@link com.google.gson.JsonSerializationContext}/{@link com.google.gson.JsonDeserializationContext} of your gson object.
 * <p>
 * This module should always override an existing base module that contains all your bindings.
 * <p>
 * Use like this:
 * <pre>{@code
 * Module appModule = new MyAppModule();
 * appModule = Modules.override(appModule)
 *     .with(new EnvOverrideBindingsModule(appModule, "MY_ENV_VAR"));
 * }</pre>
 */
public class EnvOverrideBindingsModule extends AbstractModule {

    private static final Logger log = LoggerFactory.getLogger(EnvOverrideBindingsModule.class);


    private static class BindingListType extends TypeToken<List<Binding>> {
    }

    private final Module baseModule;
    private final String envVar;
    private final GsonFactory gsonFactory;

    public EnvOverrideBindingsModule(Module baseModule, String envVar, GsonFactory gsonFactory) {
        this.baseModule = baseModule;
        this.envVar = envVar;
        this.gsonFactory = gsonFactory;
    }

    public EnvOverrideBindingsModule(Module baseModule, String envVar) {
        this(baseModule, envVar, new DefaultGsonFactory());
    }

    @Override
    protected void configure() {
        String customBindings = System.getenv(envVar);
        if (customBindings == null)
            return;

        log.debug("bindings string: {}", customBindings);
        List<Binding> bindings = gsonFactory.create().fromJson(customBindings, new BindingListType());
        log.debug("bindings: {}", bindings);
        for (Binding binding : bindings) {
            log.debug("configuring binding: {}", binding);
            List<String> args = binding.getArgs();
            if (args != null && !args.isEmpty()) {
                String argsQualifier = String.format("%sArgs", binding.getImpl().getSimpleName());
                bind(new TypeLiteral<List<String>>() {
                })
                        .annotatedWith(Names.named(argsQualifier))
                        .toInstance(args);
            }

            Object implInstance = binding.getImplInstance();
            if (binding.getName() == null) {
                if (implInstance == null) {
                    bind(binding.getIntf()).to(binding.getImpl());
                } else {
                    bind(binding.getIntf()).toInstance(implInstance);
                }
            } else {
                if (implInstance == null) {
                    bind(binding.getIntf())
                            .annotatedWith(Names.named(binding.getName()))
                            .to(binding.getImpl());
                } else {
                    bind(binding.getIntf())
                            .annotatedWith(Names.named(binding.getName()))
                            .toInstance(implInstance);
                }
            }

            if (binding.isInjectOriginal()) {
                Map<Key<?>, Class<?>> originalBindings = extractOriginalBindings(baseModule);
                Class<?> originalImpl = originalBindings.get(Key.get(binding.getIntf()));
                Preconditions.checkState(originalImpl != null,
                        "Did not find original impl for if %s. " +
                                "Have you bound a provider (unsupported)? ", binding.getIntf());
                log.debug("binding original implementation: {} with name 'original'", originalImpl);
                bind(binding.getIntf())
                        .annotatedWith(Names.named("original"))
                        .to(originalImpl);
            }
        }
    }

    private Map<Key<?>, Class<?>> extractOriginalBindings(Module module) {
        Map<Key<?>, Class<?>> bindings = new HashMap<>();

        List<Element> elements = Elements.getElements(module);
        for (Element element : elements) {
            element.acceptVisitor(new DefaultElementVisitor<Void>() {

                @Override
                public <T> Void visit(com.google.inject.Binding<T> binding) {
                    Key<T> key = binding.getKey();

                    // Extract the target implementation class
                    binding.acceptTargetVisitor(new DefaultBindingTargetVisitor<T, Void>() {
                        @Override
                        public Void visit(LinkedKeyBinding<? extends T> binding) {
                            // For bind(A.class).to(B.class)
                            bindings.put(key, binding.getLinkedKey().getTypeLiteral().getRawType());
                            return null;
                        }

                        @Override
                        public Void visit(InstanceBinding<? extends T> binding) {
                            // For bind(A.class).toInstance(instance)
                            bindings.put(key, binding.getInstance().getClass());
                            return null;
                        }

                        @Override
                        public Void visit(ProviderInstanceBinding<? extends T> binding) {
                            // For bind(A.class).toProvider(provider)
                            // This is harder - we'd need to instantiate to know the actual type
                            // todo could introduce typeAwareProvider interface here
                            log.trace("Provider binding found for {}, cannot determine implementation class", key);
                            return null;
                        }

                        @Override
                        public Void visit(ProviderKeyBinding<? extends T> binding) {
                            // For bind(A.class).toProvider(ProviderClass.class)
                            // todo could introduce typeAwareProvider interface here
                            log.trace("Provider key binding found for {}, cannot determine implementation class", key);
                            return null;
                        }

                        @Override
                        public Void visit(ConstructorBinding<? extends T> binding) {
                            // For bind(A.class).toConstructor(...) or implicit bindings
                            bindings.put(key, binding.getKey().getTypeLiteral().getRawType());
                            return null;
                        }

                        @Override
                        public Void visit(UntargettedBinding<? extends T> binding) {
                            // For bind(A.class) without a target
                            bindings.put(key, key.getTypeLiteral().getRawType());
                            return null;
                        }
                    });

                    return null;
                }
            });
        }

        return bindings;
    }
}
