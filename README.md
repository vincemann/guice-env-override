# guice-env-override
Allows you to overwrite guice bindings via env var.  
This is useful for artifact integration testing -> you execute your program as jar/jlink-image/artifact in your tests.  
## example usage  
### test
```java

@Test
public void myTest(){
    // we dont want alert popups in test
    Binding binding = Binding.Builder.bind(AlertDialog.class)
        .to(SilentAlertDialog.class)
        .build();
  
    List<Binding> bindings = List.of(binding);
    String jsonBindings = new DefaultEnvOverrideBindingsGsonFactory().create().toJson(bindings);
    
    // you dont need to use ProcessBuilder, this ist just a demonstration
    ProcessBuilder pb = new ProcessBuilder("java", "-jar", "/path/to/my/app.jar");
    pb.environment().put("OVERRIDE_GUICE_BINDINGS", jsonBindings);
    Process process = pb.start();
    
    // do your testing ...
}
```

### application  
```java

public static void main(String[] args) {
    // this code needs to be in your application under test (/path/to/my/app.jar)
    Module module = new MyGuiceModule();
    
    // this will override your bindings
    module = Modules.override(module)
        .with(new EnvOverrideBindingsModule(module, "OVERRIDE_GUICE_BINDINGS"));

    // now you can create injector
    Injector injector = Guice.createInjector(List.of(mainModule));

    // ...
}       

```

## adding to your project  
### gradle  
```gradle
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation ('com.github.vincemann:guice-env-override:0.0.1')
}
```
### maven  
```maven

<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.vincemann</groupId>
        <artifactId>guice-env-override</artifactId>
        <version>0.0.1</version>
    </dependency>
</dependencies>
```


## complex example  
### test
```java

@Test
public void myTest(){
    // we want short alert popups in test that autoclose and the alert messages should be saved to file
    Path alertFilePath = Files.createTempFile(...);
    Binding alertBinding = Binding.Builder.bind(AlertDialog.class)
        .to(TestAlertDialog.class)
        .withArgs(alertFilePath.toString())
        .injectOriginal()
        .build();

    List<Binding> bindings = List.of(binding);
    String jsonBindings = new DefaultEnvOverrideBindingsGsonFactory().create().toJson(bindings);

    // you dont need to use ProcessBuilder, this ist just a demonstration
    ProcessBuilder pb = new ProcessBuilder("java", "-jar", "/path/to/my/app.jar");
    pb.environment().put("OVERRIDE_GUICE_BINDINGS", jsonBindings);
    Process process = pb.start();
    
    // do your testing ...
    
    String alertMsgDisplayed = Files.readString(alertFilePath);
    assertEquals("Invalid username", alertMsgDisplayed);
}
```
### application
```java

public class TestAlertDialog implements AlertDialog {
    private final Path filePath;
    private final AlertDialog original;

    @Inject
    public TestAlertDialog(@Named("TestAlertDialogArgs") List<String> args,
                           @Named("original") AlertDialog original
    ){
        this.filePath = Path.of(args.get(0));
        this.original = original;
    }

    @Override
    public void alert(String msg) {
        Files.write(filePath, msg);
        original.alert(msg);
        Thread.sleep(200);
        original.close();
    }

    @Override
    public void close(){
        original.close()
    }
}

```
