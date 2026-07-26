# gRPC Support

## Overview

This project now includes initial gRPC server support alongside the existing REST API.

The current implementation introduces the required infrastructure for gRPC communication while keeping the existing REST endpoints unchanged. A simple `HelloService` is provided as a proof of concept to validate the integration.

## Features

* gRPC server integration
* Protocol Buffers code generation
* Maven build integration
* Java 21 compatible configuration
* Sample `HelloService`
* Verified using `grpcurl`

## Project Structure

```text
src/
├── main/
│   ├── java/
│   │   └── ...grpc...
│   └── proto/
│       └── hello.proto
```

## Configuration

The gRPC server runs independently from the REST server.

Example configuration:

```yaml
server:
  port: 8080

grpc:
  server:
    port: 9090
```

## Building

Compile the project normally:

```bash
./mvnw clean compile
```

Run the application:

```bash
./mvnw spring-boot:run
```

## Testing

List available services:

```bash
grpcurl -plaintext -proto hello.proto localhost:9090 list
```

Expected output:

```text
hello.HelloService
```

Describe the service:

```bash
grpcurl -plaintext -proto hello.proto localhost:9090 describe hello.HelloService
```

Invoke the sample service:

```bash
grpcurl -plaintext -proto hello.proto -d '{"name":"Pramod"}' localhost:9090 hello.HelloService/SayHello
```

Expected response:

```json
{
  "message": "Hello Pramod from Distributed Rate Limiter"
}
```

> **PowerShell users**
>
> Depending on your shell, JSON escaping may differ. If the command above fails because of quoting, use:
>
> ```powershell
> grpcurl -plaintext -proto hello.proto -d '{\"name\":\"Pramod\"}' localhost:9090 hello.HelloService/SayHello
> ```

## Current Status

This is the initial gRPC integration.

The current implementation includes a sample service to verify that:

* Protocol Buffer generation works correctly.
* The gRPC server starts successfully.
* Requests and responses are correctly serialized.
* Clients can communicate with the server using `grpcurl`.

## Future Work

Potential future enhancements include:

* Implementing the existing rate limiting API over gRPC.
* Server Reflection support.
* `grpcui` integration.
* Additional streaming APIs where appropriate.
* Production-ready gRPC documentation.
