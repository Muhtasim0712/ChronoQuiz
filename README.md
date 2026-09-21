# ChronoQuiz — Timed Desktop Examination System

ChronoQuiz is a high-precision, JavaFX-based desktop examination and quiz application designed for strictly timed multiple-choice (MCQ) and short-answer exams with SQLite persistence, multithreaded background timers, and online trivia API synchronization.

---

## Key Features

### 1. Dedicated Administrator Control Panel
- **Password-Protected Access**: Admin features are locked behind password authentication (Default: `admin123`, stored in SQLite and customizable inside the panel).
- **Active Exam Parameter Management**:
  - Set exam time limits in seconds (with presets e.g., 30s, 45s, 60s, 90s, 120s, 180s, 300s).
  - Set total question count served to participants (3, 5, 10, 15, 20, 25, or custom).
  - Select exam category/subject or "All Categories (Mixed)".
  - Choose exam difficulty level (`ANY`, `EASY`, `MEDIUM`, `HARD`).
  - Choose question source (`Local Database (SQLite)` or `Online Trivia API (OpenTDB)`).
  - Changes saved are instantly active for all participants.
- **Admin Question Bank Manager**:
  - Full CRUD operations: Add new questions, edit existing questions, and delete questions.
  - **Fixed Question Prompt Visibility**: High-contrast, vividly visible white text rendering in the Question Editor `TextArea` and answer fields.
  - Batch import questions from the online OpenTDB API with duplicate prevention.
  - Import and export question banks to/from JSON files.
- **Participant Attempt History & Analytics**:
  - Inspect all participant submissions with timestamps, scores, percentages, and time taken.
  - Double-click any submission or select "Review" for a full side-by-side answer key inspection.
  - Clear history or filter attempts by category.
- **Security & Password Management**:
  - Securely update the administrator password directly from the panel.

### 2. Participant Examination Portal
- **Participant Sign-In**: Participants only enter their Participant ID or Name (e.g. `STU-2024-001` or `Alex`) to start the exam.
- **Pre-Configured Specifications**: All parameters (questions, category, difficulty, time limit) are enforced by the administrator and clearly displayed on the entry card.
- **Timer Lockdown**: The countdown timer runs non-stop in the background and cannot be paused, reset, or altered by participants.
- **Automatic Submission on Timeout**: If the time expires before completion, ChronoQuiz captures the active response and submits automatically.

---

## Default Credentials
- **Admin Password**: `admin123`

---

## How to Run

### Option 1: Double-Click Batch Runner (Windows)
Double-click `run.bat` in the project root directory.

### Option 2: Maven Command Line
```powershell
mvn clean compile exec:java -Dexec.mainClass="com.chronoquiz.Launcher"
```

### Option 3: Run Tests
```powershell
mvn test
```