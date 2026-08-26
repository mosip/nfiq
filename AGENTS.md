# AGENTS.md

## Repository Overview

This repository holds MOSIP's implementation of **NFIQ 1.0** (NIST Fingerprint
Image Quality), a fingerprint quality-scoring algorithm originally published
by NIST in C. The code here is **not** a vendored copy of NIST's C source —
it is a from-scratch **Java re-implementation** of the NFIQ 1.0 algorithm,
organized under `org.mosip.nist.nfiq1` with package names (`mindtct`, `mlp`,
`imagetools`, `common`, `util`) that mirror the structure of NIST's original
C modules so the two can be cross-referenced, but every class is Java source
maintained in this repo — there is no `patches/` folder, no bundled `.c`/`.h`
files, and no CMake/autotools build.

The only functional module is `nfiq1.0/` (a single Maven project). A
`nfiq2.0/` directory also exists at the repo root, but it currently contains
only an empty placeholder file (`nfiq2.0/test.txt`) — there is no NFIQ 2.0
implementation in this repo yet. Per `nfiq1.0/README.md`, MOSIP components
(registration devices, MDS, Biometric SDK, ID Authentication, Registration
Processor) that need NFIQ 2.0 scoring today consume NIST's own
[NFIQ2](https://github.com/usnistgov/NFIQ2) project directly, not this repo.

Because there is exactly one real, buildable module, this repo uses a single
root `AGENTS.md` rather than a per-module tree — there is no second module
with its own build/run/deploy story to warrant a split guide.

## Technology Stack

- **Language**: Java 21 (`--enable-preview` compiler flag is set in the POM,
  so preview features may be in use — build and run with a JDK that supports
  it)
- **Build tool**: Maven (single module, `nfiq1.0/pom.xml`, artifact
  `io.mosip:nfiq1.0`)
- **Test framework**: JUnit 5 (`junit-jupiter`) with Mockito
  (`mockito-inline`, `mockito-junit-jupiter`)
- **Key libraries**: `jai-imageio-jpeg2000` (JPEG2000/JP2 decoding),
  `jnbis` (WSQ decoding), `io.mosip.kernel:kernel-bom` /
  `io.mosip.biometric.util:biometrics-util` (MOSIP kernel BOM and
  biometrics utilities), Lombok, SLF4J + `slf4j-log4j12`. **Log4j 1.x is
  end-of-life with no planned vulnerability fixes** — this is a known
  risk to track toward migrating to a supported logging backend
  (e.g. Log4j 2 or Logback via SLF4J), not something to extend.
  `nfiq1.0/src/main/resources/log4j.properties` currently configures
  only console and rolling-file appenders — do not add remote, JMS, or
  socket appenders, which carry known Log4j 1.x deserialization risks.
- **Coverage**: JaCoCo (`jacoco-maven-plugin`)
- **Static analysis**: SonarCloud, wired up via a Maven profile named
  `sonar` (see `nfiq1.0/pom.xml`, not active by default)

## Build & Test Commands

All commands run from the `nfiq1.0/` directory (the only module):

```bash
cd nfiq1.0
```

Build and install to the local Maven repository (matches the top-level
`README.md`, which skips GPG signing for local builds):

```bash
mvn clean install -Dgpg.skip=true
```

Run tests only:

```bash
mvn test
```

Run a single test class:

```bash
mvn test -Dtest=Nfiq1HelperTest
```

Run the sample application after a build (from `nfiq1.0/target`, using the
version currently in `pom.xml`, e.g. `0.1.1-SNAPSHOT` on `develop`). The
classes are compiled with `--enable-preview`, so the same flag is required
at runtime too:

POSIX shell:

```bash
cd target
java --enable-preview -cp "nfiq1.0-0.1.1-SNAPSHOT.jar:lib/*:test-classes" \
  org.mosip.nist.nfiq1.test.NfiqApplication "imgfile=info_jp2.iso" "logs=0"
```

Windows (`cmd.exe`, matching `nfiq1.0/runJP2.bat`'s classpath syntax):

```bat
java --enable-preview -cp nfiq1.0-0.1.1-SNAPSHOT.jar;lib\*;test-classes org.mosip.nist.nfiq1.test.NfiqApplication "imgfile=info_jp2.iso" "logs=0"
```

The repo also ships `nfiq1.0/runJP2.bat` and `nfiq1.0/runWSQ.bat`, which run
the same `NfiqApplication` test harness against the bundled sample ISO files
(`info_jp2.iso`, `info_wsq.iso`) copied into `target/` by the
`maven-resources-plugin` during the `validate` phase. These `.bat` files
hardcode a jar version (currently `nfiq1.0-0.1.1-SNAPSHOT.jar`) — check it
matches the version you actually built before running, and update it if
`pom.xml`'s `<version>` has moved on. Note that, as checked in, neither
`.bat` file actually passes `--enable-preview` even though it's required at
runtime (see above) — prefer the documented `--enable-preview` command
above for local runs, since the bundled scripts as-is can fail if the code
path you're exercising uses preview APIs.

`NfiqApplication` expects two arguments: `imgfile` (path to an ISO file
containing a JP2- or WSQ-compressed fingerprint) and `logs` (`0` for a
one-line score, `1` for detailed quality-map output). It prints a score of
1 (best) to 5 (worst) plus a confidence value.

## Configuration

- Logging is configured via `nfiq1.0/src/main/resources/log4j.properties`
  (log4j 1.x style, matching the `slf4j-log4j12` binding declared in the
  POM).
- `nfiq1.0/src/main/resources/znorm.dat` holds the Z-normalization
  coefficients the algorithm needs at runtime (`Nfiq1ZNormalization.java`)
  — do not delete or reformat it; it is binary/numeric reference data, not
  source.
- No `application.properties`/`application.yml`/Spring config exists in
  this repo — it is a plain library JAR, not a Spring Boot service, and it
  exposes no HTTP endpoints or database connections. There are no secrets
  or credential files checked into the repo to worry about.
- CI publishing credentials (`OSSRH_USER`, `OSSRH_SECRET`, `OSSRH_TOKEN`,
  `GPG_SECRET`, `SLACK_WEBHOOK`, `SONAR_TOKEN`, `ORG_KEY`) are consumed only
  as GitHub Actions secrets in `.github/workflows/push-trigger.yml` — never
  hardcode any of these locally.

## Project Structure Notes

```text
nfiq/
├── nfiq1.0/                     # the only real module (Maven project)
│   ├── pom.xml
│   ├── README.md                # module-specific setup notes
│   ├── info_jp2.iso, info_wsq.iso   # sample fingerprint ISO images
│   ├── runJP2.bat, runWSQ.bat   # convenience scripts for NfiqApplication
│   └── src/
│       ├── main/java/org/mosip/nist/nfiq1/
│       │   ├── mindtct/         # minutiae detection (Java port of NIST MINDTCT)
│       │   ├── mlp/             # multi-layer perceptron classifier
│       │   ├── imagetools/      # JP2/WSQ decoding helpers
│       │   ├── common/          # shared interfaces/constants
│       │   └── util/            # string/image utility helpers
│       ├── main/resources/      # log4j.properties, znorm.dat
│       └── test/java/org/mosip/nist/nfiq1/
│           ├── .../*Test.java   # JUnit 5 unit tests, one per main class
│           └── test/NfiqApplication.java  # runnable sample/demo app
├── nfiq2.0/
│   └── test.txt                 # empty placeholder — no real code here
├── licenses/                    # third-party license texts (MPL, Apache, BSD, etc.)
├── .github/workflows/push-trigger.yml  # CI (build, publish, Sonar)
└── README.md                    # repo-level overview and usage-in-MOSIP notes
```

Test classes largely shadow the main class names 1:1 (e.g. `Block.java` →
`BlockTest.java`), so when changing a main class, check for and update its
matching test first.

## Development Workflow

- Verify the current default/active branch yourself (see the command
  under Agent rules below) rather than assuming `develop` or `master` —
  do not rely on a hardcoded branch name here.
- CI (`.github/workflows/push-trigger.yml`) triggers on: a published
  release, PRs (`opened`, `reopened`, `synchronize`) against any base
  branch, manual `workflow_dispatch`, and pushes to branches matching
  `MOSIP*`, `develop*`, `master`, `1.*`, or `release*`. It always runs the
  Maven build (`mosip/kattu` reusable workflow against
  `SERVICE_LOCATION: ./nfiq1.0`); it additionally runs
  `publish_to_nexus` and `sonar_analysis` on non-PR events. There is no
  `paths:` filter, so any change anywhere in the repo triggers the full
  workflow.
- Before opening a PR, build and run the full test suite locally from
  `nfiq1.0/`:

  ```bash
  cd nfiq1.0
  mvn clean install -Dgpg.skip=true
  ```

- Keep the `mindtct`/`mlp` package structure aligned with NIST's original
  NFIQ 1.0 module layout where practical — it makes cross-referencing
  against upstream NIST documentation and future NFIQ2 work easier for the
  next contributor.

## Pull Request Guidelines

- Verify the current default branch with `gh repo view mosip/nfiq --json
  defaultBranchRef` before opening a PR — do not assume. As of this
  writing GitHub reports `master` as the default, but `develop` is the
  active integration branch that recent feature PRs target; open feature
  PRs against `develop` unless you are specifically backporting to a
  release branch.
- Reference the tracking issue in the PR title/description (this repo's
  history uses `#<issue-number>: <summary>`-style commit/PR titles).
- Sign off commits (`git commit -s`) — MOSIP repos expect a DCO
  sign-off trailer.
- Keep changes scoped to `nfiq1.0/` unless the PR is specifically about
  root-level repo concerns (CI, licensing, top-level docs).
- Don't bump the module `<version>` in `nfiq1.0/pom.xml` as part of an
  unrelated change — version bumps are handled as their own PRs (see the
  `master` history, e.g. "Updated Pom versions for release changes").
- If you touch `runJP2.bat`/`runWSQ.bat` or `NfiqApplication`, double check
  the hardcoded jar filename still matches `pom.xml`'s `<version>`.

## Repository-Specific Considerations

- The `licenses/` folder contains file names with trailing invisible
  Unicode marks (e.g. a left-to-right mark after `.txt`) inherited from
  how they were originally checked in — this is expected; don't "fix" the
  filenames without checking downstream references to them first.
- `nfiq1.0/logs/nfiq1.log` is a tracked-but-empty log file; avoid growing
  it with local run output in the same commit as unrelated changes.
- The project enables Java `--enable-preview` in both the compiler and
  surefire `argLine` — if you add a JVM invocation of your own (e.g. a
  local script), any `-D` system properties must be placed **before** the
  `-jar`/`-cp` main-class argument, for example:

  ```bash
  java -Dlog4j.configuration=file:log4j.properties --enable-preview \
    -cp "nfiq1.0-0.1.1-SNAPSHOT.jar:lib/*:test-classes" \
    org.mosip.nist.nfiq1.test.NfiqApplication "imgfile=info_jp2.iso" "logs=0"
  ```

- This is a library consumed by other MOSIP services (Biometric SDK,
  Registration Processor, ID Authentication, MDS) as a Maven dependency
  (`io.mosip:nfiq1.0`) — a change here can ripple into those consumers, so
  treat public method signatures in `org.mosip.nist.nfiq1` as an API
  surface, not internal detail.

## Agent rules

### Do

1. Verify the current default branch with `gh repo view mosip/nfiq --json defaultBranchRef` before branching — do not assume `develop` or `master`.
2. Build and test from inside `nfiq1.0/` using `mvn clean install -Dgpg.skip=true`, matching the documented local setup.
3. Keep new/changed classes under `org.mosip.nist.nfiq1`, matching the existing `mindtct`/`mlp`/`imagetools`/`common`/`util` package layout.
4. Add or update a matching `*Test.java` under `nfiq1.0/src/test/java` for every changed class under `nfiq1.0/src/main/java`.
5. Sign off commits (`git commit -s`) and reference the tracking issue number in commit/PR titles.
6. Treat `nfiq2.0/` as an empty placeholder — call this out explicitly if a task assumes NFIQ 2.0 code exists here.

### Do not

1. Do not describe this repo as a vendored/patched copy of NIST's C source — it is an original Java implementation; do not invent a `patches/` folder or C build system that does not exist.
2. Do not put `-D` JVM system properties after `-jar`/`-cp` in any example command — they must precede it.
3. Do not bump `nfiq1.0/pom.xml`'s `<version>` as a side effect of an unrelated change.
4. Do not assume this is a Spring Boot service — there is no `application.yml`/`application.properties`, no controllers, and no HTTP layer here.
5. Do not commit build output (`nfiq1.0/target/`) or modify the tracked sample ISO files (`info_jp2.iso`, `info_wsq.iso`) used by the test harness.
