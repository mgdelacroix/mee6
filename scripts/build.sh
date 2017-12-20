#!/usr/bin/env sh

echo "Cleaning build directories..."
rm -rf resources/public/js
rm -rf target/

echo "Build web application..."
lein with-profile +front cljsbuild once prod

echo "Build standalone jar executable..."
lein uberjar
