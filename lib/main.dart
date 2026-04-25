import 'package:flutter/material.dart';

void main() {
  runApp(const LibreNotesApp());
}

class LibreNotesApp extends StatefulWidget {
  const LibreNotesApp({super.key});

  @override
  State<LibreNotesApp> createState() => _LibreNotesAppState();
}

class _LibreNotesAppState extends State<LibreNotesApp> {
  ThemeMode themeMode = ThemeMode.dark;
  bool amoledDark = false;
  bool accentTintBackground = true;
  bool letterIcons = true;
  bool compactNotes = false;
  bool attachmentPreviewCards = true;
  bool sidebarCollapsed = false;
  Color accent = const Color(0xFF5EEAD4);

  @override
  Widget build(BuildContext context) {
    final lightScheme = ColorScheme.fromSeed(seedColor: accent);
    final darkScheme = ColorScheme.fromSeed(
      seedColor: accent,
      brightness: Brightness.dark,
    ).copyWith(surface: amoledDark ? Colors.black : const Color(0xFF101A17));

    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'Libre Notes',
      themeMode: themeMode,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: lightScheme,
        fontFamily: 'Segoe UI',
      ),
      darkTheme: ThemeData(
        useMaterial3: true,
        colorScheme: darkScheme,
        scaffoldBackgroundColor: amoledDark ? Colors.black : const Color(0xFF0E1714),
        fontFamily: 'Segoe UI',
      ),
      home: LibreNotesHome(
        accent: accent,
        themeMode: themeMode,
        amoledDark: amoledDark,
        accentTintBackground: accentTintBackground,
        letterIcons: letterIcons,
        compactNotes: compactNotes,
        attachmentPreviewCards: attachmentPreviewCards,
        sidebarCollapsed: sidebarCollapsed,
        onThemeModeChanged: (value) => setState(() => themeMode = value),
        onAmoledChanged: (value) => setState(() => amoledDark = value),
        onAccentChanged: (value) => setState(() => accent = value),
        onAccentTintChanged: (value) => setState(() => accentTintBackground = value),
        onLetterIconsChanged: (value) => setState(() => letterIcons = value),
        onCompactNotesChanged: (value) => setState(() => compactNotes = value),
        onAttachmentModeChanged: (value) => setState(() => attachmentPreviewCards = value),
        onSidebarChanged: (value) => setState(() => sidebarCollapsed = value),
      ),
    );
  }
}

class LibreNotesHome extends StatefulWidget {
  const LibreNotesHome({
    super.key,
    required this.accent,
    required this.themeMode,
    required this.amoledDark,
    required this.accentTintBackground,
    required this.letterIcons,
    required this.compactNotes,
    required this.attachmentPreviewCards,
    required this.sidebarCollapsed,
    required this.onThemeModeChanged,
    required this.onAmoledChanged,
    required this.onAccentChanged,
    required this.onAccentTintChanged,
    required this.onLetterIconsChanged,
    required this.onCompactNotesChanged,
    required this.onAttachmentModeChanged,
    required this.onSidebarChanged,
  });

  final Color accent;
  final ThemeMode themeMode;
  final bool amoledDark;
  final bool accentTintBackground;
  final bool letterIcons;
  final bool compactNotes;
  final bool attachmentPreviewCards;
  final bool sidebarCollapsed;
  final ValueChanged<ThemeMode> onThemeModeChanged;
  final ValueChanged<bool> onAmoledChanged;
  final ValueChanged<Color> onAccentChanged;
  final ValueChanged<bool> onAccentTintChanged;
  final ValueChanged<bool> onLetterIconsChanged;
  final ValueChanged<bool> onCompactNotesChanged;
  final ValueChanged<bool> onAttachmentModeChanged;
  final ValueChanged<bool> onSidebarChanged;

  @override
  State<LibreNotesHome> createState() => _LibreNotesHomeState();
}

class _LibreNotesHomeState extends State<LibreNotesHome> {
  final TextEditingController editor = TextEditingController(text: sampleNotes.first.markdown);
  Note selectedNote = sampleNotes.first;
  String selectedFolder = 'Libre Notes';
  final Set<String> expandedFolders = {'Projects'};

  @override
  void dispose() {
    editor.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final dark = Theme.of(context).brightness == Brightness.dark;
    final bg = widget.accentTintBackground
        ? [
            BoxShadow(
              color: widget.accent.withOpacity(dark ? 0.12 : 0.15),
              blurRadius: 120,
              spreadRadius: 40,
              offset: const Offset(-160, -120),
            ),
          ]
        : <BoxShadow>[];

    return Scaffold(
      body: Container(
        decoration: BoxDecoration(
          color: scheme.surface,
          boxShadow: bg,
        ),
        child: SafeArea(
          child: Column(
            children: [
              AppHeader(
                accent: widget.accent,
                themeMode: widget.themeMode,
                sidebarCollapsed: widget.sidebarCollapsed,
                onToggleSidebar: () => widget.onSidebarChanged(!widget.sidebarCollapsed),
                onQuickTheme: () => widget.onThemeModeChanged(
                  widget.themeMode == ThemeMode.dark ? ThemeMode.light : ThemeMode.dark,
                ),
                onOpenSettings: () => _openSettings(context),
              ),
              Expanded(
                child: Row(
                  children: [
                    FolderSidebar(
                      collapsed: widget.sidebarCollapsed,
                      letterIcons: widget.letterIcons,
                      expandedFolders: expandedFolders,
                      selectedFolder: selectedFolder,
                      onToggleFolder: (name) => setState(() {
                        if (expandedFolders.contains(name)) {
                          expandedFolders.remove(name);
                        } else {
                          expandedFolders.add(name);
                        }
                      }),
                      onSelectFolder: (name) => setState(() => selectedFolder = name),
                    ),
                    NotesColumn(
                      compact: widget.compactNotes,
                      selected: selectedNote,
                      onToggleCompact: () => widget.onCompactNotesChanged(!widget.compactNotes),
                      onSelect: (note) => setState(() {
                        selectedNote = note;
                        editor.text = note.markdown;
                      }),
                    ),
                    Expanded(
                      child: Workspace(
                        selectedNote: selectedNote,
                        editor: editor,
                        attachmentPreviewCards: widget.attachmentPreviewCards,
                        onAttachmentModeChanged: widget.onAttachmentModeChanged,
                        onOpenSettings: () => _openSettings(context),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _openSettings(BuildContext context) {
    showDialog<void>(
      context: context,
      builder: (context) => SettingsDialog(
        accent: widget.accent,
        themeMode: widget.themeMode,
        amoledDark: widget.amoledDark,
        accentTintBackground: widget.accentTintBackground,
        letterIcons: widget.letterIcons,
        onThemeModeChanged: widget.onThemeModeChanged,
        onAmoledChanged: widget.onAmoledChanged,
        onAccentChanged: widget.onAccentChanged,
        onAccentTintChanged: widget.onAccentTintChanged,
        onLetterIconsChanged: widget.onLetterIconsChanged,
      ),
    );
  }
}

class AppHeader extends StatelessWidget {
  const AppHeader({
    super.key,
    required this.accent,
    required this.themeMode,
    required this.sidebarCollapsed,
    required this.onToggleSidebar,
    required this.onQuickTheme,
    required this.onOpenSettings,
  });

  final Color accent;
  final ThemeMode themeMode;
  final bool sidebarCollapsed;
  final VoidCallback onToggleSidebar;
  final VoidCallback onQuickTheme;
  final VoidCallback onOpenSettings;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 70,
      padding: const EdgeInsets.symmetric(horizontal: 18),
      decoration: BoxDecoration(
        border: Border(bottom: BorderSide(color: Theme.of(context).dividerColor.withOpacity(0.35))),
      ),
      child: Row(
        children: [
          CircleAvatar(
            backgroundColor: accent,
            foregroundColor: Colors.white,
            child: const Text('L', style: TextStyle(fontWeight: FontWeight.w900)),
          ),
          const SizedBox(width: 12),
          const Text('Libre Notes', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w800)),
          const Spacer(),
          SizedBox(
            width: 460,
            child: SearchBar(
              leading: const Icon(Icons.search),
              hintText: 'Search notes or type a command...',
              trailing: const [Chip(label: Text('Ctrl')), Chip(label: Text('K'))],
              padding: const MaterialStatePropertyAll(EdgeInsets.symmetric(horizontal: 14)),
            ),
          ),
          const Spacer(),
          IconButton.filledTonal(
            tooltip: sidebarCollapsed ? 'Expand sidebar' : 'Collapse sidebar',
            onPressed: onToggleSidebar,
            icon: const Icon(Icons.view_sidebar_outlined),
          ),
          IconButton.filledTonal(
            tooltip: 'Quick light/dark toggle',
            onPressed: onQuickTheme,
            icon: Icon(themeMode == ThemeMode.dark ? Icons.light_mode_outlined : Icons.dark_mode_outlined),
          ),
          IconButton.filledTonal(
            tooltip: 'Settings',
            onPressed: onOpenSettings,
            icon: const Icon(Icons.settings_outlined),
          ),
        ],
      ),
    );
  }
}

class FolderSidebar extends StatelessWidget {
  const FolderSidebar({
    super.key,
    required this.collapsed,
    required this.letterIcons,
    required this.expandedFolders,
    required this.selectedFolder,
    required this.onToggleFolder,
    required this.onSelectFolder,
  });

  final bool collapsed;
  final bool letterIcons;
  final Set<String> expandedFolders;
  final String selectedFolder;
  final ValueChanged<String> onToggleFolder;
  final ValueChanged<String> onSelectFolder;

  @override
  Widget build(BuildContext context) {
    final width = collapsed ? 76.0 : 260.0;
    return AnimatedContainer(
      duration: const Duration(milliseconds: 180),
      width: width,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        border: Border(right: BorderSide(color: Theme.of(context).dividerColor.withOpacity(0.35))),
      ),
      child: ListView(
        children: [
          FilledButton.icon(
            onPressed: () {},
            icon: const Icon(Icons.add),
            label: collapsed ? const SizedBox.shrink() : const Text('New note'),
          ),
          const SizedBox(height: 18),
          ...['Inbox', 'Starred', 'Drafts', 'Recent', 'Trash'].map((item) => SidebarTile.system(
                name: item,
                collapsed: collapsed,
              )),
          if (!collapsed) const SectionLabel('FOLDERS'),
          FolderNode(
            name: 'Personal',
            color: Colors.lightBlue,
            selectedFolder: selectedFolder,
            collapsed: collapsed,
            letterIcons: letterIcons,
            onSelect: onSelectFolder,
          ),
          FolderNode(
            name: 'Work',
            color: Colors.deepPurple,
            selectedFolder: selectedFolder,
            collapsed: collapsed,
            letterIcons: letterIcons,
            onSelect: onSelectFolder,
          ),
          ExpandableFolderNode(
            name: 'Projects',
            color: Colors.deepOrange,
            expanded: expandedFolders.contains('Projects'),
            collapsed: collapsed,
            letterIcons: letterIcons,
            selectedFolder: selectedFolder,
            onToggle: () => onToggleFolder('Projects'),
            children: [
              FolderNode(
                name: 'Libre Notes',
                color: Colors.teal,
                selectedFolder: selectedFolder,
                collapsed: collapsed,
                letterIcons: letterIcons,
                indent: true,
                onSelect: onSelectFolder,
              ),
              FolderNode(
                name: 'Website Redesign',
                color: Colors.pink,
                selectedFolder: selectedFolder,
                collapsed: collapsed,
                letterIcons: letterIcons,
                indent: true,
                onSelect: onSelectFolder,
              ),
            ],
          ),
          FolderNode(
            name: 'Learn',
            color: Colors.lightGreen,
            selectedFolder: selectedFolder,
            collapsed: collapsed,
            letterIcons: letterIcons,
            onSelect: onSelectFolder,
          ),
          SidebarTile.system(name: 'Archive', collapsed: collapsed, icon: Icons.archive_outlined),
          if (!collapsed) const SectionLabel('TAGS'),
          if (!collapsed)
            const Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                Chip(label: Text('# planning')),
                Chip(label: Text('# ideas')),
                Chip(label: Text('# reference')),
              ],
            ),
        ],
      ),
    );
  }
}

class SectionLabel extends StatelessWidget {
  const SectionLabel(this.label, {super.key});

  final String label;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(8, 24, 8, 8),
      child: Text(label, style: Theme.of(context).textTheme.labelLarge),
    );
  }
}

class SidebarTile extends StatelessWidget {
  const SidebarTile.system({
    super.key,
    required this.name,
    required this.collapsed,
    this.icon,
  })  : color = null,
        letterIcons = false,
        selected = false,
        indent = false,
        onTap = null;

  const SidebarTile.folder({
    super.key,
    required this.name,
    required this.collapsed,
    required this.color,
    required this.letterIcons,
    required this.selected,
    this.indent = false,
    this.onTap,
  }) : icon = null;

  final String name;
  final bool collapsed;
  final IconData? icon;
  final Color? color;
  final bool letterIcons;
  final bool selected;
  final bool indent;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final actualIcon = icon ?? systemIcon(name);
    final leading = color != null && letterIcons
        ? CircleAvatar(
            radius: 13,
            backgroundColor: color,
            child: Text(name.substring(0, 1), style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w900)),
          )
        : Icon(actualIcon, size: 22);

    return Padding(
      padding: EdgeInsets.only(left: collapsed ? 0 : (indent ? 28 : 0), bottom: 4),
      child: ListTile(
        selected: selected,
        dense: true,
        minLeadingWidth: 28,
        horizontalTitleGap: 8,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
        leading: leading,
        title: collapsed ? null : Text(name, style: const TextStyle(fontWeight: FontWeight.w700)),
        onTap: onTap,
      ),
    );
  }
}

class FolderNode extends StatelessWidget {
  const FolderNode({
    super.key,
    required this.name,
    required this.color,
    required this.selectedFolder,
    required this.collapsed,
    required this.letterIcons,
    required this.onSelect,
    this.indent = false,
  });

  final String name;
  final Color color;
  final String selectedFolder;
  final bool collapsed;
  final bool letterIcons;
  final bool indent;
  final ValueChanged<String> onSelect;

  @override
  Widget build(BuildContext context) {
    return SidebarTile.folder(
      name: name,
      collapsed: collapsed,
      color: color,
      letterIcons: letterIcons,
      selected: selectedFolder == name,
      indent: indent,
      onTap: () => onSelect(name),
    );
  }
}

class ExpandableFolderNode extends StatelessWidget {
  const ExpandableFolderNode({
    super.key,
    required this.name,
    required this.color,
    required this.expanded,
    required this.collapsed,
    required this.letterIcons,
    required this.selectedFolder,
    required this.onToggle,
    required this.children,
  });

  final String name;
  final Color color;
  final bool expanded;
  final bool collapsed;
  final bool letterIcons;
  final String selectedFolder;
  final VoidCallback onToggle;
  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    if (collapsed) {
      return SidebarTile.folder(
        name: name,
        collapsed: true,
        color: color,
        letterIcons: letterIcons,
        selected: selectedFolder == name,
        onTap: onToggle,
      );
    }
    return Column(
      children: [
        ListTile(
          dense: true,
          selected: selectedFolder == name,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
          leading: letterIcons
              ? CircleAvatar(
                  radius: 13,
                  backgroundColor: color,
                  child: Text(name.substring(0, 1), style: const TextStyle(color: Colors.white, fontWeight: FontWeight.w900)),
                )
              : const Icon(Icons.folder_open_outlined),
          title: Text(name, style: const TextStyle(fontWeight: FontWeight.w700)),
          trailing: Icon(expanded ? Icons.expand_less : Icons.expand_more),
          onTap: onToggle,
        ),
        AnimatedCrossFade(
          firstChild: const SizedBox.shrink(),
          secondChild: Column(children: children),
          crossFadeState: expanded ? CrossFadeState.showSecond : CrossFadeState.showFirst,
          duration: const Duration(milliseconds: 160),
        ),
      ],
    );
  }
}

class NotesColumn extends StatelessWidget {
  const NotesColumn({
    super.key,
    required this.compact,
    required this.selected,
    required this.onToggleCompact,
    required this.onSelect,
  });

  final bool compact;
  final Note selected;
  final VoidCallback onToggleCompact;
  final ValueChanged<Note> onSelect;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: compact ? 260 : 320,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        border: Border(right: BorderSide(color: Theme.of(context).dividerColor.withOpacity(0.35))),
      ),
      child: Column(
        children: [
          Row(
            children: [
              const Expanded(child: Text('Libre Notes', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w800))),
              IconButton(onPressed: () {}, icon: const Icon(Icons.sort)),
              IconButton(onPressed: () {}, icon: const Icon(Icons.tune)),
              IconButton(onPressed: onToggleCompact, icon: const Icon(Icons.view_headline)),
            ],
          ),
          const SizedBox(height: 8),
          Expanded(
            child: ListView.separated(
              itemCount: sampleNotes.length,
              separatorBuilder: (_, __) => const SizedBox(height: 10),
              itemBuilder: (context, index) {
                final note = sampleNotes[index];
                return NoteCard(
                  note: note,
                  compact: compact,
                  selected: selected.title == note.title,
                  onTap: () => onSelect(note),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}

class NoteCard extends StatelessWidget {
  const NoteCard({
    super.key,
    required this.note,
    required this.compact,
    required this.selected,
    required this.onTap,
  });

  final Note note;
  final bool compact;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Tooltip(
      message: '${note.title}\n${note.summary}\nOpen · Star · Rename',
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(8),
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 160),
          padding: EdgeInsets.all(compact ? 12 : 16),
          decoration: BoxDecoration(
            color: selected ? scheme.primaryContainer.withOpacity(0.35) : scheme.surfaceContainerHighest.withOpacity(0.45),
            borderRadius: BorderRadius.circular(8),
            border: Border.all(color: selected ? scheme.primary.withOpacity(0.55) : scheme.outlineVariant),
          ),
          child: compact
              ? Text(note.title, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontWeight: FontWeight.w800))
              : Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Expanded(child: Text(note.title, style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 16))),
                        const Icon(Icons.star_border, size: 20),
                      ],
                    ),
                    const SizedBox(height: 10),
                    Text(note.summary, maxLines: 2, overflow: TextOverflow.ellipsis),
                    const SizedBox(height: 14),
                    Row(
                      children: [
                        const Text('Today', style: TextStyle(fontWeight: FontWeight.w700)),
                        const Spacer(),
                        Chip(label: Text(note.tag)),
                      ],
                    ),
                  ],
                ),
        ),
      ),
    );
  }
}

class Workspace extends StatelessWidget {
  const Workspace({
    super.key,
    required this.selectedNote,
    required this.editor,
    required this.attachmentPreviewCards,
    required this.onAttachmentModeChanged,
    required this.onOpenSettings,
  });

  final Note selectedNote;
  final TextEditingController editor;
  final bool attachmentPreviewCards;
  final ValueChanged<bool> onAttachmentModeChanged;
  final VoidCallback onOpenSettings;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Container(
          height: 70,
          padding: const EdgeInsets.symmetric(horizontal: 18),
          decoration: BoxDecoration(border: Border(bottom: BorderSide(color: Theme.of(context).dividerColor.withOpacity(0.35)))),
          child: Row(
            children: [
              Expanded(child: Text(selectedNote.title, style: const TextStyle(fontSize: 24, fontWeight: FontWeight.w900))),
              IconButton.filledTonal(onPressed: () {}, icon: const Icon(Icons.edit_outlined)),
              IconButton.filledTonal(onPressed: () {}, icon: const Icon(Icons.copy_outlined)),
              IconButton.filledTonal(onPressed: onOpenSettings, icon: const Icon(Icons.palette_outlined)),
              IconButton.filledTonal(onPressed: () {}, icon: const Icon(Icons.refresh)),
            ],
          ),
        ),
        Expanded(
          child: Row(
            children: [
              Expanded(
                child: Pane(
                  label: 'Edit',
                  child: TextField(
                    controller: editor,
                    expands: true,
                    maxLines: null,
                    minLines: null,
                    style: const TextStyle(fontFamily: 'Consolas', fontSize: 16),
                    decoration: const InputDecoration(border: OutlineInputBorder(), contentPadding: EdgeInsets.all(18)),
                  ),
                ),
              ),
              Expanded(
                child: Pane(
                  label: 'Preview',
                  child: MarkdownPreview(text: editor.text),
                ),
              ),
            ],
          ),
        ),
        AttachmentShelf(
          previewCards: attachmentPreviewCards,
          onModeChanged: onAttachmentModeChanged,
        ),
        const StatusBar(),
      ],
    );
  }
}

class Pane extends StatelessWidget {
  const Pane({super.key, required this.label, required this.child});

  final String label;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(18),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Chip(label: Text(label)),
          const SizedBox(height: 10),
          Expanded(child: child),
        ],
      ),
    );
  }
}

class MarkdownPreview extends StatelessWidget {
  const MarkdownPreview({super.key, required this.text});

  final String text;

  @override
  Widget build(BuildContext context) {
    final blocks = text.split('\n');
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(26),
      decoration: BoxDecoration(
        border: Border.all(color: Theme.of(context).colorScheme.outlineVariant),
        borderRadius: BorderRadius.circular(8),
      ),
      child: ListView(
        children: [
          for (final line in blocks)
            if (line.startsWith('# '))
              Padding(
                padding: const EdgeInsets.only(bottom: 20),
                child: Text(line.substring(2), style: const TextStyle(fontSize: 32, fontWeight: FontWeight.w900)),
              )
            else if (line.startsWith('## '))
              Padding(
                padding: const EdgeInsets.only(top: 18, bottom: 8),
                child: Text(line.substring(3), style: const TextStyle(fontSize: 21, fontWeight: FontWeight.w800)),
              )
            else if (line.startsWith('- '))
              Text('• ${line.substring(2)}', style: const TextStyle(fontSize: 16, height: 1.6))
            else if (line.startsWith('|'))
              Text(line, style: TextStyle(fontFamily: 'Consolas', color: Theme.of(context).colorScheme.primary))
            else if (line.trim().isEmpty)
              const SizedBox(height: 8)
            else
              Text(line, style: const TextStyle(fontSize: 16, height: 1.55)),
        ],
      ),
    );
  }
}

class AttachmentShelf extends StatelessWidget {
  const AttachmentShelf({
    super.key,
    required this.previewCards,
    required this.onModeChanged,
  });

  final bool previewCards;
  final ValueChanged<bool> onModeChanged;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(18, 0, 18, 14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Attachments', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w900)),
                    Text('Choose filename rows or richer file previews.'),
                  ],
                ),
              ),
              SegmentedButton<bool>(
                segments: const [
                  ButtonSegment(value: true, icon: Icon(Icons.grid_view), label: Text('Preview')),
                  ButtonSegment(value: false, icon: Icon(Icons.list), label: Text('Filename')),
                ],
                selected: {previewCards},
                onSelectionChanged: (value) => onModeChanged(value.first),
              ),
            ],
          ),
          const SizedBox(height: 10),
          previewCards
              ? Row(children: attachments.map((file) => Expanded(child: AttachmentCard(file: file))).toList())
              : Column(children: attachments.map((file) => AttachmentRow(file: file)).toList()),
        ],
      ),
    );
  }
}

class AttachmentCard extends StatelessWidget {
  const AttachmentCard({super.key, required this.file});

  final AttachmentFile file;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(10),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              height: 88,
              decoration: BoxDecoration(
                color: file.color.withOpacity(0.22),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Center(child: Icon(file.icon, color: file.color, size: 32)),
            ),
            const SizedBox(height: 8),
            Text(file.name, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontWeight: FontWeight.w800)),
            Text(file.meta, maxLines: 1, overflow: TextOverflow.ellipsis),
          ],
        ),
      ),
    );
  }
}

class AttachmentRow extends StatelessWidget {
  const AttachmentRow({super.key, required this.file});

  final AttachmentFile file;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: file.color.withOpacity(0.25),
          child: Icon(file.icon, color: file.color),
        ),
        title: Text(file.name, style: const TextStyle(fontWeight: FontWeight.w800)),
        subtitle: Text(file.meta),
        trailing: const Icon(Icons.more_vert),
      ),
    );
  }
}

class StatusBar extends StatelessWidget {
  const StatusBar({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 36,
      padding: const EdgeInsets.symmetric(horizontal: 18),
      decoration: BoxDecoration(border: Border(top: BorderSide(color: Theme.of(context).dividerColor.withOpacity(0.35)))),
      child: const Row(
        children: [
          Text('Ln 19, Col 24'),
          Spacer(),
          Text('Markdown'),
          Spacer(),
          Icon(Icons.check_circle_outline, size: 16),
          SizedBox(width: 6),
          Text('Autosaved'),
          Spacer(),
          Text('168 words'),
        ],
      ),
    );
  }
}

class SettingsDialog extends StatelessWidget {
  const SettingsDialog({
    super.key,
    required this.accent,
    required this.themeMode,
    required this.amoledDark,
    required this.accentTintBackground,
    required this.letterIcons,
    required this.onThemeModeChanged,
    required this.onAmoledChanged,
    required this.onAccentChanged,
    required this.onAccentTintChanged,
    required this.onLetterIconsChanged,
  });

  final Color accent;
  final ThemeMode themeMode;
  final bool amoledDark;
  final bool accentTintBackground;
  final bool letterIcons;
  final ValueChanged<ThemeMode> onThemeModeChanged;
  final ValueChanged<bool> onAmoledChanged;
  final ValueChanged<Color> onAccentChanged;
  final ValueChanged<bool> onAccentTintChanged;
  final ValueChanged<bool> onLetterIconsChanged;

  @override
  Widget build(BuildContext context) {
    return Dialog(
      child: SizedBox(
        width: 1040,
        height: 680,
        child: Padding(
          padding: const EdgeInsets.all(18),
          child: Column(
            children: [
              Row(
                children: [
                  const Expanded(child: Text('Libre Notes Preferences', style: TextStyle(fontSize: 22, fontWeight: FontWeight.w900))),
                  IconButton(onPressed: () => Navigator.pop(context), icon: const Icon(Icons.close)),
                ],
              ),
              const SearchBar(leading: Icon(Icons.search), hintText: 'Search settings'),
              const SizedBox(height: 14),
              Expanded(
                child: Row(
                  children: [
                    SizedBox(
                      width: 220,
                      child: ListView(
                        children: const [
                          ListTile(selected: true, leading: Icon(Icons.auto_awesome), title: Text('Commonly Used')),
                          ListTile(leading: Icon(Icons.palette_outlined), title: Text('Appearance')),
                          ListTile(leading: Icon(Icons.text_fields), title: Text('Editor')),
                          ListTile(leading: Icon(Icons.folder_open), title: Text('Files')),
                          ListTile(leading: Icon(Icons.attach_file), title: Text('Attachments')),
                          ListTile(leading: Icon(Icons.security), title: Text('Privacy')),
                        ],
                      ),
                    ),
                    const VerticalDivider(),
                    Expanded(
                      child: ListView(
                        padding: const EdgeInsets.only(left: 22),
                        children: [
                          const Text('Commonly Used', style: TextStyle(fontSize: 30, fontWeight: FontWeight.w900)),
                          const SizedBox(height: 18),
                          SettingRow(
                            title: 'Workbench: Color Theme',
                            description: 'Controls light and dark appearance across editor, preview, files, and settings.',
                            control: SegmentedButton<ThemeMode>(
                              segments: const [
                                ButtonSegment(value: ThemeMode.system, label: Text('Auto'), icon: Icon(Icons.monitor_outlined)),
                                ButtonSegment(value: ThemeMode.light, label: Text('Light'), icon: Icon(Icons.light_mode_outlined)),
                                ButtonSegment(value: ThemeMode.dark, label: Text('Dark'), icon: Icon(Icons.dark_mode_outlined)),
                              ],
                              selected: {themeMode},
                              onSelectionChanged: (value) => onThemeModeChanged(value.first),
                            ),
                          ),
                          SettingSwitchRow(
                            title: 'Workbench: AMOLED Dark',
                            description: 'Uses true black surfaces, but only when dark mode is active.',
                            value: amoledDark,
                            onChanged: onAmoledChanged,
                          ),
                          SettingRow(
                            title: 'Workbench: Accent Color',
                            description: 'Choose a quick accent color for the whole app.',
                            control: Wrap(
                              spacing: 8,
                              children: [
                                for (final color in accentChoices)
                                  InkWell(
                                    onTap: () => onAccentChanged(color),
                                    borderRadius: BorderRadius.circular(999),
                                    child: CircleAvatar(
                                      backgroundColor: color,
                                      child: accent.value == color.value ? const Icon(Icons.check, color: Colors.white) : null,
                                    ),
                                  ),
                              ],
                            ),
                          ),
                          SettingSwitchRow(
                            title: 'Workbench: Accent-Tinted Background',
                            description: 'Let the accent color softly tint the full app background in light and LCD dark modes.',
                            value: accentTintBackground,
                            onChanged: onAccentTintChanged,
                          ),
                          SettingSwitchRow(
                            title: 'Files: Letter Icons',
                            description: 'Show first letters inside colored folder icons. System items keep normal symbols.',
                            value: letterIcons,
                            onChanged: onLetterIconsChanged,
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class SettingRow extends StatelessWidget {
  const SettingRow({
    super.key,
    required this.title,
    required this.description,
    required this.control,
  });

  final String title;
  final String description;
  final Widget control;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 16),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: const TextStyle(fontWeight: FontWeight.w900)),
                const SizedBox(height: 6),
                Text(description),
              ],
            ),
          ),
          const SizedBox(width: 22),
          SizedBox(width: 420, child: control),
        ],
      ),
    );
  }
}

class SettingSwitchRow extends StatelessWidget {
  const SettingSwitchRow({
    super.key,
    required this.title,
    required this.description,
    required this.value,
    required this.onChanged,
  });

  final String title;
  final String description;
  final bool value;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) {
    return SettingRow(
      title: title,
      description: description,
      control: Align(
        alignment: Alignment.centerRight,
        child: Switch(value: value, onChanged: onChanged),
      ),
    );
  }
}

IconData systemIcon(String name) {
  return switch (name) {
    'Inbox' => Icons.inbox_outlined,
    'Starred' => Icons.star_border,
    'Drafts' => Icons.drive_file_rename_outline,
    'Recent' => Icons.history,
    'Trash' => Icons.delete_outline,
    'Archive' => Icons.archive_outlined,
    _ => Icons.folder_outlined,
  };
}

class Note {
  const Note({
    required this.title,
    required this.summary,
    required this.tag,
    required this.markdown,
  });

  final String title;
  final String summary;
  final String tag;
  final String markdown;
}

class AttachmentFile {
  const AttachmentFile({
    required this.name,
    required this.meta,
    required this.icon,
    required this.color,
  });

  final String name;
  final String meta;
  final IconData icon;
  final Color color;
}

const sampleMarkdown = '''
# Project Roadmap

## Overview
This roadmap outlines the key milestones for Libre Notes.

## Goals
- Build a privacy-first notes app
- Support **Markdown** natively
- Deliver on Windows and Android

## Phases
| Phase | Focus | Timeline |
| --- | --- | --- |
| 1 | Core editor | Q2 |
| 2 | Platform apps | Q3 |

## Next Steps
- Finalize MVP scope
- Validate key workflows
''';

final sampleNotes = [
  const Note(title: 'Project Roadmap', summary: 'This roadmap outlines the key milestones for Libre Notes over...', tag: '#planning', markdown: sampleMarkdown),
  const Note(title: 'Editor Improvements', summary: 'Ideas to improve the editing experience and productivity.', tag: '#ideas', markdown: '# Editor Improvements\n\nIdeas to improve the editing experience and productivity.'),
  const Note(title: 'Privacy Principles', summary: 'Guiding principles for privacy and data protection.', tag: '#reference', markdown: '# Privacy Principles\n\nGuiding principles for privacy and data protection.'),
  const Note(title: 'Meeting Notes - 2026-04-25', summary: 'Discussed MVP direction and mobile preview polish.', tag: '#planning', markdown: '# Meeting Notes - 2026-04-25\n\nDiscussed MVP direction and mobile preview polish.'),
];

final attachments = [
  const AttachmentFile(name: 'lecture-outline.pdf', meta: 'PDF · 2.4 MB · cached', icon: Icons.picture_as_pdf_outlined, color: Colors.orange),
  const AttachmentFile(name: 'whiteboard-photo.jpg', meta: 'Image · preview ready', icon: Icons.image_outlined, color: Colors.green),
  const AttachmentFile(name: 'meeting-audio.m4a', meta: 'Audio · linked to note', icon: Icons.graphic_eq, color: Colors.blue),
];

final accentChoices = [
  const Color(0xFF2E8F83),
  const Color(0xFF14B8A6),
  const Color(0xFF22C55E),
  const Color(0xFFEAB308),
  const Color(0xFFF97316),
  const Color(0xFFEF4444),
  const Color(0xFFA855F7),
  const Color(0xFF3B82F6),
  const Color(0xFF06B6D4),
];
