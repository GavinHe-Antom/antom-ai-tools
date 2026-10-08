# Domains, Paths, Sandbox Traffic and Platform HTTP

## 1. Who selects the destination

The platform configures and selects the institution domain/path. Adapters map business fields and may read the selected address when needed for signing.
HttpRequest in `PlatformChannelHttpService` has no URL, timeout or connection-pool fields. Do not bypass the platform with HttpClient/OkHttp/URLConnection or replace Host through a header.

The default and configurable iPay domain use the same `DomainControlConfig.ipayDomain`. Notification paths combine platform code templates with channel-module identity. Adapters do not select iPay destinations.

## 2. From traffic marker to outbound call

```mermaid
sequenceDiagram
    participant I as iPay
    participant W as Platform entry
    participant F as Channel Facade
    participant C as DomainControlConfig
    participant A as Adapter
    participant H as Platform HTTP
    participant D as Institution
    I->>W: /{channelCode}/channel/pay + loadMode
    W->>W: Identify shadow marker; save request context
    W->>F: Route by channelCode
    F->>C: resolve(channelCode, pay)
    C->>C: Select normal or sandbox configuration from context
    C-->>F: domain + path; retain this request's selection
    F->>A: PayRequest with domain/path populated
    A->>A: Map fields, transport properties and security values
    A->>H: executeFixedUrl / executeDynamicUrl
    H->>H: Check channel/domain/path/sandbox identity; substitute parameters
    H->>D: Send HTTP to platform-selected destination
    D-->>H: Status, headers and body
    H-->>A: HttpResponse, including non-2xx
```

The retained selection is a route value in request context, not an additional SDK RouteExecutionRef interface.
Platform HTTP rejects changed request domain/path or calls outside the original thread context. Creating another BaseChannelRequest does not bypass platform route selection.

## 3. DRM configuration structure: platform-owned

`DomainControlConfig.apiItemList` is a JSON-array string. This illustrates structure only; example domains are not usable integration endpoints:

```json
[
  {
    "channelCode": "examplechannel",
    "channelDomain": "https://api.example.com",
    "pathMap": {
      "pay": "/v1/payments",
      "inquiryPayment": "/v1/payments/{paymentId}",
      "refund": "/v1/refunds"
    },
    "sandbox": {
      "channelDomain": "https://sandbox.example.com",
      "pathMap": {
        "pay": "/v1/payments",
        "inquiryPayment": "/v1/payments/{paymentId}",
        "refund": "/v1/refunds"
      }
    }
  }
]
```

Supported operation keys are `pay / authorize / cancel / capture / inquiryPayment / refund / inquiryRefund`. Java method authenticateAuthorize uses configuration key authorize. Configure routes for registered operations. Missing routes are not inferred, and missing sandbox routes do not fall back to production.
Repeated selection of the same operation within a request retains its chosen route, preventing mid-request configuration changes from producing inconsistent destinations.

Configure `ipayDomain` as a complete base address, such as `https://open-sea.alipay.com`, which is also the default. Do not supply only a hostname expecting HTTPS to be added.
See [notifications](07-notifications-and-callbacks.md) for iPay paths.

## 4. Where sandbox identity is recognized and consumed

| Stage | Behavior |
| --- | --- |
| ShadowTagInboundFilter: transactions | Inbound loadMode=2 marks the request as shadow; other values mean normal traffic. It does not infer shadow state from an existing Tracer |
| ShadowTagInboundFilter: notifications and ACS | The four connected callback paths use isSandbox from request parameters, normally carried in the callback URL. Missing, empty or false means normal traffic even when loadMode=2 is received; an invalid Boolean produces HTTP 400 |
| Platform request context | Retains shadow state and trusted routing channel identity; logging context distinguishes traffic |
| ChannelRouteService.resolve | Selects normal or sandbox domain/path based on shadow state |
| Platform HTTP | Checks destination consistency with the selected route and sandbox identity |
| Platform key integration | Passes shadow-related markers downstream from request context |
| Tracer synchronization | Creates a fallback span if context is absent; attempts loadMode=2, instMock=O and mark=T for shadow requests |
| iPay forwarding | Rebuilds loadmode=2 and markuid=0A from platform shadow context for sandbox requests; removes conflicting sandbox headers for normal requests. Both use the configured ipayDomain, not separate sandbox domains |
| Request completion | Clears IaisRequestContext, pops/finishes spans created by this filter, and restores prior MDC shadow; does not promise to restore all propagation properties on an existing span |

**Sandbox handling is not limited to prod.** Do not create a separate dev/prod switch in the adapter.
If fallback Tracer creation or marker updates fail, the filter logs a warning and continues; request-context shadow and logging markers are set independently. Channel egress can still select a sandbox route, but downstream services such as key lookup that depend on Tracer propagation must be verified in integration tests. Do not promise that all downstream systems retain sandbox identity after such failures.
Selecting a channel sandbox endpoint does not guarantee institution data isolation, generated test data, accounting isolation or network allowlists. End-to-end behavior requires platform/institution verification.
Manually created threads and asynchronous tasks lose ThreadLocal context. Automatic cross-thread propagation is not promised; confirm a mechanism with the platform before introducing asynchronous calls.

The SDK [ChannelRequestContext](reference/context.md) separately exposes channel, merchant and `runtimeEnv`; it does not carry selected routes or shadow state. The platform populates both `BaseChannelRequest.runtimeEnv` and the invocation context. A missing environment is not implicitly a test environment, and dev/prod checks must not replace the platform shadow marker.
Call platform HTTP with the original request on the original thread. Do not construct destinations from environment fields.

For an adapter, the callback URL is just a mapping value. Preserve its full query string; do not parse `isSandbox` to choose a destination, key or computation mode. The platform currently does not append this parameter automatically. See the [callback contract](07-notifications-and-callbacks.md#callback-sandbox-contract) for registration and host-test responsibilities.

## 5. Fixed and dynamic paths

| Mode | Call | Adapter supplies |
| --- | --- | --- |
| Fixed | executeFixedUrl(context, request) | Original platform request plus method/headers/body |
| Dynamic | executeDynamicUrl(context, urlParameters, request) | Additionally, raw business values for template placeholders |

For a platform path `/v1/payments/{paymentId}`, the adapter returns `{"paymentId":"A/B"}`; the platform encodes it as one path segment, `A%2FB`. Do not pre-encode it as `A%2FB`, which may encode the percent sign again, or supply a complete URL.
Fixed paths must not retain placeholders. Dynamic paths require all replacement values; null and dot path segments are not substitutes.

When signatures include Request-Target, use the final path and required Query per institution protocol. Do not sign a placeholder-containing template or URL-encode twice. The platform does not generically construct institution-specific signing input for adapters.

## 6. HttpRequest usage rules

- method: use the institution-defined method; GET/HEAD currently send no body.
- headers: include only institution-required entries; Host is prohibited. Do not copy all inbound headers.
- body: institution payload text; JSON, Form, XML and other representations must match actual encoding.
- queryParams/formParams: provide raw values to avoid double encoding; nonempty Form currently takes precedence over Body.
- contentType: match the actual body; changing Content-Type alone must not leave signing input encoded differently.
- attributes: transport extensions, not a bypass for domains or connection settings.

The platform controls connections, timeouts and retries. Adapters must not blindly retry non-idempotent transactions; review institution idempotency keys together with platform policy.
There is no general guarantee that all redirects are disabled. Initial destination validation does not equal network-level protection against every redirect or arbitrary outbound connection.

## 7. Template differences and special protocols

The CLI template centers on JSONObject. Unsupported payload formats must not be handled by constructing a URL and creating another client.
Use `ChannelOutboundRequest.setRawBody` for exact text or explicit bodyless requests, `getSerializedBody` for final signing text, and the anonymous ChannelApiExtension's `acceptResponseStatus` for confirmed non-2xx business bodies. More complex multi-stage protocols may need targeted workflow changes and ordinary JUnit tests. Every actual request still uses platform HTTP and a platform-supplied route.
If multiple institution API calls require routes beyond the operation model, confirm the model with the platform; do not borrow another business operation's destination.

See the [HTTP reference](reference/http.md) for complete fields.

Evidence: DomainControlConfig, ShadowTagInboundFilter, IaisRequestContext, DefaultPlatformChannelHttpService, DefaultHttpService, ChannelTransportCustomization and ChannelInvocationTemplate.
