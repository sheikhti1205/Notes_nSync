import React from 'react';
import ReactDOM from 'react-dom/client';
import {
  Archive,
  BadgeCheck,
  BookOpenText,
  Check,
  ChevronRight,
  Download,
  Eye,
  FileText,
  Fingerprint,
  FolderLock,
  KeyRound,
  LayoutDashboard,
  LockKeyhole,
  Moon,
  Palette,
  Paperclip,
  Search,
  Settings,
  ShieldCheck,
  Smartphone,
  Sparkles,
  Star,
  Sun,
  Upload,
  type LucideIcon,
} from 'lucide-react';
import { marked } from 'marked';
import './styles.css';

type SetupStep = readonly [
  title: string,
  detail: string,
  Icon: LucideIcon,
];

const markdown = `# Project Roadmap

Libre Notes is a local-first Markdown notebook for focused writing.

## This sprint

- [x] First-run presenter
- [x] Encrypted vault setup
- [ ] Android native parity

> Notes should stay readable, portable, and private.

\`\`\`kotlin
val vault = "libre-notes.vault"
\`\`\`
`;

const notes = [
  { title: 'Project Roadmap', folder: 'Projects', tag: '#planning', words: 73, active: true },
  { title: 'Privacy checklist', folder: 'Security', tag: '#vault', words: 112, active: false },
  { title: 'Study outline', folder: 'Study', tag: '#markdown', words: 94, active: false },
];

const settings = [
  ['Profile', 'Local display name and workspace identity'],
  ['Vault & Security', 'Folder, encryption, PIN/password, biometric unlock'],
  ['Appearance', 'Theme, accent color, density, motion'],
  ['Editor', 'Markdown assist, preview behavior, attachment display'],
  ['Export', 'Default format and rendered export choices'],
  ['Backup/Restore', 'Encrypted vault file and recovery actions'],
  ['About', 'Libre Notes version, license, and build details'],
];

const setupSteps: SetupStep[] = [
  ['Choose folder', 'Documents/Libre Notes', FolderLock],
  ['Create PIN or password', 'Digits now, symbols optional', KeyRound],
  ['Enable fingerprint', 'Fast unlock after password', Fingerprint],
  ['Pick preferences', 'Theme, accent, export defaults', Palette],
];

function App() {
  return (
    <main>
      <header className="topbar">
        <div className="brand">
          <img src="/icon.jpg" alt="" />
          <div>
            <strong>Libre Notes</strong>
            <span>native rebuild preview</span>
          </div>
        </div>
        <nav>
          <button className="ghost"><Search size={18} /> Search</button>
          <button className="ghost"><Moon size={18} /> Dark</button>
          <button className="primary"><Download size={18} /> Export</button>
        </nav>
      </header>

      <section className="hero">
        <div className="hero-copy">
          <span className="eyebrow"><Sparkles size={16} /> First-run presenter</span>
          <h1>Libre Notes</h1>
          <p>
            A private Markdown workspace that opens with a clear setup path, stores notes in one encrypted vault file,
            and exports rendered documents without losing structure.
          </p>
          <div className="hero-actions">
            <button className="primary"><FolderLock size={18} /> Set up encrypted vault</button>
            <button className="secondary"><Eye size={18} /> Preview workspace</button>
          </div>
        </div>
        <div className="setup-panel" aria-label="Setup checklist">
          <div className="panel-title">
            <ShieldCheck />
            <div>
              <strong>Setup</strong>
              <span>Required before first vault use</span>
            </div>
          </div>
          {setupSteps.map(([title, detail, Icon], index) => (
            <div className="step" key={title}>
              <span className={index < 2 ? 'done' : ''}>{index < 2 ? <Check size={14} /> : index + 1}</span>
              <Icon size={18} />
              <div>
                <strong>{title}</strong>
                <small>{detail}</small>
              </div>
            </div>
          ))}
        </div>
      </section>

      <section className="workspace">
        <aside className="sidebar">
          <div className="vault-card">
            <LockKeyhole />
            <strong>libre-notes.vault</strong>
            <span>Encrypted, selected folder</span>
          </div>
          {['All Notes', 'Projects', 'Security', 'Study', 'Archive'].map((item) => (
            <button className={item === 'Projects' ? 'nav-item active' : 'nav-item'} key={item}>
              <LayoutDashboard size={18} /> {item}
            </button>
          ))}
        </aside>

        <section className="note-list">
          <div className="section-head">
            <div>
              <strong>Notes</strong>
              <span>Markdown-first, local-first</span>
            </div>
            <button className="icon"><Star size={17} /></button>
          </div>
          {notes.map((note) => (
            <article className={note.active ? 'note-card active' : 'note-card'} key={note.title}>
              <strong>{note.title}</strong>
              <p>{note.folder} · {note.tag} · {note.words} words</p>
            </article>
          ))}
        </section>

        <section className="editor">
          <div className="section-head">
            <div>
              <strong>Editor</strong>
              <span>Raw Markdown stays portable</span>
            </div>
            <div className="segmented"><button>Edit</button><button>Preview</button></div>
          </div>
          <pre>{markdown}</pre>
        </section>

        <section className="preview">
          <div className="section-head">
            <div>
              <strong>Preview</strong>
              <span>Shared export renderer</span>
            </div>
            <BookOpenText size={20} />
          </div>
          <div className="markdown" dangerouslySetInnerHTML={{ __html: marked.parse(markdown) }} />
        </section>
      </section>

      <section className="lower-grid">
        <article className="card export-card">
          <div className="section-head">
            <div>
              <strong>Rendered exports</strong>
              <span>Markdown, HTML, PDF, DOC</span>
            </div>
            <Upload />
          </div>
          {['Markdown source is exact', 'HTML renders headings, lists, code, quotes', 'PDF/DOC use the same structure pipeline'].map((item) => (
            <p className="checkline" key={item}><BadgeCheck size={17} /> {item}</p>
          ))}
        </article>

        <article className="card lock-card">
          <div className="phone">
            <Smartphone size={22} />
            <Fingerprint size={54} />
            <strong>Unlock Libre Notes</strong>
            <span>Fingerprint or password after phone lock</span>
            <div className="pin"><i /><i /><i /><i /></div>
          </div>
        </article>

        <article className="card settings-card">
          <div className="section-head">
            <div>
              <strong>Settings</strong>
              <span>Structured for growth</span>
            </div>
            <Settings />
          </div>
          <div className="settings-list">
            {settings.map(([title, detail]) => (
              <div key={title}>
                <FileText size={17} />
                <span><strong>{title}</strong><small>{detail}</small></span>
                <ChevronRight size={16} />
              </div>
            ))}
          </div>
        </article>

        <article className="card tokens-card">
          <strong>Shared design tokens</strong>
          <div className="swatches"><i /><i /><i /><i /></div>
          <p>These colors, radii, spacing, density rules, and controls are intended to be matched in the native Android app.</p>
          <div className="toolbar"><Sun /><Moon /><Paperclip /><Archive /></div>
        </article>
      </section>
    </main>
  );
}

ReactDOM.createRoot(document.getElementById('root')!).render(<App />);
