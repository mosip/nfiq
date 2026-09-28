#!/usr/bin/env bash
# Scores the bundled JPEG2000 sample (src/test/resources/info_jp2.iso)
# with the Nfiq2Application harness. Lives in nfiq2.0; can be called from any folder.
# Linux, macOS, Windows Git Bash/MSYS. Windows cmd: use run-local-JP2.bat
#
#   ./run-local-JP2.sh [0|1]        0 = one-line score (default), 1 = feedback + 69 measures + timings
#   ./run-local-JP2.sh build [0|1]  force a fresh "mvn clean package" first
# Optional env: JAVA_OPTS (extra JVM flags)
set -euo pipefail

IMG_NAME="info_jp2.iso"

MODULE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IMG_REL="src/test/resources/${IMG_NAME}"
LOG_DIR="${MODULE_DIR}/.local/logs"
MAIN_CLASS="org.mosip.nist.nfiq2.test.Nfiq2Application"

FORCE_BUILD=""
if [[ "${1:-}" == "build" ]]; then
  FORCE_BUILD=1
  shift
fi
LOGS="${1:-0}"

# Java classpath separator: ';' on Windows shells (Git Bash/MSYS/Cygwin), ':' elsewhere.
case "$(uname -s 2>/dev/null || echo unknown)" in
  MINGW*|MSYS*|CYGWIN*) CP_SEP=";" ;;
  *) CP_SEP=":" ;;
esac

need_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "error: '$1' is required on PATH" >&2
    exit 1
  }
}

find_jar() {
  local jar
  [[ -f "${MODULE_DIR}/target/test-classes/org/mosip/nist/nfiq2/test/Nfiq2Application.class" ]] || return 0
  [[ -d "${MODULE_DIR}/target/lib" ]] || return 0
  for jar in "${MODULE_DIR}"/target/nfiq2.0-*.jar; do
    [[ -f "$jar" ]] || continue
    case "$jar" in
      *sources*|*javadoc*) continue ;;
    esac
    echo "$jar"
    return 0
  done
}

build() {
  need_cmd mvn
  echo "==> building nfiq2.0 (skip tests)"
  (cd "$MODULE_DIR" && mvn -q clean package -DskipTests "-Dgpg.skip=true" "-Dmaven.javadoc.skip=true")
}

if [[ ! -f "${MODULE_DIR}/${IMG_REL}" ]]; then
  echo "error: sample not found: ${MODULE_DIR}/${IMG_REL}" >&2
  exit 1
fi
need_cmd java

jar=""
[[ -n "$FORCE_BUILD" ]] || jar="$(find_jar)"
if [[ -z "$jar" ]]; then
  build
  jar="$(find_jar)"
  if [[ -z "$jar" ]]; then
    echo "error: build produced no nfiq2.0 jar in target" >&2
    exit 1
  fi
fi

base="$MODULE_DIR"
if [[ "$CP_SEP" == ";" ]] && command -v cygpath >/dev/null 2>&1; then
  # Windows java cannot read MSYS paths (/d/...) inside a classpath list.
  base="$(cygpath -w "$MODULE_DIR")"
  jar="$(cygpath -w "$jar")"
fi
cp="${jar}${CP_SEP}${base}/target/lib/*${CP_SEP}${base}/target/test-classes"

mkdir -p "$LOG_DIR"
run_log="${LOG_DIR}/nfiq2-${IMG_NAME%.iso}.log"

echo "==> NFIQ 2.0  imgfile=${IMG_REL}  logs=${LOGS}"
# imgfile is resolved relative to the working directory, so run from nfiq2.0.
# JVM options (-D..., --enable-preview) must precede -cp / main class.
set +e
(
  cd "$MODULE_DIR"
  # shellcheck disable=SC2086
  java ${JAVA_OPTS:-} --enable-preview -Dfile.encoding=UTF-8 -cp "$cp" "$MAIN_CLASS" \
    "imgfile=${IMG_REL}" "logs=${LOGS}"
) >"$run_log" 2>&1
rc=$?
set -e
cat "$run_log"
echo "    log ${run_log}"
if [[ "$rc" -ne 0 ]]; then
  echo "error: Nfiq2Application exited with ${rc}" >&2
fi
exit "$rc"
