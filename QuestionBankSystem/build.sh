#!/bin/bash
# Compilation script for QuestionBankSystem
mkdir -p out
javac -d out $(find src -name "*.java")
echo "Compiled successfully into 'out/' directory."
