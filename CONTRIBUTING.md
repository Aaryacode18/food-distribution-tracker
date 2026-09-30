# Contributing

## Branch naming

| Prefix | Use for | Merges into |
|---|---|---|
| `feature/<short-description>` | New MVP behaviour | `develop` |
| `fix/<short-description>` | Bug fix | `develop` |
| `chore/<short-description>` | Build, CI, or docs only | `develop` |

Rules:

- Lowercase, hyphen-separated. No spaces, no underscores, no ticket-only names.
- One feature per branch. If a change does not fit the branch's purpose, open
  another branch.
- Branch from an up-to-date `develop`.

Examples in use in this repository:

```
feature/mvp-enhancements
feature/live-inventory-dashboard
```

## Commit messages

Use the imperative mood and keep the subject under ~72 characters.

```
Fix delivery summary and add status drill-down
Add Reorder Level column to Live Inventory
```

## Pull requests

1. Push the branch and open a PR against `develop` using the PR template.
2. Fill in the checklist. `mvn clean package` and `mvn test` must pass.
3. Get at least one review comment before merging.
4. Merge with a merge commit so the branch history stays visible.

## Before you push

```bash
mvn clean package
```

Then confirm the change on
http://localhost:8081/food-distribution-tracker/ after deploying the WAR.

## Releases

Release baselines live on `main`. Cut an annotated tag:

```bash
git tag -a v1.0.0 -m "MVP release: <summary>"
git push origin v1.0.0
```
