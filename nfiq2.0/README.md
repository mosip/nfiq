# NFIQ 2.0 for MOSIP

A Java implementation of **NIST Fingerprint Image Quality 2 (NFIQ 2) v2.3.0**, the reference
implementation of ISO/IEC 29794-4, packaged as a library (`io.mosip:nfiq2.0`) for the MOSIP biometric
stack. Image operations use OpenCV (`org.openpnp:opencv`), making the same calls as the NIST C++. FingerJetFX and the random forest are plain Java.

Give it a 500 ppi fingerprint (an ISO/IEC 19794-4 record holding a JPEG 2000 lossless or WSQ image, or
raw 8-bit grey pixels). It returns:

| Output | Meaning |
|---|---|
| **Unified quality score** | `0` (worst) … `100` (best), from NIST's random forest model |
| **Native quality measures** | the 69 NIST features (`FDA_Bin10_Mean`, `FingerJetFX_MinutiaeCount`, …) |
| **Actionable feedback** | `UniformImage`, `EmptyImageOrContrastTooLow`, `FingerprintImageWithMinutiae`, `SufficientFingerprintForeground` |

> This product includes the DigitalPersona FingerJetFX OSE fingerprint feature extractor.
> (http://digitalpersona.com/fingerjetfx)

---

## Use as a dependency

```xml
<dependency>
    <groupId>io.mosip</groupId>
    <artifactId>nfiq2.0</artifactId>
    <version>${nfiq.version}</version>
</dependency>
```

```java
Nfiq2 nfiq2 = new Nfiq2();                                   // embedded NIST model, MD5-checked; reuse it
FingerprintImageData image = IsoImageDecoder.decode(Path.of("finger.iso"));
int score = nfiq2.computeUnifiedQualityScore(image);         // 0..100

// Measures and feedback without running the modules twice:
List<QualityModule> modules = QualityMeasures.computeNativeQualityMeasureAlgorithms(image);
int same = nfiq2.computeUnifiedQualityScore(modules);
Map<String, Double> measures = QualityMeasures.getNativeQualityMeasures(modules);
Map<String, Double> feedback = QualityMeasures.getActionableQualityFeedback(modules);

// Raw pixels (row-major, 0 = black, 500 ppi):
int fromRaw = nfiq2.computeUnifiedQualityScore(new FingerprintImageData(pixels, width, height, 0, 500));
```

- Failures throw `Nfiq2Exception`, a kernel-core `BaseUncheckedException`. `getErrorCode()` returns the MOSIP
  code (`MOS-NFIQ2-002` …), and `getNfiq2ErrorCode()` returns the `ErrorCode` with NIST's names
  (`BadArguments`, `InvalidImageSize`, `FJFX_CannotCreateFeatureSet`, …).
- The OpenCV native library is extracted and loaded on first use (`nu.pattern.OpenCV.loadLocally()`).
- `Nfiq2` is immutable and thread-safe once built. Parsing and hashing the 22 MB model is the expensive
  part, so build it once and share it.
- Another model can be loaded with `new Nfiq2(Path yamlOrGz, String md5)` or `Nfiq2.fromModelInfo(Path)`
  (NIST model-info format: `Path =` / `Hash =`).
- Only 500 ppi images are supported; the library does not resample.

---

## How it works

```text
ISO 19794-4 record ──IsoImageDecoder (JP2 / WSQ)──► 8-bit grey, 500 ppi
   │  FingerprintImageData.copyRemovingNearWhiteFrame
   ▼
quality modules (NIST order): FDA · FingerJetFX · FJFXMinutiaeQuality · ImgProcROI · LCS · Mu
                              OCLHistogram · OF · QualityMap · RVUPHistogram
   │  69 native quality measures
   ▼
RandomForestModel (nist_plain_tir-ink.yaml, MD5 b4a1e7586b3be906f9770e4b77768038)
   │  votes / trees × 100, floor(x + 0.5)
   ▼
unified quality score 0..100
```

| Package | NIST origin | License |
|---|---|---|
| `org.mosip.nist.nfiq2` | `NFIQ2Algorithm/src/nfiq2` (API, data, model info) | MPL-2.0 |
| `.qualitymeasures` | `quality_modules/*` (one class per module) | MPL-2.0 |
| `.prediction` | `prediction/RandomForestML` (OpenCV RTrees YAML) | MPL-2.0 |
| `.cv` | thin wrapper over the OpenCV calls NFIQ 2 makes (blur, erode, Otsu, contours, flood fill, warp, QR solve, DFT) | MPL-2.0 |
| `.imagetools` | — (ISO 19794-4 decoding) | MPL-2.0 |
| `.frfxll` | FingerJetFX OSE `libFRFXLL` (NIST fork) | **LGPL-3.0-or-later** + DigitalPersona conditions |

---

## Accuracy

Every native quality measure matches the NIST NFIQ 2.3.0 Windows binary (built with OpenCV 4.10; this
library runs on openpnp OpenCV 4.9) to 5 decimals on the sample records. `NistReferenceTest` checks scores, feedback and a selection of measures:

| Sample (`nfiq2.0/src/test/resources`) | NIST score | Java score |
|---|---|---|
| `info_jp2.iso` (JPEG 2000 lossless) | 72 | 72 |
| `info_wsq.iso` (WSQ) | 75 | 75 |

A few OpenCV behaviours still had to be reproduced by hand where the Java code works on plain arrays.
For example, `copyMakeBorder` on a sub-matrix pads with the real neighbouring pixels, and `compare`
with an out-of-range threshold does not saturate. See [`AGENTS.md`](AGENTS.md). ISO/IEC 29794-4 conformance
still has to be checked on NIST's licensed conformance dataset.

---

## Build and test

```bash
mvn clean install "-Dgpg.skip=true"          # repo root: parent, nfiq1.0 and nfiq2.0
mvn -pl nfiq2.0 verify "-Dgpg.skip=true"      # this module: tests + JaCoCo 90 % gate
```

Tests read `info_jp2.iso` and `info_wsq.iso` from `src/test/resources` in place (they are copies of the
nfiq1.0 samples and are never copied into `target/`). The coverage report is at
`nfiq2.0/target/site/jacoco/index.html`.

---

## Run locally

The launchers live in `nfiq2.0/` and score the samples with the `Nfiq2Application` harness. They find
their own folder, so they can be called from anywhere:

| Windows cmd | Linux / macOS / Git Bash | Sample | Expected |
|---|---|---|---|
| `run-local-JP2.bat [build] [0\|1]` | `./run-local-JP2.sh [build] [0\|1]` | `info_jp2.iso` (JPEG 2000) | `NFIQ2=72` |
| `run-local-WSQ.bat [build] [0\|1]` | `./run-local-WSQ.sh [build] [0\|1]` | `info_wsq.iso` (WSQ) | `NFIQ2=75` |

- If `nfiq2.0/target/` has no jar yet, the script runs `mvn clean package -DskipTests` first; `build`
  forces that rebuild.
- `0` (default) prints the score; `1` also prints the actionable feedback, the 69 native quality measures
  and the module timings.
- Output is also saved to `nfiq2.0/.local/logs/nfiq2-<image>.log` (ignored by Git). Extra JVM flags go in
  `JAVA_OPTS`.

Manual command (from `nfiq2.0/`; on Linux or macOS use `:` and `/`):

```bat
java --enable-preview -cp target\nfiq2.0-<version>.jar;target\lib\*;target\test-classes org.mosip.nist.nfiq2.test.Nfiq2Application "imgfile=src\test\resources\info_wsq.iso" "logs=1"
```

---

## License

The project is under the [Mozilla Public License 2.0](../LICENSE), **except** the package
`org.mosip.nist.nfiq2.frfxll`. That package is a MODIFIED version (a Java translation) of the Digital
Persona FingerJetFX OSE fingerprint feature extractor and is licensed under the
[GNU LGPL 3.0 or later](../licenses/LGPL-3.0-only.txt), subject to the conditions in
[`licenses/FingerJetFX-OSE-COPYRIGHT.txt`](../licenses/FingerJetFX-OSE-COPYRIGHT.txt). Both files are
also packaged in the JAR under `META-INF/licenses/`. "FingerJet" and "FingerJetFX" are trademarks of
DigitalPersona, Inc.; this product does not use those names and is not endorsed by DigitalPersona.

The NFIQ 2 algorithm and model are NIST works, not subject to copyright in the United States, and are
provided "AS IS". Attributions: [NOTICE](../NOTICE) · [THIRD-PARTY-NOTICES](../THIRD-PARTY-NOTICES) ·
[licenses/](../licenses/).
