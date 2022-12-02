#!/bin/bash -e

prior_version=$1

echo "### Enhancements"
echo
echo
echo "### Bug fixes"
echo

git log --reverse \
        --author='^(?!dependabot\[bot\] )' \
        --perl-regexp \
        --pretty=format:"* %s" \
        $prior_version..HEAD \
  | sed -E 's,\(#([0-9]+)\)$,\n  ([#\1](https://github.com/microsoft/ApplicationInsights-Java/pull/\1)),'
