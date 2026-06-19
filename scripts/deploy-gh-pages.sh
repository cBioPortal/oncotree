#!/usr/bin/env bash
#
# Build the frontend as a static demo and publish it to the gh-pages branch of a
# fork, served at https://<user>.github.io/<repo>/.
#
# The demo is fully static, so it fetches tree data from a public OncoTree API
# instead of a co-hosted Go backend. Configure via env vars:
#
#   DEPLOY_REMOTE   git remote/URL to push to        (default: inodb)
#   DEPLOY_BASE     base path the site is served at   (default: /oncotree/)
#   ONCOTREE_API    OncoTree API origin to fetch from (default: https://oncotree.mskcc.org)
#
# Usage: scripts/deploy-gh-pages.sh
set -euo pipefail

DEPLOY_REMOTE="${DEPLOY_REMOTE:-inodb}"
DEPLOY_BASE="${DEPLOY_BASE:-/oncotree/}"
ONCOTREE_API="${ONCOTREE_API:-https://oncotree.mskcc.org}"

repo_root="$(git rev-parse --show-toplevel)"
js_dir="$repo_root/web/src/main/javascript"
static_dir="$repo_root/web/src/main/resources/static"
stage_dir="$(mktemp -d)"
trap 'rm -rf "$stage_dir"' EXIT

echo "Building static demo (base=$DEPLOY_BASE, api=$ONCOTREE_API)..."
cd "$js_dir"
DEPLOY_BASE="$DEPLOY_BASE" VITE_ONCOTREE_BASE_URL="$ONCOTREE_API" pnpm run build

echo "Staging build output..."
cp -r "$static_dir/." "$stage_dir/"
cp "$stage_dir/index.html" "$stage_dir/404.html" # SPA fallback for client routing
touch "$stage_dir/.nojekyll"

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
