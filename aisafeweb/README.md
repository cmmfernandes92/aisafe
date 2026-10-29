# AISafe Web

HTMX/Tailwind web adapter over the AISafe core: a second presentation layer that calls the same use-case controllers as the console apps. [How it fits together](docs/aisafeweb_readme.md).

It lives outside `../aisafe` so the Applications Engineering project stays unchanged.

## Build and run

Run these from `aisafeweb/`. The core must be installed in the local Maven repository first.

```bash
../aisafe/mvnw -f ../aisafe/pom.xml install -DskipTests -Dmaven.javadoc.skip=true
../aisafe/mvnw -f pom.xml spring-boot:run
```

On Windows use `..\aisafe\mvnw.cmd` and quote the `-D` arguments. Then open http://localhost:8081.

Running from `aisafe/` instead is the usual cause of `No plugin found for prefix 'spring-boot'` (wrong pom).

## Bootstrap

The web app doesn't seed data by default; run `run-bootstrap` from `aisafe/` (see the root README), or seed at startup:

```bash
../aisafe/mvnw -f pom.xml spring-boot:run -Dspring-boot.run.arguments=--aisafeweb.bootstrap.enabled=true
```

Don't call `AisafeBootstrap.main()` from this app: the console bootstrap launcher exits the JVM.
