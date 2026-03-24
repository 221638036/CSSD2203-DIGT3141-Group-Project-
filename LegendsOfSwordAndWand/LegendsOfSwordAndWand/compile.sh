#!/bin/bash
# compile.sh - Compile and run Legends of Sword and Wand

echo "=== Compiling Legends of Sword and Wand ==="
mkdir -p out

find src -name "*.java" > sources.txt

javac -d out @sources.txt

if [ $? -eq 0 ]; then
    echo "=== Compilation successful! ==="
    echo "=== Launching game... ==="
    java -cp out gui.MainWindow
else
    echo "=== Compilation FAILED ==="
fi
