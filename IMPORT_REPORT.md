# Independent snapshot import — 2026-11-08

Source: https://github.com/Gathin23/cleanyourtv

Pinned commit: `294d3d59dc2514e69fed557f46c1ecdfb87a95ec`

Destination: https://github.com/killupdate/CleanMyTV

The local destination folder and GitHub repository were empty before import. No existing user files were replaced. The earlier full-history clone was no longer running when checked. No source Git history or upstream remote is included.

## Completeness

All **3494** source Git blobs were checked against the recursive GitHub tree at the pinned commit. Archive CRLF conversion in 36 Windows batch files was normalized to Git's stored LF before comparing blob SHA-1 values.

- **3350** source files remain byte-for-byte identical to their Git blobs.
- **141** files have intentional changes, recorded individually in `IMPORT_MANIFEST.json`.
- **3** Actions workflows were moved byte-for-byte to inert `.txt` records under `docs/import-disabled/github-actions/`.
- The original Dockerfile is additionally retained byte-for-byte as `docs/import-disabled/Dockerfile.txt`.
- `IMPORT_MANIFEST.json` and this report are new provenance documents.
- Final snapshot: **3497 files**, including those three added documents/records.

MIT `LICENSE`, original copyright attribution, font licenses and all other existing license files are unchanged. All source directories, tests, add-ons, examples, translations and assets are retained.

## Intentional changes

- README identifies the source commit, original author, independent ownership and explicit-owner-permission update policy. It marks inherited instructions as reference only and the API 24 adaptation as pending.
- AGENTS.md repeats that owner policy before the original project guidance.
- No active `.github/workflows` remain. The three original workflows are inert text records.
- 36 POSIX and 36 Windows Gradle wrappers stop before starting Java or any bootstrap code. Their original bodies and license headers remain below the unconditional stop.
- All 28 wrapper distribution URLs point to a nonexistent local file rather than an external distribution server.
- All 36 Gradle settings files force offline dependency resolution. An approved local toolchain/cache will be needed for future builds.
- Both Docker helper entry points stop before invoking Docker. The active Dockerfile uses `FROM scratch` and contains no download or execution steps; the original downloading recipe is an inert reference.
- Perfetto reporting refuses to start without an explicitly supplied local engine, before importing the Perfetto package. Automatic engine download is disabled.

## Verification and limits

Checks were implemented outside the imported project code: source blob hashes, file accounting, unchanged LICENSE, inactive workflow locations, wrapper stop placement, local distribution URLs, offline Gradle settings and Python AST parsing. No inherited scripts, tests, Gradle builds, Docker builds or installation commands were executed. No TV was contacted or changed.

This is an import and a focused removal of automatic executable bootstrap paths, **not a completed security audit or proof that all inherited code is safe**. Historical documentation can still contain original build/install commands and external links; the policy at the top of README takes precedence. Optional add-ons retain network functionality for their data sources, which requires review in the later audit. Manual installation of dependencies is not authorized by this import.

The launcher's source minSdk remains 26. ASANO 32LH7030S, Android 7.0 / API 24, is not yet supported or tested. Audit, compatibility adaptation, dependency approval and TV installation are separate steps requiring the owner's explicit authorization. All future updates require explicit owner permission.
