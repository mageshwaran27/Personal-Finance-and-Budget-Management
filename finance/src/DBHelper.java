import java.sql.*;
import java.io.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.time.LocalDate;
import java.time.YearMonth;

public class DBHelper {
    private static final String URL = "jdbc:oracle:thin:@localhost:1521:XE";
    private static final String USER = "C##financeapp";
    private static final String PASS = "pfbt";

    // Database initialization
    public static void initializeDatabase() {
        try (Connection conn = getConnection()) {
            // Create users table
            String createUsersTable = "CREATE TABLE users (" +
                    "email VARCHAR2(100) PRIMARY KEY, " +
                    "name VARCHAR2(100) NOT NULL, " +
                    "phone VARCHAR2(15), " +
                    "password VARCHAR2(100) NOT NULL, " +
                    "theme VARCHAR2(20) DEFAULT 'Light', " +
                    "currency VARCHAR2(5) DEFAULT '₹', " +
                    "budget_alerts NUMBER(1) DEFAULT 1, " +
                    "auto_logout_minutes NUMBER DEFAULT 0)";
            executeUpdate(conn, createUsersTable);
            
            // Create transactions table
            String createTransactionsTable = "CREATE TABLE transactions (" +
                    "transaction_id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                    "user_email VARCHAR2(100) REFERENCES users(email), " +
                    "txn_date DATE NOT NULL, " +
                    "txn_type VARCHAR2(10) CHECK (txn_type IN ('Income','Expense')), " +
                    "category VARCHAR2(50) NOT NULL, " +
                    "tag VARCHAR2(50), " +
                    "amount NUMBER(15,2) NOT NULL, " +
                    "recurring_type VARCHAR2(20), " +
                    "description VARCHAR2(500))";
            executeUpdate(conn, createTransactionsTable);
            
            // Create budgets table
            String createBudgetsTable = "CREATE TABLE budgets (" +
                    "budget_id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                    "user_email VARCHAR2(100) REFERENCES users(email), " +
                    "category VARCHAR2(50) NOT NULL, " +
                    "budget_amount NUMBER(15,2) NOT NULL, " +
                    "month_year VARCHAR2(7) NOT NULL)";
            executeUpdate(conn, createBudgetsTable);
            
            System.out.println("✅ Database initialized successfully!");
            
        } catch (SQLException e) {
            System.out.println("Database already exists or error: " + e.getMessage());
        }
    }
    
    private static void executeUpdate(Connection conn, String sql) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.execute();
        } catch (SQLException e) {
            // Table might already exist
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    // Save a transaction with description
    public static void saveTransaction(String userEmail, String dateIso, String type,
                                       String category, String tag, double amount,
                                       String recurring, String description) {
        String sql = "INSERT INTO transactions (user_email, txn_date, txn_type, category, tag, amount, recurring_type, description) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userEmail);
            ps.setDate(2, java.sql.Date.valueOf(dateIso));
            ps.setString(3, type);
            ps.setString(4, category);
            ps.setString(5, tag);
            ps.setDouble(6, amount);
            ps.setString(7, recurring);
            ps.setString(8, description);
            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "❌ Error saving transaction: " + e.getMessage());
        }
    }

    // Load transactions into table model with description
    public static void loadTransactions(String userEmail, DefaultTableModel model, HashMap<String, Double> expenseMap) {
        String sql = "SELECT txn_date, txn_type, category, tag, amount, description FROM transactions WHERE user_email=? ORDER BY txn_date DESC";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userEmail);
            ResultSet rs = ps.executeQuery();
            model.setRowCount(0);
            expenseMap.clear();

            while (rs.next()) {
                String date = rs.getDate("txn_date").toString();
                String type = rs.getString("txn_type");
                String category = rs.getString("category");
                String tag = rs.getString("tag");
                double amount = rs.getDouble("amount");
                String description = rs.getString("description");
                
                if("Expense".equals(type)) {
                    expenseMap.put(category, expenseMap.getOrDefault(category, 0.0) + amount);
                }
                
                model.addRow(new Object[]{
                    date, type, category, tag, 
                    "₹" + String.format("%.2f", amount),
                    description != null ? description : "",
                    "" // For remaining budget
                });
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Get financial summary
    public static double[] getFinancialSummary(String userEmail, String monthYear) {
        double[] summary = new double[3]; // income, expense, balance
        String sql = "SELECT txn_type, SUM(amount) as total FROM transactions " +
                    "WHERE user_email=? AND TO_CHAR(txn_date, 'YYYY-MM') = ? " +
                    "GROUP BY txn_type";
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userEmail);
            ps.setString(2, monthYear);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String type = rs.getString("txn_type");
                double total = rs.getDouble("total");
                if ("Income".equals(type)) {
                    summary[0] = total;
                } else {
                    summary[1] = total;
                }
            }
            summary[2] = summary[0] - summary[1];

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return summary;
    }

    // Save budget
    public static boolean saveBudget(String userEmail, String category, double amount, String monthYear) {
        String sql = "MERGE INTO budgets b " +
                    "USING (SELECT ? as user_email, ? as category, ? as month_year FROM dual) tmp " +
                    "ON (b.user_email = tmp.user_email AND b.category = tmp.category AND b.month_year = tmp.month_year) " +
                    "WHEN MATCHED THEN UPDATE SET b.budget_amount = ? " +
                    "WHEN NOT MATCHED THEN INSERT (user_email, category, budget_amount, month_year) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userEmail);
            ps.setString(2, category);
            ps.setString(3, monthYear);
            ps.setDouble(4, amount);
            ps.setString(5, userEmail);
            ps.setString(6, category);
            ps.setDouble(7, amount);
            ps.setString(8, monthYear);
            
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Get budget for category
    public static double getBudget(String userEmail, String category, String monthYear) {
        String sql = "SELECT budget_amount FROM budgets WHERE user_email=? AND category=? AND month_year=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userEmail);
            ps.setString(2, category);
            ps.setString(3, monthYear);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getDouble("budget_amount");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    // Get category-wise spending
    public static HashMap<String, Double> getCategorySpending(String userEmail, String monthYear) {
        HashMap<String, Double> spending = new HashMap<>();
        String sql = "SELECT category, SUM(amount) as total FROM transactions " +
                    "WHERE user_email=? AND txn_type='Expense' AND TO_CHAR(txn_date, 'YYYY-MM') = ? " +
                    "GROUP BY category";
        
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userEmail);
            ps.setString(2, monthYear);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                spending.put(rs.getString("category"), rs.getDouble("total"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return spending;
    }

    // Register new user with enhanced validation
    public static boolean registerUser(String email, String name, String phone, String password) {
        if (email.isEmpty() || name.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(null, "❌ Please fill all required fields!");
            return false;
        }
        
        if (!isValidEmail(email)) {
            JOptionPane.showMessageDialog(null, "❌ Please enter a valid email address!");
            return false;
        }

        String sql = "INSERT INTO users (email, name, phone, password) VALUES (?,?,?,?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ps.setString(2, name);
            ps.setString(3, phone);
            ps.setString(4, password);
            ps.executeUpdate();
            
            // Create default budgets for new user
            createDefaultBudgets(email);
            
            return true;

        } catch(SQLException e){
            JOptionPane.showMessageDialog(null,"❌ Registration failed! Email might already exist.");
            return false;
        }
    }

    private static boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private static void createDefaultBudgets(String userEmail) {
        String[] categories = {"Food", "Transport", "Shopping", "Entertainment", "Bills", "Rent"};
        String currentMonth = YearMonth.now().toString();
        
        for (String category : categories) {
            saveBudget(userEmail, category, 0.0, currentMonth);
        }
    }

    // Login check with enhanced security
    public static boolean loginUser(String email, String password) {
        String sql = "SELECT * FROM users WHERE email=? AND password=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            return rs.next();

        } catch(SQLException e){
            e.printStackTrace();
            return false;
        }
    }

    // Update user settings
    public static boolean updateUserSettings(String email, String name, String phone, String theme, 
                                           String currency, boolean budgetAlerts, int autoLogout) {
        String sql = "UPDATE users SET name=?, phone=?, theme=?, currency=?, budget_alerts=?, auto_logout_minutes=? WHERE email=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, theme);
            ps.setString(4, currency);
            ps.setInt(5, budgetAlerts ? 1 : 0);
            ps.setInt(6, autoLogout);
            ps.setString(7, email);
            
            return ps.executeUpdate() > 0;

        } catch(SQLException e){
            e.printStackTrace();
            return false;
        }
    }

    // Get user settings
    public static Object[] getUserSettings(String email) {
        String sql = "SELECT name, phone, theme, currency, budget_alerts, auto_logout_minutes FROM users WHERE email=?";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new Object[]{
                    rs.getString("name"),
                    rs.getString("phone"),
                    rs.getString("theme"),
                    rs.getString("currency"),
                    rs.getInt("budget_alerts") == 1,
                    rs.getInt("auto_logout_minutes")
                };
            }

        } catch(SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    // Export table model to CSV with enhanced formatting
    public static void exportToCSVFromTable(DefaultTableModel model) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Transactions to CSV");
        fileChooser.setSelectedFile(new File("financial_transactions_" + LocalDate.now() + ".csv"));
        
        int userSelection = fileChooser.showSaveDialog(null);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".csv")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".csv");
            }
            
            try (FileWriter fw = new FileWriter(fileToSave)) {
                // Write column headers
                for (int i = 0; i < model.getColumnCount(); i++) {
                    fw.write("\"" + model.getColumnName(i) + "\"" + (i < model.getColumnCount() - 1 ? "," : ""));
                }
                fw.write("\n");

                // Write rows
                for (int row = 0; row < model.getRowCount(); row++) {
                    for (int col = 0; col < model.getColumnCount(); col++) {
                        String value = model.getValueAt(row, col).toString();
                        fw.write("\"" + value.replace("\"", "\"\"") + "\"" + (col < model.getColumnCount() - 1 ? "," : ""));
                    }
                    fw.write("\n");
                }

                JOptionPane.showMessageDialog(null, "✅ Transactions exported to CSV successfully!\nFile: " + fileToSave.getAbsolutePath());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "❌ Error exporting CSV: " + e.getMessage());
            }
        }
    }

    // Enhanced CSV import with validation
    public static void importFromCSV(DefaultTableModel model, String userEmail) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Import Transactions from CSV");
        int userSelection = fileChooser.showOpenDialog(null);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();
            int importedCount = 0;
            
            try (BufferedReader br = new BufferedReader(new FileReader(fileToOpen))) {
                String line;
                boolean isHeader = true;
                String[] headers = null;

                while ((line = br.readLine()) != null) {
                    if (isHeader) { 
                        headers = parseCSVLine(line);
                        isHeader = false; 
                        continue;
                    }
                    
                    String[] values = parseCSVLine(line);
                    if (values.length >= 5) {
                        // Map CSV columns to transaction fields
                        String date = values[0].trim();
                        String type = values[1].trim();
                        String category = values[2].trim();
                        String tag = values[3].trim();
                        String amountStr = values[4].replaceAll("[^\\d.]", "");
                        String description = values.length > 5 ? values[5].trim() : "";
                        
                        try {
                            double amount = Double.parseDouble(amountStr);
                            saveTransaction(userEmail, date, type, category, tag, amount, "One-time", description);
                            importedCount++;
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid amount format: " + amountStr);
                        }
                    }
                }

                JOptionPane.showMessageDialog(null, "✅ " + importedCount + " transactions imported from CSV successfully!");
                // Refresh the table
                HashMap<String, Double> expenseMap = new HashMap<>();
                loadTransactions(userEmail, model, expenseMap);
                
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "❌ Error importing CSV: " + e.getMessage());
            }
        }
    }

    private static String[] parseCSVLine(String line) {
        java.util.List<String> values = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder value = new StringBuilder();
        
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(value.toString());
                value = new StringBuilder();
            } else {
                value.append(c);
            }
        }
        values.add(value.toString());
        return values.toArray(new String[0]);
    }

    // Enhanced PDF export with styling - FIXED FONT CONFLICTS
    public static void exportToPDF(DefaultTableModel model) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Transactions as PDF");
        fileChooser.setSelectedFile(new File("financial_report_" + LocalDate.now() + ".pdf"));
        
        int userSelection = fileChooser.showSaveDialog(null);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".pdf")) {
                file = new File(file.getAbsolutePath() + ".pdf");
            }
            
            try {
                Document document = new Document(PageSize.A4.rotate());
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();
                
                // Title - FIXED: Use fully qualified Font class
                com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 18, com.itextpdf.text.Font.BOLD);
                Paragraph title = new Paragraph("Personal Finance Tracker - Transaction Report", titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                document.add(title);
                
                // Date - FIXED: Use fully qualified Font class
                com.itextpdf.text.Font dateFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 12, com.itextpdf.text.Font.ITALIC);
                Paragraph date = new Paragraph("Generated on: " + new java.util.Date(), dateFont);
                date.setAlignment(Element.ALIGN_CENTER);
                document.add(date);
                
                document.add(new Paragraph(" "));
                
                // Table
                PdfPTable table = new PdfPTable(model.getColumnCount());
                table.setWidthPercentage(100);
                
                // Header
                for (int i = 0; i < model.getColumnCount(); i++) {
                    PdfPCell header = new PdfPCell(new Phrase(model.getColumnName(i)));
                    header.setBackgroundColor(BaseColor.LIGHT_GRAY);
                    header.setHorizontalAlignment(Element.ALIGN_CENTER);
                    table.addCell(header);
                }
                
                // Data
                for (int row = 0; row < model.getRowCount(); row++) {
                    for (int col = 0; col < model.getColumnCount(); col++) {
                        table.addCell(new Phrase(model.getValueAt(row, col).toString()));
                    }
                }
                
                document.add(table);
                document.close();
                
                JOptionPane.showMessageDialog(null, "✅ PDF report generated successfully!\nFile: " + file.getAbsolutePath());
                
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "❌ Error generating PDF: " + e.getMessage());
            }
        }
    }

    // Delete transaction
    public static boolean deleteTransaction(int rowIndex, DefaultTableModel model, String userEmail) {
        if (rowIndex >= 0 && rowIndex < model.getRowCount()) {
            String date = model.getValueAt(rowIndex, 0).toString();
            String type = model.getValueAt(rowIndex, 1).toString();
            String category = model.getValueAt(rowIndex, 2).toString();
            double amount = Double.parseDouble(model.getValueAt(rowIndex, 4).toString().replaceAll("[^\\d.]", ""));
            
            String sql = "DELETE FROM transactions WHERE user_email=? AND txn_date=? AND txn_type=? AND category=? AND amount=?";
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, userEmail);
                ps.setDate(2, java.sql.Date.valueOf(date));
                ps.setString(3, type);
                ps.setString(4, category);
                ps.setDouble(5, amount);
                
                int result = ps.executeUpdate();
                if (result > 0) {
                    model.removeRow(rowIndex);
                    return true;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}