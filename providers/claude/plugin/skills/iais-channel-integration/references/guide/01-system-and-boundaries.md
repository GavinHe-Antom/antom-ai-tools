# System Purpose and Responsibility Boundaries

## 1. The problem the system solves

The IAIS integration platform receives standard payment requests from iPay and routes them to the designated channel adapter, which converts them to the institution protocol. Institution notifications are converted into standard iPay notifications. The integration deliverable is a JAR implementing the common-sdk SPIs. The platform owns service entry points, channel module assembly, domain control, network transport, foundational security services and runtime observability.

Integrators do not need to copy platform controllers, implement a service registry or maintain another institution HTTP client. They must accurately implement institution fields, payloads, signing input and transaction-result semantics.

## 2. Layers and code ownership

| Layer | Code/module | Owner | Responsibilities | Out of scope |
| --- | --- | --- | --- | --- |
| Standard contracts | `app/common/common-sdk` | Platform | SPI request/response models; platform HTTP, security and result-code interfaces | Spring containers, institution address configuration, platform service implementations |
| HTTP entry points | `app/web` | Platform | iPay/institution entry points, SOFA Facade references selected by channelCode, ACS 302 redirects | Institution field mapping |
| Channel modules | `app/channel-modules/channel-integrations/*` | Platform | Module identity, Facade instances, capability registration, route injection before invocation, adapter assembly | Scanning all channel beans into the main application |
| Shared processing | `channel-framework` | Platform | Facade templates, SPI capability gates, exception fallback, module-local platform service bridges | Institution-specific fields |
| Institution adapters | Adapter `src/main/java/<unique-package>` | Integrator | Validation, mapping, channel signature/encryption orchestration, response/notification conversion | Replacing egress destinations, accessing IBCM directly, controlling connection pools |
| Platform integration services | `common-service-integration` | Platform | DRM routing, shared HTTP, IBCM queries and security computation, iPay forwarding, result-code queries | Embedding every institution protocol in generic classes |
| Other platform layers | `common-util / common-dal / model / core-service / bootstrap` | Platform | Shared utilities, application assembly and other host responsibilities | Internal SDK dependencies required by adapters |

Here, Facade means a channel module entry class, not an additional public HTTP facade that external developers must implement.

## 3. Two directions and two contracts

- **Institution capability SPIs:** the platform calls the adapter. common-sdk defines `PaymentService / RefundService / NotificationService`. Browser ACS callbacks are handled by the platform, not an adapter SPI.
- **Platform service APIs:** the adapter calls the platform. The most commonly used interfaces are `PlatformChannelHttpService / PlatformChannelSecurityService / ResultCodeService`.

Being defined in the same SDK JAR does not mean every interface is implemented by external developers. The host supplies the latter three interfaces, and adapters consume them through Spring injection.
`ChannelRouteService` and `IpayGatewayService` primarily serve platform channel modules and Facades; they are not integration entry points for bypassing routing or forwarding to iPay manually.

## 4. What SOFABoot modularity means

Channels are assembled as SOFABoot modules. Channel beans, configuration and SPI lookup must remain in that channel's Spring context. The platform selects a channel through the SOFA `uniqueId` of `ChannelFacade` / `NotifyFacade`, then invokes the adapter within that module.

There is no independent class loader per business module. Every JAR must follow the host's dependency versions, fully qualified class-name rules and shared-runtime constraints. Spring context isolation cannot resolve identical Java package names across channels, conflicting library versions or request state stored in static fields. Modularity is not a security sandbox for untrusted JARs; an API that does not expose a URL setter is not strong isolation against arbitrary malicious Java code.

Generated Spring XML performs component scanning only; it does not create a SOFA module. The platform must import the adapter into the target module, not the main application's global scan scope.

## 5. Capabilities outside adapter integration scope

The SDK does not provide vaulting or dispute SPIs. There is no integration commitment for automated JAR scanning, automatic gradual rollout, reconciliation/settlement statements, a transaction database or idempotency storage. Generated projects do not provide REST services, a main method or a standalone transaction server. Default tests are local tests, not acceptance tests for real institution connectivity.

Evidence: common-sdk SPIs; ChannelFacadeRegistry and NotifyFacadeRegistry; channel module Spring XML; ChannelSpiCapabilityValidator; generated Spring scan configuration.
