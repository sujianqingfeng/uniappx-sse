# Repository Guidelines

## Project Structure & Module Organization
The repo is organized per platform so you can work in parallel. `sse-uniapp-demo/` and `sse-uniapp-v3-demo/` host the uvue/uts and Vue3 demos, each embedding `uni_modules/sse-plugin/` where the shared UTS, JS, and platform bridges live. Native libraries live in `sse-android/` and `sse-ios-framework/`, while `sse-ios-demo/` consumes the built Framework for smoke tests. `sse-server/` provides the Express-based SSE endpoint, and `scripts/` plus `docs/` collect helper utilities and deeper guides. Keep screenshots and marketing artifacts under `screenshots/`.

## Build, Test & Development Commands
- `cd sse-server && pnpm install && pnpm dev` — boots the local SSE endpoint at `http://localhost:3000/sse`.
- `cd sse-android && ./build-aar.sh -e debug|release|release-minified` — assembles the Android AAR and copies it into the demo plugin libs.
- `cd sse-ios-framework && ./build-framework.sh [-d|-u|--clean]` — builds the Swift framework (device, simulator, or fat).
- Use HBuilderX to run `sse-uniapp-demo` or `sse-uniapp-v3-demo` against Web, Android, or iOS for integration checks.

## Coding Style & Naming Conventions
UTS/TS/JS files use 2-space indentation, semicolons omitted, camelCase for functions, PascalCase for exported types, and keep request identifiers in the `sse_<timestamp>` format used in `sseConnectApi`. Kotlin/Java follows Android defaults (4 spaces, `SseService`-style class names) and Swift mirrors Apple’s guidelines (UpperCamel types, lowerCamel members). Run `pnpm format` if you add Prettier configs; otherwise align with existing files before committing.

## Testing Guidelines
There is no automated unit suite yet, so treat platform demos as acceptance tests. Always run `pnpm dev` in `sse-server/` and point clients to `http://10.0.2.2:3000/sse` when emulating Android. For iOS, verify ATS exceptions before pushing. Name manual test docs `TEST-<module>-<date>.md` under `docs/` when you capture repro steps.

## Commit & Pull Request Guidelines
Follow conventional commits (`chore: readme`, `fix: ios reconnect`) as seen in `git log`. Scope commits per platform to keep diffs reviewable, and reference issues in the body (`Refs #123`). PRs must summarize platform impact, list tested targets (Web/Android/iOS), attach screenshots or console logs for UI/regression changes, and mention any new headers/configurations. Keep branches rebased before requesting review.

## Security & Configuration Tips
Avoid checking real API keys into `uni_modules/sse-plugin/utssdk`. Android debugging relies on `network_security_config.xml`; confirm local domains are whitelisted. iOS builds that need HTTP must update `Info.plist` with ATS overrides and revert before release.
