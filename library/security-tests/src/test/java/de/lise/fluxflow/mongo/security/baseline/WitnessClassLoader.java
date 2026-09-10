package de.lise.fluxflow.mongo.security.baseline;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Loads each harmless witness afresh, so initialization cannot leak between tests. */
public final class WitnessClassLoader extends ClassLoader {
    private static final String FIXTURE_PACKAGE =
        "de.lise.fluxflow.mongo.security.baseline.fixture.";
    private final List<String> events = new CopyOnWriteArrayList<>();

    public WitnessClassLoader() {
        super(WitnessClassLoader.class.getClassLoader());
    }

    public void record(String event) {
        events.add(event);
    }

    public List<String> getEvents() {
        return List.copyOf(events);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (!name.startsWith(FIXTURE_PACKAGE)) {
            return super.loadClass(name, resolve);
        }
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) {
                String resource = name.replace('.', '/') + ".class";
                try (InputStream input = getParent().getResourceAsStream(resource)) {
                    if (input == null) {
                        throw new ClassNotFoundException(name);
                    }
                    byte[] bytes = input.readAllBytes();
                    loaded = defineClass(name, bytes, 0, bytes.length);
                } catch (IOException exception) {
                    throw new ClassNotFoundException(name, exception);
                }
            }
            if (resolve) {
                resolveClass(loaded);
            }
            return loaded;
        }
    }
}
