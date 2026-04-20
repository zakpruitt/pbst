---
name: Java service/architecture style
description: No unnecessary interfaces, mappers only where they add value, thin controllers, business logic in services
type: feedback
---

Don't create interfaces for services that will only have one implementation. Use concrete classes directly.

Only use inheritance and MapStruct mappers where they genuinely reduce boilerplate — not as a blanket pattern.

Keep business logic, repo calls, and mapping in the service layer. Controllers should only call service methods and handle HTTP response logic (redirects, model attributes, view names).

**Why:** User finds over-engineering annoying — interfaces-for-everything is ceremony without benefit when there's a single impl.

**How to apply:** When building Java services, default to concrete classes. Only introduce an interface when there's a real second implementation or a testing seam that can't be handled by mocking the concrete class.
