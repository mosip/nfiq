# NFIQ 1.0

## Overview
This repository provides the implementation for NFIQ 1.0, a fingerprint quality assessment service based on the NFIQ (NIST Fingerprint Image Quality) standard. This implementation is tailored to work with MOSIP's biometric ecosystem and supports quality scoring for fingerprint images.

## Table of Contents
- [Prerequisites](#prerequisites)
- [Setting Up Locally](#setting-up-locally)
- [License](#license)

## Prerequisites

Ensure you have the following installed before proceeding:

1. **Java**: Version 21.0.3
2. **Maven**: Version 3.9.6
3. **Git**: To clone the repository
4. **Postman (optional)**: For testing the APIs

---

## Setting Up Locally

### Steps to Set Up:

1. **Clone the repository**
   ```bash
   git clone https://github.com/mosip/nfiq.git
   cd nfiq/nfiq1.0
   ```

2. **Build the project**
   Use Maven to build the project and resolve dependencies.
   ```bash
   mvn clean install
   ```

3. **Start the application**
   Use the following command to run the application for JP2 image ISO:
   ```bash
   java -cp nfiq1.0--<version>.jar;lib\*;test-classes\ org.mosip.nist.nfiq1.test.NfiqApplication "imgfile=info_jp2.iso" "logs=0"
   ```

   Use the following command to run the application for WSQ image ISO:
   ```bash
   java -cp nfiq1.0--<version>.jar;lib\*;test-classes\ org.mosip.nist.nfiq1.test.NfiqApplication "imgfile=info_wsq.iso" "logs=0"
   ```

---

## License

This project is licensed under the [MOSIP License](LICENSE).  

---