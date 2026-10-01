# Platform-Supplied Generic Scaffold: Check When Applicable

Read this only when the platform supplied the target project directly and it does not follow the CLI's per-method Mapping structure. This Skill does not include that project. Follow the user's actual files and SDK; names below are not directories that must be created.

## Differences from CLI-generated projects

| Check | Possible generic-scaffold structure | Integration action |
| --- | --- | --- |
| Mapping | PaymentApiCustomization, RefundApiCustomization, NotificationApiCustomization | Implement rules in existing extensions; do not also recreate the CLI Mapping layer |
| SPI | Some methods may still return null | Wire selected methods; restore SDK default exceptions for unsupported optional methods |
| Transaction extensions | pay()/authorize()/cancel()/capture()/inquiryPayment()/refund()/inquiryRefund() | Check actual methods rather than guessing helper names from SPI names |
| Notification extensions | paymentNotification()/captureNotification(); refund extension may be absent | Implement and wire ChannelNotificationExtension<RefundNotifyRequest> when needed |
| Result mapping | ChannelResultMapper and example success codes | Verify standard results per API; replacing one success-code constant is not complete mapping |
| Security helpers | May not forward SecurityCipherParameters | Complete forwarding against the actual SDK or call platform security directly; do not change protocol algorithms |
| Tests | PaymentTemplateScenarioTest, RefundTemplateScenarioTest, SecurityExamplesTest and similar | Reference only existing files; acceptance tests must call real SPIs, not copied substitutes |

The platform supplies the SDK JAR/POM separately, matching the target project's version. Do not reuse an unknown-version local file repository or change version strings alone.

## Testing and delivery

Read the target POM, test classes and resources before choosing `mvn clean verify` or an existing `-Dtest` class. Whether tests start Spring depends on the project. Local tests do not start SOFABoot or connect to real institutions.
For markers such as ADAPTER_READY, determine whether an executable validator checks them; a constant value is not evidence of a passing test.

Without adapter-spec.json and generation-lock.json required by the CLI, do not assume `ais package` can validate the project. First run real SPI tests with Maven and inspect dependencies/plain JARs, then record evidence using the [delivery checklist](delivery.md). Do not fabricate lock files to bypass checks.
