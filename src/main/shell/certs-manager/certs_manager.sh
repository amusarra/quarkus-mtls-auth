#!/usr/bin/env bash

# Title: Certificates Manager
#
# This script generates a tree of files for a CA, server or client certificate using OpenSSL.
# The script requires at least 2 parameters:
# 1. Operation: generate-ca, generate-server or generate-client
# 2. Working directory
# The script also requires additional parameters depending on the operation.
#
# Usage: ./certs_manager.sh generate-[ca|server|client] --working-dir <working-directory> [parameters...]
#
# Example for generate-ca:
# ./certs_manager.sh generate-ca --working-dir /tmp/ca \
#   --private-key-file private.key --ca-certificate-file ca.crt \
#   --validity-days 365 --country US --state California \
#   --locality SanFrancisco --organization MyCompany \
#   --organizational-unit MyUnit \
#   --common-name "My Company CA".com \
#   --output-p12-file output.p12
#
# Example for generate-server:
# ./certs_manager.sh generate-server --working-dir /tmp/server \
#   --private-key-file private.key --csr-file server.csr \
#   --server-cert-file server.crt --validity-days 365 \
#   --ca-cert-file ca.crt --ca-key-file ca.key \
#   --ca-key-password password --country US --state California \
#   --locality SanFrancisco --organization MyCompany \
#   --organizational-unit MyUnit \
#   --common-name "server.mycompany.com" \
#   --san-domains "DNS:server.mycompany.com,DNS:server"
#
# Example for generate-client:
# ./certs_manager.sh generate-client --working-dir /tmp/client \
#   --private-key-file private.key --csr-file client.csr \
#   --client-cert-file client.crt --validity-days 365 \
#   --ca-cert-file ca.crt --ca-key-file ca.key \
#   --ca-key-password password --country US --state California \
#   --locality SanFrancisco --organization MyCompany \
#   --organizational-unit MyUnit \
#   --common-name "client.mycompany.com" \
#   --extensions-file extensions.cnf \
#   --ext-cert-role "client" --ext-cert-device-id "1234" \
#   --output-p12-file output.p12
#
# Author: Antonio Musarra <antonio.musarra[at]gmail.com>

# Configuration to enable debugging
if [[ "${TRACE-0}" == "1" ]]; then set -o xtrace; fi

# Get the directory of the script
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)

# Source common functions
source "$SCRIPT_DIR/_common.sh"

# Check if OpenSSL is installed and the version is correct
check_openssl_installed
check_openssl_version

# Function to print usage
print_usage() {
  echo -e "${GREEN}Usage: \$0 generate-[ca|server|client] --working-dir <working-directory> [parameters...]${NC}"
  echo -e "${BLUE}Parameters for generate-ca:${NC}"
  echo -e "  ${YELLOW}--private-key-file <file>${NC}"
  echo -e "  ${YELLOW}--ca-certificate-file <file>${NC}"
  echo -e "  ${YELLOW}--validity-days <days>${NC}"
  echo -e "  ${YELLOW}--country <country>${NC}"
  echo -e "  ${YELLOW}--state <state>${NC}"
  echo -e "  ${YELLOW}--locality <locality>${NC}"
  echo -e "  ${YELLOW}--organization <organization>${NC}"
  echo -e "  ${YELLOW}--organizational-unit <unit>${NC}"
  echo -e "  ${YELLOW}--common-name <name>${NC}"
  echo -e "  ${YELLOW}[--output-p12-file <file>]${NC}"
  echo -e "${BLUE}Parameters for generate-server:${NC}"
  echo -e "  ${YELLOW}--private-key-file <file>${NC}"
  echo -e "  ${YELLOW}--csr-file <file>${NC}"
  echo -e "  ${YELLOW}--server-cert-file <file>${NC}"
  echo -e "  ${YELLOW}--validity-days <days>${NC}"
  echo -e "  ${YELLOW}--ca-cert-file <file>${NC}"
  echo -e "  ${YELLOW}--ca-key-file <file>${NC}"
  echo -e "  ${YELLOW}--ca-key-password <password>${NC}"
  echo -e "  ${YELLOW}--country <country>${NC}"
  echo -e "  ${YELLOW}--state <state>${NC}"
  echo -e "  ${YELLOW}--locality <locality>${NC}"
  echo -e "  ${YELLOW}--organization <organization>${NC}"
  echo -e "  ${YELLOW}--organizational-unit <unit>${NC}"
  echo -e "  ${YELLOW}--common-name <name>${NC}"
  echo -e "  ${YELLOW}[--san-domains <domains>]${NC}"
  echo -e "  ${YELLOW}[--output-p12-file <file>]${NC}"
  echo -e "${BLUE}Parameters for generate-client:${NC}"
  echo -e "  ${YELLOW}--private-key-file <file>${NC}"
  echo -e "  ${YELLOW}--csr-file <file>${NC}"
  echo -e "  ${YELLOW}--client-cert-file <file>${NC}"
  echo -e "  ${YELLOW}--validity-days <days>${NC}"
  echo -e "  ${YELLOW}--ca-cert-file <file>${NC}"
  echo -e "  ${YELLOW}--ca-key-file <file>${NC}"
  echo -e "  ${YELLOW}--ca-key-password <password>${NC}"
  echo -e "  ${YELLOW}--country <country>${NC}"
  echo -e "  ${YELLOW}--state <state>${NC}"
  echo -e "  ${YELLOW}--locality <locality>${NC}"
  echo -e "  ${YELLOW}--organization <organization>${NC}"
  echo -e "  ${YELLOW}--organizational-unit <unit>${NC}"
  echo -e "  ${YELLOW}--common-name <name>${NC}"
  echo -e "  ${YELLOW}[--extensions-file <file>]${NC}"
  echo -e "  ${YELLOW}[--ext-cert-role <role>]${NC}"
  echo -e "  ${YELLOW}[--ext-cert-device-id <id>]${NC}"
  echo -e "  ${YELLOW}[--output-p12-file <file>]${NC}"
}

# Parse named parameters
OPERATION=$1
shift

declare -A PARAMS
while [[ "$#" -gt 0 ]]; do
  case $1 in
  --working-dir)
    PARAMS["WORKING_DIR"]=$2
    shift
    ;;
  --private-key-file)
    PARAMS["PRIVATE_KEY_FILE"]=$2
    shift
    ;;
  --ca-certificate-file)
    PARAMS["CA_CERTIFICATE_FILE"]=$2
    shift
    ;;
  --validity-days)
    PARAMS["VALIDITY_DAYS"]=$2
    shift
    ;;
  --country)
    PARAMS["COUNTRY"]=$2
    shift
    ;;
  --state)
    PARAMS["STATE"]=$2
    shift
    ;;
  --locality)
    PARAMS["LOCALITY"]=$2
    shift
    ;;
  --organization)
    PARAMS["ORGANIZATION"]=$2
    shift
    ;;
  --organizational-unit)
    PARAMS["ORGANIZATIONAL_UNIT"]=$2
    shift
    ;;
  --common-name)
    PARAMS["COMMON_NAME"]=$2
    shift
    ;;
  --output-p12-file)
    PARAMS["OUTPUT_P12_FILE"]=$2
    shift
    ;;
  --csr-file)
    PARAMS["CSR_FILE"]=$2
    shift
    ;;
  --server-cert-file)
    PARAMS["SERVER_CERT_FILE"]=$2
    shift
    ;;
  --ca-cert-file)
    PARAMS["CA_CERT_FILE"]=$2
    shift
    ;;
  --ca-key-file)
    PARAMS["CA_KEY_FILE"]=$2
    shift
    ;;
  --ca-key-password)
    PARAMS["CA_KEY_PASSWORD"]=$2
    shift
    ;;
  --san-domains)
    PARAMS["SAN_DOMAINS"]=$2
    shift
    ;;
  --client-cert-file)
    PARAMS["CLIENT_CERT_FILE"]=$2
    shift
    ;;
  --extensions-file)
    PARAMS["EXTENSIONS_FILE"]=$2
    shift
    ;;
  --ext-cert-role)
    PARAMS["EXT_CERT_ROLE"]=$2
    shift
    ;;
  --ext-cert-device-id)
    PARAMS["EXT_CERT_DEVICE_ID"]=$2
    shift
    ;;
  *)
    echo -e "${RED}Unknown parameter passed: $1${NC}"
    print_usage
    exit 1
    ;;
  esac
  shift
done

# Check if working directory is provided
if [ -z "${PARAMS["WORKING_DIR"]}" ]; then
  echo -e "${RED}--working-dir is required${NC}"
  print_usage
  exit 1
fi

# Create the working directory if it doesn't exist
mkdir -p "${PARAMS["WORKING_DIR"]}"

case $OPERATION in
generate-ca)
  if [ -z "${PARAMS["PRIVATE_KEY_FILE"]}" ] || [ -z "${PARAMS["CA_CERTIFICATE_FILE"]}" ] || [ -z "${PARAMS["VALIDITY_DAYS"]}" ] || [ -z "${PARAMS["COUNTRY"]}" ] || [ -z "${PARAMS["STATE"]}" ] || [ -z "${PARAMS["LOCALITY"]}" ] || [ -z "${PARAMS["ORGANIZATION"]}" ] || [ -z "${PARAMS["ORGANIZATIONAL_UNIT"]}" ] || [ -z "${PARAMS["COMMON_NAME"]}" ]; then
    echo -e "${RED}Missing required parameters for generate-ca${NC}"
    print_usage
    exit 1
  fi
  PRIVATE_KEY_FILE="${PARAMS["WORKING_DIR"]}/${PARAMS["PRIVATE_KEY_FILE"]}"
  CA_CERTIFICATE_FILE="${PARAMS["WORKING_DIR"]}/${PARAMS["CA_CERTIFICATE_FILE"]}"
  OUTPUT_P12_FILE="${PARAMS["OUTPUT_P12_FILE"]:+${PARAMS["WORKING_DIR"]}/${PARAMS["OUTPUT_P12_FILE"]}}"

  if [ -f "$CA_CERTIFICATE_FILE" ]; then
    echo -e "${YELLOW}Certificate file $CA_CERTIFICATE_FILE already exists. Skipping creation.${NC}"
    exit 0
  fi

  generate_private_key "$PRIVATE_KEY_FILE"
  PRIVATE_KEY_PASSWORD=$(get_private_key_password "$PRIVATE_KEY_FILE")
  generate_ca_certificate "$PRIVATE_KEY_FILE" "$CA_CERTIFICATE_FILE" "${PARAMS["VALIDITY_DAYS"]}" "${PARAMS["COUNTRY"]}" "${PARAMS["STATE"]}" "${PARAMS["LOCALITY"]}" "${PARAMS["ORGANIZATION"]}" "${PARAMS["ORGANIZATIONAL_UNIT"]}" "${PARAMS["COMMON_NAME"]}" "$PRIVATE_KEY_PASSWORD"

  verify_certificate_issuer "${CA_CERTIFICATE_FILE}"
  verify_certificate_serial "${CA_CERTIFICATE_FILE}"
  verify_certificate_subject "${CA_CERTIFICATE_FILE}"

  if [ -n "$OUTPUT_P12_FILE" ]; then
    generate_p12_file "$PRIVATE_KEY_FILE" "$CA_CERTIFICATE_FILE" "$OUTPUT_P12_FILE" "$PRIVATE_KEY_PASSWORD"
  fi
  ;;
generate-server)
  if [ -z "${PARAMS["PRIVATE_KEY_FILE"]}" ] || [ -z "${PARAMS["CSR_FILE"]}" ] || [ -z "${PARAMS["SERVER_CERT_FILE"]}" ] || [ -z "${PARAMS["VALIDITY_DAYS"]}" ] || [ -z "${PARAMS["CA_CERT_FILE"]}" ] || [ -z "${PARAMS["CA_KEY_FILE"]}" ] || [ -z "${PARAMS["COUNTRY"]}" ] || [ -z "${PARAMS["STATE"]}" ] || [ -z "${PARAMS["LOCALITY"]}" ] || [ -z "${PARAMS["ORGANIZATION"]}" ] || [ -z "${PARAMS["ORGANIZATIONAL_UNIT"]}" ] || [ -z "${PARAMS["COMMON_NAME"]}" ]; then
    echo -e "${RED}Missing required parameters for generate-server${NC}"
    print_usage
    exit 1
  fi
  PRIVATE_KEY_FILE="${PARAMS["WORKING_DIR"]}/${PARAMS["PRIVATE_KEY_FILE"]}"
  CSR_FILE="${PARAMS["WORKING_DIR"]}/${PARAMS["CSR_FILE"]}"
  CA_CERT_FILE="${PARAMS["WORKING_DIR"]}/${PARAMS["CA_CERT_FILE"]}"
  CA_KEY_FILE="${PARAMS["WORKING_DIR"]}/${PARAMS["CA_KEY_FILE"]}"
  SERVER_CERT_FILE="${PARAMS["WORKING_DIR"]}/${PARAMS["SERVER_CERT_FILE"]}"
  OUTPUT_P12_FILE="${PARAMS["OUTPUT_P12_FILE"]:+${PARAMS["WORKING_DIR"]}/${PARAMS["OUTPUT_P12_FILE"]}}"

  if [ -f "$SERVER_CERT_FILE" ]; then
    echo -e "${YELLOW}Certificate file $SERVER_CERT_FILE already exists. Skipping creation.${NC}"
    exit 0
  fi

  generate_private_key "$PRIVATE_KEY_FILE"
  PRIVATE_KEY_PASSWORD=$(get_private_key_password "$PRIVATE_KEY_FILE")
  if [ -z "${PARAMS["CA_KEY_PASSWORD"]}" ]; then
    CA_KEY_PASSWORD=$(get_private_key_password "$CA_KEY_FILE")
  else
    CA_KEY_PASSWORD="${PARAMS["CA_KEY_PASSWORD"]}"
  fi
  if [ -n "${PARAMS["SAN_DOMAINS"]}" ]; then
    CONFIG_FILE="${PARAMS["WORKING_DIR"]}/openssl.cnf"
    generate_openssl_server_certificate_config "$CONFIG_FILE" "${PARAMS["SAN_DOMAINS"]}"
  fi
  generate_csr "$PRIVATE_KEY_FILE" "$CSR_FILE" "${PARAMS["COUNTRY"]}" "${PARAMS["STATE"]}" "${PARAMS["LOCALITY"]}" "${PARAMS["ORGANIZATION"]}" "${PARAMS["ORGANIZATIONAL_UNIT"]}" "${PARAMS["COMMON_NAME"]}" "$PRIVATE_KEY_PASSWORD" "$CONFIG_FILE"
  generate_server_certificate "$CSR_FILE" "${CA_CERT_FILE}" "${CA_KEY_FILE}" "$SERVER_CERT_FILE" "${PARAMS["VALIDITY_DAYS"]}" "$CA_KEY_PASSWORD" "$CONFIG_FILE"

  verify_certificate_issuer "${SERVER_CERT_FILE}"
  verify_certificate_serial "${SERVER_CERT_FILE}"
  verify_certificate_subject "${SERVER_CERT_FILE}"

  if [ -n "$OUTPUT_P12_FILE" ]; then
    generate_p12_file "$PRIVATE_KEY_FILE" "$SERVER_CERT_FILE" "$OUTPUT_P12_FILE" "$PRIVATE_KEY_PASSWORD"
  fi
  ;;
generate-client)
  if [ -z "${PARAMS["PRIVATE_KEY_FILE"]}" ] || [ -z "${PARAMS["CSR_FILE"]}" ] || [ -z "${PARAMS["CLIENT_CERT_FILE"]}" ] || [ -z "${PARAMS["VALIDITY_DAYS"]}" ] || [ -z "${PARAMS["CA_CERT_FILE"]}" ] || [ -z "${PARAMS["CA_KEY_FILE"]}" ] || [ -z "${PARAMS["COUNTRY"]}" ] || [ -z "${PARAMS["STATE"]}" ] || [ -z "${PARAMS["LOCALITY"]}" ] || [ -z "${PARAMS["ORGANIZATION"]}" ] || [ -z "${PARAMS["ORGANIZATIONAL_UNIT"]}" ] || [ -z "${PARAMS["COMMON_NAME"]}" ]; then
    echo -e "${RED}Missing required parameters for generate-client${NC}"
    print_usage
    exit 1
  fi
  PRIVATE_KEY_FILE="${PARAMS["WORKING_DIR"]}/${PARAMS["PRIVATE_KEY_FILE"]}"
  CSR_FILE="${PARAMS["WORKING_DIR"]}/${PARAMS["CSR_FILE"]}"
  CA_CERT_FILE="${PARAMS["WORKING_DIR"]}/${PARAMS["CA_CERT_FILE"]}"
  CA_KEY_FILE="${PARAMS["WORKING_DIR"]}/${PARAMS["CA_KEY_FILE"]}"
  CLIENT_CERT_FILE="${PARAMS["WORKING_DIR"]}/${PARAMS["CLIENT_CERT_FILE"]}"
  OUTPUT_P12_FILE="${PARAMS["OUTPUT_P12_FILE"]:+${PARAMS["WORKING_DIR"]}/${PARAMS["OUTPUT_P12_FILE"]}}"

  if [ -f "$CLIENT_CERT_FILE" ]; then
    echo -e "${YELLOW}Certificate file $CLIENT_CERT_FILE already exists. Skipping creation.${NC}"
    exit 0
  fi

  generate_private_key "$PRIVATE_KEY_FILE"
  PRIVATE_KEY_PASSWORD=$(get_private_key_password "$PRIVATE_KEY_FILE")
  if [ -z "${PARAMS["CA_KEY_PASSWORD"]}" ]; then
    CA_KEY_PASSWORD=$(get_private_key_password "$CA_KEY_FILE")
  else
    CA_KEY_PASSWORD="${PARAMS["CA_KEY_PASSWORD"]}"
  fi
  generate_csr "$PRIVATE_KEY_FILE" "$CSR_FILE" "${PARAMS["COUNTRY"]}" "${PARAMS["STATE"]}" "${PARAMS["LOCALITY"]}" "${PARAMS["ORGANIZATION"]}" "${PARAMS["ORGANIZATIONAL_UNIT"]}" "${PARAMS["COMMON_NAME"]}" "$PRIVATE_KEY_PASSWORD"
  if [ -n "${PARAMS["EXTENSIONS_FILE"]}" ]; then
    TEMP_EXTENSIONS_FILE=$(mktemp)
    cp "${PARAMS["EXTENSIONS_FILE"]}" "$TEMP_EXTENSIONS_FILE"
    sed -i '' "s/\${ext_cert_role}/${PARAMS["EXT_CERT_ROLE"]}/g" "$TEMP_EXTENSIONS_FILE"
    sed -i '' "s/\${ext_cert_device_id}/${PARAMS["EXT_CERT_DEVICE_ID"]}/g" "$TEMP_EXTENSIONS_FILE"
    generate_client_certificate "$CSR_FILE" "${CA_CERT_FILE}" "${CA_KEY_FILE}" "$CLIENT_CERT_FILE" "${PARAMS["VALIDITY_DAYS"]}" "$CA_KEY_PASSWORD" "$TEMP_EXTENSIONS_FILE"
    rm "$TEMP_EXTENSIONS_FILE"
  else
    generate_client_certificate "$CSR_FILE" "${CA_CERT_FILE}" "${CA_KEY_FILE}" "$CLIENT_CERT_FILE" "${PARAMS["VALIDITY_DAYS"]}" "$CA_KEY_PASSWORD"
  fi

  verify_certificate_issuer "${CLIENT_CERT_FILE}"
  verify_certificate_serial "${CLIENT_CERT_FILE}"
  verify_certificate_subject "${CLIENT_CERT_FILE}"

  if [ -n "$OUTPUT_P12_FILE" ]; then
    generate_p12_file "$PRIVATE_KEY_FILE" "$CLIENT_CERT_FILE" "$OUTPUT_P12_FILE" "$PRIVATE_KEY_PASSWORD"
  fi
  ;;
*)
  echo -e "${RED}Invalid operation: $OPERATION${NC}"
  print_usage
  exit 1
  ;;
esac

echo -e "${GREEN}All files generated in: ${NC}${PARAMS["WORKING_DIR"]}"
