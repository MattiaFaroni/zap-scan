## Unversioned

### Dependency Versions
- **SpringBoot** 4.0.6
- **Sentry** 6.6.0
- **Spotless** 8.4.0
- **Lombok** 1.18.46
- **PostgreSQL** 42.7.11
- **HttpClient5** 5.6.1
- **Jsoup** 1.22.2

## Version 2.0.0
**Release Date:** 2026-03-14

### What's Changed
- **Spring Boot:** Upgraded to 4.0.3.
- **Scan Lifecycle:** Replaced active scan check with progress polling.
- **SSE Streaming:** Fixed connection lifecycle management.
- **PDF Report:** Improved HTML-to-PDF conversion pipeline.
- **ZAP Integration:** Updated replacer rule setup.
- **Mail Service:** Skip sending when no reports are available.
- **Tests:** Added missing test cases across all modules.

### Dependency Versions
- **SpringBoot** 4.0.3
- **Spotless** 8.3.0
- **PostgreSQL:** 42.7.10
- **Sentry** 6.1.0
- **ZapClient** 1.17.0
- **HttpClient5** 5.6
- **OpenHtmlToPdf** 1.1.37

## Version 1.0.0
**Release Date:** 2025-10-05

### New Features
- **Scan & Report:** Generate reports for endpoints via DB ID.
- **Download Reports:** Fetch reports by endpoint ID, date, and optional time.
- **List Endpoints:** View all endpoints or details for a specific ID.

### Dependency Versions
- **SpringBoot** 3.5.6
- **Lombok:** 1.18.42
- **PostgreSQL:** 42.7.8
- **ZapClient** 1.16.0
- **HttpClient5** 5.5.1
- **OpenHtmlToPdf** 1.1.31
- **Sentry** 8.23.0
- **Spotless** 8.0.0
- **ShadowJar** 8.1.1