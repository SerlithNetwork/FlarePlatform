# Flare
This project's main goal is to bring the [Flare profiler](https://blog.airplane.gg/flare/), known from some [Airplane](https://github.com/Technove/Airplane) forks to platforms,
which don't include it by default.

Downloads can be found under releases.

## Setup

There are two primary things that should be setup in order to optimally use Flare, however they are not required.

### 1. Add the Java flags

```
-XX:+UnlockDiagnosticVMOptions -XX:+DebugNonSafepoints
```

These two flags will ensure that you are getting the most accurate data in your Flares.

### 2. Use a JVM with debug symbols

Most JDK distributions include debug symbols for Java 16+, but some distributions do not.
You can test with the following command:

```bash
gdb $JAVA_HOME/lib/server/libjvm.so -ex 'info address UseG1GC'
```

If the UseG1GC symbol is found, then the debug symbols are present and you can use memory profiling.


### 3. Configure the profiler

Historically, Flare was a paid product and thus subscriptions or other means of paid access were required in order to get a token and use it.
This is no longer the case and you can use Flare for free, given you have access to a Flare server with or without a token, depending on its auth setup.
Such a server can be set up yourself, using both the frontend [flare-viewer](https://github.com/SerlithNetwork/flare-viewer) and backend [Jet](https://github.com/SerlithNetwork/Jet) open-source implementations.

Once you have set up or got access to a server, it's required to configure the plugin to use them.
For how to do that, you can refer to this example configuration under `plugins/Flare/config.yml`:

```yaml
profiling:
    # Your Flare authorization token
    # If your viewer is public, you can input anything here
    token: "very-secret-token"

    # URI to upload the profiler samples
    # It will be used for both the backend and viewer
    backend-url: "https://flare.airplane.gg"

    # If provided, it will replace the backend URI before handling it to you
    # Useful if your backend and frontend do not share the same path
    frontend-url: "https://flare-frontend.airplane.gg"
```

Additionally, it is advised not to delete the below section of the config file, as it is **VERY IMPORTANT** to have some filtering in order to prevent leaking sensitive information to malicious backends.

```yaml
  # Fields to ignore in the above configurations
  hidden-entries:
    - "proxies.velocity.secret"
    - "web-services.token"
    - "misc.sentry-dsn"
    - "database"
    - "server-ip"
    - "motd"
    - "resource-pack"
    - "level-seed"
    - "rcon.password"
    - "rcon.ip"
    - "feature-seeds"
    - "world-settings.*.feature-seeds"
    - "world-settings.*.seed-*"
    - "seed-*"
```

Finally, you can run `/flare` or `/vflare` in-game or in-console to start profiling or keep configuring other aspects of the plugin to your liking!

## License
[MIT](https://github.com/SerlithNetwork/FlarePlatform/blob/HEAD/LICENSE)
