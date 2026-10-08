# AIS SDK HTTP, Routing and Result-Code API Reference

[Domain and HTTP guide](../06-routing-and-http.md) · [Mapping guide](../04-mapping-and-results.md)

## PlatformChannelHttpService

Interface: `com.alipay.iacqintegrationhub.channel.sdk.api.http.PlatformChannelHttpService`. Implemented by the platform and consumed by the Adapter. context is the
original BaseChannelRequest supplied by the Facade: do not create a new identity or change domain/path. Both methods return HttpResponse. HTTP success does not mean transaction success.

### executeFixedUrl

`HttpResponse executeFixedUrl(BaseChannelRequest context, HttpRequest request)`

#### Request

| Parameter | Type | Description |
| --- | --- | --- |
| `context` | `BaseChannelRequest` | Platform request and routing context, including channelCode/domain/path; do not override |
| `request` | `HttpRequest` | Transport attributes without a URL; see the fields below |

#### Response

| Return item | Type | Description |
| --- | --- | --- |
| Return value | HttpResponse | Preserves the status, headers, and body, including non-2xx responses. Parameter/routing validation or other failures may throw exceptions |

### executeDynamicUrl

`HttpResponse executeDynamicUrl(BaseChannelRequest context, Map<String, String> urlParameters, HttpRequest request)`

#### Request

| Parameter | Type | Description |
| --- | --- | --- |
| `context` | `BaseChannelRequest` | Platform request and routing context, including channelCode/domain/path; do not override |
| `urlParameters` | `Map<String, String>` | Raw business values for placeholders; the platform encodes each path segment. Pre-encoded values, null, and dot segments are forbidden |
| `request` | `HttpRequest` | Transport attributes without a URL; see the fields below |

#### Response

| Return item | Type | Description |
| --- | --- | --- |
| Return value | HttpResponse | Preserves the status, headers, and body, including non-2xx responses. Parameter/routing validation or other failures may throw exceptions |

### HttpRequest fields

| Field | Java type | Validation / usage | Source description |
| --- | --- | --- | --- |
| `method` | `String` | Follow the institution protocol; GET/HEAD do not send a body | HTTP method: GET, POST, PUT, DELETE, etc. |
| `headers` | `Map<String, String>` | Host is forbidden; do not forward the entire inbound header collection | Request headers. |
| `body` | `String` | Actual request body; nonempty formParams overrides it | Request body (for POST/PUT). |
| `contentType` | `ContentType` | Takes precedence over Content-Type in headers | Content type; takes precedence over Content-Type in headers. |
| `queryParams` | `Map<String, String>` | Raw values; encoded by the platform | Query parameters, automatically appended to the URL. |
| `formParams` | `Map<String, String>` | Raw values; form-urlencoded by the platform | When nonempty, the platform form-urlencoded-encodes these values as the body, overriding body. If Content-Type is absent, it adds the form content type.<br>Ensure an explicit contentType matches the form body; the platform does not ignore nonempty form parameters merely because the declared type differs. |
| `attributes` | `Map<String, Object>` | Only extensions with explicit platform consumers | Extension attributes retained in the internal platform HTTP request. Arbitrary attributes are not necessarily consumed by interceptors.<br>Do not pass key material or override platform domain/sandbox identity here. New attributes require explicit platform handling. |

### HttpResponse fields

| Field | Java type | Validation / usage | Source description |
| --- | --- | --- | --- |
| `statusCode` | `int` | 2xx/non-2xx; client transport failure may be -1 | Actual HTTP status; -1 denotes a transport failure caught by the client, not a real downstream response status. |
| `headers` | `Map<String, String>` | Single-value Map; not a lossless multi-value header API | Response headers. |
| `body` | `String` | Original response text, including non-2xx responses | Response body. |
| `success` | `boolean` | isSuccess() checks statusCode, not this field | Retained builder field. The public isSuccess() method checks statusCode; this field cannot override a non-2xx result. |
| `costMs` | `long` | Elapsed milliseconds returned by this HTTP execution | Elapsed time in milliseconds for this HTTP execution. The platform outbound digest separately records total retry duration. |

### ContentType

| Value | HTTP type |
| --- | --- |
| `JSON` | "application/json" |
| `FORM_URLENCODED` | "application/x-www-form-urlencoded" |
| `XML` | "application/xml" |

Calls reject unresolved placeholders on the fixed-path API, invalid dynamic parameters, Host overrides, missing context, cross-channel identity, and destination mismatches. See the SDK interface JavaDoc for the actual error types.

## ChannelRouteService (used by platform channel modules)

`ChannelRoute resolve(String channelCode, String operation)` returns the following object. Its platform implementation reads the sandbox identity of the current request.

| Field | Java type | Validation / usage | Source description |
| --- | --- | --- | --- |
| `domain` | `String` | Selected domain for this invocation | Configured channel base URL supplied by the platform. Do not derive it from external headers. |
| `path` | `String` | Selected path/template for this invocation | Operation path, which may retain business placeholders for the platform dynamic URL API to replace. |

The Adapter normally does not call this service itself. The Facade resolves the route before invoking
SPI. Supported keys: pay, authorize, cancel, capture, inquiryPayment, refund, and inquiryRefund. Do not guess missing configuration.

## ResultCodeService

Interface: `com.alipay.iacqintegrationhub.channel.sdk.api.error.ResultCodeService`. The platform queries the result-code configuration.

### mapping (one-level result code)

`Result mapping(String channelCode, String channelResultCode, String channelResultMsg, String api)`

#### Request

| Parameter | Type | Meaning |
| --- | --- | --- |
| `channelCode` | `String` | Platform channel code; must match the result-code configuration |
| `channelResultCode` | `String` | Original institution primary code |
| `channelResultMsg` | `String` | Original institution message; do not append sensitive payload data |
| `api` | `String` | API scope identifier in the result-code configuration; not necessarily the SPI method name |

#### Response

| Return value | Field | Description |
| --- | --- | --- |
| Result | resultStatus / resultCode / resultMessage | One combined-code mapping result, not separate success checks for each level |

### mapping (two-level result code)

`Result mapping(String channelCode, String channelResultCode, String secondResultCode, String channelResultMsg, String api)`

#### Request

| Parameter | Type | Meaning |
| --- | --- | --- |
| `channelCode` | `String` | Platform channel code; must match the result-code configuration |
| `channelResultCode` | `String` | Original institution primary code |
| `secondResultCode` | `String` | Original institution secondary code |
| `channelResultMsg` | `String` | Original institution message; do not append sensitive payload data |
| `api` | `String` | API scope identifier in the result-code configuration; not necessarily the SPI method name |

#### Response

| Return value | Field | Description |
| --- | --- | --- |
| Result | resultStatus / resultCode / resultMessage | One combined-code mapping result, not separate success checks for each level |

### mapping (three-level result code)

`Result mapping(String channelCode, String channelResultCode, String secondResultCode, String thirdResultCode, String channelResultMsg, String api)`

#### Request

| Parameter | Type | Meaning |
| --- | --- | --- |
| `channelCode` | `String` | Platform channel code; must match the result-code configuration |
| `channelResultCode` | `String` | Original institution primary code |
| `secondResultCode` | `String` | Original institution secondary code |
| `thirdResultCode` | `String` | Original institution tertiary code |
| `channelResultMsg` | `String` | Original institution message; do not append sensitive payload data |
| `api` | `String` | API scope identifier in the result-code configuration; not necessarily the SPI method name |

#### Response

| Return value | Field | Description |
| --- | --- | --- |
| Result | resultStatus / resultCode / resultMessage | One combined-code mapping result, not separate success checks for each level |

Multilevel codes are matched as `primary|secondary|tertiary`. A missing mapping or unsuccessful query result yields an unknown result. Actual invocation exceptions may still propagate.
Adapter mappings must handle null and unknown results explicitly; do not fabricate success. See [Result](models/Result.md) for all fields.

## Source

- `app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/api/http/PlatformChannelHttpService.java`
- `app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/api/http/HttpRequest.java`
- `app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/api/http/HttpResponse.java`
- `app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/api/config/ChannelRouteService.java`
- `app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/api/config/ChannelRoute.java`
- `app/common/common-sdk/src/main/java/com/alipay/iacqintegrationhub/channel/sdk/api/error/ResultCodeService.java`
