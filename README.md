# NFIQ 1.0

## Overview

This repository provides the implementation for NFIQ 1.0, a fingerprint quality assessment service based on the NFIQ (NIST Fingerprint Image Quality) standard. This implementation is tailored to work with MOSIP's biometric ecosystem and supports quality scoring for fingerprint images.

## Usage in MOSIP

The NFIQ 1.0 and NFIQ 2.0 used as library in the following MOSIP services and components for fingerprint quality assessment:

### Registration Devices
- **Biometric Capture Devices**: Registration devices use NFIQ 2.0 scores (scaled from 0-100) to determine fingerprint quality during capture and auto-capture when quality thresholds are met.
- **MOSIP Device Service (MDS)**: Implements NFIQ 2.0 scoring for real-time quality feedback during fingerprint capture in registration scenarios.

### Biometric SDK
- **Quality Check Module**: The Biometric SDK integrates NFIQ 2.0 for quality assessment of fingerprint images captured during registration.
- **Registration Client**: Uses NFIQ 2.0 scores to validate fingerprint quality before submission.
- **ID Authentication**: Employs NFIQ 1.0-based quality checks for fingerprint authentication requests.

### Registration Processor
- **Biometric Quality Check Stage**: Validates fingerprint quality using NFIQ 2.0 scores during packet processing.

For more information on how MOSIP uses NFIQ for fingerprint quality assessment, refer to:
- [Biometric Specification](https://docs.mosip.io/1.2.0/biometrics/biometric-specification)
- [MDS Specification](https://docs.mosip.io/1.2.0/biometrics/mds-specification)

## Prerequisites

Ensure you have the following installed before proceeding:

- **Java**: Version 21.0.3
- **Maven**: Version 3.9.6
- **Git**: To clone the repository

## Using NFIQ as a Dependency

To use NFIQ 1.0 in your MOSIP service, add it as a Maven dependency:

```xml
<dependency>
    <groupId>io.mosip</groupId>
    <artifactId>nfiq1.0</artifactId>
    <version>${nfiq.version}</version>
</dependency>
```

## Setting Up Locally (for Development or Contribution)

### Steps to Set Up:

1. **Clone the repository**
   ```text
   git clone https://github.com/mosip/nfiq.git
   cd nfiq/nfiq1.0
   ```

2. **Build the project**

   Use Maven to build the project and install it to your local Maven repository.
   ```text
   mvn clean install -Dgpg.skip=true
   ```

3. **Run the test application**

   The project includes a test application (`NfiqApplication.java`) located in the `test` folder. This utility demonstrates NFIQ quality computation on sample fingerprint images in ISO format (JP2 or WSQ).

   **Sample ISO files** for testing are also available in the `test` folder for reference.

   Navigate to the target directory and execute the batch files:
   ```text
   cd target
   ```

   For JP2 image format:
   ```text
   runJP2.bat
   ```

   For WSQ image format:
   ```text
   runWSQ.bat
   ```

   **What it does:**
    - Reads ISO-formatted fingerprint images (JP2 or WSQ compression)
    - Decodes the image and extracts metadata (width, height, depth, PPI)
    - Computes the NFIQ quality score (1-5 scale, where 1 is highest quality)
    - Outputs the NFIQ score and confidence value

   **Expected Output:**
   ```
   NFIQ=<quality_score> Conf=<confidence_value>
   ```

   **Sample Output with Detailed Metrics:**
   ```
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
   NFIQ=4 Conf=0.625591
   ```

   **Understanding the Output:**
    - **Quality Map Foreground Count**: Number of foreground pixels analyzed
    - **Number of minutiae**: Total fingerprint minutiae detected
    - **Reliability counts**: Minutiae distribution across reliability thresholds (0.5 to 0.9)
    - **Quality map (qmap) distribution**: Proportion of pixels in each quality level (1=excellent to 4=poor)
    - **NFIQ Score**: Final quality score (1=highest quality, 5=lowest quality)
    - **Confidence**: Confidence value for the computed quality score

   **Command-line usage:**
   ```text
   java -cp nfiq1.0-<version>.jar;lib\*;test-classes\ org.mosip.nist.nfiq1.test.NfiqApplication "imgfile=<path-to-iso-file>" "logs=0"
   ```

   Parameters:
    - `imgfile`: Path to the ISO file containing JP2 or WSQ fingerprint image
    - `logs`: Set to `0` for minimal output, `1` for detailed logs

## Documentation

- **NFIQ 2.0 Official Documentation**: Refer to [NIST NFIQ 2](https://www.nist.gov/services-resources/software/nfiq-2) and [NFIQ 2 GitHub Repository](https://github.com/usnistgov/NFIQ2) for the latest NFIQ 2.0 implementation.
- **NFIQ 2 API Documentation**: Complete API reference is available at [NFIQ 2 API Docs](https://pages.nist.gov/NFIQ2/).
- **MOSIP Biometric Specification**: For implementation details and quality standards, refer to the [MOSIP Biometric Specification](https://docs.mosip.io/1.2.0/biometrics/biometric-specification).

## License

This project is licensed under the [MOSIP License](LICENSE).