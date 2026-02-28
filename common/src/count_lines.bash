#!/bin/bash

# Set the directory to current if not provided
DIR="${1:-.}"

# Find all .java files recursively and count semicolons
count=$(find "$DIR" -type f -name "*.java" -exec cat {} + | grep -o ";" | wc -l)

echo "Total number of semicolons (approx. Java code lines): $count"