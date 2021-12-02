# MOSIP Nfiq1.0

## Introduction

The Nfiq1.0 code here is used to get NIST Fingerprint Quality Score in the range of 1 to 5 from ISO/IEC 19794-4:2011 ISO template file having JP2000 or WSQ images. <br /> 
The ISO template is converted to raw image using https://github.com/mosip/bio-utils. <br />
The Java Code is written with the help from NFIQ Code written at<br />
https://github.com/lessandro/nbis [https://www.nist.gov/itl/iad/ig/nbis.cfm] 
 <br />and also validated against the output from it.

## OS
**Windows** on all machines.

## Samples
* Contains Sample ISO/IEC 19794-4:2011 ISO template file having JP2000 or WSQ images. 
```
* info_jp2.iso
* info_wsq.iso
```

## Useful tools
* If you use `command` tool, run following file as below:
```
* runJP2.bat [Note# To Get NFIQ1.0 Quality Score for info_jp2.iso]
```
```
* runWSQ.bat [Note# To Get NFIQ1.0 Quality Score for info_wsq.iso]
```