# Using the Skill with an Agent

[SKILL.md](../../SKILL.md) is the entry point and defines task-based reading paths and essential boundaries.
Follow the [implementation workflow](../implementation-workflow.md) for code work; collect missing information with the [rules template](../../assets/integration-rules-template.md).

## Contracts and business rules

- SDK types, fields and methods come from the target project's actual source or delivered JAR; use [contracts.json](reference/contracts.json) for indexing and comparison.
- Institution semantics come from user-confirmed mappings, security rules and institution protocols, not assumptions based on platform examples.
- Payment/refund field business rules are in their SPI pages and field-notes.json. A required parent object does not make every child field required.
- Document injected fields by lifecycle stage; do not promise every BaseChannelRequest field is populated at every entry point.
- Read only the selected SPI, its referenced models and relevant topics; loading every complete type is unnecessary.

Code tasks should produce actual file changes, real SPI tests and unresolved-item reports. Knowledge questions do not require project changes. See [delivery requirements](../delivery.md) for completion status.
