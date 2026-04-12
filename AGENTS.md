# Repository Guidelines

## Project Structure & Module Organization
The repo now centers on two demo apps plus the local SSE server. `uniappx-sse-playground/` hosts the uni-app X example and embeds the plugin at `uni_modules/sse-plugin/`. `sse-uniapp-v3-demo/` hosts the Vue3 uni-app example and embeds the plugin at `uni_modules/hens-sse/`. Native Android and iOS implementations live inside those plugin directories under `utssdk/app-android/` and `utssdk/app-ios/`, not as standalone top-level projects. `sse-server/` provides the Express-based SSE endpoint, `scripts/` contains helper scripts, `docs/` stores deeper notes, and `screenshots/` is reserved for demo assets.

## Build, Test & Development Commands
- `cd sse-server && pnpm install && pnpm dev` — boots the local SSE endpoint at `http://localhost:3000/sse`.
- Use HBuilderX to run `uniappx-sse-playground` against Web, Android, or iOS for uni-app X integration checks.
- Use HBuilderX to run `sse-uniapp-v3-demo` against Web, Android, or iOS for Vue3 uni-app integration checks.
- `bash scripts/rename-template.sh` — applies the template rename flow when cloning this repo into a new plugin/demo variant.

## Coding Style & Naming Conventions
UTS, TS, and JS files use 2-space indentation, semicolons omitted, camelCase for functions, and PascalCase for exported types. Keep request identifiers in the `sse_<timestamp>` format used by `sseConnectApi`. Kotlin under `utssdk/app-android/` follows standard Android formatting with 4-space indentation and `SSEManager`-style class names. Swift under `utssdk/app-ios/` follows Apple naming conventions. If you add formatting tooling, align it with the existing files rather than reformatting the repo opportunistically.

## Testing Guidelines
There is no automated unit suite yet, so treat the demos as acceptance tests. Start `sse-server/` before manual verification, and use `http://10.0.2.2:3000/sse` when testing Android emulators. Android cleartext access depends on the demo `network_security_config.xml`, currently present under `sse-uniapp-v3-demo/nativeResources/android/res/xml/`. For iOS HTTP testing, verify the app ATS settings before pushing. Name manual test notes `TEST-<module>-<date>.md` under `docs/` when capturing reproducible steps.

## Commit & Pull Request Guidelines
Follow conventional commits such as `chore: readme` or `fix: ios reconnect`, matching the existing history. Keep commits scoped to one concern or one platform area when possible, and reference issues in the body with `Refs #123`. PRs should summarize impact by target platform, list what was tested, and include screenshots or logs for behavior changes. Rebase before requesting review.

## Security & Configuration Tips
Avoid checking real API keys or secrets into either plugin directory, especially under `uniappx-sse-playground/uni_modules/sse-plugin/utssdk/` and `sse-uniapp-v3-demo/uni_modules/hens-sse/utssdk/`. When testing Android against local HTTP endpoints, confirm the demo network security configuration still whitelists the required host. For iOS, keep ATS exceptions limited to development scenarios and review them before release.
