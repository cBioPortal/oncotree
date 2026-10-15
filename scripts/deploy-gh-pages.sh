#!/usr/bin/env bash
#
# Build the frontend as a static demo and publish it to the gh-pages branch of a
# fork, served at https://<user>.github.io/<repo>/.
#
# The demo is fully self-contained: a snapshot of the OncoTree API is fetched at
# build time and served alongside the app (same origin), so the running page
# never makes a cross-origin request. This avoids CORS / Private Network Access
# blocks when the page is embedded elsewhere. Configure via env vars:
#
#   DEPLOY_REMOTE   git remote/URL to push to            (default: inodb)
#   DEPLOY_BASE     base path the site is served at       (default: /oncotree/)
#   ONCOTREE_API    OncoTree API to snapshot at build time(default: https://oncotree.mskcc.org)
#   TREE_VERSION    tree version to snapshot              (default: oncotree_latest_stable)
#
# Usage: scripts/deploy-gh-pages.sh
set -euo pipefail

DEPLOY_REMOTE="${DEPLOY_REMOTE:-inodb}"
DEPLOY_BASE="${DEPLOY_BASE:-/oncotree/}"
ONCOTREE_API="${ONCOTREE_API:-https://oncotree.mskcc.org}"
TREE_VERSION="${TREE_VERSION:-oncotree_latest_stable}"

# The app fetches `${API_BASE}/api/...`; point it at the deploy's own subpath so
# requests resolve to the snapshot files staged below (same origin as the app).
API_BASE="${DEPLOY_BASE%/}"

repo_root="$(git rev-parse --show-toplevel)"
js_dir="$repo_root/web/src/main/javascript"
static_dir="$repo_root/web/src/main/resources/static"
stage_dir="$(mktemp -d)"
trap 'rm -rf "$stage_dir"' EXIT

echo "Building static demo (base=$DEPLOY_BASE, apiBase=$API_BASE)..."
cd "$js_dir"
DEPLOY_BASE="$DEPLOY_BASE" VITE_ONCOTREE_BASE_URL="$API_BASE" pnpm run build

echo "Staging build output..."
cp -r "$static_dir/." "$stage_dir/"
cp "$stage_dir/index.html" "$stage_dir/404.html" # SPA fallback for client routing
touch "$stage_dir/.nojekyll"

# The demo build (subpath base) overwrites the repo's committed build artifacts;
# restore them so the working tree stays clean.
git -C "$repo_root" checkout -- \
  web/src/main/resources/static web/src/main/javascript/public 2>/dev/null || true

echo "Snapshotting API from $ONCOTREE_API ..."
mkdir -p "$stage_dir/api/tumorTypes"
# GitHub Pages ignores the query string and serves these files for the app's
# `/api/versions` and `/api/tumorTypes/tree?version=...` requests.
curl -fsSL "$ONCOTREE_API/api/versions" -o "$stage_dir/api/versions"
curl -fsSL "$ONCOTREE_API/api/tumorTypes/tree?version=$TREE_VERSION" \
  -o "$stage_dir/api/tumorTypes/tree"

remote_url="$(git -C "$repo_root" remote get-url "$DEPLOY_REMOTE")"
echo "Publishing to $remote_url (gh-pages)..."
cd "$stage_dir"
git init -q
git checkout -q -b gh-pages
git add -A
git commit -q -m "Deploy oncotree annotation overlay demo"
git push -f "$remote_url" gh-pages

echo "Done. Enable Pages once with:"
echo "  gh api -X POST repos/<owner>/<repo>/pages -f source.branch=gh-pages -f source.path=/"
