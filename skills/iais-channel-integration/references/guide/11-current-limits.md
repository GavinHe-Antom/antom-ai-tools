# Current Limits and Integration Confirmations

This page records source-code facts and outstanding handoff questions, not features that have automatically been resolved.

| Item | Current status | Integration impact and action |
| --- | --- | --- |
| SDK artifact matching | CLI template 0.1.0 requires SDK 1.5.2; SDK is absent from the public repository | Obtain JAR and standalone consumer POM separately from the platform; verify actual APIs and artifacts |
| Sandbox Tracer | Shadow state follows loadMode; Tracer preparation/marking failures warn and continue | Verify downstream services such as keys that rely on propagation; a sandbox domain alone is not end-to-end isolation |
| Generated SPI | Selected methods are wired with anonymous mapping extensions; enabled security hooks remain unfinished | Complete institution rules and per-method DeliveryTest; structure success is not business success |
| Notification mapping | Anonymous ChannelNotificationExtension inside each selected notifyPayment/notifyCapture/notifyRefund method | Implement verification, validate and map; do not forward to iPay directly or create ACKs |
| notifyOnlineBankPayment / notifyReceivePayment | Optional SDK declarations exist, platform APIs are not connected | The generator excludes their implementations and operation constants; confirm platform work first |
| ACS browser callback | Platform-owned flow; callback SPI removed and DTOs relocated to sdk.api.gateway | No adapter callback service or callback operation constants are generated |
| onlineBankUrlCallback | Removed from the SDK; no current platform entry point | Do not generate an implementation or reuse the ACS URL |
| Vault / Dispute | No SPIs | Do not generate vaulting/dispute implementations; residual enum values do not expose services |
| AuthorizeRequest / AuthorizeResponse | Retained SDK types, unused by current authorization SPI | Use AuthenticateAuthorizeRequest/Response |
| Non-2xx | Platform HTTP retains response; CLI template throws early | Adjust adapter template when institution error-body mapping is required |
| JSON template | mapRequestBody requires JSONObject | Explicit template changes for non-JSON protocols; do not create another HTTP client |
| Institution notification ACK | Controller returns NotifyResponse JSON | Platform assessment required for fixed text, special headers/statuses |
| Raw payload/headers | String text and single-value header Map | Confirm preservation for byte-level verification and multi-value headers |
| Security algorithm enums | Independent catalogs: 16 sign, 12 cipher and 48 queryKey entries | Check the security reference; enum presence alone is not compatibility |
| Key versions | Current valid material only; SDK cannot select historical versions | Confirm rotation windows and delayed-notification verification |
| Key value encoding | Returned unchanged without codeType | Confirm stored material format; do not guess |
| Parameter validation | Mostly plain SDK fields; template validation is illustrative | Complete protocol and requiredness conditions in mapping documents |
| SOFABoot modules | Spring context isolation with shared classpath | Review dependency conflicts and static state |
| Domain control | Initial destination validation, not globally disabled redirects | Do not claim isolation of every network action |
| Asynchronous context | Routing depends on thread context | Do not switch threads for platform HTTP on your own |
| Example idempotency header | Not idempotency storage | Confirm institution idempotency and platform retry policy |
| SDK CacheService / MessageService | Interfaces remain; usable host implementations are not confirmed | Do not present them as delivered external integration services |
| ais CLI | Source, init/package and versioned templates available; no automatic installation | Build and obtain authorized SDK first; Java 8, macOS/Linux; Windows unverified |

Define institution idempotency headers and extendInfo scope for each selected method; example headers and complete raw institution bodies are not default protocols.
For a platform-supplied generic scaffold, also check [scaffold differences](../baseline-scaffold.md).

These limits do not block every integration. Address or confirm only applicable scenarios; acceptance of basic card/non-card payments follows their actual capability scope.
