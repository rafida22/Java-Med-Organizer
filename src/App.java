import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class App {

    // --- OOP PRINCIPLE: ABSTRACTION (via Interface) & POLYMORPHISM ---
    // This is a contract. Any class that implements this interface PROMISES
    // to have a method called `toJson()`. This allows us to treat different
    // objects (like User and Medicine) in the same way (polymorphism).
    public interface JsonSerializable {
        JSONObject toJson();
    }

    // --- OOP PRINCIPLE: INHERITANCE ---
    // This is our base class. It contains common properties that other
    // models will inherit. Both User and Medicine have an 'id'.
    public static abstract class BaseModel {
        protected Long id;
        public Long getId() { return id; }
    }

    // --- NESTED DATA CLASSES ---
    // User now INHERITS from BaseModel and IMPLEMENTS JsonSerializable.
    // It also demonstrates ENCAPSULATION with private fields.
    public static class User extends BaseModel implements JsonSerializable {
        private String username;
        
        public User(long id, String username) {
            this.id = id;
            this.username = username;
        }
        
        public String getUsername() { return username; }

        @Override // This annotation indicates we are overriding a method from an interface
        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            json.put("id", this.id);
            json.put("username", this.username);
            return json;
        }
    }

    // Medicine now INHERITS from BaseModel and IMPLEMENTS JsonSerializable.
    // It also demonstrates ENCAPSULATION with private fields.
    public static class Medicine extends BaseModel implements JsonSerializable {
        private String name, purpose, dosage, notes, recommendedTo;
        
        public Medicine() {}
        public Medicine(Long id, String name, String p, String d, String n, String r) {
            this.id = id; this.name = name; this.purpose = p; this.dosage = d; this.notes = n; this.recommendedTo = r;
        }

        @Override
        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            json.put("id", this.id);
            json.put("name", this.name);
            json.put("purpose", this.purpose);
            json.put("dosage", this.dosage);
            json.put("notes", this.notes);
            json.put("recommendedTo", this.recommendedTo);
            return json;
        }
        
        // Getters and Setters for private fields (Encapsulation)
        public String getName() { return name; }
        public String getPurpose() { return purpose; }
        public String getDosage() { return dosage; }
        public String getNotes() { return notes; }
        public String getRecommendedTo() { return recommendedTo; }
        public void setName(String n) { this.name = n; }
        public void setPurpose(String p) { this.purpose = p; }
        public void setDosage(String d) { this.dosage = d; }
        public void setNotes(String n) { this.notes = n; }
        public void setRecommendedTo(String r) { this.recommendedTo = r; }
    }


    // --- DATABASE LOGIC (This is a form of ABSTRACTION) ---
    private static final String DB_URL = "jdbc:sqlite:medicines_project.db";

    static {
        try (Connection conn = DriverManager.getConnection(DB_URL); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE NOT NULL, password TEXT NOT NULL)");
            stmt.execute("CREATE TABLE IF NOT EXISTS medicine (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, purpose TEXT NOT NULL, dosage TEXT NOT NULL, notes TEXT, recommendedTo TEXT NOT NULL, user_id INTEGER, FOREIGN KEY(user_id) REFERENCES users(id))");
        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
        }
    }
    
    // All other database methods remain the same...
    public static synchronized Optional<User> addUser(String username, String password) {
        String sql = "INSERT INTO users(username, password) VALUES(?,?)";
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, username); pstmt.setString(2, password);
            if (pstmt.executeUpdate() > 0) {
                try (ResultSet keys = pstmt.getGeneratedKeys()) { if (keys.next()) return Optional.of(new User(keys.getLong(1), username)); }
            }
        } catch (SQLException e) { System.err.println("Error adding user: " + e.getMessage()); }
        return Optional.empty();
    }
    public static synchronized Optional<User> validateUser(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username); pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return Optional.of(new User(rs.getLong("id"), rs.getString("username")));
        } catch (SQLException e) { System.err.println("Error validating user: " + e.getMessage()); }
        return Optional.empty();
    }
    public static synchronized void addMedicine(Medicine med, long userId) {
        String sql = "INSERT INTO medicine(name, purpose, dosage, notes, recommendedTo, user_id) VALUES(?,?,?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, med.getName()); pstmt.setString(2, med.getPurpose()); pstmt.setString(3, med.getDosage());
            pstmt.setString(4, med.getNotes()); pstmt.setString(5, med.getRecommendedTo()); pstmt.setLong(6, userId);
            int rowsAffected = pstmt.executeUpdate();
            System.out.println("Added medicine: " + med.getName() + " for user " + userId + ", rows affected: " + rowsAffected);
        } catch (SQLException e) { System.err.println("Error adding medicine: " + e.getMessage()); }
    }
    public static synchronized void updateMedicine(Medicine med) {
        String sql = "UPDATE medicine SET name = ?, purpose = ?, dosage = ?, notes = ?, recommendedTo = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, med.getName()); pstmt.setString(2, med.getPurpose()); pstmt.setString(3, med.getDosage());
            pstmt.setString(4, med.getNotes()); pstmt.setString(5, med.getRecommendedTo()); pstmt.setLong(6, med.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) { System.err.println("Error updating medicine: " + e.getMessage()); }
    }
    public static synchronized void deleteMedicine(long medicineId, long userId) {
        String sql = "DELETE FROM medicine WHERE id = ? AND user_id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, medicineId); pstmt.setLong(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) { System.err.println("Error deleting medicine: " + e.getMessage()); }
    }
    public static synchronized Optional<Medicine> getMedicineById(long medicineId, long userId) {
        String sql = "SELECT * FROM medicine WHERE id = ? AND user_id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, medicineId); pstmt.setLong(2, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return Optional.of(new Medicine(rs.getLong("id"), rs.getString("name"), rs.getString("purpose"), rs.getString("dosage"), rs.getString("notes"), rs.getString("recommendedTo")));
        } catch (SQLException e) { System.err.println("Error fetching single medicine: " + e.getMessage()); }
        return Optional.empty();
    }
    public static synchronized List<Medicine> getMedicines(long userId, String searchTerm, String personFilter) {
        List<Medicine> medicines = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM medicine WHERE user_id = ?");
        if (searchTerm != null && !searchTerm.isEmpty()) sql.append(" AND name LIKE ?");
        if (personFilter != null && !personFilter.equals("All")) sql.append(" AND recommendedTo = ?");
        sql.append(" ORDER BY name");
        System.out.println("Fetching medicines for user " + userId + " with SQL: " + sql.toString());
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            int i = 1; pstmt.setLong(i++, userId);
            if (searchTerm != null && !searchTerm.isEmpty()) pstmt.setString(i++, "%" + searchTerm + "%");
            if (personFilter != null && !personFilter.equals("All")) pstmt.setString(i++, personFilter);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                medicines.add(new Medicine(rs.getLong("id"), rs.getString("name"), rs.getString("purpose"), rs.getString("dosage"), rs.getString("notes"), rs.getString("recommendedTo")));
            }
        } catch (SQLException e) { System.err.println("Error fetching medicines: " + e.getMessage()); }
        System.out.println("Found " + medicines.size() + " medicines for user " + userId);
        return medicines;
    }
    public static synchronized List<String> getDistinctPeople(long userId) {
        List<String> people = new ArrayList<>();
        String sql = "SELECT DISTINCT recommendedTo FROM medicine WHERE user_id = ? ORDER BY recommendedTo";
        try (Connection conn = DriverManager.getConnection(DB_URL); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) people.add(rs.getString("recommendedTo"));
        } catch (SQLException e) { System.err.println("Error fetching people: " + e.getMessage()); }
        return people;
    }

    // --- SERVER LOGIC ---
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", App::handleLoginSignupPage);
        server.createContext("/login", App::handleLogin);
        server.createContext("/signup", App::handleSignup);
        server.createContext("/dashboard", App::handleDashboard);
        server.createContext("/add", App::handleAddOrEdit);
        server.createContext("/api/medicines", App::handleApiGetList);
        server.createContext("/api/medicines/single", App::handleApiGetSingle);
        server.createContext("/api/medicines/delete", App::handleApiDelete);
        server.createContext("/resources/", App::handleStaticFile);
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port 8080. Open http://localhost:8080");
    }

    // --- ROUTE HANDLERS ---
    private static void handleLoginSignupPage(HttpExchange e) throws IOException { sendHtml(e, "resources/index.html"); }
    private static void handleDashboard(HttpExchange e) throws IOException {
        Map<String, String> p = parseQueryParams(e.getRequestURI().getQuery());
        if (!p.containsKey("userId")) { redirect(e, "/"); return; }
        long userId = Long.parseLong(p.get("userId"));
        String username = p.getOrDefault("username", "User");
        String html = loadHtml("resources/dashboard.html")
            .replace("<!-- @@PEOPLE_OPTIONS@@ -->", getPeopleOptions(userId))
            .replace("<!-- @@USERNAME@@ -->", username)
            .replace("<!-- @@USER_INITIAL@@ -->", username.substring(0, 1).toUpperCase());
        sendResponse(e, 200, "text/html", html);
    }
    private static void handleLogin(HttpExchange e) throws IOException {
        Map<String, String> p = parseFormData(readRequestBody(e));
        validateUser(p.get("username"), p.get("password")).ifPresentOrElse(
            user -> sendResponse(e, 200, "application/json", String.format("{\"success\":true,\"userId\":%d,\"username\":\"%s\"}", user.getId(), user.getUsername())),
            () -> sendResponse(e, 200, "application/json", "{\"success\":false,\"message\":\"Invalid username or password\"}")
        );
    }
    private static void handleSignup(HttpExchange e) throws IOException {
        Map<String, String> p = parseFormData(readRequestBody(e));
        addUser(p.get("username"), p.get("password")).ifPresentOrElse(
            user -> sendResponse(e, 200, "application/json", String.format("{\"success\":true,\"userId\":%d,\"username\":\"%s\"}", user.getId(), user.getUsername())),
            () -> sendResponse(e, 200, "application/json", "{\"success\":false,\"message\":\"Username already exists or invalid\"}")
        );
    }
    private static void handleAddOrEdit(HttpExchange e) throws IOException {
        Map<String, String> qp = parseQueryParams(e.getRequestURI().getQuery());
        long userId = Long.parseLong(qp.getOrDefault("userId", "0"));
        if (userId == 0) { redirect(e, "/"); return; }
        if ("GET".equals(e.getRequestMethod())) {
            String html = loadHtml("resources/add-medicine.html").replace("<!-- @@PEOPLE_OPTIONS@@ -->", getPeopleOptions(userId));
            sendResponse(e, 200, "text/html", html);
        } else if ("POST".equals(e.getRequestMethod())) {
            try {
                Map<String, String> p = parseFormData(readRequestBody(e));
                Medicine med = new Medicine();
                med.setName(p.get("name")); med.setPurpose(p.get("purpose")); med.setDosage(p.get("dosage"));
                med.setNotes(p.get("notes")); med.setRecommendedTo("new".equals(p.get("recommendedToSelect")) ? p.get("newPersonName") : p.get("recommendedToSelect"));
                String medIdStr = p.get("medicineId");
                if (medIdStr != null && !medIdStr.isEmpty()) {
                    med.id = Long.parseLong(medIdStr);
                    updateMedicine(med);
                } else {
                    addMedicine(med, userId);
                }
                redirect(e, "/dashboard?userId=" + userId + "&username=" + qp.get("username"));
            } catch (Exception ex) {
                System.err.println("Error adding/editing medicine: " + ex.getMessage());
                sendResponse(e, 500, "text/plain", "Error saving medicine: " + ex.getMessage());
            }
        }
    }
    private static void handleApiGetList(HttpExchange e) throws IOException {
        Map<String, String> p = parseQueryParams(e.getRequestURI().getQuery());
        long userId = Long.parseLong(p.getOrDefault("userId", "0"));
        if (userId == 0) { sendResponse(e, 401, "application/json", "[]"); return; }
        List<Medicine> medicines = getMedicines(userId, p.get("search"), p.get("person"));
        sendResponse(e, 200, "application/json", listToJson(medicines).toString()); // Using new polymorphic helper
    }
    private static void handleApiGetSingle(HttpExchange e) throws IOException {
        Map<String, String> p = parseQueryParams(e.getRequestURI().getQuery());
        long userId = Long.parseLong(p.getOrDefault("userId", "0"));
        long medId = Long.parseLong(p.getOrDefault("medicineId", "0"));
        if (userId == 0 || medId == 0) { sendResponse(e, 400, "text/plain", "Bad Request"); return; }
        getMedicineById(medId, userId).ifPresentOrElse(
            med -> sendResponse(e, 200, "application/json", med.toJson().toString()), // Using new polymorphic method
            () -> sendResponse(e, 404, "text/plain", "Not Found")
        );
    }
    private static void handleApiDelete(HttpExchange e) throws IOException {
        Map<String, String> p = parseQueryParams(e.getRequestURI().getQuery());
        long userId = Long.parseLong(p.getOrDefault("userId", "0"));
        long medId = Long.parseLong(p.getOrDefault("medicineId", "0"));
        if (userId != 0 && medId != 0) deleteMedicine(medId, userId);
        sendResponse(e, 200, "text/plain", "OK");
    }
    private static void handleStaticFile(HttpExchange e) throws IOException {
        Path path = Paths.get(e.getRequestURI().getPath().substring(1));
        if (Files.exists(path)) {
            String mime = "application/octet-stream";
            if (path.toString().endsWith(".jpg")) mime = "image/jpeg";
            if (path.toString().endsWith(".png")) mime = "image/png";
            e.getResponseHeaders().set("Content-Type", mime);
            e.sendResponseHeaders(200, 0);
            try (OutputStream os = e.getResponseBody()) { Files.copy(path, os); }
        } else { sendResponse(e, 404, "text/plain", "404 Not Found"); }
    }

    // --- HELPER UTILITY METHODS ---
    private static void sendHtml(HttpExchange e, String path) throws IOException { sendResponse(e, 200, "text/html", loadHtml(path)); }
    private static void redirect(HttpExchange e, String loc) { try { e.getResponseHeaders().set("Location", loc); e.sendResponseHeaders(302, -1); } catch (IOException ex) {} }
    private static String loadHtml(String fileName) throws IOException { return new String(Files.readAllBytes(Paths.get(fileName)), StandardCharsets.UTF_8); }
    private static String readRequestBody(HttpExchange e) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(e.getRequestBody(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
    private static Map<String, String> parseFormData(String data) {
        Map<String, String> map = new HashMap<>(); if (data == null || data.isEmpty()) return map;
        try { for (String pair : data.split("&")) { int idx = pair.indexOf("="); if (idx > 0) map.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8")); } } catch (UnsupportedEncodingException ex) {}
        return map;
    }
    private static Map<String, String> parseQueryParams(String q) { return q == null ? new HashMap<>() : parseFormData(q); }
    private static void sendResponse(HttpExchange e, int code, String type, String body) { try { e.getResponseHeaders().set("Content-Type", type); byte[] bytes = body.getBytes(StandardCharsets.UTF_8); e.sendResponseHeaders(code, bytes.length); try (OutputStream os = e.getResponseBody()) { os.write(bytes); } } catch (IOException ex) {} }
    private static String getPeopleOptions(long userId) { return getDistinctPeople(userId).stream().map(p -> String.format("<option value=\"%s\">%s</option>", p, p)).collect(Collectors.joining()); }

    // --- NEW HELPER using POLYMORPHISM ---
    // This method can take a list of ANY object that implements JsonSerializable
    // and turn it into a JSON array. It doesn't care if it's a List<Medicine>
    // or a List<User>, demonstrating polymorphism.
    private static <T extends JsonSerializable> JSONArray listToJson(List<T> list) {
        JSONArray array = new JSONArray();
        list.forEach(item -> array.put(item.toJson()));
        return array;
    }
}

// javac -cp "lib/*" src/App.java -d bin
// java -cp "bin;lib/*" App-