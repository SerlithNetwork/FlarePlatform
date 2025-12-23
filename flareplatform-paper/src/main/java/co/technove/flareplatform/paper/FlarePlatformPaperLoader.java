package co.technove.flareplatform.paper;

import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;

@SuppressWarnings("UnstableApiUsage")
public class FlarePlatformPaperLoader implements PluginLoader, Versions {

    @Override
    public void classloader(PluginClasspathBuilder classpathBuilder) {

        MavenLibraryResolver resolver = new MavenLibraryResolver();

        resolver.addDependency(new Dependency(new DefaultArtifact("net.serlith:Flare:" + FLARE_VERSION), null));
        resolver.addDependency(new Dependency(new DefaultArtifact("com.github.oshi:oshi-core:" + OSHI_VERSION), null));

        resolver.addRepository(new RemoteRepository.Builder("central", "default",
            MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR).build()); // oshi
        resolver.addRepository(new RemoteRepository.Builder("jitpack", "default",
            "https://jitpack.io").build()); // flare

        classpathBuilder.addLibrary(resolver);
    }
}
