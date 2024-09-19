#!/usr/bin/env bash

# Title: Device ID Generator
# This script generates a unique Device ID based on the current timestamp, UUID, and hostname.
# The Device ID is then encoded using Base64. The script also provides a function to verify
# the integrity of a Base64 encoded Device ID by comparing the HMAC with the calculated HMAC.
# The HMAC is generated using an HMAC SHA-256 hash based on a secret key.
#
# Usage:
#   ./generate_device_id.sh generate
#   ./generate_device_id.sh verify <compressed_device_id>
#
# Dependencies:
#   - openssl
#   - uuidgen
#
# Author: Antonio Musarra <antonio.musarra[at]gmail.com>

# Configuration to enable debugging
if [[ "${TRACE-0}" == "1" ]]; then set -o xtrace; fi

# Get the directory of the script
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)

# Source common functions
source "$SCRIPT_DIR/_common.sh"

# Define a secret key (this can be changed)
# In a real-world scenario, this should be stored securely
DEVICE_ID_SECRET_KEY="${DEVICE_ID_SECRET_KEY:-my_secret_key_for_generate_device_id}"

# Check if the required tools are installed
check_zsh_version
check_bash_version
check_openssl_installed
check_uuidgen_installed

# Function to generate a unique Device ID
# The Device ID is created using the current timestamp, UUID, and hostname.
# It is then combined with an HMAC SHA-256 hash and encoded in Base64.
generate_device_id() {
  # Check if the option to return only the device ID is set
  only_id="$1"

  # Generate a unique string based on timestamp, UUID, and hostname
  timestamp=$(date +%s%N)                      # Get the current timestamp with nanoseconds
  uuid=$(uuidgen | tr '[:upper:]' '[:lower:]') # Generate a UUID and convert it to lowercase
  hostname=$(hostname)                         # Use the device's hostname for additional uniqueness

  # Combine the information using # as the separator
  combined_string="${timestamp}#${uuid}#${hostname}"

  # Generate an HMAC SHA-256 based on the combined string and the secret key
  hmac=$(echo -n "$combined_string" | openssl dgst -sha256 -hmac "$DEVICE_ID_SECRET_KEY" | awk '{print $2}')

  # Combine the device ID with the HMAC
  device_id="${combined_string}#${hmac}"

  # Encode the result using Base64
  encoded_device_id=$(echo -n "$device_id" | base64)

  if [ "$only_id" == "true" ]; then
    # Output only the Base64 encoded device ID
    echo -n "$encoded_device_id"
  else
    # Output the Base64 encoded device ID with a success message
    echo -e "${GREEN}✅ Device ID successfully generated!${NC}"
    echo -e "${BLUE}🔑 Your Device ID:${NC} $encoded_device_id"
  fi
}

# Function to verify a Base64 encoded Device ID
# This function takes a Base64 encoded Device ID as input,
# decodes it, extracts the combined string and HMAC,
# regenerates the HMAC, and compares it with the provided HMAC
# to verify the integrity of the Device ID.
verify_device_id() {
  encoded_device_id_to_verify="$1"

  # Decode the Base64 encoded Device ID
  device_id_to_verify=$(echo "$encoded_device_id_to_verify" | base64 --decode)

  # Debugging: Print the decoded device ID
  echo -e "${YELLOW}🔍 Decoded Device ID: ${NC}$device_id_to_verify"

  # Extract the combined string and the provided HMAC from the Device ID
  combined_string=$(echo "$device_id_to_verify" | awk -F'#' 'BEGIN{OFS="#"} {print $1,$2,$3}')
  hmac_provided=$(echo "$device_id_to_verify" | awk -F'#' '{print $4}')

  # Debugging: print values to compare during verification
  echo -e "${YELLOW}🔍 Combined string: ${NC}$combined_string"
  echo -e "${YELLOW}🔍 Provided HMAC: ${NC}$hmac_provided"

  # Regenerate the HMAC from the combined string
  hmac_calculated=$(echo -n "$combined_string" | openssl dgst -sha256 -hmac "$DEVICE_ID_SECRET_KEY" | awk '{print $2}')

  # Debugging: print the calculated HMAC
  echo -e "${YELLOW}🔍 Calculated HMAC: ${NC}$hmac_calculated"

  # Ensure HMACs are compared in a consistent format
  if [ "${hmac_provided^^}" == "${hmac_calculated^^}" ]; then
    echo -e "${GREEN}✅ Device ID verified successfully!${NC} 🎉"
  else
    echo -e "${RED}❌ Verification failed: Device ID is not valid.${NC} 🚫"
  fi
}

# Usage logic
# Usage logic
if [ "$1" == "generate" ]; then
  if [ "$2" == "true" ]; then
    generate_device_id "true"
  else
    generate_device_id
  fi
elif [ "$1" == "verify" ]; then
  if [ -z "$2" ]; then
    echo -e "${YELLOW}⚠️  You must provide a Device ID to verify.${NC}"
    exit 1
  fi
  verify_device_id "$2"
else
  echo -e "${YELLOW}⚠️  Usage: $0 generate [true] | verify <encoded_device_id>${NC}"
fi
