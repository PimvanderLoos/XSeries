#!/bin/bash
set -euo pipefail

# Automates the update process described in UPDATE.md:
#   1) Bumps <spigotVersion> in pom.xml to the given version.
#   2) Runs XMaterialTest to find materials missing from the XMaterial enum.
#   3) Adds them (alphabetically, as reported by the test) at the top of the
#      enum in XMaterial.java under a "// <mc-version>" comment.
#   4) Re-runs the test to verify the new materials are working as intended.
#
# Usage: ./update_to_new_mc_version.sh "26.2-R0.1-SNAPSHOT"

if [[ $# -ne 1 ]]; then
    echo "Usage: $0 <spigot-version>  (e.g. $0 \"26.2-R0.1-SNAPSHOT\")" >&2
    exit 1
fi

NEW_VERSION="$1"
if [[ ! "$NEW_VERSION" =~ ^[0-9]+\.[0-9]+(\.[0-9]+)?-R[0-9]+\.[0-9]+-SNAPSHOT$ ]]; then
    echo "Error: '$NEW_VERSION' does not look like a spigot version (expected e.g. 26.2-R0.1-SNAPSHOT)." >&2
    exit 1
fi
MC_VERSION="${NEW_VERSION%%-*}"

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
POM="$REPO_ROOT/pom.xml"
XMATERIAL="$REPO_ROOT/src/main/java/com/cryptomorin/xseries/XMaterial.java"
REPORT="$REPO_ROOT/target/surefire-reports/com.cryptomorin.xseries.XMaterialTest.txt"

cd "$REPO_ROOT"

echo "==> Updating spigotVersion in pom.xml to $NEW_VERSION"
sed -i "s|<spigotVersion>[^<]*</spigotVersion>|<spigotVersion>$NEW_VERSION</spigotVersion>|" "$POM"
if ! grep -q "<spigotVersion>$NEW_VERSION</spigotVersion>" "$POM"; then
    echo "Error: failed to update <spigotVersion> in pom.xml." >&2
    exit 1
fi

# The 'tester' profile is required: without it the pom skips test compilation.
MVN_TEST=(mvn clean test -Dtest=XMaterialTest -Ptester)

echo "==> Running XMaterialTest to find missing materials"
if "${MVN_TEST[@]}"; then
    echo "==> Test already passes: no new materials in $MC_VERSION. Only pom.xml was changed."
    exit 0
fi

if [[ ! -f "$REPORT" ]] || ! grep -q "Unmatched Materials:" "$REPORT"; then
    echo "Error: test run failed, but not because of unmatched materials. See the Maven output above." >&2
    exit 1
fi

mapfile -t MATERIALS < <(awk '
    /Unmatched Materials:/ { found = 1; next }
    found {
        if ($0 ~ /^[A-Z0-9_]+,$/) { sub(/,$/, ""); print } else { exit }
    }
' "$REPORT")

if [[ ${#MATERIALS[@]} -eq 0 ]]; then
    echo "Error: test reported unmatched materials, but none could be parsed from $REPORT." >&2
    exit 1
fi

echo "==> Found ${#MATERIALS[@]} new material(s):"
printf '    %s\n' "${MATERIALS[@]}"

if grep -q "^    // $MC_VERSION\$" "$XMATERIAL"; then
    echo "Error: XMaterial.java already has a '// $MC_VERSION' section. Add the materials above to it manually." >&2
    exit 1
fi
if [[ "$(grep -c '^public enum XMaterial' "$XMATERIAL")" -ne 1 ]]; then
    echo "Error: expected exactly one 'public enum XMaterial' line in XMaterial.java." >&2
    exit 1
fi

echo "==> Adding them to XMaterial.java under '// $MC_VERSION'"
MATERIALS_FILE="$(mktemp)"
NEW_XMATERIAL="$(mktemp)"
trap 'rm -f "$MATERIALS_FILE" "$NEW_XMATERIAL"' EXIT
printf '%s\n' "${MATERIALS[@]}" > "$MATERIALS_FILE"

awk -v ver="$MC_VERSION" -v matfile="$MATERIALS_FILE" '
    { print }
    /^public enum XMaterial/ {
        print "    // " ver
        while ((getline material < matfile) > 0) print "    " material ","
        print ""
    }
' "$XMATERIAL" > "$NEW_XMATERIAL"
cat "$NEW_XMATERIAL" > "$XMATERIAL"

echo "==> Re-running XMaterialTest to verify the new materials"
"${MVN_TEST[@]}"

echo
echo "==> Done. pom.xml and XMaterial.java are updated for $NEW_VERSION."
echo "    Review the changes, then commit and push so the new version is available via JitPack, e.g.:"
echo "      git add pom.xml src/main/java/com/cryptomorin/xseries/XMaterial.java"
echo "      git commit -m \"Add $MC_VERSION materials\""
echo "      git push"
