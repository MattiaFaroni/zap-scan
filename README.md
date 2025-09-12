<h1 align="center">Zap-Scan</h1>

<p align="center">
<img width="100" src="img/api-logo.png" alt=""/>
</p>

<p align="center">
<img src="https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="">
<img src="https://img.shields.io/badge/gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white" alt="">
<img src="https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white" alt="">
<img src="https://img.shields.io/badge/Sentry-black?style=for-the-badge&logo=Sentry&logoColor=#362D59" alt="">
</p>

--------

Zap-scan is a Java application designed to interact with the OWASP ZAP API, enabling automated scans of a list of endpoints stored in a PostgreSQL database.
The application generates detailed PDF reports for each scan, simplifying web application security monitoring.
The project is developed with Java 21, built with Gradle, and based on Spring Boot for deployment.  
⚠️ **Note**: To use the ZAP APIs, the ZAP service must be properly configured and running on your local machine or an accessible server.

## Features
* **Scan & Report:**
  * Generate a report for a specific endpoint by providing its database ID. If no ID is specified, reports will be generated for all endpoints in the database.
* **Download Reports:**
  * Download a report for a specific endpoint by specifying its ID, the report creation date, and optionally the exact creation time.
* **List Endpoints:**
  * Retrieve and view the full list of endpoints stored in the database, or provide a specific endpoint ID to view information for that endpoint only.

## Scan & Report
Perform automated security scans on one or more endpoints and generate PDF reports.  
You can provide a specific `endpoint_id` to scan a single endpoint or leave it empty to scan all endpoints in the database.
The service queues scan requests and processes them in a single background worker thread.
It streams real-time progress via SSE, converts ZAP HTML reports to PDF, stores them in the database, and optionally sends the generated PDFs by email.
*Scans are processed sequentially (not in parallel).*

Example request:

```http request
GET /scan/stream?endpoint_id=1
```

The response is streamed as Server-Sent Events (SSE). Example response:

```text
event:info
data:Scan started

event:progress
data:Scanning https://example.url.com:8080/Example/api/test

event:progress
data:https://example.url.com:8080/Example/api/test -> 0%

event:progress
data:https://example.url.com:8080/Example/api/test -> 30%

event:progress
data:https://example.url.com:8080/Example/api/test -> 60%

event:progress
data:https://example.url.com:8080/Example/api/test -> 90%

event:result
data:Completed: https://example.url.com:8080/Example/api/test

event:done
data:All endpoints completed
```

## Download Reports 
Download a previously generated PDF report for a specific endpoint.  
You must provide the `endpoint_id` and the `date` of the report. Optionally, you can also specify the exact `time`.
- If `time` is omitted, the service returns the **first report found for that day**.
- If `time` is provided, the service returns the report created at that exact moment (if it exists).

The report is returned as a PDF file attachment.

Example request:

```http request
GET /scan/reports?endpoint_id=1&date=2025-09-04&time=10:23:53
```

## List Endpoints
Retrieve information about the endpoints stored in the database.
- If no `id` is provided, the service returns a list of all endpoints.
- If a specific `id` is provided, the service returns information only for that endpoint.
- If the specified endpoint does not exist, the service responds with `404 Not Found`.

Example request:

```http request
GET /scan/endpoints?id=1
```

Example response:

```json
{
    "id": 1,
    "name": "Test Api",
    "url": "https://example.url.com:8080/Example/api/test",
    "httpMethod": "GET",
    "queryParams": {
      "Param1": "Example1",
      "Param2": "Example2"
    },
    "headers": {
        "Token": "Example@Example"
    },
    "requestBody": null
}
```