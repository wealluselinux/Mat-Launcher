Put the release notes for each version in `v<versionName>.md` (for example `v1.6.0.md`).
When `versionName` in `app/build.gradle.kts` changes and is pushed to `main`, the build workflow
creates the GitHub release for it, attaches both APKs and uses that file as the notes
(falling back to GitHub's generated notes if the file is missing).
