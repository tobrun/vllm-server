# Repository Guidelines

## Project Structure & Module Organization
- `main.py`: FastAPI backend exposing vLLM control endpoints on port `9090`.
- Root config/templates: `config.yaml.example`, `start_model.sh.example`, `*.service.example`, `sudoers.vllm-manager.example`.
- `web/`: Next.js 16 + TypeScript web dashboard (`src/app`, `src/components`, `src/hooks`, `src/lib`).
- `android/`: Native Android app (Kotlin + Compose) using clean layers under `domain/`, `data/`, `presentation/`, and `di/`.
- Operational docs: `README.md` and `POST_INSTALL.md`.

## Build, Test, and Development Commands
- Backend setup: `python3 -m venv .venv && .venv/bin/pip install -r requirements.txt`.
- Run backend locally: `.venv/bin/python main.py`.
- Web dev server: `cd web && npm install && npm run dev`.
- Web production build: `cd web && npm run build && npm run start`.
- Web lint: `cd web && npm run lint`.
- Android debug APK: `cd android && ./gradlew assembleDebug`.
- Android install on device: `cd android && ./gradlew installDebug`.

## Coding Style & Naming Conventions
- Python: follow PEP 8, 4-space indentation, `snake_case` for functions/variables, `PascalCase` for classes.
- TypeScript/React: prefer typed APIs, `PascalCase` components, `kebab-case` filenames for reusable UI files (for example `status-section.tsx`).
- Kotlin: 4-space indentation, `PascalCase` classes, `camelCase` members, package paths under `com.nurbot.vllmremote`.
- Keep modules focused: API access in `web/src/lib/api.ts`, domain/use-case logic in Android `domain/usecase`.

## Testing Guidelines
- No automated test suite is committed yet for backend, web, or Android.
- Minimum pre-PR checks: run `npm run lint` in `web/`, build Android with `./gradlew assembleDebug`, and smoke-test backend endpoints with `curl` (see `POST_INSTALL.md`).
- When adding tests, place them near each target stack’s conventions (`web/src/**`, `android/app/src/test`, backend `tests/`).

## Commit & Pull Request Guidelines
- Current history favors short, imperative commit subjects (for example `add web client`, `update start_model.sh.example`).
- Keep subject lines concise and action-oriented; scope by area when helpful (for example `web: refine status polling`).
- PRs should include: purpose, impacted areas (`backend`, `web`, `android`), manual verification steps, and screenshots for UI changes.

## Security & Configuration Tips
- This service has no built-in auth; only run it behind a trusted network (for example Tailscale).
- Never commit real `config.yaml`, secrets, or host-specific service files; commit only sanitized examples.
