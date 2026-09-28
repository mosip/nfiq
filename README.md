# NFIQ for MOSIP

Java implementations of NIST fingerprint image quality for the MOSIP biometric stack:

| Module | Algorithm | Score | Docs |
|---|---|---|---|
| `io.mosip:nfiq1.0` | NIST NFIQ 1.0 | 1 (best) … 5 (worst) + confidence | this README |
| `io.mosip:nfiq2.0` | NIST NFIQ 2 v2.3.0 (ISO/IEC 29794-4) | 0 (worst) … 100 (best) | [`nfiq2.0/README.md`](nfiq2.0/README.md) |

The rest of this README covers **NFIQ 1.0**. NFIQ 2.0 is documented in its own README.

Give it a fingerprint (an ISO/IEC 19794-4 record holding a JPEG2000 or WSQ image). It returns:

| Output | Meaning |
|---|---|
| **NFIQ score** | `1` (excellent) … `5` (poor) |
| **Confidence** | `0.0` … `1.0`, the MLP activation for the chosen class |

> This is a from-scratch **Java re-implementation**, not a wrapper around NIST's C code. Package
> names (`mindtct`, `mlp`, …) mirror NIST's modules so the two can be cross-referenced.

---

## Contents

- [Quick start](#quick-start)
- [Repository layout](#repository-layout)
- [Technology stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Build](#build)
- [Run locally](#run-locally)
- [Use as a dependency](#use-as-a-dependency)
- [How it works](#how-it-works)
- [Testing, coverage and Sonar](#testing-coverage-and-sonar)
- [Logging and configuration](#logging-and-configuration)
- [Known limitations](#known-limitations)
- [Usage in MOSIP](#usage-in-mosip)
- [Contributing and CI](#contributing-and-ci)
- [License](#license)

---

## Quick start

```bash
git clone https://github.com/mosip/nfiq.git
cd nfiq/nfiq1.0            # the launchers live in the module folder

run-local-JP2.bat          # Windows cmd
./run-local-JP2.sh         # Linux / macOS / Git Bash
```

The first run builds `nfiq1.0` automatically, then scores the JPEG2000 sample.

Expected output:

```text
NFIQ=1 Conf=0.7483608380532185
```

With `1` (for example `run-local-JP2.bat 1`) the `COMPUTED NFIQ1.0 VALUES` block (the 11 features) is printed before the score. Pipeline tracing is logged at DEBUG.

---

## Repository layout

```text
nfiq/
├── pom.xml                       parent io.mosip:nfiq · all versions · Java 21 · no kernel-bom
├── nfiq1.0/                      module io.mosip:nfiq1.0 (the library)
│   ├── pom.xml                   dependencies, coverage excludes, JAR manifest
│   ├── run-local-JP2.bat / .sh   build if needed + score info_jp2.iso  [build] [0|1]
│   ├── run-local-WSQ.bat / .sh   build if needed + score info_wsq.iso  [build] [0|1]
│   ├── .local/logs/              run output (ignored by Git)
│   └── src/
│       ├── main/java/org/mosip/nist/nfiq1/
│       │   ├── Nfiq1Helper       entry point: computeNfiq(...)
│       │   ├── imagetools/       ISO → greyscale decoding (JP2 / WSQ)
│       │   ├── mindtct/          minutiae detection + quality maps (NIST MINDTCT)
│       │   ├── mlp/              multi-layer perceptron classifier
│       │   ├── common/           constants and data types (ILfs, INfiq, IMlp …)
│       │   └── util/             image / string / statistics helpers
│       ├── main/resources/znorm.dat         Z-normalisation coefficients (do not edit)
│       └── test/
│           ├── java/.../*Test.java          JUnit 5 tests, one per main class
│           ├── java/.../test/NfiqApplication  runnable sample harness
│           └── resources/
│               ├── info_jp2.iso             sample ISO 19794-4 record, JPEG2000
│               ├── info_wsq.iso             sample ISO 19794-4 record, WSQ
│               └── logback-test.xml         logging for tests and the harness
├── nfiq2.0/                      module io.mosip:nfiq2.0: NIST NFIQ 2 v2.3.0 port (see nfiq2.0/README.md)
│   ├── run-local-JP2.bat / .sh   build if needed + score info_jp2.iso  [build] [0|1]  (NFIQ2=72)
│   ├── run-local-WSQ.bat / .sh   build if needed + score info_wsq.iso  [build] [0|1]  (NFIQ2=75)
│   └── src/test/resources/       info_jp2.iso · info_wsq.iso (copies of the nfiq1.0 samples)
├── licenses/                     license texts and the MOSIP compatibility matrix
├── NOTICE · THIRD-PARTY-NOTICES  attribution for every dependency
└── .github/workflows/            CI: build, publish, Sonar (mosip/kattu)
```

---

## Technology stack

| Area | Choice |
|---|---|
| Language | Java 21, compiled and run with `--enable-preview` |
| Build | Maven 3.9+ reactor: parent `io.mosip:nfiq` → `spring-boot-starter-parent` (dependency management only) |
| MOSIP libraries | `kernel-core`, `biometrics-util` (pinned in the parent; **no `kernel-bom`**) |
| Image codecs | `jai-imageio-jpeg2000` (JP2), `jnbis` (WSQ) |
| Logging | SLF4J API only; the consuming service supplies the backend (Logback is test scope here) |
| Tests | JUnit Jupiter, Mockito, AssertJ, spring-test (from `spring-boot-starter-test`) |
| Quality gates | JaCoCo (≥ 90 % gate), SonarCloud (`-Psonar`) |

All versions are defined once in the parent [`pom.xml`](pom.xml) `<properties>` (or come from the
Spring Boot BOM); this README intentionally does not repeat them.

The library has no Spring context, HTTP endpoints or database. Spring Boot is used only as a BOM
and plugin manager.

---

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| JDK | **21** | Needed for `--enable-preview` |
| Maven | 3.9+ | |
| Git | any | |
| MOSIP libraries | as in parent `pom.xml` | `kernel-core` and `biometrics-util`. Resolved from Central Snapshots, or installed locally first. |

---

## Build

```bash
mvn clean install "-Dgpg.skip=true"      # from the repo root: installs the parent, nfiq1.0 and nfiq2.0
```

The root `pom.xml` is the parent. It holds every version, the plugin
setup, the JaCoCo gate, the Sonar profile and Maven Central publishing. `nfiq1.0/pom.xml` only
lists dependencies (no versions) and module-specific settings. Building inside `nfiq1.0/` still
works, because the parent is found at `../pom.xml`. Consumers need the parent POM published too,
so release from the repo root.

What gets produced in `nfiq1.0/target/`:

| Path | What it is |
|---|---|
| `nfiq1.0-<version>.jar` | the library |
| `nfiq1.0-<version>-sources.jar` / `-javadoc.jar` | Maven Central attachments |
| `lib/` | runtime dependencies (used by the sample harness) |
| `test-classes/` | compiled tests and `NfiqApplication` |
| `site/jacoco/index.html` | coverage report |

---

## Run locally

The launchers live in `nfiq1.0/` and score the samples in `nfiq1.0/src/test/resources/`. They find
their own folder, so you can also call them from elsewhere (for example `nfiq1.0\run-local-JP2.bat`
from the repo root):

| Windows cmd | Linux / macOS / Git Bash | Sample |
|---|---|---|
| `run-local-JP2.bat [build] [0\|1]` | `./run-local-JP2.sh [build] [0\|1]` | `info_jp2.iso` (JPEG2000) |
| `run-local-WSQ.bat [build] [0\|1]` | `./run-local-WSQ.sh [build] [0\|1]` | `info_wsq.iso` (WSQ) |

- If `nfiq1.0/target/` has no jar yet, the script runs `mvn clean package -DskipTests` first.
- `build` forces that rebuild even when a jar exists.
- `0` (default) prints the one-line score; `1` prints the detailed quality map.
- Output is also saved to `nfiq1.0/.local/logs/nfiq-<image>.log` (ignored by Git).
- Extra JVM flags go in `JAVA_OPTS`.
- The ISO files are read in place and never copied into `target/`.

For tests, coverage and Sonar, use Maven directly (see
[Testing, coverage and Sonar](#testing-coverage-and-sonar)).

### Manual command

JVM options (`-D…`, `--enable-preview`) must come **before** `-cp` and the main class. A relative
`imgfile` is resolved against the working directory, so run from `nfiq1.0/`:

```bat
cd nfiq1.0
java --enable-preview -cp target\*;target\lib\*;target\test-classes org.mosip.nist.nfiq1.test.NfiqApplication "imgfile=src\test\resources\info_wsq.iso" "logs=1"
```

On Linux or macOS, use `:` instead of `;` and `/` instead of `\`.

### Reading the detailed output (`logs=1`)

```text
COMPUTED NFIQ1.0 VALUES
[
 Quality Map Foreground Count=3384,
 number of minutiae=112,
 (reliability count > 0.5) = 54,
 (reliability count > 0.6) = 54,
 (reliability count > 0.7) = 54,
 (reliability count > 0.8) = 14,
 (reliability count > 0.9) = 0,
 (qmap count == 1) = 0.089244,
 (qmap count == 2) = 0.093972,
 (qmap count == 3) = 0.191194,
 (qmap count == 4) = 0.625591
]
NFIQ=1 Conf=0.6195380315622201
```

| Field | Meaning |
|---|---|
| Quality Map Foreground Count | number of foreground blocks in the quality map |
| number of minutiae | total minutiae detected by MINDTCT |
| reliability count > t | minutiae whose reliability exceeds threshold *t* |
| qmap count == n | share of foreground blocks at quality level *n* (1 = worst … 4 = best) |
| NFIQ / Conf | final class (1 = best … 5 = worst) and MLP confidence |

These 11 values make up the NFIQ feature vector. It is Z-normalised with `znorm.dat` and then
classified by the MLP.

---

## Use as a dependency

```xml
<dependency>
    <groupId>io.mosip</groupId>
    <artifactId>nfiq1.0</artifactId>
    <version>${nfiq.version}</version>
</dependency>
```

```java
AtomicInteger rc = new AtomicInteger(), type = new AtomicInteger(), len = new AtomicInteger();
AtomicInteger w = new AtomicInteger(), h = new AtomicInteger(), depth = new AtomicInteger(), ppi = new AtomicInteger();
AtomicReference<String> fileType = new AtomicReference<>();

BufferedImage img = ImageDecoder.getInstance()
        .readAndDecodeGrayscaleImage(rc, "finger.iso", type, len, w, h, depth, ppi, fileType); // relative to CWD
int[] pixels = ImageUtil.convertTo1DWithoutUsingGetRGB(img, "jpg");

AtomicInteger nfiq = new AtomicInteger();
AtomicReference<Double> conf = new AtomicReference<>(0.0);
int ret = new Nfiq1Helper().computeNfiq(nfiq, conf, pixels, w.get(), h.get(), depth.get(), ppi.get(), 0);
// ret == 0 → success; nfiq.get() in 1..5, conf.get() in 0..1
```

The public methods under `org.mosip.nist.nfiq1` are an API used by Biometric SDK, ID Authentication,
Registration Processor and MDS, so treat signature changes as breaking.

---

## How it works

```text
ISO 19794-4 record
   │  imagetools.ImageDecoder      (biometrics-util + JP2 / WSQ codecs)
   ▼
8-bit greyscale pixels
   │  mindtct.Detect / GetMinutiae (maps → binarisation → minutiae → false-minutiae removal)
   ▼
minutiae + quality map
   │  Nfiq1Helper.computeNfiqFeatureVector   (11 features)
   ▼
feature vector ──Z-norm (znorm.dat)──►  mlp.RunMlp / MlpCla  ──►  NFIQ 1..5 + confidence
```

| Package | NIST origin | Responsibility |
|---|---|---|
| `imagetools` | an2k / image utils | read the ISO record and decode it to greyscale |
| `mindtct` | MINDTCT (`lfs`) | direction, low-contrast, low-flow and high-curvature maps; binarisation; minutiae detection and cleanup; quality map |
| `mlp` | `mlp` | feed-forward neural network that turns the feature vector into a class and confidence |
| `common` | headers (`lfs.h`, `nfiq.h`, `mlp.h` …) | constants and data structures |
| `util` | — | helpers |

---

## Testing, coverage and Sonar

```bash
mvn test                          # unit tests (repo root or nfiq1.0/)
mvn test -Dtest=Nfiq1HelperTest   # one test class
mvn verify "-Dgpg.skip=true"      # tests + JaCoCo report + 90 % gate
```

- **Coverage gate:** JaCoCo checks at least 90 % instruction and line coverage in `verify`
  (property `jacoco.coverage.ratio`). The report is at `target/site/jacoco/index.html`.
- **Excluded from coverage:** constant-only interfaces in `common/**`, plus `Defs`, `Nfiq1`,
  `Nfiq1Globals` and `Nist`. The same list is used in `sonar.coverage.exclusions`, so local and
  SonarCloud numbers match.
- **SonarCloud:** `mvn verify -Psonar` (CI runs it via `mosip/kattu`, project key `mosip_nfiq`).
- **Shared state:** several classes are singletons (`Globals`, `ImageDecoder` …). Tests that change
  shared or static state must restore it in `@AfterEach`.

---

## Logging and configuration

- Code logs through the **SLF4J** API only. The library does not ship a logging configuration, so
  consumers keep their own Logback setup.
- `src/test/resources/logback-test.xml` configures console output for tests and for the
  `NfiqApplication` harness.
- The end-of-life log4j 1.x / `slf4j-log4j12` pair has been removed.
- `src/main/resources/znorm.dat` holds the Z-normalisation means and standard deviations. It is
  reference data: do not edit or reformat it.
- There are no secrets in this repo. CI credentials (`OSSRH_*`, `GPG_SECRET`, `SONAR_TOKEN`, …) exist
  only as GitHub Actions secrets.

---

## Known limitations

- `kernel-core` is used as a jar only (all its transitive dependencies are excluded), because
  that graph carries licenses MOSIP does not allow. If a future `biometrics-util` needs one of
  those classes, add that single artifact explicitly and check it against the matrix in
  [`licenses/NOTICE`](licenses/NOTICE).
- NFIQ 2.0 is available as `io.mosip:nfiq2.0` ([`nfiq2.0/README.md`](nfiq2.0/README.md)). It matches the
  NIST 2.3.0 binary on the sample records, but ISO/IEC 29794-4 conformance has not yet been checked on
  NIST's licensed conformance dataset.

---

## Usage in MOSIP

| Component | How it uses NFIQ |
|---|---|
| Biometric capture devices / MDS | NFIQ 2.0 scores (0–100) for capture feedback and auto-capture |
| Biometric SDK: quality check | NFIQ 2.0 quality assessment during registration |
| Registration Client | NFIQ 2.0 scores to validate quality before submission |
| **ID Authentication** | **NFIQ 1.0** (this library) quality checks on authentication requests |
| Registration Processor | NFIQ 2.0 scores in the biometric quality-check stage |

More detail: [Biometric Specification](https://docs.mosip.io/1.2.0/biometrics/biometric-specification) ·
[MDS Specification](https://docs.mosip.io/1.2.0/biometrics/mds-specification) ·
[NIST NFIQ 2](https://www.nist.gov/services-resources/software/nfiq-2) ·
[NFIQ 2 API docs](https://pages.nist.gov/NFIQ2/)

---

## Contributing and CI

- CI (`.github/workflows/push-trigger.yml`) builds the whole reactor (`./`) on PRs and on pushes to
  `MOSIP*`, `develop*`, `master`, `1.*` and `release*`, and uploads both library jars as the `nfiq`
  build artifact (`MAVEN_NON_EXEC_ARTIFACTS: nfiq1.0,nfiq2.0`). Non-PR events also publish to Nexus
  (parent POM, `nfiq1.0` and `nfiq2.0`, each with sources, javadoc and signatures) and run Sonar.
- Before a PR, run `mvn clean install "-Dgpg.skip=true"` from the repo root, then the
  `run-local-JP2` and `run-local-WSQ` launchers of `nfiq1.0` and `nfiq2.0` as a smoke check.
- Put new versions and plugins in the root `pom.xml`, not in `nfiq1.0/pom.xml`.
- Sign off commits (`git commit -s`) and reference the issue: `#<issue>: <summary>`.
- Add or update the matching `*Test.java` for every changed main class.
- Don't bump `<version>` in an unrelated PR, and don't commit `target/` or `.local/`.

---

## License

[Mozilla Public License 2.0](LICENSE), except the FingerJetFX OSE port in `nfiq2.0` (package
`org.mosip.nist.nfiq2.frfxll`). That package is LGPL-3.0-or-later with DigitalPersona's conditions
([`licenses/FingerJetFX-OSE-COPYRIGHT.txt`](licenses/FingerJetFX-OSE-COPYRIGHT.txt)).
This product includes the DigitalPersona FingerJetFX OSE fingerprint feature extractor.
(http://digitalpersona.com/fingerjetfx)

Third-party attributions: [NOTICE](NOTICE) · [THIRD-PARTY-NOTICES](THIRD-PARTY-NOTICES) ·
[licenses/](licenses/).
