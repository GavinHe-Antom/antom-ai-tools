# Platform request context

[Integration guide](../README.md) · [Model index](models/README.md) · [Security API](security.md)

`com.alipay.iacqintegrationhub.channel.sdk.context.ChannelRequestContext`

Channel, merchant, and runtime environment bound by the platform host for one synchronous Adapter invocation. This is a plain `ThreadLocal`, not a serializable business DTO or a set of SPI input fields. The Adapter reads it through `current()` and must not select or override platform identity.

## Method contracts

| Method | Return value or behavior | Consumer |
| --- | --- | --- |
| `static ChannelRequestContext current()` | Returns the current thread context; throws `IllegalStateException` if unbound | Adapter (read only) |
| `String getChannelCode()` | Returns the channel identity; throws `IllegalStateException` for null or blank | Adapter (read only) |
| `String getMerchantId()` | Returns the business merchant ID; throws `IllegalStateException` for null or blank | Adapter (read only) |
| `String getRuntimeEnv()` | Returns the runtime environment unchanged; null is allowed and blank values are not rejected | Adapter (read only) |
| `static void bind(String channelCode, String merchantId, String runtimeEnv)` | Replaces the current thread binding without validating parameters or saving an earlier context | Platform host or isolated unit test |
| `static void clear()` | Removes the current thread binding; does not restore an earlier binding | Platform host or isolated unit test |

A missing channel or merchant fails when its getter is called, not during `bind`. A missing context fails at `current()`. Do not catch these failures to fall back to a default merchant, derive identity from the business body, or bind your own context.

## Identity source and lifecycle

The platform binds the context before invoking SPI and clears it in `finally`. The channel comes from the platform module declaration; the merchant comes from the platform-processed `merchantaccount` header. For notifications, the platform handles the merchant in the inbound path and headers. Do not infer the key-query merchant from business JSON, `Order.merchant`, notification bodies, or institution Client-Id.

Although `bind/clear` are public, they are host-lifecycle and isolated-test APIs, not tamper-resistant security isolation within the process. Production Adapter code must not call them or let an external body choose the binding. The platform remains responsible for validating inbound identity and managing routes and key subjects.

- A plain `ThreadLocal` does not propagate to new threads, thread-pool tasks, or asynchronous callbacks. Keep calls synchronous on the original thread; do not copy or rebind context in asynchronous tasks to bypass the host lifecycle.
- There is no nested stack or restoration. Another `bind` overwrites the previous value, and `clear` removes it without restoring an outer identity.
- The host must clean up on success and failure to prevent leakage when pooled threads are reused. Isolated unit tests must do the same.
- Methods with only an SDK contract and no connected platform endpoint are not guaranteed to receive a context.

## Runtime environment does not select routes

[BaseChannelRequest.runtimeEnv](models/BaseChannelRequest.md) and the context runtime environment are platform-provided and may be absent. SDK comments list `test`, `dev`, `pre`, `gray`, and `prod`, but the type is String, without enum validation or an automatic default. `PayRequest.env` describes the buyer terminal; do not confuse them.

Unknown, null, or blank environments must not imply a test environment or enable a sandbox by default. The context contains neither shadow identity nor the selected route. Do not use `runtimeEnv` in place of the platform load-test marker, sandbox decision, or institution destination selection. Platform HTTP still uses the original request and platform-selected destination; see [Routing and HTTP](../06-routing-and-http.md).

## Adapter read example

```java
ChannelRequestContext context = ChannelRequestContext.current();
String merchantId = context.getMerchantId();
String signature = securityService.sign(SecuritySignRequest.builder()
        .merchantId(merchantId)
        .algorithm(SecuritySignAlgorithm.HMAC_SHA256_BASE64)
        .content(canonicalText)
        .build());
```

Security requests still explicitly accept merchantId. CLI-generated security wrappers fill it from the context. Protocol hooks supply content, signatures, result placement, or algorithm parameters without selecting another merchant. Other security API methods and algorithms remain unchanged; see [Security reference](security.md).

## Simulating the host in isolated tests

Unit tests without a real platform Facade may bind synthetic values and clear them in `finally`. This is not a production Adapter template and does not support nested-context restoration.

```java
ChannelRequestContext.bind("examplechannel", "synthetic-merchant", "dev");
try {
    adapter.pay(request);
} finally {
    ChannelRequestContext.clear();
}
```

Cover unbound context, missing channel, missing merchant, missing environment, and cleanup after success and exceptions. Identity fields in input business JSON must not replace the test context. See the [Testing guide](../../TESTING.md) for fixtures and execution.

## Source

SDK 1.5.2; snapshot date 2026-09-29. See `ChannelRequestContext` in [contracts.json](contracts.json) for method and thread semantics. Equal version numbers do not guarantee identical artifact contents; verify the SDK artifact SHA-256 at delivery.

`app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/context/ChannelRequestContext.java`
SHA-256: `4e839ca259774b370119f92ef9b5c75192b4550e5ed0b6203ac481fbc39b0c1d`
