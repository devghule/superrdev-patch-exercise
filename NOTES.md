# Notes

## Summary of changes
1. **Search SQL precedence** (`TaskRepository`): `AND` binds tighter than `OR`, so archived tasks leaked in and the status filter was ignored for title matches (`status=DONE` returned 49 rows, not 5). Added parentheses and an `id` tiebreaker. Mirrored in `db/queries` and both Oracle queries.
2. **Artificial delay** (`TaskController`): a `Thread.sleep` of up to 1s, longer for shorter queries. Removed.
3. **Stale requests and loading/error state** (`useTasks`): slower old responses overwrote newer ones; errors left "Loading..." stuck; old errors were never cleared. Outdated requests are now aborted, the error resets per request, and loading always ends.
4. **Pagination reset** (`App.jsx`): a new search or status kept the old page ("No tasks found"). Now resets to page 1.
5. **Validation**: invalid `status`, `page < 1`, or `pageSize` outside 1–100 returned 500; now 400.

## Testing
Added `spring-boot-starter-test` and 9 MockMvc tests (0/9 passing before, 9/9 after). Verified live with curl, and in the browser: page reset, error recovery, and the race (with a browser-side fetch delay).

## Not changed
- In-memory pagination (loads all matches, then `subList`): needs a `Pageable` refactor.
- Unescaped `%`/`_` in search terms: low impact.
- Dev config (`show-sql`, H2 console, CORS, `System.out`).
- Oracle `v_term VARCHAR2(257)` overflow for long terms: not runnable locally.
- No frontend tests (no test runner exists; avoided new dependencies).

## Biggest remaining risk
In-memory pagination: every request loads all matching rows, so it degrades as data grows.

## Assumptions
Archived tasks never appear in search results. A maximum page size of 100. The deadlines conflict (7 Oct vs 11 Oct); submitting as early as possible.

## AI usage
Claude Code (Claude Opus) did the audit, the reproductions, the fixes and the tests under my direction; I reviewed each step and diff. ChatGPT independently reviewed the approach.
