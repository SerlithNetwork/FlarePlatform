package co.technove.flareplatform.paper;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import io.papermc.paper.util.JarManifests;
import java.util.jar.Manifest;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;

@SuppressWarnings("UnstableApiUsage")
public class FlarePlatformPaperLoader implements PluginLoader {

    @Override
    public void classloader(PluginClasspathBuilder classpathBuilder) {
        final Manifest manifest = JarManifests.manifest(FlarePlatformPaperLoader.class);
        final String flareVersion = manifest != null ? manifest.getMainAttributes().getValue("flare-version") : "null";
        final String oshiVersion = manifest != null ? manifest.getMainAttributes().getValue("oshi-version") : "null";

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
