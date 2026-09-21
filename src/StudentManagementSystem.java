import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Student Management System - plain Java edition.
 *
 * No Spring Boot, no external dependencies. Uses the JDK's built in
 * com.sun.net.httpserver.HttpServer to:
 *   1. Serve the front-end (HTML/CSS/JS) from the ./web folder.
 *   2. Expose a small JSON REST API under /api/students for CRUD.
 *
 * Run:
 *   javac -d out src/*.java
 *   java -cp out StudentManagementSystem
 *
 * Then open http://localhost:8080 in a browser.
 */
public class StudentManagementSystem {

    private static final int PORT = 8080;
    private static final String WEB_ROOT = "web";

    // In-memory "database". ConcurrentHashMap keeps it thread-safe since
    // HttpServer handles each request on its own worker thread.
    private static final Map<Integer, Student> STUDENTS = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        seedSampleData();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // REST API
        server.createContext("/api/students", new StudentsApiHandler());

        // Static front-end files (index.html, style.css, script.js, ...)
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("Student Management System running at http://localhost:" + PORT);
    }

    private static void seedSampleData() {
        addStudent(new Student("Asha Rao", "asha.rao@example.com", "B.Sc Computer Science", 87.5));
        addStudent(new Student("Vikram Iyer", "vikram.iyer@example.com", "B.Com", 74.0));
        addStudent(new Student("Meera Nair", "meera.nair@example.com", "B.A English", 91.2));
    }

    private static void addStudent(Student s) {
        STUDENTS.put(s.getId(), s);
    }

    // ------------------------------------------------------------------
    // REST API handler: /api/students  and  /api/students/{id}
    // ------------------------------------------------------------------
    static class StudentsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath(); // e.g. /api/students/1001
            String[] parts = path.split("/");                 // ["", "api", "students", "1001"]
            Integer id = null;
            if (parts.length >= 4 && !parts[3].isEmpty()) {
                try { id = Integer.parseInt(parts[3]); } catch (NumberFormatException ignored) { }
            }

            try {
                switch (method) {
                    case "GET":
                        if (id == null) {
                            handleList(exchange);
                        } else {
                            handleGetOne(exchange, id);
                        }
                        break;
                    case "POST":
                        handleCreate(exchange);
                        break;
                    case "PUT":
                        if (id == null) {
                            sendJson(exchange, 400, "{\"error\":\"Student id required in URL\"}");
                        } else {
                            handleUpdate(exchange, id);
                        }
                        break;
                    case "DELETE":
                        if (id == null) {
                            sendJson(exchange, 400, "{\"error\":\"Student id required in URL\"}");
                        } else {
                            handleDelete(exchange, id);
                        }
                        break;
                    default:
                        exchange.getResponseHeaders().add("Allow", "GET, POST, PUT, DELETE");
                        sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
                }
            } catch (Exception e) {
                sendJson(exchange, 500, "{\"error\":" + Json.quote("Server error: " + e.getMessage()) + "}");
            }
        }

        private void handleList(HttpExchange exchange) throws IOException {
            Collection<Student> all = STUDENTS.values();
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Student s : all) {
                if (!first) sb.append(",");
                sb.append(s.toJson());
                first = false;
            }
            sb.append("]");
            sendJson(exchange, 200, sb.toString());
        }

        private void handleGetOne(HttpExchange exchange, int id) throws IOException {
            Student s = STUDENTS.get(id);
            if (s == null) {
                sendJson(exchange, 404, "{\"error\":\"Student not found\"}");
                return;
            }
            sendJson(exchange, 200, s.toJson());
        }

        private void handleCreate(HttpExchange exchange) throws IOException {
            Map<String, String> body = Json.parseObject(readBody(exchange));
            String name = body.getOrDefault("name", "").trim();
            String email = body.getOrDefault("email", "").trim();
            String course = body.getOrDefault("course", "").trim();
            double marks = parseDoubleSafe(body.get("marks"));

            if (name.isEmpty() || email.isEmpty()) {
                sendJson(exchange, 400, "{\"error\":\"name and email are required\"}");
                return;
            }

            Student s = new Student(name, email, course, marks);
            STUDENTS.put(s.getId(), s);
            sendJson(exchange, 201, s.toJson());
        }

        private void handleUpdate(HttpExchange exchange, int id) throws IOException {
            Student existing = STUDENTS.get(id);
            if (existing == null) {
                sendJson(exchange, 404, "{\"error\":\"Student not found\"}");
                return;
            }
            Map<String, String> body = Json.parseObject(readBody(exchange));
            if (body.containsKey("name")) existing.setName(body.get("name"));
            if (body.containsKey("email")) existing.setEmail(body.get("email"));
            if (body.containsKey("course")) existing.setCourse(body.get("course"));
            if (body.containsKey("marks")) existing.setMarks(parseDoubleSafe(body.get("marks")));

            sendJson(exchange, 200, existing.toJson());
        }

        private void handleDelete(HttpExchange exchange, int id) throws IOException {
            Student removed = STUDENTS.remove(id);
            if (removed == null) {
                sendJson(exchange, 404, "{\"error\":\"Student not found\"}");
                return;
            }
            sendJson(exchange, 200, "{\"deleted\":" + id + "}");
        }

        private double parseDoubleSafe(String v) {
            if (v == null || v.isEmpty()) return 0.0;
            try { return Double.parseDouble(v); } catch (NumberFormatException e) { return 0.0; }
        }
    }

    // ------------------------------------------------------------------
    // Static file handler: serves index.html/style.css/script.js from /web
    // ------------------------------------------------------------------
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";

            String decoded = URLDecoder.decode(path, StandardCharsets.UTF_8);
            File file = new File(WEB_ROOT, decoded).getCanonicalFile();
            File webRootFile = new File(WEB_ROOT).getCanonicalFile();

            // Basic protection against path traversal (e.g. ../../etc/passwd)
            if (!file.getPath().startsWith(webRootFile.getPath()) || !file.isFile()) {
                byte[] notFound = "404 Not Found".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, notFound.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(notFound); }
                return;
            }

            String contentType = guessContentType(file.getName());
            exchange.getResponseHeaders().add("Content-Type", contentType);

            byte[] bytes;
            try (InputStream in = new FileInputStream(file)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
                bytes = out.toByteArray();
            }

            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        }

        private String guessContentType(String fileName) {
            if (fileName.endsWith(".html")) return "text/html; charset=utf-8";
            if (fileName.endsWith(".css")) return "text/css; charset=utf-8";
            if (fileName.endsWith(".js")) return "application/javascript; charset=utf-8";
            if (fileName.endsWith(".svg")) return "image/svg+xml";
            String probed = null;
            try { probed = Files.probeContentType(new File(fileName).toPath()); } catch (IOException ignored) { }
            return probed != null ? probed : "application/octet-stream";
        }
    }

    // ------------------------------------------------------------------
    // Shared helpers
    // ------------------------------------------------------------------
    private static String readBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = is.read(buffer)) != -1) out.write(buffer, 0, read);
            return out.toString(StandardCharsets.UTF_8);
        }
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
    }
}
