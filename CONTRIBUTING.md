# Contributing

Contributions are welcome: bug reports, feature requests and pull requests.

## Reporting issues

Use [GitHub Issues](https://github.com/madgeek-arc/eosc-observatory/issues) for bugs, feature requests and
questions. For bugs, include the version, the environment, the steps to reproduce, and the expected
and actual behaviour.

Don't report security vulnerabilities in public issues; follow [SECURITY.md](SECURITY.md) instead.

## Branching

- `main` is the release branch. `develop` is used for integration builds (Jenkins pushes the
  `dev` image tag from SNAPSHOT builds).
- Work on `feature/<name>`, `fix/<name>` or `docs/<name>` branches and open a pull request.
- Without write access to the repository, fork it, create the branch in your fork and open the
  pull request from there.

## Commit messages

[Conventional Commits](https://www.conventionalcommits.org/) are **required**. release-please uses
them to compute versions and build `CHANGELOG.md` sections (`feat` → Features, `fix` → Bug Fixes,
`refactor` → Refactoring, `perf` → Performance Improvements, `docs` → Documentation). Use `!` or a
`BREAKING CHANGE:` footer for breaking changes.

## Pull requests

- Describe the change and its motivation, and link the related issue.
- At least one maintainer review is required before merging.
- Update `README.md`/`docs/` when endpoints, configuration keys, integrations or deployment change.
- Don't edit `CHANGELOG.md` or versions by hand. release-please maintains them.

## Required checks

- `./mvnw -B verify` (unit and integration tests) should pass locally where Docker is available.
- Jenkins also runs OWASP dependency-check and MegaLinter (checkstyle `google_checks.xml`, PMD,
  cspell, jscpd, hadolint, trivy). New findings make the build unstable. Fix them before merging.
- Security-relevant changes (URL rules, `@PreAuthorize`, CORS/CSRF, privacy entries) need tests,
  for example in `SecurityConfigTest` or `MethodSecurityExpressionsServiceTest`.

## Releases and versioning

- [Semantic Versioning](https://semver.org/).
- Merging the release-please PR on `main` tags `<version>` (no `v` prefix), creates the GitHub
  release, and Jenkins pushes the Docker image as `<version>`, `<major.minor>` and `latest`.
