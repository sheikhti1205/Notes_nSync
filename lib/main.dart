import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_markdown_plus/flutter_markdown_plus.dart';
import 'package:shared_preferences/shared_preferences.dart';

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
  bool denseNotes = false;
  Color accent = const Color(0xFF14B8A6);

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
        visualDensity: VisualDensity.standard,
      ),
      darkTheme: ThemeData(
        useMaterial3: true,
        colorScheme: darkScheme,
        scaffoldBackgroundColor: amoledDark ? Colors.black : const Color(0xFF0E1714),
        visualDensity: VisualDensity.standard,
      ),
      home: LibreNotesHome(
        accent: accent,
        themeMode: themeMode,
        amoledDark: amoledDark,
        accentTintBackground: accentTintBackground,
        letterIcons: letterIcons,
        denseNotes: denseNotes,
        onThemeModeChanged: (value) => setState(() => themeMode = value),
        onAmoledChanged: (value) => setState(() => amoledDark = value),
        onAccentChanged: (value) => setState(() => accent = value),
        onAccentTintChanged: (value) => setState(() => accentTintBackground = value),
        onLetterIconsChanged: (value) => setState(() => letterIcons = value),
        onDenseNotesChanged: (value) => setState(() => denseNotes = value),
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
    required this.denseNotes,
    required this.onThemeModeChanged,
    required this.onAmoledChanged,
    required this.onAccentChanged,
    required this.onAccentTintChanged,
    required this.onLetterIconsChanged,
    required this.onDenseNotesChanged,
  });

  final Color accent;
  final ThemeMode themeMode;
  final bool amoledDark;
  final bool accentTintBackground;
  final bool letterIcons;
  final bool denseNotes;
  final ValueChanged<ThemeMode> onThemeModeChanged;
  final ValueChanged<bool> onAmoledChanged;
  final ValueChanged<Color> onAccentChanged;
  final ValueChanged<bool> onAccentTintChanged;
  final ValueChanged<bool> onLetterIconsChanged;
  final ValueChanged<bool> onDenseNotesChanged;

  @override
  State<LibreNotesHome> createState() => _LibreNotesHomeState();
}

class _LibreNotesHomeState extends State<LibreNotesHome> with TickerProviderStateMixin {
  final TextEditingController editor = TextEditingController();
  final TextEditingController search = TextEditingController();
  final GlobalKey<ScaffoldState> scaffoldKey = GlobalKey<ScaffoldState>();
  late final TabController editorTabs;
  final SharedPreferencesAsync prefs = SharedPreferencesAsync();
  Timer? saveDebounce;

  List<Note> notes = seedNotes;
  String selectedNoteId = seedNotes.first.id;
  String selectedFolder = 'All';
  String query = '';
  int compactPage = 0;
  bool previewCards = true;
  bool syncingEditor = false;

  Color get accent => widget.accent;
  ThemeMode get themeMode => widget.themeMode;
  bool get amoledDark => widget.amoledDark;
  bool get accentTintBackground => widget.accentTintBackground;
  bool get letterIcons => widget.letterIcons;
  bool get denseNotes => widget.denseNotes;
  ValueChanged<ThemeMode> get onThemeModeChanged => widget.onThemeModeChanged;
  ValueChanged<bool> get onAmoledChanged => widget.onAmoledChanged;
  ValueChanged<Color> get onAccentChanged => widget.onAccentChanged;
  ValueChanged<bool> get onAccentTintChanged => widget.onAccentTintChanged;
  ValueChanged<bool> get onLetterIconsChanged => widget.onLetterIconsChanged;
  ValueChanged<bool> get onDenseNotesChanged => widget.onDenseNotesChanged;

  Note get selectedNote => notes.firstWhere((note) => note.id == selectedNoteId);

  List<String> get folders {
    final names = notes.map((note) => note.folder).toSet().toList()..sort();
    return ['All', 'Starred', ...names];
  }

  List<Note> get visibleNotes {
    final lowerQuery = query.trim().toLowerCase();
    return notes.where((note) {
      final folderMatch = selectedFolder == 'All' ||
          (selectedFolder == 'Starred' && note.starred) ||
          note.folder == selectedFolder;
      final queryMatch = lowerQuery.isEmpty ||
          note.title.toLowerCase().contains(lowerQuery) ||
          note.body.toLowerCase().contains(lowerQuery) ||
          note.tag.toLowerCase().contains(lowerQuery);
      return folderMatch && queryMatch;
    }).toList()
      ..sort((a, b) => b.updatedAt.compareTo(a.updatedAt));
  }

  @override
  void initState() {
    super.initState();
    editorTabs = TabController(length: 2, vsync: this);
    editor.text = selectedNote.body;
    editor.addListener(_updateSelectedNoteBody);
    search.addListener(() => setState(() => query = search.text));
    _loadVault();
  }

  @override
  void dispose() {
    saveDebounce?.cancel();
    editor.dispose();
    search.dispose();
    editorTabs.dispose();
    super.dispose();
  }

  Future<void> _loadVault() async {
    final raw = await prefs.getString(vaultStorageKey);
    if (!mounted || raw == null || raw.isEmpty) {
      return;
    }
    try {
      final decoded = jsonDecode(raw) as List<dynamic>;
      final restored = decoded
          .map((item) => Note.fromJson(item as Map<String, dynamic>))
          .toList();
      if (restored.isEmpty) {
        return;
      }
      setState(() {
        notes = restored;
        selectedNoteId = restored.first.id;
        selectedFolder = 'All';
        syncingEditor = true;
        editor.text = restored.first.body;
        syncingEditor = false;
      });
    } catch (_) {
      _showSnack('Saved vault data could not be loaded. Seed notes are still available.');
    }
  }

  void _queuePersist() {
    saveDebounce?.cancel();
    saveDebounce = Timer(const Duration(milliseconds: 450), () async {
      final data = jsonEncode(notes.map((note) => note.toJson()).toList());
      await prefs.setString(vaultStorageKey, data);
    });
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final dark = Theme.of(context).brightness == Brightness.dark;
    final tint = widget.accentTintBackground
        ? BoxShadow(
            color: widget.accent.withValues(alpha: dark ? 0.12 : 0.14),
            blurRadius: 120,
            spreadRadius: 30,
            offset: const Offset(-120, -90),
          )
        : null;

    return LayoutBuilder(
      builder: (context, constraints) {
        final width = constraints.maxWidth;
        final compact = width < 700;
        final medium = width >= 700 && width < 1050;

        return Scaffold(
          key: scaffoldKey,
          drawer: compact ? AppDrawer(home: this) : null,
          appBar: compact ? _buildCompactAppBar() : null,
          bottomNavigationBar: compact ? _buildBottomNavigation() : null,
          floatingActionButton: compact && compactPage == 0
              ? FloatingActionButton.extended(
                  onPressed: _createNote,
                  icon: const Icon(Icons.note_add_outlined),
                  label: const Text('New'),
                )
              : null,
          body: Container(
            decoration: BoxDecoration(
              color: scheme.surface,
              boxShadow: tint == null ? null : [tint],
            ),
            child: SafeArea(
              child: compact
                  ? _CompactHome(home: this)
                  : medium
                      ? _MediumHome(home: this)
                      : _ExpandedHome(home: this),
            ),
          ),
        );
      },
    );
  }

  PreferredSizeWidget _buildCompactAppBar() {
    return AppBar(
      title: Text(selectedNote.title, maxLines: 1, overflow: TextOverflow.ellipsis),
      leading: IconButton(
        tooltip: 'Menu',
        icon: const Icon(Icons.menu),
        onPressed: () => scaffoldKey.currentState?.openDrawer(),
      ),
      actions: [
        IconButton(
          tooltip: selectedNote.starred ? 'Unstar' : 'Star',
          onPressed: _toggleStar,
          icon: Icon(selectedNote.starred ? Icons.star : Icons.star_border),
        ),
        PopupMenuButton<String>(
          onSelected: (value) {
            switch (value) {
              case 'rename':
                _renameNote();
                break;
              case 'save':
                _saveNote();
                break;
              case 'delete':
                _deleteSelectedNote();
                break;
              case 'settings':
                _openSettings();
                break;
            }
          },
          itemBuilder: (context) => const [
            PopupMenuItem(value: 'rename', child: Text('Rename')),
            PopupMenuItem(value: 'save', child: Text('Save')),
            PopupMenuItem(value: 'delete', child: Text('Delete')),
            PopupMenuDivider(),
            PopupMenuItem(value: 'settings', child: Text('Settings')),
          ],
        ),
      ],
    );
  }

  Widget _buildBottomNavigation() {
    return NavigationBar(
      selectedIndex: compactPage,
      onDestinationSelected: (value) => setState(() => compactPage = value),
      destinations: const [
        NavigationDestination(icon: Icon(Icons.article_outlined), selectedIcon: Icon(Icons.article), label: 'Notes'),
        NavigationDestination(icon: Icon(Icons.edit_outlined), selectedIcon: Icon(Icons.edit), label: 'Edit'),
        NavigationDestination(icon: Icon(Icons.visibility_outlined), selectedIcon: Icon(Icons.visibility), label: 'Preview'),
        NavigationDestination(icon: Icon(Icons.attach_file), selectedIcon: Icon(Icons.attach_file), label: 'Files'),
      ],
    );
  }

  void _updateSelectedNoteBody() {
    if (syncingEditor) {
      return;
    }
    final index = notes.indexWhere((note) => note.id == selectedNoteId);
    if (index == -1 || notes[index].body == editor.text) {
      return;
    }
    setState(() {
      notes[index] = notes[index].copyWith(
        body: editor.text,
        updatedAt: DateTime.now(),
        wordCount: countWords(editor.text),
      );
    });
    _queuePersist();
  }

  void _selectNote(Note note, {int? page}) {
    setState(() {
      selectedNoteId = note.id;
      compactPage = page ?? compactPage;
      syncingEditor = true;
      editor.text = note.body;
      syncingEditor = false;
    });
  }

  void _selectFolder(String folder) {
    setState(() {
      selectedFolder = folder;
      compactPage = 0;
    });
    final candidates = visibleNotes;
    if (candidates.isNotEmpty && !candidates.any((note) => note.id == selectedNoteId)) {
      _selectNote(candidates.first);
    }
  }

  void _createNote() {
    final folder = selectedFolder == 'All' || selectedFolder == 'Starred' ? 'Inbox' : selectedFolder;
    final now = DateTime.now();
    final note = Note(
      id: now.microsecondsSinceEpoch.toString(),
      title: 'Untitled note',
      folder: folder,
      tag: '#draft',
      summary: 'Start writing. The preview updates as you type.',
      body: '# Untitled note\n\nStart writing in Markdown.\n\n- Draft ideas\n- Add attachments\n- Preview on the next tab\n',
      updatedAt: now,
      wordCount: 13,
      attachments: const [],
    );
    setState(() {
      notes = [note, ...notes];
      selectedNoteId = note.id;
      compactPage = 1;
      syncingEditor = true;
      editor.text = note.body;
      syncingEditor = false;
    });
    _queuePersist();
  }

  void _toggleStar() {
    final index = notes.indexWhere((note) => note.id == selectedNoteId);
    setState(() {
      notes[index] = notes[index].copyWith(starred: !notes[index].starred);
    });
    _queuePersist();
  }

  Future<void> _renameNote() async {
    final titleController = TextEditingController(text: selectedNote.title);
    final folderController = TextEditingController(text: selectedNote.folder);
    final tagController = TextEditingController(text: selectedNote.tag);
    final result = await showDialog<NoteEditResult>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Note details'),
        content: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 420),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(controller: titleController, decoration: const InputDecoration(labelText: 'Title')),
              const SizedBox(height: 12),
              TextField(controller: folderController, decoration: const InputDecoration(labelText: 'Folder')),
              const SizedBox(height: 12),
              TextField(controller: tagController, decoration: const InputDecoration(labelText: 'Tag')),
            ],
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancel')),
          FilledButton(
            onPressed: () => Navigator.pop(
              context,
              NoteEditResult(
                titleController.text.trim(),
                folderController.text.trim(),
                tagController.text.trim(),
              ),
            ),
            child: const Text('Apply'),
          ),
        ],
      ),
    );
    titleController.dispose();
    folderController.dispose();
    tagController.dispose();
    if (result == null || result.title.isEmpty || result.folder.isEmpty || result.tag.isEmpty) {
      return;
    }
    final index = notes.indexWhere((note) => note.id == selectedNoteId);
    setState(() {
      notes[index] = notes[index].copyWith(
        title: result.title,
        folder: result.folder,
        tag: result.tag.startsWith('#') ? result.tag : '#${result.tag}',
        updatedAt: DateTime.now(),
      );
      selectedFolder = result.folder;
    });
    _queuePersist();
  }

  void _deleteSelectedNote() {
    if (notes.length == 1) {
      _showSnack('Keep at least one note in the vault.');
      return;
    }
    final deletedTitle = selectedNote.title;
    setState(() {
      notes = notes.where((note) => note.id != selectedNoteId).toList();
      selectedNoteId = notes.first.id;
      syncingEditor = true;
      editor.text = notes.first.body;
      syncingEditor = false;
      compactPage = 0;
    });
    _queuePersist();
    _showSnack('$deletedTitle moved out of the sample vault.');
  }

  Future<void> _createFolder() async {
    final controller = TextEditingController();
    final folder = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('New folder'),
        content: TextField(
          controller: controller,
          autofocus: true,
          decoration: const InputDecoration(labelText: 'Folder name'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancel')),
          FilledButton(onPressed: () => Navigator.pop(context, controller.text.trim()), child: const Text('Create')),
        ],
      ),
    );
    controller.dispose();
    if (folder == null || folder.isEmpty) {
      return;
    }
    setState(() => selectedFolder = folder);
    _createNote();
  }

  Future<void> _addAttachment() async {
    final nameController = TextEditingController();
    final result = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Attach file'),
        content: TextField(
          controller: nameController,
          autofocus: true,
          decoration: const InputDecoration(
            labelText: 'Filename',
            hintText: 'diagram.png, notes.pdf, audio.m4a',
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancel')),
          FilledButton(onPressed: () => Navigator.pop(context, nameController.text.trim()), child: const Text('Attach')),
        ],
      ),
    );
    nameController.dispose();
    if (result == null || result.isEmpty) {
      return;
    }
    final file = AttachmentFile.fromName(result);
    final index = notes.indexWhere((note) => note.id == selectedNoteId);
    setState(() {
      notes[index] = notes[index].copyWith(
        attachments: [...notes[index].attachments, file],
        updatedAt: DateTime.now(),
      );
    });
    _queuePersist();
  }

  void _removeAttachment(AttachmentFile file) {
    final index = notes.indexWhere((note) => note.id == selectedNoteId);
    setState(() {
      notes[index] = notes[index].copyWith(
        attachments: notes[index].attachments.where((item) => item.name != file.name).toList(),
      );
    });
    _queuePersist();
  }

  void _setPreviewCards(bool value) {
    setState(() => previewCards = value);
  }

  void _saveNote() {
    final index = notes.indexWhere((note) => note.id == selectedNoteId);
    setState(() {
      notes[index] = notes[index].copyWith(updatedAt: DateTime.now(), wordCount: countWords(editor.text));
    });
    _queuePersist();
    _showSnack('${selectedNote.title} saved locally.');
  }

  void _showSnack(String message) {
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  void _openSettings() {
    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      builder: (context) => SettingsSheet(home: this),
    );
  }
}

class _CompactHome extends StatelessWidget {
  const _CompactHome({required this.home});

  final _LibreNotesHomeState home;

  @override
  Widget build(BuildContext context) {
    return IndexedStack(
      sizing: StackFit.expand,
      index: home.compactPage,
      children: [
        NotesPane(home: home, fullWidth: true),
        EditorPane(home: home, compact: true),
        PreviewPane(note: home.selectedNote, compact: true),
        AttachmentsPane(home: home, compact: true),
      ],
    );
  }
}

class _MediumHome extends StatelessWidget {
  const _MediumHome({required this.home});

  final _LibreNotesHomeState home;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        FolderRail(home: home, railOnly: true),
        SizedBox(width: 310, child: NotesPane(home: home)),
        Expanded(
          child: EditorPreviewTabs(home: home),
        ),
      ],
    );
  }
}

class _ExpandedHome extends StatelessWidget {
  const _ExpandedHome({required this.home});

  final _LibreNotesHomeState home;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        SizedBox(width: 252, child: FolderRail(home: home)),
        SizedBox(width: home.denseNotes ? 280 : 332, child: NotesPane(home: home)),
        Expanded(
          child: Column(
            children: [
              WorkspaceToolbar(home: home),
              Expanded(
                child: Row(
                  children: [
                    Expanded(child: EditorPane(home: home)),
                    const VerticalDivider(width: 1),
                    Expanded(child: PreviewPane(note: home.selectedNote)),
                  ],
                ),
              ),
              AttachmentsPane(home: home),
              StatusBar(note: home.selectedNote),
            ],
          ),
        ),
      ],
    );
  }
}

class AppDrawer extends StatelessWidget {
  const AppDrawer({super.key, required this.home});

  final _LibreNotesHomeState home;

  @override
  Widget build(BuildContext context) {
    return NavigationDrawer(
      selectedIndex: home.folders.indexOf(home.selectedFolder).clamp(0, home.folders.length - 1).toInt(),
      onDestinationSelected: (index) {
        Navigator.pop(context);
        home._selectFolder(home.folders[index]);
      },
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(24, 18, 16, 18),
          child: Row(
            children: [
              CircleAvatar(
                backgroundColor: home.accent,
                foregroundColor: Colors.white,
                child: const Text('L', style: TextStyle(fontWeight: FontWeight.w900)),
              ),
              const SizedBox(width: 12),
              const Expanded(
                child: Text('Libre Notes', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w900)),
              ),
            ],
          ),
        ),
        for (final folder in home.folders)
          NavigationDrawerDestination(
            icon: Icon(folderIcon(folder)),
            selectedIcon: Icon(folderIcon(folder, selected: true)),
            label: Text(folder),
          ),
        const Divider(),
        ListTile(
          leading: const Icon(Icons.create_new_folder_outlined),
          title: const Text('New folder'),
          onTap: () {
            Navigator.pop(context);
            home._createFolder();
          },
        ),
        ListTile(
          leading: const Icon(Icons.settings_outlined),
          title: const Text('Settings'),
          onTap: () {
            Navigator.pop(context);
            home._openSettings();
          },
        ),
      ],
    );
  }
}

class FolderRail extends StatelessWidget {
  const FolderRail({super.key, required this.home, this.railOnly = false});

  final _LibreNotesHomeState home;
  final bool railOnly;

  @override
  Widget build(BuildContext context) {
    final selectedIndex = home.folders.indexOf(home.selectedFolder).clamp(0, home.folders.length - 1).toInt();
    if (railOnly) {
      return NavigationRail(
        selectedIndex: selectedIndex,
        onDestinationSelected: (index) => home._selectFolder(home.folders[index]),
        labelType: NavigationRailLabelType.selected,
        leading: Padding(
          padding: const EdgeInsets.only(bottom: 12),
          child: IconButton.filled(
            tooltip: 'New note',
            onPressed: home._createNote,
            icon: const Icon(Icons.add),
          ),
        ),
        trailing: Expanded(
          child: Align(
            alignment: Alignment.bottomCenter,
            child: Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: IconButton(
                tooltip: 'Settings',
                onPressed: home._openSettings,
                icon: const Icon(Icons.settings_outlined),
              ),
            ),
          ),
        ),
        destinations: [
          for (final folder in home.folders)
            NavigationRailDestination(
              icon: Icon(folderIcon(folder)),
              selectedIcon: Icon(folderIcon(folder, selected: true)),
              label: Text(folder),
            ),
        ],
      );
    }

    return DecoratedBox(
      decoration: BoxDecoration(
        border: Border(right: BorderSide(color: Theme.of(context).dividerColor.withValues(alpha: 0.35))),
      ),
      child: ListView(
        padding: const EdgeInsets.fromLTRB(14, 16, 14, 16),
        children: [
          Row(
            children: [
              CircleAvatar(
                backgroundColor: home.accent,
                foregroundColor: Colors.white,
                child: const Text('L', style: TextStyle(fontWeight: FontWeight.w900)),
              ),
              const SizedBox(width: 10),
              const Expanded(
                child: Text('Libre Notes', style: TextStyle(fontSize: 20, fontWeight: FontWeight.w900)),
              ),
            ],
          ),
          const SizedBox(height: 18),
          FilledButton.icon(
            onPressed: home._createNote,
            icon: const Icon(Icons.note_add_outlined),
            label: const Text('New note'),
          ),
          OutlinedButton.icon(
            onPressed: home._createFolder,
            icon: const Icon(Icons.create_new_folder_outlined),
            label: const Text('New folder'),
          ),
          const SizedBox(height: 14),
          Text('Vault', style: Theme.of(context).textTheme.labelLarge),
          const SizedBox(height: 8),
          for (final folder in home.folders)
            FolderTile(
              name: folder,
              selected: folder == home.selectedFolder,
              letterIcons: home.letterIcons,
              onTap: () => home._selectFolder(folder),
            ),
          const SizedBox(height: 18),
          Text('Tags', style: Theme.of(context).textTheme.labelLarge),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              for (final tag in notesTags(home.notes)) ActionChip(label: Text(tag), onPressed: () => home.search.text = tag),
            ],
          ),
          const SizedBox(height: 20),
          ListTile(
            leading: const Icon(Icons.settings_outlined),
            title: const Text('Settings'),
            onTap: home._openSettings,
          ),
        ],
      ),
    );
  }
}

class FolderTile extends StatelessWidget {
  const FolderTile({
    super.key,
    required this.name,
    required this.selected,
    required this.letterIcons,
    required this.onTap,
  });

  final String name;
  final bool selected;
  final bool letterIcons;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return ListTile(
      selected: selected,
      dense: true,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      leading: letterIcons && name != 'All' && name != 'Starred'
          ? CircleAvatar(
              radius: 14,
              child: Text(name.substring(0, 1), style: const TextStyle(fontWeight: FontWeight.w900)),
            )
          : Icon(folderIcon(name, selected: selected)),
      title: Text(name, maxLines: 1, overflow: TextOverflow.ellipsis),
      onTap: onTap,
    );
  }
}

class NotesPane extends StatelessWidget {
  const NotesPane({super.key, required this.home, this.fullWidth = false});

  final _LibreNotesHomeState home;
  final bool fullWidth;

  @override
  Widget build(BuildContext context) {
    final notes = home.visibleNotes;
    return DecoratedBox(
      decoration: BoxDecoration(
        border: fullWidth ? null : Border(right: BorderSide(color: Theme.of(context).dividerColor.withValues(alpha: 0.35))),
      ),
      child: Column(
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(14, 12, 14, 8),
            child: Column(
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        home.selectedFolder,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900),
                      ),
                    ),
                    IconButton(tooltip: 'Sort', onPressed: () {}, icon: const Icon(Icons.sort)),
                    IconButton(tooltip: 'Dense notes', onPressed: () => home.onDenseNotesChanged(!home.denseNotes), icon: const Icon(Icons.view_headline)),
                  ],
                ),
                SearchBar(
                  controller: home.search,
                  leading: const Icon(Icons.search),
                  hintText: 'Search notes',
                  trailing: home.query.isEmpty
                      ? null
                      : [
                          IconButton(
                            tooltip: 'Clear',
                            onPressed: home.search.clear,
                            icon: const Icon(Icons.close),
                          ),
                        ],
                ),
              ],
            ),
          ),
          Expanded(
            child: notes.isEmpty
                ? EmptyState(
                    icon: Icons.search_off,
                    title: 'No notes found',
                    action: 'Create note',
                    onPressed: home._createNote,
                  )
                : ListView.separated(
                    padding: const EdgeInsets.fromLTRB(14, 8, 14, 90),
                    itemCount: notes.length,
                    separatorBuilder: (_, __) => const SizedBox(height: 10),
                    itemBuilder: (context, index) {
                      final note = notes[index];
                      return NoteCard(
                        note: note,
                        dense: home.denseNotes,
                        selected: note.id == home.selectedNoteId,
                        onTap: () => home._selectNote(note, page: 1),
                        onStar: () {
                          home._selectNote(note);
                          home._toggleStar();
                        },
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
    required this.dense,
    required this.selected,
    required this.onTap,
    required this.onStar,
  });

  final Note note;
  final bool dense;
  final bool selected;
  final VoidCallback onTap;
  final VoidCallback onStar;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Card(
      margin: EdgeInsets.zero,
      color: selected ? scheme.primaryContainer.withValues(alpha: 0.45) : scheme.surfaceContainerHighest.withValues(alpha: 0.55),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(8),
        side: BorderSide(color: selected ? scheme.primary.withValues(alpha: 0.55) : scheme.outlineVariant),
      ),
      child: InkWell(
        borderRadius: BorderRadius.circular(8),
        onTap: onTap,
        child: Padding(
          padding: EdgeInsets.all(dense ? 10 : 14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                    child: Text(
                      note.title,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(fontWeight: FontWeight.w900),
                    ),
                  ),
                  IconButton(
                    tooltip: note.starred ? 'Unstar' : 'Star',
                    visualDensity: VisualDensity.compact,
                    onPressed: onStar,
                    icon: Icon(note.starred ? Icons.star : Icons.star_border, size: 20),
                  ),
                ],
              ),
              if (!dense) ...[
                const SizedBox(height: 6),
                Text(note.summary, maxLines: 2, overflow: TextOverflow.ellipsis),
                const SizedBox(height: 10),
              ],
              Wrap(
                spacing: 8,
                runSpacing: 6,
                crossAxisAlignment: WrapCrossAlignment.center,
                children: [
                  Chip(label: Text(note.tag), visualDensity: VisualDensity.compact),
                  Text(relativeDate(note.updatedAt), style: Theme.of(context).textTheme.labelMedium),
                  Text('${note.wordCount} words', style: Theme.of(context).textTheme.labelMedium),
                  if (note.attachments.isNotEmpty)
                    Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(Icons.attach_file, size: 16),
                        Text('${note.attachments.length}', style: Theme.of(context).textTheme.labelMedium),
                      ],
                    ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class WorkspaceToolbar extends StatelessWidget {
  const WorkspaceToolbar({super.key, required this.home});

  final _LibreNotesHomeState home;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 64,
      padding: const EdgeInsets.symmetric(horizontal: 14),
      decoration: BoxDecoration(
        border: Border(bottom: BorderSide(color: Theme.of(context).dividerColor.withValues(alpha: 0.35))),
      ),
      child: Row(
        children: [
          Expanded(
            child: Text(
              home.selectedNote.title,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900),
            ),
          ),
          IconButton.filledTonal(tooltip: 'Rename', onPressed: home._renameNote, icon: const Icon(Icons.drive_file_rename_outline)),
          IconButton.filledTonal(tooltip: 'Save', onPressed: home._saveNote, icon: const Icon(Icons.save_outlined)),
          IconButton.filledTonal(tooltip: 'Attach', onPressed: home._addAttachment, icon: const Icon(Icons.attach_file)),
          IconButton.filledTonal(tooltip: 'Settings', onPressed: home._openSettings, icon: const Icon(Icons.palette_outlined)),
        ],
      ),
    );
  }
}

class EditorPreviewTabs extends StatelessWidget {
  const EditorPreviewTabs({super.key, required this.home});

  final _LibreNotesHomeState home;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        WorkspaceToolbar(home: home),
        TabBar(
          controller: home.editorTabs,
          tabs: const [
            Tab(icon: Icon(Icons.edit_outlined), text: 'Edit'),
            Tab(icon: Icon(Icons.visibility_outlined), text: 'Preview'),
          ],
        ),
        Expanded(
          child: TabBarView(
            controller: home.editorTabs,
            children: [
              EditorPane(home: home),
              PreviewPane(note: home.selectedNote),
            ],
          ),
        ),
        AttachmentsPane(home: home),
      ],
    );
  }
}

class EditorPane extends StatelessWidget {
  const EditorPane({super.key, required this.home, this.compact = false});

  final _LibreNotesHomeState home;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.all(compact ? 12 : 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (compact)
            Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: NoteHeader(home: home),
            ),
          Expanded(
            child: TextField(
              controller: home.editor,
              expands: true,
              maxLines: null,
              minLines: null,
              textAlignVertical: TextAlignVertical.top,
              keyboardType: TextInputType.multiline,
              style: TextStyle(
                fontFamily: 'monospace',
                fontSize: compact ? 15 : 16,
                height: 1.45,
              ),
              decoration: InputDecoration(
                filled: true,
                hintText: 'Write Markdown here...',
                alignLabelWithHint: true,
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(8)),
                contentPadding: EdgeInsets.all(compact ? 14 : 18),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class PreviewPane extends StatelessWidget {
  const PreviewPane({super.key, required this.note, this.compact = false});

  final Note note;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.all(compact ? 12 : 16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (compact)
            Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: NoteHeader(note: note),
            ),
          Expanded(
            child: DecoratedBox(
              decoration: BoxDecoration(
                color: Theme.of(context).colorScheme.surfaceContainerLow.withValues(alpha: 0.65),
                border: Border.all(color: Theme.of(context).colorScheme.outlineVariant),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Markdown(
                data: note.body,
                selectable: true,
                padding: EdgeInsets.all(compact ? 18 : 24),
                styleSheet: MarkdownStyleSheet.fromTheme(Theme.of(context)).copyWith(
                  h1: Theme.of(context).textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.w900),
                  h2: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900),
                  p: Theme.of(context).textTheme.bodyLarge?.copyWith(height: 1.55),
                  codeblockDecoration: BoxDecoration(
                    color: Theme.of(context).colorScheme.surfaceContainerHighest,
                    borderRadius: BorderRadius.circular(8),
                  ),
                  blockquoteDecoration: BoxDecoration(
                    color: Theme.of(context).colorScheme.primaryContainer.withValues(alpha: 0.35),
                    border: Border(left: BorderSide(color: Theme.of(context).colorScheme.primary, width: 4)),
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class NoteHeader extends StatelessWidget {
  const NoteHeader({super.key, this.home, this.note});

  final _LibreNotesHomeState? home;
  final Note? note;

  @override
  Widget build(BuildContext context) {
    final current = note ?? home!.selectedNote;
    return Row(
      children: [
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(current.title, maxLines: 1, overflow: TextOverflow.ellipsis, style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900)),
              Text('${current.folder}  -  ${current.tag}', maxLines: 1, overflow: TextOverflow.ellipsis),
            ],
          ),
        ),
        if (home != null) ...[
          IconButton(tooltip: 'Rename', onPressed: home!._renameNote, icon: const Icon(Icons.drive_file_rename_outline)),
          IconButton(tooltip: 'Save', onPressed: home!._saveNote, icon: const Icon(Icons.save_outlined)),
        ],
      ],
    );
  }
}

class AttachmentsPane extends StatelessWidget {
  const AttachmentsPane({super.key, required this.home, this.compact = false});

  final _LibreNotesHomeState home;
  final bool compact;

  @override
  Widget build(BuildContext context) {
    final attachments = home.selectedNote.attachments;
    return Container(
      constraints: BoxConstraints(maxHeight: compact ? double.infinity : 178),
      padding: EdgeInsets.fromLTRB(compact ? 12 : 16, 10, compact ? 12 : 16, compact ? 80 : 12),
      decoration: compact
          ? null
          : BoxDecoration(
              border: Border(top: BorderSide(color: Theme.of(context).dividerColor.withValues(alpha: 0.35))),
            ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text('Attachments', style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w900)),
              ),
              if (!compact)
                SegmentedButton<bool>(
                  segments: const [
                    ButtonSegment(value: true, icon: Icon(Icons.grid_view), label: Text('Cards')),
                    ButtonSegment(value: false, icon: Icon(Icons.list), label: Text('Rows')),
                  ],
                  selected: {home.previewCards},
                  onSelectionChanged: (value) => home._setPreviewCards(value.first),
                ),
              IconButton.filledTonal(tooltip: 'Attach file', onPressed: home._addAttachment, icon: const Icon(Icons.add)),
            ],
          ),
          const SizedBox(height: 10),
          Expanded(
            child: attachments.isEmpty
                ? EmptyState(
                    icon: Icons.attach_file,
                    title: 'No attachments yet',
                    action: 'Attach',
                    onPressed: home._addAttachment,
                  )
                : home.previewCards && !compact
                    ? ListView.separated(
                        scrollDirection: Axis.horizontal,
                        itemCount: attachments.length,
                        separatorBuilder: (_, __) => const SizedBox(width: 10),
                        itemBuilder: (context, index) => SizedBox(
                          width: 210,
                          child: AttachmentCard(file: attachments[index], onRemove: () => home._removeAttachment(attachments[index])),
                        ),
                      )
                    : ListView.separated(
                        itemCount: attachments.length,
                        separatorBuilder: (_, __) => const SizedBox(height: 8),
                        itemBuilder: (context, index) => AttachmentRow(file: attachments[index], onRemove: () => home._removeAttachment(attachments[index])),
                      ),
          ),
        ],
      ),
    );
  }
}

class AttachmentCard extends StatelessWidget {
  const AttachmentCard({super.key, required this.file, required this.onRemove});

  final AttachmentFile file;
  final VoidCallback onRemove;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: EdgeInsets.zero,
      child: Padding(
        padding: const EdgeInsets.all(10),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                CircleAvatar(
                  backgroundColor: file.color.withValues(alpha: 0.18),
                  child: Icon(file.icon, color: file.color),
                ),
                const Spacer(),
                IconButton(tooltip: 'Remove', onPressed: onRemove, icon: const Icon(Icons.close, size: 18)),
              ],
            ),
            const Spacer(),
            Text(file.name, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontWeight: FontWeight.w900)),
            Text(file.meta, maxLines: 1, overflow: TextOverflow.ellipsis),
          ],
        ),
      ),
    );
  }
}

class AttachmentRow extends StatelessWidget {
  const AttachmentRow({super.key, required this.file, required this.onRemove});

  final AttachmentFile file;
  final VoidCallback onRemove;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: EdgeInsets.zero,
      child: ListTile(
        leading: CircleAvatar(
          backgroundColor: file.color.withValues(alpha: 0.2),
          child: Icon(file.icon, color: file.color),
        ),
        title: Text(file.name, maxLines: 1, overflow: TextOverflow.ellipsis, style: const TextStyle(fontWeight: FontWeight.w800)),
        subtitle: Text(file.meta, maxLines: 1, overflow: TextOverflow.ellipsis),
        trailing: IconButton(tooltip: 'Remove', onPressed: onRemove, icon: const Icon(Icons.close)),
      ),
    );
  }
}

class SettingsSheet extends StatelessWidget {
  const SettingsSheet({super.key, required this.home});

  final _LibreNotesHomeState home;

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: ConstrainedBox(
        constraints: BoxConstraints(maxHeight: MediaQuery.sizeOf(context).height * 0.88),
        child: ListView(
          padding: const EdgeInsets.fromLTRB(20, 0, 20, 24),
          children: [
            Text('Settings', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900)),
            const SizedBox(height: 16),
            SettingBlock(
              title: 'Theme',
              child: SegmentedButton<ThemeMode>(
                segments: const [
                  ButtonSegment(value: ThemeMode.system, icon: Icon(Icons.monitor_outlined), label: Text('Auto')),
                  ButtonSegment(value: ThemeMode.light, icon: Icon(Icons.light_mode_outlined), label: Text('Light')),
                  ButtonSegment(value: ThemeMode.dark, icon: Icon(Icons.dark_mode_outlined), label: Text('Dark')),
                ],
                selected: {home.themeMode},
                onSelectionChanged: (value) => home.onThemeModeChanged(value.first),
              ),
            ),
            SwitchListTile(
              title: const Text('AMOLED dark'),
              subtitle: const Text('Use true black app surfaces in dark mode.'),
              value: home.amoledDark,
              onChanged: home.onAmoledChanged,
            ),
            SwitchListTile(
              title: const Text('Accent-tinted background'),
              subtitle: const Text('Tint large surfaces with the selected accent color.'),
              value: home.accentTintBackground,
              onChanged: home.onAccentTintChanged,
            ),
            SwitchListTile(
              title: const Text('Letter folder icons'),
              subtitle: const Text('Show folder initials when there is room.'),
              value: home.letterIcons,
              onChanged: home.onLetterIconsChanged,
            ),
            SwitchListTile(
              title: const Text('Dense notes'),
              subtitle: const Text('Fit more notes on smaller screens.'),
              value: home.denseNotes,
              onChanged: home.onDenseNotesChanged,
            ),
            SettingBlock(
              title: 'Accent color',
              child: Wrap(
                spacing: 10,
                runSpacing: 10,
                children: [
                  for (final color in accentChoices)
                    InkWell(
                      onTap: () => home.onAccentChanged(color),
                      borderRadius: BorderRadius.circular(999),
                      child: CircleAvatar(
                        radius: 22,
                        backgroundColor: color,
                        child: home.accent == color ? const Icon(Icons.check, color: Colors.white) : null,
                      ),
                    ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class SettingBlock extends StatelessWidget {
  const SettingBlock({super.key, required this.title, required this.child});

  final String title;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: const TextStyle(fontWeight: FontWeight.w900)),
          const SizedBox(height: 10),
          child,
        ],
      ),
    );
  }
}

class StatusBar extends StatelessWidget {
  const StatusBar({super.key, required this.note});

  final Note note;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 36,
      padding: const EdgeInsets.symmetric(horizontal: 16),
      decoration: BoxDecoration(border: Border(top: BorderSide(color: Theme.of(context).dividerColor.withValues(alpha: 0.35)))),
      child: Row(
        children: [
          Text(relativeDate(note.updatedAt)),
          const Spacer(),
          Text('${note.wordCount} words'),
          const Spacer(),
          const Icon(Icons.check_circle_outline, size: 16),
          const SizedBox(width: 6),
          const Text('Autosaved'),
        ],
      ),
    );
  }
}

class EmptyState extends StatelessWidget {
  const EmptyState({
    super.key,
    required this.icon,
    required this.title,
    required this.action,
    required this.onPressed,
  });

  final IconData icon;
  final String title;
  final String action;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 42, color: Theme.of(context).colorScheme.primary),
            const SizedBox(height: 10),
            Text(title, textAlign: TextAlign.center, style: const TextStyle(fontWeight: FontWeight.w900)),
            const SizedBox(height: 12),
            FilledButton(onPressed: onPressed, child: Text(action)),
          ],
        ),
      ),
    );
  }
}

class NoteEditResult {
  const NoteEditResult(this.title, this.folder, this.tag);

  final String title;
  final String folder;
  final String tag;
}

class Note {
  const Note({
    required this.id,
    required this.title,
    required this.folder,
    required this.tag,
    required this.summary,
    required this.body,
    required this.updatedAt,
    required this.wordCount,
    required this.attachments,
    this.starred = false,
  });

  final String id;
  final String title;
  final String folder;
  final String tag;
  final String summary;
  final String body;
  final DateTime updatedAt;
  final int wordCount;
  final List<AttachmentFile> attachments;
  final bool starred;

  factory Note.fromJson(Map<String, dynamic> json) {
    final body = json['body'] as String? ?? '';
    return Note(
      id: json['id'] as String? ?? DateTime.now().microsecondsSinceEpoch.toString(),
      title: json['title'] as String? ?? 'Untitled note',
      folder: json['folder'] as String? ?? 'Inbox',
      tag: json['tag'] as String? ?? '#draft',
      summary: json['summary'] as String? ?? 'Saved note',
      body: body,
      updatedAt: DateTime.tryParse(json['updatedAt'] as String? ?? '') ?? DateTime.now(),
      wordCount: json['wordCount'] as int? ?? countWords(body),
      attachments: ((json['attachments'] as List<dynamic>?) ?? const [])
          .map((name) => AttachmentFile.fromName(name as String))
          .toList(),
      starred: json['starred'] as bool? ?? false,
    );
  }

  Note copyWith({
    String? title,
    String? folder,
    String? tag,
    String? summary,
    String? body,
    DateTime? updatedAt,
    int? wordCount,
    List<AttachmentFile>? attachments,
    bool? starred,
  }) {
    return Note(
      id: id,
      title: title ?? this.title,
      folder: folder ?? this.folder,
      tag: tag ?? this.tag,
      summary: summary ?? this.summary,
      body: body ?? this.body,
      updatedAt: updatedAt ?? this.updatedAt,
      wordCount: wordCount ?? this.wordCount,
      attachments: attachments ?? this.attachments,
      starred: starred ?? this.starred,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'title': title,
      'folder': folder,
      'tag': tag,
      'summary': summary,
      'body': body,
      'updatedAt': updatedAt.toIso8601String(),
      'wordCount': wordCount,
      'attachments': attachments.map((file) => file.name).toList(),
      'starred': starred,
    };
  }
}

class AttachmentFile {
  const AttachmentFile({
    required this.name,
    required this.meta,
    required this.icon,
    required this.color,
  });

  factory AttachmentFile.fromName(String name) {
    final lower = name.toLowerCase();
    if (lower.endsWith('.pdf')) {
      return AttachmentFile(name: name, meta: 'PDF document', icon: Icons.picture_as_pdf_outlined, color: Colors.orange);
    }
    if (lower.endsWith('.png') || lower.endsWith('.jpg') || lower.endsWith('.jpeg') || lower.endsWith('.webp')) {
      return AttachmentFile(name: name, meta: 'Image preview', icon: Icons.image_outlined, color: Colors.green);
    }
    if (lower.endsWith('.m4a') || lower.endsWith('.mp3') || lower.endsWith('.wav')) {
      return AttachmentFile(name: name, meta: 'Audio note', icon: Icons.graphic_eq, color: Colors.blue);
    }
    return AttachmentFile(name: name, meta: 'Linked file', icon: Icons.insert_drive_file_outlined, color: Colors.purple);
  }

  final String name;
  final String meta;
  final IconData icon;
  final Color color;
}

IconData folderIcon(String name, {bool selected = false}) {
  return switch (name) {
    'All' => Icons.all_inbox_outlined,
    'Starred' => selected ? Icons.star : Icons.star_border,
    'Inbox' => Icons.inbox_outlined,
    'Projects' => selected ? Icons.folder : Icons.folder_outlined,
    'Study' => Icons.school_outlined,
    'Personal' => Icons.person_outline,
    _ => selected ? Icons.folder : Icons.folder_outlined,
  };
}

List<String> notesTags(List<Note> notes) {
  final tags = notes.map((note) => note.tag).toSet().toList()..sort();
  return tags;
}

int countWords(String text) {
  return RegExp(r"[A-Za-z0-9_']+").allMatches(text).length;
}

String relativeDate(DateTime date) {
  final diff = DateTime.now().difference(date);
  if (diff.inMinutes < 1) {
    return 'Just now';
  }
  if (diff.inHours < 1) {
    return '${diff.inMinutes}m ago';
  }
  if (diff.inDays < 1) {
    return '${diff.inHours}h ago';
  }
  if (diff.inDays == 1) {
    return 'Yesterday';
  }
  return '${diff.inDays}d ago';
}

const vaultStorageKey = 'libre_notes_vault_v1';

final sampleUpdatedAt = DateTime(2026, 4, 29, 19, 45);

final seedNotes = [
  Note(
    id: 'roadmap',
    title: 'Project Roadmap',
    folder: 'Projects',
    tag: '#planning',
    summary: 'Milestones for turning Libre Notes into a polished local-first Android notes app.',
    updatedAt: sampleUpdatedAt,
    wordCount: countWords(roadmapMarkdown),
    starred: true,
    attachments: const [
      AttachmentFile(name: 'lecture-outline.pdf', meta: 'PDF document', icon: Icons.picture_as_pdf_outlined, color: Colors.orange),
      AttachmentFile(name: 'whiteboard-photo.jpg', meta: 'Image preview', icon: Icons.image_outlined, color: Colors.green),
    ],
    body: roadmapMarkdown,
  ),
  Note(
    id: 'editor',
    title: 'Editor Improvements',
    folder: 'Projects',
    tag: '#ideas',
    summary: 'Android editing improvements with tabs, drawer actions, save flow, and preview parity.',
    updatedAt: sampleUpdatedAt.subtract(const Duration(hours: 3)),
    wordCount: countWords(editorMarkdown),
    attachments: const [
      AttachmentFile(name: 'meeting-audio.m4a', meta: 'Audio note', icon: Icons.graphic_eq, color: Colors.blue),
    ],
    body: editorMarkdown,
  ),
  Note(
    id: 'privacy',
    title: 'Privacy Principles',
    folder: 'Personal',
    tag: '#reference',
    summary: 'Local-first promises and product boundaries for a privacy-respecting notes app.',
    updatedAt: sampleUpdatedAt.subtract(const Duration(days: 1)),
    wordCount: countWords(privacyMarkdown),
    starred: true,
    attachments: const [],
    body: privacyMarkdown,
  ),
  Note(
    id: 'meeting',
    title: 'Meeting Notes - 2026-04-25',
    folder: 'Study',
    tag: '#meeting',
    summary: 'MVP decisions, Android responsive layout notes, and next implementation steps.',
    updatedAt: sampleUpdatedAt.subtract(const Duration(days: 3)),
    wordCount: countWords(meetingMarkdown),
    attachments: const [],
    body: meetingMarkdown,
  ),
];

const roadmapMarkdown = '''
# Project Roadmap

Libre Notes is a local-first Markdown notebook for quick capture, clean reading, and organized project work.

## Goals

- Make editing comfortable on small Android phones
- Keep preview one tap away, like SimpleMarkdown
- Support side-by-side editing on tablets and desktop
- Make folders, search, note actions, and attachments functional

## MVP checklist

- [x] Responsive Android layout
- [x] Live Markdown preview
- [x] Note creation and editing
- [x] Folder filtering and starred notes
- [x] Attachment management
- [ ] Real file-system persistence

## Release focus

| Area | Status | Notes |
| --- | --- | --- |
| Android | Active | Primary build target |
| Web | Later | Keep code portable |
| Windows | Later | Restore after Android stabilizes |

> Keep the first release small, fast, and honest.
''';

const editorMarkdown = '''
# Editor Improvements

The editor should feel like a real writing surface, not a resized desktop app.

## Android behavior

1. Notes list is its own screen on phones.
2. Editing and preview use tabs or bottom navigation.
3. Tablets can keep the note list visible while editing.
4. Wide displays get a split editor and preview.

```md
Use Markdown every day.
Keep the preview live.
Avoid overflowing controls.
```
''';

const privacyMarkdown = '''
# Privacy Principles

Libre Notes should be useful without requiring an account or network connection.

- Local notes stay local by default
- Attachments belong to the vault
- Sync should be optional and transparent
- Export should use plain Markdown whenever possible

## Product promise

The app should never make users wonder where their writing went.
''';

const meetingMarkdown = '''
# Meeting Notes - 2026-04-25

## Decisions

- Android build is the first CI priority.
- The desktop layout must collapse before it becomes cramped.
- Preview needs a maintained Markdown renderer, not a hand-rolled parser.

## Follow-ups

- Add persistent storage
- Add import and export
- Add tests after the Android UI stabilizes
''';

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
