# First Configuration Flow Design

## Context

fnOSTV already opens the account form when no usable server profile exists. Before this change, saving the first valid account could leave the user inside the settings management screen, which added an extra remote-control step before the app continued to login and show the native home page.

## Goals

- Make first launch configuration feel like a direct setup-to-home flow.
- Preserve the existing settings management behavior for users who already have a valid profile.
- Keep connection-error recovery conservative: after editing an existing profile, stay in settings so users can keep adjusting libraries or account details.
- Cover the routing decision with JVM unit tests.

## Non-Goals

- No multi-page setup wizard in this slice.
- No QR-code pairing or server discovery.
- No change to credential validation beyond the existing profile validator.
- No new visual design system work.

## Design

`SettingsActivity` records whether it was opened without an existing valid profile. When the account editor saves a valid profile:

- First configuration: return `RESULT_OK` and finish the settings activity. `MainActivity.onActivityResult` already reloads the profile and starts native authentication, so the user continues toward the home screen.
- Existing profile or connection-error edit: stay inside native settings and show the saved status, preserving the current management workflow.
- Invalid or incomplete profile: remain in the account editor.

The decision is extracted into `SettingsCompletionFlow`, a small pure Java helper that can be tested without Android lifecycle dependencies.

## Success Criteria

- Tests prove first configuration finishes settings after a valid profile save.
- Tests prove existing-profile account edits remain in settings.
- Tests prove invalid profile saves keep the account editor open.
- Existing unit tests and debug build still pass.

