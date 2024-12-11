# NFIQ

This repository hosts the NFIQ (NIST Fingerprint Image Quality) implementations and services, providing tools to assess the quality of fingerprint images in compliance with NFIQ standards. It supports MOSIP's biometric ecosystem, offering reliable and standardized quality scoring methods.

## Table of Contents
- [Overview](#overview)
- [Directories](#directories)
- [Setting Up](#setting-up)
- [License](#license)

---

## Overview
The NFIQ repository includes implementations and tools for fingerprint quality assessment. These tools help evaluate fingerprint images based on quality scores, which are critical for biometric applications such as deduplication, authentication, and identification.

The repository is structured to accommodate multiple versions or implementations of NFIQ. The currently supported version is:
- **[NFIQ1.0](nfiq1.0/README.md)**

---

## Directories

| Directory  | Description                                                  |
|------------|--------------------------------------------------------------|
| `nfiq1.0`  | Contains the implementation for NFIQ 1.0. Refer to the [README](nfiq1.0/README.md) for detailed documentation. |

---

## Setting Up

### Steps to Clone the Repository

1. **Clone the repository**
   ```bash
   git clone https://github.com/mosip/nfiq.git
   cd nfiq
   ```

2. **Navigate to the specific implementation directory**
   For example, to set up NFIQ 1.0:
   ```bash
   cd nfiq1.0
   ```

3. **Follow the setup instructions**
   Each implementation contains its own `README.md` file with detailed instructions. Refer to the [NFIQ1.0 README](nfiq1.0/README.md) for further guidance.

---

## License

This project is licensed under the [MOSIP License](LICENSE).
