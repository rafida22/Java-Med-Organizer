import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Database {
    // All the code for Database.java goes here...
    // (This is the same code as before, just without the "package" line)
    private static final String DB_URL = "jdbc:sqlite:medicines.db";

    static {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            String sql = "CREATE TABLE IF NOT EXISTS medicine (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                         "name TEXT NOT NULL," +
                         "purpose TEXT NOT NULL," +
                         "dosage TEXT NOT NULL," +
                         "notes TEXT," +
                         "recommendedTo TEXT NOT NULL," +
                         "userId INTEGER NOT NULL)";
            stmt.execute(sql);

            // Create users table
            String userSql = "CREATE TABLE IF NOT EXISTS users (" +
                             "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                             "username TEXT UNIQUE NOT NULL," +
                             "password TEXT NOT NULL)";
            stmt.execute(userSql);
        } catch (SQLException e) {
            System.err.println("Error creating table: " + e.getMessage());
        }
    }

    public static void addMedicine(Medicine med) {
        String sql = "INSERT INTO medicine(name, purpose, dosage, notes, recommendedTo, userId) VALUES(?,?,?,?,?,?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, med.getName());
            pstmt.setString(2, med.getPurpose());
            pstmt.setString(3, med.getDosage());
            pstmt.setString(4, med.getNotes());
            pstmt.setString(5, med.getRecommendedTo());
            pstmt.setLong(6, med.getUserId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error adding medicine: " + e.getMessage());
        }
    }

    public static List<Medicine> getMedicines(String searchTerm, String personFilter, Long userId) {
        List<Medicine> medicines = new ArrayList<>();
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM medicine WHERE userId = ?");
        if (searchTerm != null && !searchTerm.isEmpty()) { sqlBuilder.append(" AND name LIKE ?"); }
        if (personFilter != null && !personFilter.equals("All")) { sqlBuilder.append(" AND recommendedTo = ?"); }
        sqlBuilder.append(" ORDER BY name");
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            int paramIndex = 1;
            pstmt.setLong(paramIndex++, userId);
            if (searchTerm != null && !searchTerm.isEmpty()) { pstmt.setString(paramIndex++, "%" + searchTerm + "%"); }
            if (personFilter != null && !personFilter.equals("All")) { pstmt.setString(paramIndex++, personFilter); }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                medicines.add(new Medicine(rs.getLong("id"), rs.getString("name"), rs.getString("purpose"), rs.getString("dosage"), rs.getString("notes"), rs.getString("recommendedTo"), rs.getLong("userId")));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching medicines: " + e.getMessage());
        }
        return medicines;
    }

    public static List<String> getDistinctPeople(Long userId) {
        List<String> people = new ArrayList<>();
        String sql = "SELECT DISTINCT recommendedTo FROM medicine WHERE userId = ? ORDER BY recommendedTo";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, userId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                people.add(rs.getString("recommendedTo"));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching distinct people: " + e.getMessage());
        }
        return people;
    }

    public static Long authenticateUser(String username, String password) {
        String sql = "SELECT id FROM users WHERE username = ? AND password = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("id");
            }
        } catch (SQLException e) {
            System.err.println("Error authenticating user: " + e.getMessage());
        }
        return null;
    }

    public static Long registerUser(String username, String password) {
        String sql = "INSERT INTO users(username, password) VALUES(?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error registering user: " + e.getMessage());
        }
        return null;
    }

    public static boolean userExists(String username) {
        String sql = "SELECT id FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("Error checking user existence: " + e.getMessage());
        }
        return false;
    }
}