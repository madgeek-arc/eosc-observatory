# Security Policy

## Supported Versions

| Version | Supported |
|---|---|
| Latest 4.x release | :white_check_mark: |
| Older releases | :x: |

Fixes ship in a new release; deployed environments are expected to run the latest one.

## Reporting a Vulnerability

Report vulnerabilities privately through GitHub: open the repository's **Security** tab and choose
**Report a vulnerability** ([direct link](https://github.com/madgeek-arc/eosc-observatory/security/advisories/new)).
Don't report them in public issues, pull requests or discussions.

Include:

- the affected version or commit;
- steps to reproduce, or a proof of concept;
- the impact you expect.

The maintainers follow up in the private advisory, fix the issue in the supported versions, and publish
the advisory once a fixed release is available.

## Scope

This repository covers the backend service. A vulnerability in a library it embeds (registry,
catalogue, authorization, document-analyzer, the mailer and messaging clients, re-captcha) should be
reported to that library's repository, and here as well if the version this service pins is affected.

[docs/security-model.md](docs/security-model.md) describes the component's security model and the
settings a deployment should use.
