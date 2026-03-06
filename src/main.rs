use clap::{Parser, Subcommand};
use serde::{Deserialize, Serialize};
use std::fs;
use std::path::PathBuf;

const TODO_FILE: &str = "todos.json";

#[derive(Serialize, Deserialize)]
struct Todo {
    description: String,
    completed: bool,
}

#[derive(Parser)]
#[command(name = "todo", about = "A simple CLI todo app")]
struct Cli {
    #[command(subcommand)]
    command: Command,
}

#[derive(Subcommand)]
enum Command {
    /// Add a new todo
    Add {
        /// The todo description
        description: String,
    },
    /// List all todos
    List,
    /// Mark a todo as done
    Done {
        /// The todo number (from `todo list`)
        number: usize,
    },
    /// Remove a todo
    Remove {
        /// The todo number (from `todo list`)
        number: usize,
    },
}

fn todo_path() -> PathBuf {
    PathBuf::from(TODO_FILE)
}

fn load_todos() -> Vec<Todo> {
    let path = todo_path();
    if !path.exists() {
        return Vec::new();
    }
    let data = fs::read_to_string(&path).expect("Failed to read todos file");
    serde_json::from_str(&data).expect("Failed to parse todos file")
}

fn save_todos(todos: &[Todo]) {
    let data = serde_json::to_string_pretty(todos).expect("Failed to serialize todos");
    fs::write(todo_path(), data).expect("Failed to write todos file");
}

fn main() {
    let cli = Cli::parse();

    match cli.command {
        Command::Add { description } => {
            let mut todos = load_todos();
            todos.push(Todo {
                description: description.clone(),
                completed: false,
            });
            save_todos(&todos);
            println!("Added: \"{}\"", description);
        }

        Command::List => {
            let todos = load_todos();
            if todos.is_empty() {
                println!("No todos yet! Add one with: todo add \"your task\"");
                return;
            }
            println!("\n Your Todos:\n");
            for (i, todo) in todos.iter().enumerate() {
                let status = if todo.completed { "x" } else { " " };
                println!("  [{}] {}. {}", status, i + 1, todo.description);
            }
            println!();
        }

        Command::Done { number } => {
            let mut todos = load_todos();
            if number == 0 || number > todos.len() {
                eprintln!("Invalid todo number: {}. Use `todo list` to see valid numbers.", number);
                std::process::exit(1);
            }
            todos[number - 1].completed = true;
            save_todos(&todos);
            println!("Marked as done: \"{}\"", todos[number - 1].description);
        }

        Command::Remove { number } => {
            let mut todos = load_todos();
            if number == 0 || number > todos.len() {
                eprintln!("Invalid todo number: {}. Use `todo list` to see valid numbers.", number);
                std::process::exit(1);
            }
            let removed = todos.remove(number - 1);
            save_todos(&todos);
            println!("Removed: \"{}\"", removed.description);
        }
    }
}
