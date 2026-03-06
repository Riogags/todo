# Rust Todo App - Implementation Plan

## Overview

A **command-line todo app** built with Rust. You interact with it via terminal commands to add, list, complete, and delete tasks. Todos are saved to a JSON file so they persist between sessions.

---

## What You'll Get

A CLI app you use like this:

```bash
todo add "Buy groceries"          # Add a new task
todo list                         # Show all tasks
todo done 1                       # Mark task #1 as completed
todo remove 1                     # Delete task #1
```

Output example:

```
 Your Todos:
 [ ] 1. Buy groceries
 [x] 2. Learn Rust
 [ ] 3. Walk the dog
```

---

## Tech Stack

| Component        | Choice                                                  |
| ---------------- | ------------------------------------------------------- |
| Language          | Rust 1.93                                               |
| CLI parsing       | `clap` (the standard Rust crate for command-line args)  |
| Data storage      | JSON file (`todos.json` in current directory)           |
| Serialization     | `serde` + `serde_json` (the standard for JSON in Rust)  |

---

## Project Structure

```
todo/
├── Cargo.toml          # Project manifest (dependencies, metadata)
├── src/
│   └── main.rs         # All app logic (single file, ~150 lines)
└── todos.json          # Created at runtime when you add your first todo
```

Since this is a small learning project, everything lives in a single `main.rs` file. No need for multiple modules.

---

## Features

1. **Add a todo** - provide a description, it gets saved with `completed: false`
2. **List all todos** - displays each todo with its index, status `[ ]` or `[x]`, and description
3. **Mark as done** - toggle a todo's status to completed by its number
4. **Remove a todo** - delete a todo by its number
5. **Persistent storage** - todos are saved to `todos.json` automatically

---

## Implementation Steps

### Step 1: Initialize the Cargo project

Run `cargo init` to create `Cargo.toml` and `src/main.rs`.

### Step 2: Add dependencies to `Cargo.toml`

- `clap` with `derive` feature - for parsing CLI arguments using Rust's derive macros
- `serde` with `derive` feature - for serializing/deserializing the Todo struct
- `serde_json` - for reading/writing JSON

### Step 3: Write `src/main.rs`

The file will contain:

- **`Todo` struct** - fields: `description` (String), `completed` (bool)
- **`Command` enum** (via clap) - defines the 4 subcommands: `add`, `list`, `done`, `remove`
- **`load_todos()`** - reads `todos.json` from disk, returns empty list if file doesn't exist
- **`save_todos()`** - writes the current list to `todos.json`
- **`main()`** - parses the command and calls the appropriate logic

### Step 4: Build and test

Compile with `cargo build --release` and run the binary, or use `cargo run -- <command>`.

---

## What You'll Learn Along the Way

| Rust Concept              | Where It Appears                              |
| ------------------------- | --------------------------------------------- |
| Structs and enums         | `Todo` struct, `Command` enum                 |
| Derive macros             | `#[derive(Serialize, Deserialize, Parser)]`   |
| Error handling (`Result`) | File I/O with `?` operator                    |
| Ownership & borrowing     | Passing todos around functions                |
| Pattern matching          | `match` on the CLI command                    |
| Vectors (`Vec<T>`)        | The list of todos                             |
| File I/O                  | Reading/writing `todos.json`                  |
| External crates           | Using `clap`, `serde`, `serde_json`           |

---

## Possible Future Enhancements (not included now)

- Priority levels (low, medium, high)
- Due dates
- Categories/tags
- Search/filter todos
- Colored terminal output
- SQLite instead of JSON

---

## Ready?

Review this plan and let me know if you'd like to:

- **Change any features** (add/remove commands, change storage format, etc.)
- **Change the UI** (different output format, colors, etc.)
- **Adjust complexity** (split into multiple files, add tests, etc.)

Once you approve, I'll generate all the code.
