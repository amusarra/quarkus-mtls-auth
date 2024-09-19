# Tutorial Project for Quarkus Mutual TLS (mTLS) Authentication POC

[![Keep a Changelog v1.1.0 badge](https://img.shields.io/badge/changelog-Keep%20a%20Changelog%20v1.1.0-%23E05735)](CHANGELOG.md)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![code of conduct](https://img.shields.io/badge/Conduct-Contributor%20Covenant%202.1-purple.svg)](CODE_OF_CONDUCT.md)

This tutorial project is linked to the article [Implementazione di TLS Mutual Authentication (mTLS) con Quarkus](https://bit.ly/3MQPA3v) published on Antonio Musarra Blog. To use the project properly I therefore recommend you read the article that will guide you step by step.

## Architecture Overview

The `tls-mutual-auth` project is a Quarkus-based application designed to demonstrate the implementation of Mutual TLS (mTLS) authentication. This document provides a high-level overview of the project's architecture, key components, and their interactions.

The diagram below illustrates the main components and their relationships within the `tls-mutual-auth` project.

```mermaid
flowchart
    subgraph QuarkusApp ["Quarkus Application"]
        subgraph Security ["Security"]
            SecurityIdentityAugmentors["Security Identity Augmentors"]
            TLSRegistry["TLS Registry"]
            SecurityExceptionMapper["Security Exception Mapper"]
        end
        subgraph TSLIntegration ["TSL Integration"]
            TSLUpdater["TSL Updater"]
            TSLParser["TSL Parser"]
        end
        subgraph RESTEndpoints ["REST Endpoints"]
            ConnectionInfoResourceEndPoint["Connection Info Resource End Point"]
            UserIdResourceEndPoint["User Identity Resource End Point"]
        end
    end
    
    subgraph CertsManager ["Certificates Manager Tools"]
        CertsManagerScript["Certs Manager Script"]
        DeviceIdGenerator["Device ID Generator"]
    end

    subgraph ExternalResources ["External Resources"]
        TSL["Trusted Service List (TSL)"]
        ClientCert["Client Certificates"]
        ServerCert["Server Certificate"]
        CACert["CA Certificate"]
    end

    subgraph QuarkusCLI ["Quarkus CLI"]
        DevMode["Development Mode"]
    end

    QuarkusApp --> QuarkusCLI
    Security -->|handles| SecurityExceptionMapper
    SecurityIdentityAugmentors -.->|augments| Security
    RESTEndpoints -->|uses| Security
    CertsManagerScript -->|manages| ClientCert
    CertsManagerScript -->|manages|ServerCert
    CertsManagerScript -->|manages|CACert
    DeviceIdGenerator -->|creates| ClientCert
    TSLIntegration -->|updates & parses| TSL
    TSLUpdater -->|fetches| TSL
    TSLParser -->|extracts certs| TSL
```

Components and their interactions in the `tls-mutual-auth` project.

### Codebase Structure

The project is structured into several key directories and files:

- `src/main/java/`: Contains all Java source files organized by package.
- `src/main/resources/`: Includes application properties and other resources necessary for the application configuration.
- `src/test/java/`: Houses the test cases for the application.
- `pom.xml`: Maven configuration file that manages dependencies, plugins, and other project-specific configurations.

### Key Components

#### 1. **TLS Configuration**

The TLS settings are managed through the Quarkus framework, leveraging the `application.properties` file located in `src/main/resources/`. This file specifies the server's certificate, the required client certificate for mTLS, and other TLS-related settings.

#### 2. **Resource Endpoints**

The application exposes REST endpoints defined in the `it.dontesta.quarkus.tls.auth.ws.resources.endpoint.v1` package. These endpoints provide functionalities to retrieve connection information and user identity details, demonstrating how client certificates can be used within an mTLS secured application.

#### 3. **Security Configuration**

Security settings are handled through Quarkus security extensions. The project defines custom security augmentors in the `it.dontesta.quarkus.tls.auth.ws.security.identity` package to extract roles and attributes from the client certificate. These augmentors help in enforcing security policies based on the certificate details.

#### 4. **Certificate Management**

Scripts located in `src/main/shell/certs-manager/` assist in generating and managing certificates required for mTLS. These include scripts for creating a Certificate Authority (CA), server certificates, and client certificates with specific attributes.

#### 5. **Tests**

Unit and integration tests are located in `src/test/java/`. These tests ensure that the application behaves as expected under various scenarios, particularly focusing on the security aspects and proper handling of client certificates.

### Architectural Invariants

- **mTLS Requirement:** The application mandates mutual TLS for all interactions, ensuring that both the client and the server are authenticated using certificates.
- **Certificate Validation:** All client certificates are validated against the CA certificate configured in the server's trust store.
- **Role-Based Access Control (RBAC):** Access to specific endpoints is controlled based on roles extracted from the client certificate.

### Boundaries and Interfaces

- **REST API Boundary:** The `ConnectionInfoResourceEndPoint` class defines the boundary for the REST API. It handles incoming HTTP requests and interacts with the security layer to authenticate requests.
- **Security Layer:** Interfaces with the Quarkus security framework to implement custom logic for extracting and validating certificate attributes.
- **Certificate Management Scripts:** These scripts operate outside the Java application but are crucial for setting up and managing the TLS environment necessary for mTLS.

### Conclusion

This architecture document outlines the high-level structure and components of the `tls-mutual-auth` project. By understanding the key elements and their interactions, developers can navigate the codebase effectively and contribute to the project with a clear understanding of how mTLS is implemented and managed within a Quarkus application.

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

> **_WARNING:_**  with the current configuration of the application, it's listening only on HTTPS port 8443. You need to configure your browser to accept the self-signed certificate used by the server. The Dev UI is available at <https://localhost:8443/q/dev/>.

> **_NOTE on Certificate_**: when run phase compile process, Maven will generate a self-signed CA and Server certificate in `src/main/resources/certs` directory and start the download TSL process updater and create a PEM bundle in default directory `/tmp/tsl-it`. Without the TSL bundle, the application will not start.

Below is an asciinema recording of the application running in dev mode.


[![asciicast](https://asciinema.org/a/AXCFc2ugDJTISRU6SG86l5Ihg.svg)](https://asciinema.org/a/AXCFc2ugDJTISRU6SG86l5Ihg)

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/tls-mutual-auth-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Related Guides

- Eclipse Vert.x ([guide](https://quarkus.io/guides/vertx)): Write reactive applications with the Vert.x API
- ArC ([guide](https://quarkus.io/guides/cdi-reference)): Build time CDI dependency injection
- REST ([guide](https://quarkus.io/guides/rest)): A Jakarta REST implementation utilizing build time processing and Vert.x. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it.
- REST Jackson ([guide](https://quarkus.io/guides/rest#json-serialisation)): Jackson serialization support for Quarkus REST. This extension is not compatible with the quarkus-resteasy extension, or any of the extensions that depend on it
