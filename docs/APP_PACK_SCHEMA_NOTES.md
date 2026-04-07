# App Pack Schema Notes

This is not the final schema. This is a first-pass checklist for the contract Synapse should validate.

## Required top-level fields
- appId
- packageName
- displayName
- version
- developer
- category
- capabilities
- tools
- events
- compatibility

## Recommended optional fields
- state
- examples
- policies
- signature
- trustMetadata
- uiHints
- routingHints

## Tool quality rules
- names must be specific
- descriptions must be concrete
- parameter types must be explicit
- dangerous tools must be flagged
- required capabilities must be declared
- version drift must be detectable

## Event quality rules
- event names should be domain-specific
- schemas must be typed
- noisy events should be rate-limited
- provenance and timestamping should be mandatory

## Policy examples
- requires_confirmation
- max_calls_per_minute
- background_allowed
- deny_when_screen_locked
- deny_without_user_presence
- high_risk

## Trust / signature direction
Eventually support:
- signed app packs
- developer verification
- hash checking
- local trust state
- revocation
