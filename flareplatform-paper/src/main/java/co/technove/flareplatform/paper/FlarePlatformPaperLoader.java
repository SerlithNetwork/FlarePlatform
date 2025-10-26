package co.technove.flareplatform.paper;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@SuppressWarnings("UnstableApiUsage")
@NullMarked
public class FlarePlatformPaperLoader implements PluginLoader {

    @Override
    public void classloader(PluginClasspathBuilder classpathBuilder) {
        // get versions from the property file, so we don't have to update this class when bumping deps
        final Properties properties = new Properties();
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("libraries.properties")) {
            if (stream != null) {
                properties.load(stream);
            } else {
                throw new RuntimeException("libraries.properties not found in resources!");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load libraries.properties", e);
        }

        String flareVersion = properties.getProperty("flareVersion");
        String oshiVersion = properties.getProperty("oshiVersion");

        MavenLibraryResolver resolver = new MavenLibraryResolver();

        resolver.addDependency(new Dependency(new DefaultArtifact("net.serlith:Flare:" + flareVersion), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("com.github.oshi:oshi-core:" + oshiVersion), null));

        resolver.addRepository(new RemoteRepository.Builder("central", "default",
                MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR).build()); // oshi
        resolver.addRepository(new RemoteRepository.Builder("jitpack", "default",
                "https://jitpack.io").build()); // flare

        classpathBuilder.addLibrary(resolver);
    }
}