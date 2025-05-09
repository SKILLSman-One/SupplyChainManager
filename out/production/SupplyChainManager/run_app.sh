#!/bin/bash

# Clean all class files first
rm -f *.class

# Compile only the main file
javac SupplyChainSystemGUI.java

if [ $? -eq 0 ]; then
  # Run the application
  java SupplyChainSystemGUI
else
  echo "Compilation failed. Please check for errors."
fi
