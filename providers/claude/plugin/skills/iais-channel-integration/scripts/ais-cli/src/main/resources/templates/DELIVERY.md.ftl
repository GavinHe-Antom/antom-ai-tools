# Integration delivery checklist

- [ ] SPI scope: confirm the methods in adapter-spec.json and platform capability registration; do not register unselected methods.
- [ ] Inline field mappings: complete each selected SPI method's anonymous extension in ChannelPaymentService, ChannelRefundService or ChannelNotificationService; document standard/institution fields, requiredness, units, conversion rules and examples. No standalone Mapping classes are required.
- [ ] Security design: document each direction's algorithm, canonical text, encoding, result placement, IV/randomness transfer and rationale for platform or custom computation. Deliver keys separately through the platform's approved secure channel, never in this document.
- [ ] Result-code mappings: institution primary/secondary codes → platform-standard primary/secondary codes. Unknown, processing and failure branches must not default to success.
- [ ] Routing materials: provide production/sandbox domains, path templates and HTTP methods for platform configuration; do not hard-code them in the adapter.
- [ ] Real-SPI delivery fixtures: complete input, context, expected-request, response, response-headers, expected-result and security for every method. Context supplies independent synthetic platform identity; retain complete HTTP and typed SDK parameter assertions.
- [ ] Platform request context: adapters only read ChannelRequestContext and use its merchantId in security requests. Independent tests simulate bind/finally clear, including missing identity and cleanup after exceptions. runtimeEnv does not replace sandbox routing.
- [ ] Test evidence: pass structure tests, synthetic security-control-flow tests and real-SPI inline-mapping delivery tests separately. Add institution success/failure/missing-field/non-2xx cases and independent algorithm vectors. Synthetic security tests do not establish institution algorithm compatibility.
- [ ] Dependencies: document each added library's version, purpose and license; the ordinary JAR must not contain the SDK or host runtime.
- [ ] Local reports: include target/ais-delivery/report.json, dependency-audit.json, dependency-tree.txt, resolved-dependencies.txt, the ordinary JAR and SHA-256. Obtain human confirmation that added dependencies are available in the host runtime.
- [ ] Platform assembly, institution integration testing and release review are complete; local CLI success does not replace these steps.
