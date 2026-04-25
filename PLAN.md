# Libre Notes Plan

## Product Direction

Libre Notes is a local-first Markdown notes app for mobile and desktop. Notes are saved as plain `.md` files with nearby asset folders, while the app gives users friendly editing, preview, export, and file-management tools.

## Platform Direction

- First build target: web prototype, then Flutter app.
- Primary dev environment: WSL for repository work and dev commands.
- Mobile layout: focused editor, bottom navigation, compact formatting tools.
- Desktop layout: folder sidebar, notes list, editor/preview workspace, and optional inspector panels.

## Core UX Decisions

- Support raw Markdown, visual assist editing, rendered preview, and desktop split view.
- Add Auto Format behavior:
  - Auto mode renders Markdown/LaTeX/code patterns as the user writes.
  - Assist mode highlights detected syntax and lets the user preview or format later.
  - Long press or context menu shows preview, format now, leave raw, and edit source.
- Keep generated PDF/render files as cache, not the source of truth.
- Preserve preview/edit position mapping so users can jump from a rendered page or section back into the matching editable line.

## Appearance

- Support automatic dark mode from system settings.
- Support manual Light and Dark modes.
- Add a quick light/dark button beside settings.
- Add AMOLED as a separate toggle that only changes the dark-mode rendering.
- Add an "Auto refresh UI" setting for cases where OS theme or window changes are not picked up.
- Add a manual "Refresh UI now" action in settings and the editor command bar.
- Add a compact accent-color picker with quick swatches and a browser color wheel for custom color choice.
- Add an accent-tinted background toggle so light and LCD dark themes can recolor the whole app background from the chosen accent.
- Allow background images or generated backgrounds per app area: raw editor, preview, files/sidebar, and notes list.
- Keep settings structurally close to VS Code: search, scope tabs, category list, and right-side setting rows.

## Workspace Comfort

- Desktop folder/sidebar uses one Edge-style sidebar button.
- When collapsed, the sidebar hides full text and becomes an icon rail.
- Desktop notes list has a separate compact/list-density button.
- Compact notes show one-line note titles; long titles scroll on hover.
- Compact notes show a hover preview with actions such as open, star, rename, move, export, and delete.
- User folders and note-folder icons can show colored first-letter badges; this can be disabled in settings.
- Editor and preview panes should gain width when sidebars are collapsed.
- Collapsed state should be remembered per device later.

## Attachments

Attachments should support two display modes:

- Large preview cards: thumbnail/preview area, filename, preview/cache status, quick actions.
- Filename rows: vertical, readable file list rows with full names instead of clipped ellipses.

Supported attachment targets:

- Images
- PDFs
- Audio recordings
- Video files
- Office documents
- ZIP/files
- Web links

## Planned App Areas

- Home and recent notes
- Folder/file browser
- Note editor
- Preview reader
- Outline/chapter navigation
- Attachment manager
- Settings
- Export to PDF/HTML
- Search
- Tags and starred notes
- Sync later

## Inspiration Backlog

Borrow product patterns, not branding or code, from mature note/workspace apps:

- Notion: block handles, slash commands, page database views, synced blocks.
- AppFlowy: local-first workspace model, open-source collaboration direction, kanban/table views.
- Obsidian: vault folders, backlinks, graph view, daily notes, command palette.
- Logseq: outline-first writing, block references, journaling.
- Joplin: offline-first sync targets, encryption, attachment resources.
- VS Code: searchable settings, command palette, activity/sidebar behavior, split panes.

Only adopt ideas that fit Libre Notes' markdown-first, file-portable identity.

## Licensing Strategy

Do not publish the repository publicly without choosing a license strategy.

Recommended early approach:

- Keep the first repo private while the product shape is being built.
- Add copyright notices in the README.
- Add a license before making the repo public.
- If the goal is to stop people from freely taking and commercializing the work, avoid permissive licenses like MIT/Apache for the main app.

Good options:

- Proprietary / All rights reserved: strongest control, not open source.
- AGPL-3.0: open source, but network/service users must share source changes.
- GPL-3.0: open source, strong copyleft for distributed app builds.
- Dual license later: community license plus paid commercial license.

Things licensing does not protect well:

- A raw app idea.
- The name or brand by itself.
- Someone independently building a similar app.

Things to protect separately later:

- Trademark for the final app name/logo.
- Copyright ownership of code, art, docs, and designs.
- Private repo access while the product is not ready.
