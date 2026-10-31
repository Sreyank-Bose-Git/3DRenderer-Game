# Contributing Guide

Thanks for your interest in contributing to **OpenStreets**.

## Before You Start

- Check existing issues and pull requests to avoid duplicate work.
- For larger changes, open an issue first to discuss scope and direction.

## Local Setup

1. Fork and clone the repository.
2. Open it in your Java IDE.
3. Ensure `src` is configured as source root.
4. Compile to validate:

```bash
javac -d out $(find src -name "*.java")
```

## Development Expectations

- Keep changes focused and small.
- Follow existing code style and package organization.
- Avoid unrelated refactors in feature/fix PRs.
- Update documentation when behavior changes.

## Pull Request Checklist

- [ ] Code compiles successfully
- [ ] Changes are scoped to the intended problem
- [ ] Documentation is updated (if needed)
- [ ] No secrets or credentials are introduced

## Commit and PR Notes

- Use clear commit messages.
- Describe the motivation and approach in your PR.
- Attach screenshots or short recordings when visual behavior changes.
