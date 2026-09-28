# nfiq (Maven parent)

JDK 21 `--enable-preview` · Boot parent = BOM only · no kernel-bom · all versions/plugins in root `pom.xml`.

```
nfiq/
├─ pom.xml            parent io.mosip:nfiq: versions, Boot BOM overrides, plugins, JaCoCo 90%, Sonar, publishing
├─ nfiq1.0/           io.mosip:nfiq1.0 · NFIQ 1.0 → 1..5 + conf      → nfiq1.0/AGENTS.md
├─ nfiq2.0/           io.mosip:nfiq2.0 · NFIQ 2 v2.3.0 → 0..100      → nfiq2.0/AGENTS.md
├─ licenses/          license texts (LRM chars in names: never rename) + NOTICE (MOSIP matrix)
├─ NOTICE · THIRD-PARTY-NOTICES(.txt identical)
└─ .github/workflows/push-trigger.yml   kattu, SERVICE_LOCATION ./, MAVEN_NON_EXEC_ARTIFACTS nfiq1.0,nfiq2.0
```

Build `mvn clean install "-Dgpg.skip=true"` · one module `mvn -pl nfiq1.0|nfiq2.0 verify "-Dgpg.skip=true"` · Sonar `-Psonar`

## Rules
- Module POMs: no versions. Parent + module `<version>` identical, explicit, change only when asked. Each POM has own `url/licenses/scm/developers`.
- Both modules: `biometrics-util` excludes `kernel-core`; `kernel-core` re-declared with `*:*` exclusion (its graph has EPL/LGPL/GPL-CE/CC + JUnit 4).
- Banned: kernel-bom, log4j 1.x/slf4j-log4j12, mockito-inline, JUnit 4/vintage. SLF4J API only; Logback test scope.
- Shipped jars must be "Use with MOSIP: Yes" (`licenses/NOTICE` matrix). EPL/LGPL/GPL/CC only test/build. Sole exception: `nfiq2.0` `frfxll/` (LGPL-3.0). Check `mvn dependency:list` after changes.
- No versions in README/AGENTS/launchers. Notices list third-party versions only (never MOSIP/project).
- Dep/plugin/Boot change → update NOTICE, THIRD-PARTY-NOTICES + `.txt`, `licenses/NOTICE`, add new license text.
- Never commit `target/ .local/ .idea/ effective-*.xml` or secrets (`OSSRH_* GPG_SECRET SONAR_TOKEN ORG_KEY SLACK_WEBHOOK`).
- New module → `<modules>` + `MAVEN_NON_EXEC_ARTIFACTS`.
- Commits `git commit -s`, title `#<issue>: <summary>`; PRs → `develop`.
