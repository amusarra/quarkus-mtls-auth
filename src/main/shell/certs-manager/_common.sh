# Title: Common functions for the certs-manager scripts
# This file contains common functions for the certs-manager scripts.
# Load common functions for the certs-manager scripts
# source _common.sh
#
# Author: Antonio Musarra <antonio.musarra[at]gmail.com>

# _common.sh

# Define colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to check if OpenSSL is installed
check_openssl_installed() {
  if ! command -v openssl &>/dev/null; then
    echo -e "${RED}❌ OpenSSL is not installed. Please install it to continue.${NC}"
    exit 1
  fi
}

# Function to check if uuidgen is installed
check_uuidgen_installed() {
  # Check if uuidgen is installed
  if ! command -v uuidgen &>/dev/null; then
    echo -e "${RED}❌ Error: uuidgen is not installed. Please install it to proceed.${NC}"
    exit 1
  fi
}

# Function to check OpenSSL version
# @param REQUIRED_VERSION: The required OpenSSL version
check_openssl_version() {
  local REQUIRED_VERSION="3.3.1"
  local current_version=$(openssl version | awk '{print $2}')

  if version_ge "$current_version" "$REQUIRED_VERSION"; then
    echo -e "${GREEN}✅ OpenSSL version $current_version is suitable.${NC}"
  else
    echo -e "${RED}❌ OpenSSL version $current_version is not suitable. Please upgrade to version $REQUIRED_VERSION or later.${NC}"
    exit 1
  fi
}

# Function to check if xidel is installed
check_xidel_installed() {
  if ! command -v xidel &>/dev/null; then
    echo -e "${RED}❌ Error: xidel is not installed.${NC}"
    echo -e "${YELLOW}⚙️  To install xidel, follow the instructions below:${NC}"
    echo -e "${BLUE}Ubuntu/Debian:${NC} sudo apt-get update && sudo apt-get install xidel"
    echo -e "${BLUE}Fedora:${NC} sudo dnf install xidel"
    echo -e "${BLUE}Arch Linux:${NC} sudo pacman -S xidel"
    echo -e "${BLUE}OpenSUSE:${NC} sudo zypper install xidel"
    echo -e "${BLUE}macOS (using Homebrew):${NC} brew install xidel"
    echo -e "${BLUE}Other distributions:${NC} Download from https://github.com/benibela/xidel/releases"
    exit 1
  fi
}

# Function to check if curl is installed
check_curl_installed() {
  if ! command -v curl &>/dev/null; then
    echo -e "${RED}❌ Error: curl is not installed.${NC}"
    echo -e "${YELLOW}⚙️  To install curl, follow the instructions below:${NC}"
    echo -e "${BLUE}Ubuntu/Debian:${NC} sudo apt-get update && sudo apt-get install curl"
    echo -e "${BLUE}Fedora:${NC} sudo dnf install curl"
    echo -e "${BLUE}Arch Linux:${NC} sudo pacman -S curl"
    echo -e "${BLUE}OpenSUSE:${NC} sudo zypper install curl"
    echo -e "${BLUE}macOS (using Homebrew):${NC} brew install curl"
    exit 1
  fi
}

# Function to check if keytool is installed
check_keytool_installed() {
  if ! command -v keytool &>/dev/null; then
    print_msg "${RED}" "❌ keytool is not installed. Please install it and try again."
    exit 1
  fi
}

# Function to generate a CA certificate
# @param private_key_file: The private key file path
# @param ca_certificate_file: The CA certificate file path
# @param validity_days: The number of days the certificate is valid
# @param country: The country code
# @param state: The state or province
# @param locality: The locality or city
# @param organization: The organization name
# @param organizational_unit: The organizational unit name
# @param common_name: The common name
# @param private_key_password: The private key password
generate_ca_certificate() {
  local private_key_file=$1
  local ca_certificate_file=$2
  local validity_days=$3
  local country=$4
  local state=$5
  local locality=$6
  local organization=$7
  local organizational_unit=$8
  local common_name=$9
  local private_key_password=${10}

  echo -e "${BLUE}📜 Generating the CA certificate...${NC}"
  if ! openssl req -x509 -new -nodes -key "$private_key_file" -sha256 -days "$validity_days" -out "$ca_certificate_file" \
    -subj "/C=$country/ST=$state/L=$locality/O=$organization/OU=$organizational_unit/CN=$common_name" -passin pass:"$private_key_password"; then
    echo -e "${RED}❌ Error generating the CA certificate.${NC}"
    exit 1
  fi
  echo -e "${GREEN}✅ CA certificate generated successfully!${NC}"
}

# Function to generate a CSR
# @param private_key_file: The private key file path
# @param csr_file: The CSR file path
# @param country: The country code
# @param state: The state or province
# @param locality: The locality or city
# @param organization: The organization name
# @param organizational_unit: The organizational unit name
# @param common_name: The common name
# @param private_key_password: The private key password
# @param config_file: The OpenSSL config file path (optional)
generate_csr() {
  local private_key_file=$1
  local csr_file=$2
  local country=$3
  local state=$4
  local locality=$5
  local organization=$6
  local organizational_unit=$7
  local common_name=$8
  local private_key_password=$9
  local config_file=${10}

  echo -e "${BLUE}📑 Generating the CSR...${NC}"
  if [ -n "$config_file" ]; then
    openssl req -new -key "$private_key_file" -out "$csr_file" \
      -subj "/C=$country/ST=$state/L=$locality/O=$organization/OU=$organizational_unit/CN=$common_name" \
      -passin pass:"$private_key_password" -config "$config_file"
  else
    openssl req -new -key "$private_key_file" -out "$csr_file" \
      -subj "/C=$country/ST=$state/L=$locality/O=$organization/OU=$organizational_unit/CN=$common_name" \
      -passin pass:"$private_key_password"
  fi

  if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error generating the CSR.${NC}"
    exit 1
  fi
}

# Function to generate a p12 file
# @param private_key_file: The private key file path
# @param ca_certificate_file: The CA certificate file path
# @param output_p12_file: The output p12 file path
# @param private_key_password: The private key password
generate_p12_file() {
  local private_key_file=$1
  local ca_certificate_file=$2
  local output_p12_file=$3
  local private_key_password=$4

  p12_password=$(openssl rand -base64 12)
  echo -e "${BLUE}🔒 Generating the p12 file...${NC}"
  if ! openssl pkcs12 -export -out "$output_p12_file" -inkey "$private_key_file" -passin pass:"$private_key_password" -in "$ca_certificate_file" -password pass:"$p12_password"; then
    echo -e "${RED}❌ Error generating the p12 file.${NC}"
    exit 1
  fi
  echo -e "${GREEN}✅ p12 file generated successfully!${NC}"
  echo -e "${YELLOW}p12 file: ${NC}$output_p12_file"
  echo -e "${YELLOW}p12 password: ${NC}$p12_password"
}

# Function to generate a private key
# @param private_key_file: The private key file path
generate_private_key() {
  local private_key_file=$1
  local password_file="${private_key_file}.password"
  local private_key_password=$(openssl rand -base64 12)

  echo -e "${BLUE}🔑 Generating the private key...${NC}"
  if ! openssl genpkey -algorithm RSA -out "$private_key_file" -aes256 -pass pass:"$private_key_password"; then
    echo -e "${RED}❌ Error generating the private key.${NC}"
    exit 1
  fi
  echo -e "${YELLOW}Private key password: ${NC}$private_key_password"
  echo "$private_key_password" >"$password_file"
}

# Function to generate a random password
generate_random_password() {
  local password_length=12
  openssl rand -base64 ${password_length}
}

# Function to generate OpenSSL config file for SAN support
# @param config_file: The OpenSSL config file path
# @param san_domains: The SAN domains
generate_openssl_server_certificate_config() {
  local config_file=$1
  local san_domains=$2

  echo -e "${BLUE}📝 Creating OpenSSL config file for SAN...${NC}"
  cat >"$config_file" <<EOL
[ req ]
distinguished_name = req_distinguished_name
req_extensions = v3_req
[ req_distinguished_name ]
[ v3_req ]
subjectAltName = $san_domains
EOL
  echo -e "${GREEN}✅ OpenSSL config file created for SAN.${NC}"
}

# Function to sign the CSR with the CA to generate the client certificate
# @param csr_file: The CSR file path
# @param ca_cert_file: The CA certificate file path
# @param ca_key_file: The CA key file path
# @param client_cert_file: The client certificate file path
# @param validity_days: The number of days the certificate is valid
# @param ca_key_password: The CA key password
# @param config_file: The OpenSSL config file path (optional)
generate_client_certificate() {
  local csr_file=$1
  local ca_cert_file=$2
  local ca_key_file=$3
  local client_cert_file=$4
  local validity_days=$5
  local ca_key_password=$6
  local config_file=$7

  echo -e "${BLUE}🔏 Generating the client certificate...${NC}"
  if [ -n "$config_file" ]; then
    openssl x509 -req -in "$csr_file" -CA "$ca_cert_file" -CAkey "$ca_key_file" -CAcreateserial \
      -out "$client_cert_file" -days "$validity_days" -sha256 -passin pass:"$ca_key_password" -extfile "$config_file" -extensions client_cert
  else
    openssl x509 -req -in "$csr_file" -CA "$ca_cert_file" -CAkey "$ca_key_file" -CAcreateserial \
      -out "$client_cert_file" -days "$validity_days" -sha256 -passin pass:"$ca_key_password"
  fi

  if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error generating the client certificate.${NC}"
    exit 1
  fi
}

# Function to sign the CSR with the CA to generate the server certificate
# @param csr_file: The CSR file path
# @param ca_cert_file: The CA certificate file path
# @param ca_key_file: The CA key file path
# @param server_cert_file: The server certificate file path
# @param validity_days: The number of days the certificate is valid
# @param ca_key_password: The CA key password
# @param config_file: The OpenSSL config file path (optional)
generate_server_certificate() {
  local csr_file=$1
  local ca_cert_file=$2
  local ca_key_file=$3
  local server_cert_file=$4
  local validity_days=$5
  local ca_key_password=$6
  local config_file=$7

  echo -e "${BLUE}🔏 Generating the server certificate...${NC}"
  if [ -n "$config_file" ]; then
    openssl x509 -req -in "$csr_file" -CA "$ca_cert_file" -CAkey "$ca_key_file" -CAcreateserial \
      -out "$server_cert_file" -days "$validity_days" -sha256 -passin pass:"$ca_key_password" -extfile "$config_file" -extensions v3_req
  else
    openssl x509 -req -in "$csr_file" -CA "$ca_cert_file" -CAkey "$ca_key_file" -CAcreateserial \
      -out "$server_cert_file" -days "$validity_days" -sha256 -passin pass:"$ca_key_password"
  fi

  if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Error generating the server certificate.${NC}"
    exit 1
  fi
}

# Function to retrieve the private key password from the password file
# @param private_key_file: The private key file path
get_private_key_password() {
  local private_key_file=$1
  local password_file="${private_key_file}.password"

  if [ ! -f "$password_file" ]; then
    echo -e "${RED}❌ Password file not found: ${password_file}${NC}"
    exit 1
  fi

  private_key_password=$(cat "$password_file")
  echo "$private_key_password"
}

# Function to print colored messages
print_msg() {
  local color=$1
  local msg=$2
  echo -e "${color}${msg}${NC}"
}

# Function to verify the Certificate Issuer
# @param certificate_file: The certificate file path
verify_certificate_issuer() {
  local certificate_file=$1
  local certificate_issuer=""

  echo -e "${BLUE}🔍 Verifying the Certificate Issuer...${NC}"
  certificate_issuer=$(openssl x509 -in "$certificate_file" -noout -issuer)
  echo -e "${GREEN}✅ Certificate Issuer: ${NC}$certificate_issuer"
}

# Function to verify the Certificate Serial
# @param certificate_file: The certificate file path
verify_certificate_serial() {
  local certificate_file=$1
  local certificate_serial=""

  echo -e "${BLUE}🔍 Verifying the Certificate Serial...${NC}"
  certificate_serial=$(openssl x509 -in "$certificate_file" -noout -serial)
  echo -e "${GREEN}✅ Certificate Serial (hex): ${NC}$certificate_serial"
}

# Function to verify the Certificate subject
# @param certificate_file: The certificate file path
verify_certificate_subject() {
  local certificate_file=$1
  local certificate_subject=""

  echo -e "${BLUE}🔍 Verifying the Certificate Subject...${NC}"
  certificate_subject=$(openssl x509 -in "$certificate_file" -noout -subject)
  echo -e "${GREEN}✅ Certificate Subject: ${NC}$certificate_subject"
}

# Function to verify the p12 file certificate subject
# @param output_p12_file: The p12 file path
# @param p12_password: The p12 file password
verify_p12_certificate_subject() {
  local output_p12_file=$1
  local p12_password=$2
  local p12_subject=""

  echo -e "${BLUE}🔍 Verifying the p12 file certificate subject...${NC}"
  p12_subject=$(openssl pkcs12 -in "$output_p12_file" -nokeys -passin pass:"$p12_password" | openssl x509 -noout -subject)
  echo -e "${GREEN}✅ p12 Certificate Subject: ${NC}$p12_subject"
}

# Function to compare versions
# @param version1: The first version to compare
# @param version2: The second version to compare
version_ge() {
  [ "$(printf '%s\n' "$@" | sort -V | head -n 1)" = "$2" ]
}
