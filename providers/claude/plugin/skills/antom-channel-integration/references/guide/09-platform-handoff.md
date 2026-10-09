# Adapter-to-Platform Handoff Checklist

## 1. Both parties must complete their work

An adapter JAR supplies Spring beans only. The platform channel module decides which JARs to load, packages to scan, channelCode to route and SPIs to allow.
External developers can implement field mappings without changing the main application, but final integration requires the platform owner to complete the following configuration.

| Handoff item | Adapter supplies | Platform completes |
| --- | --- | --- |
| Channel identity | Institution/product description, unique package and coordinates | Consistent channel.code, SOFA uniqueId and entry channelCode |
| Capability scope | Per-method inventory and test evidence | Facade registerSpiOperations and notification registration |
| Bean loading | SPI classes and scan packages | Channel SOFA module dependencies, scanning and Facade export |
| Routing | API methods, path parameters and signing-target rules | Normal and sandbox DomainControlConfig routes |
| iPay notifications | Notification types, standard fields and correlation IDs | ipayDomain, IpayChannelIdentity and gateway APIs |
| Keys | Relationship between context merchant and institution identity; algorithm, purpose and input rules | IBCM materials, format, current version and environment settings |
| Result codes | Institution codes, descriptions and business states | ResultCodeService configuration and fallback policy |
| Transport | Method, headers, body format and idempotency semantics | Network access, connection pools, timeouts and retries |
| Acknowledgment | Institution notification ACK specification | Assess Controller compatibility and address gaps |
| Third-party dependencies | Coordinates, versions and purpose | Host runtime dependency and conflict validation |

## 2. Scanning is not permission to invoke

The platform uses `ChannelSpiCapabilityContributor`; there is no existing public registration API named ChannelSpiCapabilityRegistry.
Startup validation checks contributed operations, missing/duplicate SPI beans and optional default methods that were not actually overridden. It does not execute method bodies, so null placeholders may still pass signature-level checks.

Runtime templates also check registration scope. An implemented capture method cannot run without platform CAPTURE registration. Conversely, a registered operation with only the SDK default implementation fails during startup validation or invocation.

## 3. SOFABoot isolation and dependency review

Module Spring contexts isolate beans and channel configuration. SOFA service references expose Facades to platform routing.
The platform shares a classpath rather than giving each adapter a separate ClassLoader; module isolation does not resolve identical fully qualified class names or incompatible library versions.

Integration rules:

- Do not broadly scan all channel SPIs in the main application.
- Use a unique package per channel; do not copy identically named classes into another channel's package.
- Do not assume static fields, singleton utilities or system properties are naturally module-isolated.
- Do not copy internal platform integration classes into adapters.
- Submit dependency:tree; the host centrally reviews additional dependencies.

## 4. Integration handoff template

```text
Channel code / product type:
Adapter coordinates / version / SDK version:
SPI methods, listed individually:
Institution API and platform operation key for each method:
3DS interaction mode:
Notification types / public callback paths / ACK:
merchantId: supplied by platform ChannelRequestContext; confirm institution identity mapping:
Signing input, algorithm and signature encoding:
Key format and environment:
Normal / sandbox route inventory:
Institution result-code mappings:
Additional third-party dependencies:
Success / failure / unknown scenario examples:
Unresolved issues and owners:
```

Do not include real keys or complete card data. Record only material types and platform-controlled configuration references.

Evidence: channel module Spring XML, ChannelFacadeRegistry, NotifyFacadeRegistry, ChannelSpiCapabilityContributor, ChannelSpiCapabilityValidator and DomainControlConfig.
