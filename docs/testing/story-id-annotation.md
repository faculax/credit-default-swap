# StoryId Annotation Guide

The `@StoryId` annotation standardises how backend tests advertise story coverage in Allure reports. It applies four labels to every annotated test:

- `story` – canonical backlog identifier (for example `UTS-220`)
- `testType` – test classification such as unit or integration
- `service` – platform domain (backend, frontend, etc.)
- `microservice` – specific service or module that owns the test (for example `cds-platform`)

## Usage

```java
import com.creditdefaultswap.platform.testing.story.StoryId;

class ExampleTest {

    @Test
    @StoryId(value = "UTS-210", testType = StoryId.TestType.UNIT, microservice = "cds-platform")
    void calculatesSettlementForHappyPath() {
        // test logic
    }
}
```

Annotate either individual methods or a test class. Method-level annotations override class-level defaults.

## Test Type Options

| Enum | Label value | When to use |
| --- | --- | --- |
| `StoryId.TestType.UNIT` | `unit` | Pure unit tests with mocked collaborators |
| `StoryId.TestType.INTEGRATION` | `integration` | Spring slices or repository tests hitting multiple components |
| `StoryId.TestType.CONTRACT` | `contract` | Producer/consumer contract suites |

## Service and Microservice Labels

- `service` defaults to `backend`. Change it if a shared module exercises other platform domains:

    ```java
    @StoryId(value = "UTS-305", service = StoryId.Service.FRONTEND)
    ```

- `microservice` defaults to blank. Provide the owning service identifier so Allure traces the result back to the correct deployment unit:

    ```java
    @StoryId(value = "UTS-402", microservice = "gateway")
    ```

Valid identifiers are documented in `unified-testing-config/label-schema.json` under `services` and `microservices`.

## CI Validation

Run `node scripts/validate-story-ids.mjs --branch <branch>` to verify that new story identifiers follow the canonical pattern before raising a pull request.

## Traceability Matrix Export

Generate a machine-readable mapping of story coverage by running:

```sh
node scripts/export-traceability-matrix.mjs \
    --results-dir backend/allure-results \
    --expected-catalog unified-testing-config/story-catalog.json
```

The script produces `traceability-matrix.json` by default, listing each story, linked tests, and their latest outcomes. Provide `--format csv` for spreadsheet-friendly exports. Stories defined in `unified-testing-config/story-catalog.json` that lack annotated tests appear in the `missingStories` section (and will fail the command when `--fail-on-missing` is supplied). Tests tagged with story IDs not yet registered in the catalog are reported as `orphanStories` for follow-up.

## Tips

- Keep story IDs in sync with the acceptance criteria defined in `unified-testing-stories`.
- For parameterised tests, place `@StoryId` on the test method so every invocation emits the same labels.
- When multiple tests satisfy the same story, reuse the same identifier to retain traceability in export reports.
