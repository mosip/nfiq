# nfiq1.0

Library `io.mosip:nfiq1.0`: Java port of NIST NFIQ 1.0. No Spring app/HTTP/DB. Used by BioSDK, IDA, RegProc, MDS.

Flow: ISO → `ImageDecoder` → `mindtct` (maps → binarize → minutiae → remove → quality map) → 11 features → z-norm → `RunMlp` → NFIQ 1 best..5 worst + conf.

```
src/main/java/org/mosip/nist/nfiq1/
├─ Nfiq1Helper    computeNfiq = public API (synchronized)
├─ imagetools/    ImageDecoder (relative or absolute path)
├─ mindtct/       NIST MINDTCT (Maps, Binarization, MinutiaHelper, RemoveMinutia, Quality …)
├─ mlp/           RunMlp, MlpCla, Acs
├─ common/        constants (coverage-excluded)
└─ util/          ImageUtil, StringUtil, SsxStats
src/main/resources/znorm.dat              do not edit
src/test/…/*Test.java · test/NfiqApplication (harness)
src/test/resources/info_{jp2,wsq}.iso     do not edit, read in place
run-local-JP2|WSQ.(bat|sh) [build] [0|1]  logs → .local/logs
```

## Rules
- Public API stable; keep NIST names; JavaDoc everywhere.
- Keep NIST parity (e.g. `&&` range checks, `getHighCurvatureContour` overwrite).
- Only the `COMPUTED NFIQ1.0 VALUES` block logs at INFO; tracing is DEBUG.
- Tests restore singletons (`Globals`, `Contour`, `Maps`, `Quality`, `MlpCla`, `Nist.showLogs`) in `@AfterEach`.
- JaCoCo ≥ 90 %; excludes `common/**, Defs, Nfiq1, Nfiq1Globals, Nist` = parent `sonar.coverage.exclusions`.
- JVM flags before `-cp`/`-jar`.
- Verify: `mvn -pl nfiq1.0 verify "-Dgpg.skip=true"`, then launchers → NFIQ=1, Conf 0.748 / 0.620.
