# Obsiditter

Obsiditter is an Android memo application designed to look and feel like a microblogging timeline (e.g., Twitter/X), but it saves all your data locally as Markdown files.
This makes it fully compatible with [Obsidian](https://obsidian.md/) and other Markdown-based knowledge base tools.

## Features

- Timeline Interface: View your daily notes in a familiar reverse-chronological timeline format.
- Markdown Storage: All notes are stored in plain text Markdown files (`YYYY-MM-DD.md`) in a user-selected directory.
- Obsidian Compatible: Designed to work seamlessly with Obsidian's Daily Notes plugin.
  - Notes are appended to a `## Journal` section.
  - Format: `- HH:mm Content #tags`.
- Local & Private: No cloud sync required. You own your data.
- Storage Access Framework (SAF): Securely access and modify files in any folder on your device (including SD cards or synced folders like Syncthing).
- Edge-to-Edge Design: Modern UI with full support for gesture navigation and edge-to-edge display.

## Tech Stack

- Language: Kotlin
- UI Framework: Jetpack Compose (Material 3)
- Architecture: MVVM
- File I/O: Android Storage Access Framework (SAF), `DocumentFile`
- Markdown Parsing: `org.jetbrains:markdown`

## Getting Started

### Prerequisites

- Android Studio Koala or newer.
- JDK 17+.

### Installation

1.  Clone the repository:
    ````bash
    git clone https://github.com/ikorihn/obsidian-memos.git
    ```
    ````
2.  Open the project in Android Studio.
3.  Sync Gradle project.
4.  Run on an emulator or physical device.

### Usage

1.  Initial Setup: Upon first launch, tap "Select Folder" to choose a directory where your Markdown files will be stored. This can be your existing Obsidian vault's
    "Daily Notes" folder.
2.  Creating Memos:
    - Use the input bar at the bottom of the Home screen.
    - Type your note and add hashtags if desired.
    - Tap "Post" to append it to today's daily note file.
3.  Editing/Deleting:
    - Tap the "Edit" (pencil) icon on a note to modify its content inline.
    - Tap the "Delete" (trash can) icon to remove a note.

## File Format

Obsiditter reads and writes to files named `YYYY-MM-DD.md`.
It specifically looks for or creates a `## Journal` heading and manages list items under it.

Example `2024-01-01.md`:

```markdown
---
date: "2024-01-01"
tags: [daily]
---

# 2024-01-01

## Journal

- 09:00 Started the day with coffee. #morning
- 12:30 Lunch at the new place.
- 18:00 Working on Obsiditter app. #coding
```
