# Transaction, Notification and Redirect Flows

This page explains where SPIs execute. See the [API index](reference/spi/README.md) for method and field definitions; diagram arrows do not define an alternative API.

## 1. iPay to platform to institution

```mermaid
sequenceDiagram
    participant I as iPay
    participant W as Platform entry and filters
    participant F as Channel module Facade
    participant A as Adapter SPI and template
    participant P as Platform services
    participant C as Institution
    I->>W: /{channelCode}/channel/pay
    W->>W: Identify sandbox traffic; establish request context and trace
    W->>W: Apply entry-policy iPay verification/decryption
    W->>F: Resolve SOFA reference by interface + uniqueId
    F->>F: Check PAY registration; construct standard request
    F->>F: Bind ChannelRequestContext: channel, merchant, environment
    F->>P: Select route by channelCode + pay + sandbox state
    P-->>F: domain / path; retain selected route in platform
    F->>A: pay(PayRequest)
    A->>A: validate → mapRequestBody → transport
    A->>P: Optional security computation: sign / encrypt
    A->>A: Insert institution signature/ciphertext
    A->>P: executeFixedUrl / executeDynamicUrl
    P->>P: Verify selected route and Host; construct final address
    P->>C: HTTP request
    C-->>A: Status, headers and body through platform HTTP
    A->>A: Verify/decrypt response → map fields and result codes
    A-->>F: Standard response
    F->>F: Clear ChannelRequestContext in finally
```

The platform entry point is `IpayController`. `PlatformSecurityFilter` handles platform-protocol security when enabled by configuration and `integrationmethod=byMiddleman`. Security processing before the SPI describes responsibility and the normal platform path; it does not imply that disabling security or omitting validation is safe.
`NonIpayRequestFilter` restricts non-iPay traffic at transaction entry points; a marker field alone is not authentication.

The adapter template executes:
`validate → mapRequestBody → [mapUrlParameters] → transport.customize → protectRequestToChannel → HTTP → unprotectResponseFromChannel → mapResponse`.
Signing must use **the payload representation actually sent**. Check Form, Query and path parameters separately; see [HTTP](06-routing-and-http.md).

## 2. Institution to platform to iPay

```mermaid
sequenceDiagram
    participant C as Institution
    participant W as ChannelController / entry filters
    participant F as Channel NotifyFacade
    participant A as NotificationService
    participant P as Platform gateway service
    participant I as iPay
    C->>W: POST /{channelCode}/channel/notifyPayment/{merchantId}
    W->>W: Identify sandbox/trace; put path merchantId in merchantaccount
    W->>F: body + headers; select channel NotifyFacade
    F->>F: Check PAYMENT_NOTIFY registration
    F->>F: Bind ChannelRequestContext using platform-resolved merchant header
    F->>A: notifyPayment(BaseChannelRequest)
    A->>A: Verify/decrypt rawBody → validate → map
    A-->>F: PaymentNotifyRequest, not an ACK
    F->>P: invokePaymentNotify(module iPay identity, standard request)
    P->>P: Set iPay domain and fixed path; apply platform security policy
    P->>I: Forward standard notification
    I-->>P: Forwarding response
    P-->>W: NotifyResponse
    F->>F: Clear ChannelRequestContext in finally
    W-->>C: Current Controller's NotifyResponse JSON
```

The key distinction: a transaction SPI returns a standard response, whereas a notification SPI returns **the standard request to send to the next hop**. The notification adapter must not call iPay HTTP directly, sign platform-standard objects for iPay itself, or put an institution ACK string in PaymentNotifyRequest. Institutions requiring plain text, specific status codes, custom headers or an independent ACK require confirmation of Controller/forwarding-response handling with the platform; do not assume the default response is compatible.

## 3. Browser ACS redirect

The implemented path is:
`Browser GET /{channelCode}/channel/acsUrlCallback → ChannelController → NotifyFacadeSupport.acsUrlCallback → IpayGatewayService → iPay → Controller 302 Location`.

The platform extracts `inSerialNo / signData / outOrderNo` and redirects to `iopengwSystemInnerRedirectionUrl`. An unparseable URL, missing host, userInfo component or scheme other than HTTP/HTTPS produces a 502 response. This path is platform-owned and does not invoke an adapter SPI. The SDK callback SPI has been removed; the CLI does not generate a callback service. ACS request/response DTOs now belong to `sdk.api.gateway.request/response`.

## 4. Where context is consumed

| Context | Producer | Consumer | Integrator rules |
| --- | --- | --- | --- |
| channelCode | Entry path, module channel.code and uniqueId | Facade, routing, capability validation, result codes | Confirm and keep consistent with the platform; do not choose another channel from institution payloads |
| ChannelRequestContext | Facade: module channelCode, platform-resolved merchantaccount, runtime environment | Adapter security requests and protocol customization | Read with current(); do not bind it yourself or transfer it across threads |
| runtimeEnv | Platform environment utility; populated in BaseChannelRequest and invocation context | Custom logic that needs environment information | May be null; not shadow state and not a basis for selecting another egress domain |
| rawBody | Body string received by the platform/passed to the SPI | Notification verification and protocol mapping | Not raw transport bytes; do not reserialize before verification |
| Sandbox state | ShadowTagInboundFilter | DRM routing, Tracer marker for IBCM queries, logging | Do not infer it again, reset it or select another domain in the adapter |
| Selected address | DomainControlConfig / SelectedChannelRoute | Outbound HTTP validation | Pass the original request along the chain; do not overwrite domain/path |
| traceId/rpcId | SOFA Tracer; entry layer populates MDC | Platform logs | Not transaction primary keys; do not overwrite or fabricate them |

Read the merchant through `ChannelRequestContext.current().getMerchantId()`, not from business payloads. The Facade clears context on success and failure. Isolated tests that bypass the Facade must simulate this lifecycle. See [invocation context](reference/context.md) for methods, missing-value behavior and thread boundaries.

Evidence: IpayController, ChannelController, AbstractFacadeSupport, ChannelFacadeSupport, NotifyFacadeSupport and ChannelInvocationTemplate.
