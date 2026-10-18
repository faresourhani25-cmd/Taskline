# Taskline

A to-do and goals app for Android, written in Kotlin with Jetpack Compose.
It has a list view, a timeline view for tasks with a start and an end date, and goals with progress.
The look is dark and soft: a near-black background, rounded cards and pills, orange for tasks in progress, green for completed ones and a white "+" button. It is always dark.

Tasks are saved on the phone only.

## Get the app

Every push to `main` builds the app on GitHub.
Open the "Releases" page of this repository, open the "Taskline (latest build)" release and download `Taskline.apk`.
Open the file on the phone and allow installs from that source when Android asks.

The app is called "Task Manager" on the phone and installs next to the earlier web-view version.

## About the signing key

`app/debug.keystore` is a standard debug key, kept in the repository so each build can be installed over the last one.
It is fine for an app you install yourself. Do not publish the app to a store with it.

## Project layout

- `Logic.kt` holds the rules (status, grouping, timeline range, progress) with no Android code.
- `Store.kt` saves and loads the data.
- `ListScreen.kt`, `TimelineScreen.kt`, `GoalsScreen.kt` and `Dialogs.kt` are the screens and forms.
