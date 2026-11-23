# 📝 Assignment Manager

A modern, robust Java Swing desktop application designed for students and educators to manage assignments, deadlines, and task statuses with optional MySQL persistence and automatic offline in-memory fallback.

---

## 🚀 Key Features & Highlights

- **Dual-Mode Operation (Offline / MySQL)**:
  - **No MySQL? No problem!** The app runs seamlessly in **Local In-Memory Mode** if MySQL is not installed or unreachable (ideal for quick testing on Windows without installing anything).
  - When MySQL is available (e.g. on Ubuntu or via Docker), it connects automatically and persists all records.
- **Full CRUD Support**:
  - ➕ **Add**: Create assignments with validation (positive IDs, required fields, duplicate ID detection).
  - ✏️ **Update / Edit**: Click any row to automatically load its details into the sidebar, edit, and click "Update Selected".
  - ❌ **Delete**: Delete selected assignments directly by ID (immune to table sorting/filtering bugs).
  - ↩️ **Undo (Pop)**: True undo operation that removes the most recently added assignment.
  - 🗑️ **Clear All**: Reset all records with confirmation safety check.
- **Search & Real-Time Filtering**:
  - Dynamic filter bar above the table searches in real-time across ID, Course Code, Title, and Status.
- **Status & Deadline Tracking**:
  - Track **Pending** vs. **Completed** assignments with distinct visual status badges.
  - **Double-click** any row in the table to instantly toggle its status between Pending and Completed.
  - Right-click context menu for quick actions.
- **Live Statistics Dashboard**:
  - Real-time counters: **Total Assignments**, **Pending**, and **Completed**.
  - Database status pill showing connection mode (🟢 MySQL Online / 🟠 In-Memory Mode).
- **In-App Database Configuration**:
  - Click **⚙️ DB Settings** to test, change host, port, credentials, and reconnect to MySQL on the fly without restarting.
- **Export to CSV**:
  - Export all assignments to a standard `.csv` file anytime.

---

## 🛠️ Glitches Analyzed & Fixed

| # | Glitch Identified | Root Cause | Fix Applied |
|---|-------------------|------------|-------------|
| 1 | **Wrong item deleted** | Deletion computed stack index via `size - 1 - selectedRow`, which breaks whenever the table is sorted or filtered. | Converted row selection to model index and retrieved the actual `targetId` directly from column 0 (`(int) tableModel.getValueAt(modelRow, 0)`). |
| 2 | **Sort ruined Stack LIFO order** | `assignmentStack.sort()` inverted the stack order; subsequent `push()` and `pop()` operations corrupted the data flow. | Maintained a distinct `undoStack` for LIFO undo history while keeping the display model cleanly sorted. |
| 3 | **No Edit / Update capability** | Once an assignment was added, users had to delete and retype it to fix mistakes. | Added row selection listener that populates the sidebar form and an `✏️ Update Selected` button. |
| 4 | **Search was limited & intrusive** | Required opening a modal popup and only matched exact integer IDs. | Integrated a real-time live search filter bar directly above the table supporting multi-field keyword matching. |
| 5 | **Windows encoding crash** | Emojis in source strings caused `javac` compilation failure on Windows with default `windows-1252` code page. | Added `-encoding UTF-8` configuration and used Unicode escape sequences for cross-platform resilience. |
| 6 | **Ant build target mismatch** | `nbproject/project.properties` targeted `source=25` and lacked JDBC driver classpath. | Configured `javac.source=17`, `javac.target=17`, and bundled `mysql-connector-j-8.4.0.jar` in `lib/`. |

---

## 🐧 Instructions for Your Friend (Ubuntu Linux)

Your friend on Ubuntu can set up MySQL and seed the database in **under 30 seconds** with the included automated script:

### Step 1: Run the Ubuntu Seeder Script
Open the terminal in the project folder and run:
```bash
chmod +x seed_db.sh scripts/seed_ubuntu.sh run.sh
./seed_db.sh
```

**What the script does automatically**:
1. Checks if `mysql-server` is installed (offers to install via `apt` if missing).
2. Starts and enables the MySQL service.
3. Creates the database `assignment_db`.
4. Creates user `assignment_user` with password `assignment_pass`.
5. Executes `database/schema.sql` to build tables and insert 6 sample assignments.
6. Automatically updates `db.properties`.

### Step 2: Run the App on Ubuntu
```bash
./run.sh
```

---

## 🐳 Alternative: Zero-Install MySQL via Docker (Any OS)

If you or your friend prefer not to install MySQL on the OS directly, use Docker Compose:
```bash
docker compose up -d
```
This spins up a pre-configured MySQL 8.0 container on port 3306 and automatically runs `database/schema.sql`.

---

## 💻 Instructions for You (Windows - No MySQL Required)

Since you don't want to install MySQL on your Windows machine:
1. Double-click **`run.bat`** (or open PowerShell / CMD and run `.\run.bat`).
2. The application will detect that MySQL is offline and automatically launch in **Local In-Memory Mode**:
   - You can add, edit, delete, search, sort, toggle status, and export to CSV immediately.
   - The status bar will show `○ In-Memory Mode`.
3. If you ever want to connect to a remote MySQL server or a cloud database, click **⚙️ DB Settings** in the top bar, enter the connection details, and click **Test Connection** & **Save & Connect**.

---

## 📂 Project Architecture

```
AssignmentManager/
├── database/
│   └── schema.sql                # MySQL DDL & Seed Data SQL script
├── lib/
│   └── mysql-connector-j-8.4.0.jar # MySQL JDBC Driver
├── nbproject/                    # NetBeans / Ant Project metadata
├── scripts/
│   └── seed_ubuntu.sh            # Full Ubuntu MySQL provisioning & seeding script
├── src/
│   └── assignmentmanager/
│       ├── Assignment.java       # Model entity
│       ├── DatabaseManager.java  # JDBC DAO & connection manager with fallback
│       ├── DbConfigDialog.java   # Settings dialog for MySQL configuration
│       └── AssignmentManager.java# Modern Swing UI application
├── db.properties                 # Connection configuration (auto-generated)
├── docker-compose.yml            # Instant Docker MySQL environment
├── run.bat                       # One-click launcher for Windows
├── run.sh                        # One-click launcher for Linux
├── seed_db.sh                    # Seeding shortcut for Ubuntu
└── README.md                     # Documentation
```
