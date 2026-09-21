# Student Management System 

A lightweight, zero-dependency Student Management System built using standard Java (`com.sun.net.httpserver.HttpServer`). 

This project demonstrates how to build a functional web application with a JSON REST API and static file serving without relying on Spring Boot, Tomcat, or third-party libraries.

---

## Features

- **Zero External Dependencies:** Runs on plain JDK 11+ standard library.
- **RESTful API:** Implements complete CRUD (Create, Read, Update, Delete) operations under `/api/students`.
- **Static File Serving:** Built-in web server to serve frontend assets (`HTML`, `CSS`, `JS`, `SVG`) with path traversal protection.
- **Thread-Safe In-Memory Store:** Uses `ConcurrentHashMap` combined with a multi-threaded HTTP worker pool (`Executors.newFixedThreadPool`).

---

## Project Structure

```text
.
├── src/
│   ├── StudentManagementSystem.java   # HTTP server, routing, and controllers
│   ├── Student.java                   # Student entity model
│   └── Json.java                      # Custom lightweight JSON serializer/parser
├── web/
│   ├── index.html                     # Frontend interface
│   ├── style.css                      # Styling
│   └── script.js                      # Client-side API interactions
└── README.md
