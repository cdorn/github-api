# Changelog

For changelog before 2.0-rc.6-SNAPSHOT see original repository.

## Changed in 2.0-rc.6-minimal-SNAPSHOT
Removal of all domain objects not needed, as all responses are mapped to JsonNode object for issues and graphQL to avoid intermediary calls and to access fields not yet supported by the java domain objects (e.g., an issue's subIssues).

Removal of all test (relying on the upstream project for integrating bug fixes to low-level classes).
