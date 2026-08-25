@CLAUDE.md

## Project context
This repository contains the full application code and documentation for this project.

Claude Code should treat documentation as part of the implementation, not a separate task.

## Operating principles
- Read the relevant existing docs before making changes.
- After any code change, update all impacted documentation in the same change set.
- Do not guess project behavior, architecture, config, or APIs when evidence is missing.
- If something cannot be verified from the repository, write `Unknown / Requires Confirmation`.
- Prefer small, precise edits over broad rewrites.
- Keep documentation current with the codebase, especially when behavior changes.

## Documentation rule
When code changes affect any of the following, update the corresponding docs immediately:
- API endpoints or request/response shapes.
- UI routes, screens, flows, or labels.
- Data models, database schema, migrations, or seed data.
- Environment variables, secrets, or configuration.
- Build, test, lint, deploy, or release commands.
- Authentication, authorization, or security behavior.
- External integrations, webhooks, jobs, queues, or scheduled tasks.
- Observability, logging, alerts, or troubleshooting steps.
- Architecture, dependencies, or module boundaries.

If a code change introduces a user-visible or developer-visible behavior change, docs must be updated in the same PR.

## How to update docs
- Update the most specific doc first.
- Update `README.md` if onboarding, setup, or project summary changes.
- Update files under `docs/` for deeper technical changes.
- Keep examples aligned with the actual code.
- Remove stale instructions when they are no longer true.
- Add "Last verified" dates or commit references where useful.

## Documentation standards
- Be accurate, concise, and specific.
- Use markdown headings and short sections.
- Include file paths, commands, and examples when relevant.
- Do not include secrets, tokens, private URLs, or personal data.
- Separate verified facts from assumptions.
- Use code references where possible.
- Prefer documented behavior over inferred behavior.

## Change workflow
For every code change:
1. Inspect the diff and identify affected docs.
2. Update docs in the same task.
3. Re-check for inconsistencies between code and docs.
4. Ensure examples, commands, and config values still match the code.
5. If docs are intentionally not updated, explain why in the final response.

## When to ask questions
Pause and ask for confirmation if:
- A change depends on undocumented product decisions.
- There are multiple possible interpretations of the intended behavior.
- Required config or deployment details are not present in the repo.
- Updating docs would require inventing missing facts.

## Preferred repo conventions
- Treat `CLAUDE.md` as project policy for Claude Code.
- Keep doc updates aligned with code changes in the same branch.
- Do not leave documentation cleanup for a later task if it is directly impacted by the change.

## Priority
When code and documentation conflict, the codebase is the source of truth.
Flag the mismatch and fix the docs immediately.

## Notes for Claude
Before finishing any task, check:
- Did behavior change?
- Did configuration change?
- Did setup or deployment change?
- Did tests or validation steps change?
- Did any doc become stale?

If yes to any of the above, update docs before responding.