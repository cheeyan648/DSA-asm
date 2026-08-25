#!/bin/bash
# ==============================================================================
# Runs the whole test suite: unit, integration and system.
#
#   Unit         adt/ and each module's core logic in isolation
#   Integration  real DAOs, initializers and ADTs working together
#   System       the whole application driven through its console menus
#
# Usage:  bash test/run-all-tests.sh
# Exits non-zero if any layer fails.
# ==============================================================================

PROJECT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$PROJECT" || exit 1

# Compile for Java 17 rather than whatever JDK happens to be first on PATH.
# A newer JDK produces class files an older JVM refuses to load
# (UnsupportedClassVersionError), and NetBeans may well launch a different
# Java than the shell does. 17 matches the JDK installed alongside the
# project and comfortably exceeds the "Java 8 or above" the assignment needs.
RELEASE=17

echo "Compiling application (targeting Java $RELEASE)..."
rm -rf build/classes
mkdir -p build/classes
javac --release "$RELEASE" -d build/classes $(find src -name "*.java") 2>&1 | grep -v "^Note:"
if [ ! -f build/classes/control/TARUMTResortUI.class ]; then
  echo "APPLICATION FAILED TO COMPILE"
  exit 1
fi

echo "Compiling tests (targeting Java $RELEASE)..."
rm -rf build/test
mkdir -p build/test
javac --release "$RELEASE" -d build/test -cp build/classes test/*.java 2>&1 | grep -v "^Note:"
if [ ! -f build/test/UnitTests.class ]; then
  echo "TESTS FAILED TO COMPILE"
  exit 1
fi

FAILED=0

java -cp "build/classes;build/test" UnitTests || FAILED=1
java -cp "build/classes;build/test" IntegrationTests || FAILED=1
bash test/run-system-tests.sh || FAILED=1

echo ""
echo "=============================================================================="
if [ "$FAILED" -eq 0 ]; then
  echo "  ALL TEST LAYERS PASSED"
else
  echo "  *** ONE OR MORE TEST LAYERS FAILED ***"
fi
echo "=============================================================================="

exit "$FAILED"
