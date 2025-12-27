# Flare

Flare, also known as the [Airplane](https://github.com/Technove/Airplane) profiler. \
Our goal is to re-vitalize the [Flare profiler](https://blog.airplane.gg/flare/) and bring it to platforms that don't include it as a patch.

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

If the UseG1GC symbol is found, then the debug symbols are present, and you can use memory profiling.


### 3. Configure the profiler

Compared to the original [Flare Plugin](https://github.com/Technove/FlarePlugin), this implementation provides an additional field besides the two base ones.
1. Token
2. Backend URL
3. Frontend URL `⭐ New`

---

Due to Flare being designed to be closed, the `token` will provide backwards compatibility with said viewer instances. \
Should you want to self-host your own instance, you can use both the frontend [flare-viewer](https://github.com/SerlithNetwork/flare-viewer) and backend [Jet](https://github.com/SerlithNetwork/Jet) open-source implementations. \
Given the open-source nature of these implementations, the token can be used to access protected instances, or be completely ignored if you're using a public instance.

The URLs will be the endpoints to access the Flare viewer. \
Legacy instances only require a `backend-url` to both submit data and view the report. \
Self-hosted instances might require a `frontend-url` if your setup doesn't allow to forward requests given a single URL.

---

Once you have set up or got access to a server, it's required to configure the plugin to use it.
You can refer to this example configuration under `plugins/Flare/config.yml`:

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
    frontend-url: "https://flare-viewer.airplane.gg"
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
