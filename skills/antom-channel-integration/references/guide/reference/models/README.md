# AIS SDK Requests, Responses and Shared Objects

[API index](../spi/README.md) · [Integration guide](../../README.md)

Each page describes one type. Tables include inherited fields and links to nested objects. Determine requiredness from the agreed contract, not from the mere existence of a field.

See [ChannelRequestContext](../context.md) for reading the platform channel, merchant and runtime environment during an invocation. It is a thread context, not a serializable request/response DTO.

## Requests, responses and base types

- [BaseChannelRequest](BaseChannelRequest.md)
- [BaseIpayRequest](BaseIpayRequest.md)
- [BaseResponse](BaseResponse.md)
- [AcsUrlCallbackRequest](AcsUrlCallbackRequest.md)
- [AcsUrlCallbackResponse](AcsUrlCallbackResponse.md)
- [CaptureNotifyRequest](CaptureNotifyRequest.md)
- [PaymentNotifyRequest](PaymentNotifyRequest.md)
- [RefundNotifyRequest](RefundNotifyRequest.md)
- [NotifyResponse](NotifyResponse.md)
- [AuthenticateAuthorizeRequest](AuthenticateAuthorizeRequest.md)
- [AuthorizeRequest](AuthorizeRequest.md) — retained type, not an argument of the current authorization SPI
- [CancelRequest](CancelRequest.md)
- [CaptureRequest](CaptureRequest.md)
- [InquiryPaymentRequest](InquiryPaymentRequest.md)
- [PayRequest](PayRequest.md)
- [AuthenticateAuthorizeResponse](AuthenticateAuthorizeResponse.md)
- [AuthorizeResponse](AuthorizeResponse.md) — retained type, not an argument of the current authorization SPI
- [CancelResponse](CancelResponse.md)
- [CaptureResponse](CaptureResponse.md)
- [InquiryPaymentResponse](InquiryPaymentResponse.md)
- [PayResponse](PayResponse.md)
- [InquiryRefundRequest](InquiryRefundRequest.md)
- [RefundRequest](RefundRequest.md)
- [InquiryRefundResponse](InquiryRefundResponse.md)
- [RefundResponse](RefundResponse.md)

## Shared business objects

- [ActionForm](ActionForm.md)
- [ChallengeActionForm](ChallengeActionForm.md)
- [PaymentCodeForm](PaymentCodeForm.md)
- [RedirectActionForm](RedirectActionForm.md)
- [Address](Address.md)
- [Amount](Amount.md)
- [BrowserInfo](BrowserInfo.md)
- [Env](Env.md)
- [Result](Result.md)
- [UserName](UserName.md)
- [CodeDetail](CodeDetail.md)
- [AcquirerInfo](AcquirerInfo.md)
- [Buyer](Buyer.md)
- [CreditPayPlanNow](CreditPayPlanNow.md)
- [Good](Good.md)
- [Merchant](Merchant.md)
- [Order](Order.md)
- [OrderCodeForm](OrderCodeForm.md)
- [PaymentFactor](PaymentFactor.md)
- [PaymentMethod](PaymentMethod.md)
- [PaymentMethodMetadata](PaymentMethodMetadata.md)
- [PaymentResultInfo](PaymentResultInfo.md)
- [Shipping](Shipping.md)
- [ShippingAddress](ShippingAddress.md)
- [ShippingName](ShippingName.md)
- [ThreeDSResult](ThreeDSResult.md)
- [Transaction](Transaction.md)

See the [Security reference](../security.md) for security DTOs and enums, the [HTTP reference](../http.md) for HTTP DTOs, and the [Enum reference](../enums.md) for business enums.
