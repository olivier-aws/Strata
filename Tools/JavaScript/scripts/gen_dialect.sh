#!/bin/sh
# Regenerate the JavaScript DDM dialect file.
#
# The dialect is defined programmatically in src/jsast.ts (genDialect function)
# and serialized to binary Ion. Run this script after modifying genDialect()
# to update the committed dialect file.
#
# Prerequisites: npm install (in Tools/JavaScript/)
set -e

script_dir="$(cd "$(dirname "$0")" && pwd)"
tools_js_dir="$(cd "$script_dir/.." && pwd)"
cd "$tools_js_dir"

strata=../../.lake/build/bin/strata

# Check prerequisites
if [ ! -d node_modules ]; then
  echo "Run 'npm install' first in Tools/JavaScript/"
  exit 1
fi

dialect_dir="dialects"
mkdir -p "$dialect_dir"

# Generate the dialect
npx tsx src/cli.ts dialect "$dialect_dir"

# Validate with strata if available
if [ -f "$strata" ]; then
  $strata check --include "$dialect_dir" "$dialect_dir/JavaScript.dialect.st.ion"
  echo "Dialect validated successfully."
else
  echo "Warning: strata not built, skipping validation."
  echo "Run 'lake build strata' in the repo root to enable validation."
fi
