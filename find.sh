#!/bin/bash

# 指定要搜索的目录
SEARCH_DIR="/Users/jensen/Projects/matrixorigin/mo-load/cases"

find "$SEARCH_DIR" -type f -name "run.yml" -print0 | xargs -0 grep -L -F '{' | while read -r file; do
  echo "File without { or }: $file"
done
