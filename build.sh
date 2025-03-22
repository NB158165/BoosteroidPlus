#!/bin/bash

# deployment.sh

# Configuration
DEVICE_IP="192.168.0.101"  # Replace with your device's IP
SSH_USER="root"        # Replace with your SSH username
REMOTE_PATH="/data/local/tmp"  # Path on the device to store the APK
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to print step information
print_step() {
    echo -e "${GREEN}=== $1 ===${NC}"
}

# Function to check if previous command was successful
check_status() {
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ $1 successful${NC}"
    else
        echo -e "${RED}✗ $1 failed${NC}"
        #exit 1
    fi
}

# Build the APK
print_step "Building APK"
./gradlew assembleDebug
check_status "Build"

# Transfer the APK to device
print_step "Transferring APK to device"
adb push $APK_PATH $REMOTE_PATH
check_status "Transfer"

# Install the APK
print_step "Installing APK"
adb shell pm install -r $REMOTE_PATH/app-debug.apk
check_status "Installation"

# Clean up (optional)
print_step "Cleaning up"
adb shell rm $REMOTE_PATH/app-debug.apk
check_status "Cleanup"

echo -e "${GREEN}Deployment completed successfully!${NC}"
