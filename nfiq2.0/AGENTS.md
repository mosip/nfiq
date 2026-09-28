# nfiq2.0

Library `io.mosip:nfiq2.0`: Java port of NIST [NFIQ2 v2.3.0](https://github.com/usnistgov/NFIQ2/tree/v2.3.0). Score 0..100 + 69 measures + feedback. OpenCV via `org.openpnp:opencv` (`nu.pattern.OpenCV.loadLocally()`).

Flow: ISO → `IsoImageDecoder` → `copyRemovingNearWhiteFrame` → `QualityMeasures` (NIST module order) → `RandomForestModel.predictRaw` → `floor(votes/trees*100+0.5)`.

```
src/main/java/org/mosip/nist/nfiq2/
├─ Nfiq2              public API computeUnifiedQualityScore + model MD5 check
├─ QualityMeasures    runner, native measures, feedback
├─ FingerprintImageData · ModelInfo · ErrorCode · Nfiq2Exception
├─ imagetools/        IsoImageDecoder (JP2 lossless / WSQ)
├─ qualitymeasures/   one class per NIST module + CommonFunctions
├─ prediction/        RandomForestModel (streaming YAML)
├─ cv/                Cv: OpenCV calls with NIST's exact args
└─ frfxll/            FingerJetFX OSE port: LGPL-3.0-or-later
src/main/resources/org/mosip/nist/nfiq2/  nist_plain_tir-ink.yaml.gz (MD5 b4a1e7586b3be906f9770e4b77768038, never edit)
src/test/…  Nfiq2Test · NistReferenceTest · per-package tests · test/Nfiq2Application (harness)
src/test/resources/info_{jp2,wsq}.iso     do not edit, read in place
run-local-JP2|WSQ.(bat|sh) [build] [0|1]  1 = feedback+measures+timings · logs → .local/logs
```

## Rules
- `frfxll/`: keep LGPL + DigitalPersona header (incl. "MODIFIED version"); no MPL code in it, no LGPL code outside; never name the product "FingerJet"; JAR bundles both licenses in `META-INF/licenses/`. Only `FeatureExtractor.createFeatureSetFromRaw` is used.
- Bit-exact with NIST binary to 5 decimals (`NistReferenceTest`: 72 jp2, 75 wsq). Compare per feature; float/double, `cvRound` half-even, saturation, loop order matter.
- OpenCV only through `Cv`, NIST args exactly; release `Mat` in `finally`; same OpenCV version as `biometrics-util`.
- Do not simplify reproduced OpenCV quirks: `copyMakeBorder` on ROI pads with real neighbours (`CommonFunctions.rotatedWindow`); `compare(uint8,double)` ceils and never saturates (`LCS.scalarThreshold`).
- Exceptions: `Nfiq2Exception` (kernel `BaseUncheckedException`, `ErrorCode` MOS-NFIQ2-xxx) and `FrfxllException` in `frfxll`. No raw runtime exceptions. Needs `jackson-module-afterburner` (declared).
- NIST names for classes/features/errors. 500 ppi only, no resampling. No NFIQ 1.0 code reuse.
- JaCoCo ≥ 90 %, no excludes; Sonar lines+branches ≥ 90 %. JavaDoc everywhere (tabs).
- Verify: `mvn -pl nfiq2.0 verify "-Dgpg.skip=true"`, then launchers → NFIQ2=72 / 75.
- NIST reference: 2.3.0 MSI (`msiexec /a`), `nfiq2 -F -v -a <png…>` on PNGs from `IsoImageDecoder` (not raw JP2). Conformance images licensed (nigos.nist.gov).
