#!/usr/bin/env bash

# Title: TSL IT Certificates Downloader
# This script downloads an XML file containing certificates from a specified URL,
# extracts the certificates, saves them as PEM files, and creates a PEM bundle.
# The script also provides options for specifying the URL, output directory,
# output file name, and XML output file path.
#
# Options:
#   --certs-url <certs_url>         URL of the XML file containing the certificates (default: https://eidas.agid.gov.it/TL/TSL-IT.xml)
#   --output-path <output_path>     Output directory for the PEM files (default: /tmp/tsl-it)
#   --output-file-name <output_file_name>  Name of the PEM bundle file (default: tsl-it_bundle.pem)
#   --xml-output-file <xml_output_file>   Path of the XML output file (default: /tmp/tsl-it.xml)
#   --generate-p12                  Generate a .p12 truststore from the PEM bundle
#   --generate-p12-with-entry-names Generate a .p12 truststore with entry names from CN
#   --fake                          Generate a fake CA certificate
#   --verbose                       Enable verbose mode
#   --help                          Show this help message
#
# Dependencies:
#   - xmlstarlet
#   - curl
#   - openssl
#
# Author: Antonio Musarra <antonio.musarra[at]gmail.com>

# Configuration to enable debugging
if [[ "${TRACE-0}" == "1" ]]; then set -o xtrace; fi

# Include common functions
source "$(dirname "$0")/_common.sh"

# URL of the XML file containing the certificates
CERTS_URL="https://eidas.agid.gov.it/TL/TSL-IT.xml"
OUTPUT_PATH_PEM_BUNDLE="/tmp/tsl-it"
OUTPUT_PEM_BUNDLE_FILE_NAME="tsl-it_bundle.pem"
XML_OUTPUT_FILE="/tmp/tsl-it.xml"
GENERATE_P12=0
GENERATE_P12_WITH_ENTRY_NAMES=0
GENERATE_FAKE=0
VERBOSE=0

# Function to clean the output directory
clean_output_path() {
  print_msg "${YELLOW}" "🧹 Cleaning output path: ${OUTPUT_PATH_PEM_BUNDLE}"
  rm -rf "${OUTPUT_PATH_PEM_BUNDLE}"/*
  print_msg "${GREEN}" "✅ Cleaned output path: ${OUTPUT_PATH_PEM_BUNDLE}"
}

# Function to save the XML content to a file
save_xml_to_file() {
  local xml_content=$1
  print_msg "${YELLOW}" "💾 Saving XML content to file: ${XML_OUTPUT_FILE}"
  echo "${xml_content}" >"${XML_OUTPUT_FILE}"
  print_msg "${GREEN}" "✅ Saved XML content to ${XML_OUTPUT_FILE}"
}

# Function to save the certificate as a PEM file and print its subject
save_certificate_as_pem() {
  local cert_base64=$1
  local output_pem_file_path=$2

  if [ $VERBOSE -eq 1 ]; then
    print_msg "${YELLOW}" "🔄 Processing certificate: ${cert_base64}"
  fi

  print_msg "${YELLOW}" "💾 Saving certificate as PEM"
  local pem_cert="-----BEGIN CERTIFICATE-----\n$(echo "${cert_base64}" | fold -w 64)\n-----END CERTIFICATE-----"
  local hash
  hash=$(echo -n "${cert_base64}" | sha256sum | awk '{print $1}')

  local cert_path
  if [ -n "${output_pem_file_path}" ]; then
    cert_path="${output_pem_file_path}/${hash}.pem"
  else
    cert_path="${OUTPUT_PATH_PEM_BUNDLE}/${hash}.pem"
  fi

  echo -e "${pem_cert}" >"${cert_path}"

  # Check if the certificate is valid
  if ! openssl x509 -in "${cert_path}" -noout -checkend 0; then
    subject=$(openssl x509 -in "${cert_path}" -noout -subject)
    print_msg "${RED}" "❌ Certificate ${subject} is expired or not yet valid. Removing ${cert_path}"
    rm "${cert_path}"
    return 1
  fi

  print_msg "${GREEN}" "✅ Saved certificate to ${cert_path}"

  # Extract and print the subject of the certificate
  local subject
  subject=$(openssl x509 -in "${cert_path}" -noout -subject)
  print_msg "${BLUE}" "🔍 Certificate subject: ${subject}"
  return 0
}

# Function to create a PEM bundle
create_pem_bundle() {
  print_msg "${YELLOW}" "📦 Creating PEM bundle"
  cat "${OUTPUT_PATH_PEM_BUNDLE}"/*.pem >"${OUTPUT_PATH_PEM_BUNDLE}/${OUTPUT_PEM_BUNDLE_FILE_NAME}"
  print_msg "${GREEN}" "✅ Created PEM bundle at ${OUTPUT_PATH_PEM_BUNDLE}/${OUTPUT_PEM_BUNDLE_FILE_NAME}"
}

# Function to generate a .p12 truststore from the PEM bundle
generate_p12() {
  local p12_password
  p12_password=$(generate_random_password)
  local p12_file="${OUTPUT_PATH_PEM_BUNDLE}/tsl-it_truststore.p12"

  # Check if the PEM bundle file exists and is not empty
  if [ ! -s "${OUTPUT_PATH_PEM_BUNDLE}/${OUTPUT_PEM_BUNDLE_FILE_NAME}" ]; then
    print_msg "${RED}" "❌ PEM bundle file is empty or does not exist."
    exit 1
  fi

  print_msg "${YELLOW}" "🔐 Generating .p12 truststore"
  openssl pkcs12 -export -nokeys -in "${OUTPUT_PATH_PEM_BUNDLE}/${OUTPUT_PEM_BUNDLE_FILE_NAME}" -out "${p12_file}" -password pass:"${p12_password}"
  if [ $? -ne 0 ]; then
    print_msg "${RED}" "❌ Error generating the .p12 truststore."
    exit 1
  fi
  print_msg "${GREEN}" "✅ Generated .p12 truststore at ${p12_file}"
  print_msg "${BLUE}" "🔑 Password for .p12 truststore: ${p12_password}"
}

generate_p12_with_entry_names() {
  local p12_password
  p12_password=$(generate_random_password)
  local p12_file="${OUTPUT_PATH_PEM_BUNDLE}/tsl-it_truststore.p12"
  declare -A cert_name_counter

  print_msg "${YELLOW}" "🔐 Generating .p12 truststore with entry names using keytool"

  # Create an empty keystore in PKCS12 format
  keytool -genkey -alias tempalias -keystore "${p12_file}" -storetype PKCS12 -storepass "${p12_password}" -keypass "${p12_password}" -dname "CN=temp" -keyalg RSA -keysize 2048 -validity 1
  keytool -delete -alias tempalias -keystore "${p12_file}" -storetype PKCS12 -storepass "${p12_password}"

  # Add each certificate to the keystore with a specific alias
  for cert_file in "${OUTPUT_PATH_PEM_BUNDLE}"/*.pem; do
    if [[ "${cert_file}" == *"${OUTPUT_PEM_BUNDLE_FILE_NAME}" ]]; then
      continue
    fi
    local cert_name
    cert_name=$(openssl x509 -in "${cert_file}" -noout -subject | sed -n 's/^.*CN=\([^,]*\).*$/\1/p')

    # Increment the counter if the cert_name already exists
    if [[ -n "${cert_name_counter[$cert_name]}" ]]; then
      cert_name_counter[$cert_name]=$((cert_name_counter[$cert_name] + 1))
      cert_name="${cert_name}_${cert_name_counter[$cert_name]}"
    else
      cert_name_counter[$cert_name]=1
    fi

    keytool -import -trustcacerts -file "${cert_file}" -alias "${cert_name}" -keystore "${p12_file}" -storetype PKCS12 -storepass "${p12_password}" -noprompt
  done

  if [ $? -ne 0 ]; then
    print_msg "${RED}" "❌ Error generating the .p12 truststore."
    exit 1
  fi
  print_msg "${GREEN}" "✅ Generated .p12 truststore at ${p12_file}"
  print_msg "${BLUE}" "🔑 Password for .p12 truststore: ${p12_password}"
}

parse_and_save_certs() {
  print_msg "${YELLOW}" "🔍 Parsing and saving certificates"
  clean_output_path

  # Extract certificates from the XML content using xidel
  local certs
  certs=$(xmlstarlet sel -T -t -m '//_:ServiceInformation[_:ServiceTypeIdentifier="http://uri.etsi.org/TrstSvc/Svctype/IdV" and _:ServiceStatus="http://uri.etsi.org/TrstSvc/TrustedList/Svcstatus/recognisedatnationallevel"]//_:X509Certificate' -v 'text()' -nl "${XML_OUTPUT_FILE}")

  # Check if the XPath found any nodes
  if [ -z "${certs}" ]; then
    print_msg "${RED}" "❌ XPath set is empty. Please check the XML structure and XPath expression."
    return
  fi

  if [ $VERBOSE -eq 1 ]; then
    print_msg "${YELLOW}" "✅ Extracted certificates: ${certs}"
  fi

  # Initialize certificate counters
  local cert_count=0
  local expired_cert_count=0

  # Save each certificate as a PEM file and print its subject
  while IFS= read -r cert_base64; do
    save_certificate_as_pem "${cert_base64}" ""
    if [ $? -eq 0 ]; then
      cert_count=$((cert_count + 1))
    else
      expired_cert_count=$((expired_cert_count + 1))
    fi
  done <<<"${certs}"

  # Print the number of processed and expired certificates
  print_msg "${GREEN}" "🏁 Processed ${cert_count} certificates"
  print_msg "${RED}" "🏁 Expired certificates: ${expired_cert_count}"
}

# Function to generate a fake CA certificate
generate_fake_ca() {
  print_msg "${YELLOW}" "🔐 Generating fake CA certificate"
  local fake_ca_cert_path="${OUTPUT_PATH_PEM_BUNDLE}/fake_ca.pem"
  generate_fake_ca_cert "${fake_ca_cert_path}"
  if [ $? -ne 0 ]; then
    print_msg "${RED}" "❌ Error generating the fake CA certificate."
    exit 1
  fi
  print_msg "${GREEN}" "✅ Generated fake CA certificate at ${fake_ca_cert_path}"

  # Add the fake CA certificate to the PEM bundle
  cat "${fake_ca_cert_path}" >> "${OUTPUT_PATH_PEM_BUNDLE}/${OUTPUT_PEM_BUNDLE_FILE_NAME}"
  print_msg "${GREEN}" "✅ Added fake CA certificate to PEM bundle"
}

# Function to show the script usage
usage() {
  echo "Usage: $0 [--certs-url <certs_url>] [--output-path <output_path>] [--output-file-name <output_file_name>] [--xml-output-file <xml_output_file>] [--generate-p12] [--generate-p12-with-entry-names] [--fake] [--verbose] [--help]"
  echo "  --certs-url <certs_url>         URL of the XML file containing the certificates (default: ${CERTS_URL})"
  echo "  --output-path <output_path>     Output directory for the PEM files (default: ${OUTPUT_PATH_PEM_BUNDLE})"
  echo "  --output-file-name <output_file_name>  Name of the PEM bundle file (default: ${OUTPUT_PEM_BUNDLE_FILE_NAME})"
  echo "  --xml-output-file <xml_output_file>   Path of the XML output file (default: ${XML_OUTPUT_FILE})"
  echo "  --generate-p12                  Generate a .p12 truststore from the PEM bundle"
  echo "  --generate-p12-with-entry-names Generate a .p12 truststore with entry names from CN"
  echo "  --fake                          Generate a fake CA certificate"
  echo "  --verbose                       Enable verbose mode"
  echo "  --help                          Show this help message"
  exit 1
}

# Parsing input parameters
while [[ "$#" -gt 0 ]]; do
  case $1 in
  --certs-url)
    CERTS_URL="$2"
    shift
    ;;
  --output-path)
    OUTPUT_PATH_PEM_BUNDLE="$2"
    shift
    ;;
  --output-file-name)
    OUTPUT_PEM_BUNDLE_FILE_NAME="$2"
    shift
    ;;
  --xml-output-file)
    XML_OUTPUT_FILE="$2"
    shift
    ;;
  --generate-p12) GENERATE_P12=1 ;;
  --generate-p12-with-entry-names) GENERATE_P12_WITH_ENTRY_NAMES=1 ;;
  --verbose) VERBOSE=1 ;;
  --fake) GENERATE_FAKE=1 ;;
  --help) usage ;;
  *) usage ;;
  esac
  shift
done

# Main function
main() {
  print_msg "${YELLOW}" "🚀 Starting the TSL-IT certificate update process"

  # Check if the PEM bundle file already exists
  if [ -f "${OUTPUT_PATH_PEM_BUNDLE}/${OUTPUT_PEM_BUNDLE_FILE_NAME}" ]; then
    print_msg "${GREEN}" "✅ PEM bundle file already exists at ${OUTPUT_PATH_PEM_BUNDLE}/${OUTPUT_PEM_BUNDLE_FILE_NAME}. Skipping generation."
    exit 0
  fi

  check_keytool_installed
  check_xmlstarlet_installed
  check_curl_installed
  mkdir -p "${OUTPUT_PATH_PEM_BUNDLE}"

  if [ $GENERATE_FAKE -eq 1 ]; then
    generate_fake_ca
    exit 0
  fi

  local xml_content
  xml_content=$(curl --progress-bar -s "${CERTS_URL}")
  print_msg "${GREEN}" "✅ Downloaded XML content"
  save_xml_to_file "${xml_content}"
  parse_and_save_certs
  create_pem_bundle
  if [ $GENERATE_P12_WITH_ENTRY_NAMES -eq 1 ]; then
    generate_p12_with_entry_names
  elif [ $GENERATE_P12 -eq 1 ]; then
    generate_p12
  fi
  print_msg "${GREEN}" "🏁 Certificate update process completed"
}

main
