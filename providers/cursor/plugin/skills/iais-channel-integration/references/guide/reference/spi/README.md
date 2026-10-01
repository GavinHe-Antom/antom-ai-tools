# SPI contract index

There are **4 interfaces and 14 methods**. Seven transaction methods and three notification methods have platform endpoints. The ACS endpoint bypasses SPI; the remaining three methods exist only as SDK
contracts. An available endpoint does not mean every channel has registered it or completed the generated Mapping and security hooks.

| Interface | Method / method reference | Java requirement | Platform availability | Route key |
| --- | --- | --- | --- | --- |
| PaymentService | [pay](pay.md) | Required abstract method within this interface | Endpoint available; registration required | pay |
| PaymentService | [authenticateAuthorize](authenticateAuthorize.md) | Optional; throws by default | Endpoint available; registration required | authorize |
| PaymentService | [cancel](cancel.md) | Optional; throws by default | Endpoint available; registration required | cancel |
| PaymentService | [capture](capture.md) | Optional; throws by default | Endpoint available; registration required | capture |
| PaymentService | [inquiryPayment](inquiryPayment.md) | Optional; throws by default | Endpoint available; registration required | inquiryPayment |
| RefundService | [refund](refund.md) | Required abstract method within this interface | Endpoint available; registration required | refund |
| RefundService | [inquiryRefund](inquiryRefund.md) | Optional; throws by default | Endpoint available; registration required | inquiryRefund |
| NotificationService | [notifyPayment](notifyPayment.md) | Required abstract method within this interface | Endpoint available; registration required | — |
| NotificationService | [notifyCapture](notifyCapture.md) | Optional; throws by default | Endpoint available; registration required | — |
| NotificationService | [notifyRefund](notifyRefund.md) | Optional; throws by default | Endpoint available; registration required | — |
| NotificationService | [notifyOnlineBankPayment](notifyOnlineBankPayment.md) | Optional; throws by default | SDK only | — |
| NotificationService | [notifyReceivePayment](notifyReceivePayment.md) | Optional; throws by default | SDK only | — |
| AcsUrlCallbackService | [acsUrlCallback](acsUrlCallback.md) | Optional; throws by default | Matching endpoint bypasses SPI | — |
| AcsUrlCallbackService | [onlineBankUrlCallback](onlineBankUrlCallback.md) | Optional; throws by default | SDK only | — |

Each method reference lists request, response, and inherited fields; see the [Model index](../models/README.md) for nested objects. Do not provide a
bean for an unsupported capability category. Leave unsupported optional methods with their default unsupported behavior; do not return null as a placeholder for completion. VaultService and ChannelDisputeService are excluded because they are not part of the
SDK SPI.
`AuthorizeRequest/Response` are not the parameters of `authenticateAuthorize`; see [Current limitations](../../11-current-limits.md).

Each method page lists request and response fields, requirements, and business meanings. See [Payment field definitions](../payment-sources.md) and [Refund field definitions](../refund-sources.md) for the respective business constraints.
The Adapter must enforce required-field and length constraints; Java types and scaffold validate examples alone are insufficient.

## Platform-provided fields

"Provided by the platform; present" applies only at the invocation stage stated in the table. It does not guarantee that all base-class fields are non-null in every scenario.

| Scenario | Field | Population rule |
| --- | --- | --- |
| Connected channel API request | requestHeaders, channelCode, rawBody, domain, path | Provided by the platform; present. The Adapter must not select channel identity or destination. |
| Connected institution notification input | requestHeaders, channelCode, rawBody | Provided by the platform; present |
| Connected channel API request or institution notification input | runtimeEnv | Provided by the platform; may be absent. Not used for shadow/sandbox routing. |
| Institution notification input | domain, path | Not applicable; no channel destination is populated. |
| Notification SPI output | requestHeaders, rawBody | Preserved by the Adapter from the notification input. |
| Notification SPI output | domain | Populated by the platform before forwarding to iPay; may be null when SPI returns. |
| ACS HTTP callback | requestHeaders | Provided by the platform; present. The endpoint does not call ACS SPI. |
| ACS HTTP callback | rawBody / domain | rawBody is not applicable; the platform fills domain before calling iPay. |
| SPI without a connected endpoint | Base-class fields | Automatic injection is not guaranteed; do not treat it as an available capability. |

See [ChannelRequestContext](../context.md) for platform identity access and missing-value behavior. The Adapter reads the context only; SPI methods without connected endpoints are not guaranteed to have a context.

A present requestHeaders field means the collection is supplied, not that any particular header exists. rawBody contains the received text; the platform does not automatically complete its business fields.
