# NFIQ 1.0 for MOSIP

A Java implementation of **NIST Fingerprint Image Quality 1.0 (NFIQ 1.0)**, packaged as a library
(`io.mosip:nfiq1.0`) for the MOSIP biometric stack. It takes an ISO/IEC 19794-4 fingerprint
(JPEG 2000 or WSQ) and returns an **NFIQ score from 1 (best) to 5 (worst)** plus a confidence value.

The [root README](../README.md) has the full documentation: architecture, API, coverage and MOSIP
usage.

---

## At a glance

| | |
|---|---|
| Java | 21 (`--enable-preview` at compile, test and run time) |
| Parent | `io.mosip:nfiq` ([`../pom.xml`](../pom.xml)) → `spring-boot-starter-parent` (BOM and plugins only, no Spring app). All versions live there. |
| MOSIP | `kernel-core` + `biometrics-util`, no `kernel-bom` |
| Entry point | `org.mosip.nist.nfiq1.Nfiq1Helper#computeNfiq` |
| Sample harness | `src/test/java/org/mosip/nist/nfiq1/test/NfiqApplication.java` |
| Coverage gate | JaCoCo ≥ 90 % (instruction + line) at `mvn verify` |

---

## Local workflow

The launchers live in this folder. They locate themselves, so they also work when called from
elsewhere (for example `nfiq1.0\run-local-JP2.bat` from the repo root):

```text
run-local-JP2.bat | .sh [build] [0|1]  →  src/test/resources/info_jp2.iso (JPEG2000)
run-local-WSQ.bat | .sh [build] [0|1]  →  src/test/resources/info_wsq.iso (WSQ)
│                          │       └─ 0 = one-line score (default), 1 = detailed quality map
│                          └─ force a fresh "mvn clean package" first
└─ builds automatically if target/ has no jar yet
```

Examples (from `nfiq1.0/`):

```bash
run-local-JP2.bat               # NFIQ=1 Conf=0.748...
./run-local-WSQ.sh 1            # detailed quality-map output
run-local-JP2.bat build         # rebuild, then score
```

Logs are saved to `nfiq1.0/.local/logs/nfiq-<image>.log`. Extra JVM flags go in `JAVA_OPTS`.
The sample ISO files are read in place and never copied into `target/`.

---

## Plain Maven

Versions and plugin setup come from the parent [`../pom.xml`](../pom.xml); run from the repo root to
build and install both, or from here to build just this module.

```bash
mvn clean install "-Dgpg.skip=true"           # build + tests + coverage gate + install
mvn verify "-Dgpg.skip=true"                  # tests + JaCoCo report (target/site/jacoco) + 90 % gate
mvn test -Dtest=Nfiq1HelperTest               # single test class
mvn verify -Psonar                            # SonarCloud (CI)
```

Manual harness run, from `nfiq1.0/` (a relative `imgfile` is resolved against the working
directory). JVM flags go **before** `-cp`:

```bat
java --enable-preview -cp target\*;target\lib\*;target\test-classes org.mosip.nist.nfiq1.test.NfiqApplication "imgfile=src\test\resources\info_jp2.iso" "logs=0"
```

```bash
java --enable-preview -cp "target/*:target/lib/*:target/test-classes" org.mosip.nist.nfiq1.test.NfiqApplication "imgfile=src/test/resources/info_jp2.iso" "logs=0"
```

---

## Sample results

| Sample | Compression | Size | NFIQ | Confidence |
|---|---|---|---|---|
| `info_jp2.iso` | JPEG2000 | 294 × 539 @ 500 ppi | 1 | 0.748 |
| `info_wsq.iso` | WSQ | 500 ppi | 1 | 0.620 |

---

## License

[MPL-2.0](../LICENSE). See also [NOTICE](../NOTICE) and [THIRD-PARTY-NOTICES](../THIRD-PARTY-NOTICES).
