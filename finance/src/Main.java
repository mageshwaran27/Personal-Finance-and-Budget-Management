import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableModel;
import java.time.LocalDate;
import java.time.YearMonth;
import org.jfree.chart.*;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;

public class Main {
    private static String userEmail = "guest@example.com"; 
    private static String userName = "Guest";
    private static String userPhone = "";
    private static String userTheme = "Light";
    private static String userCurrency = "₹";
    private static boolean darkMode = false;
    private static boolean budgetAlerts = true;
    private static int autoLogoutMinutes = 0;

    private static JLabel balanceLabel, incomeLabel, expenseLabel, savingsLabel;
    private static JTable transactionTable;
    private static DefaultTableModel tableModel;
    private static JComboBox<String> typeBox, categoryBox, tagBox, recurringBox, monthFilterBox;
    private static JTextField amountField, budgetField, searchField, descriptionField;
    private static JFormattedTextField dateField;
    private static JPanel chartPanel;
    private static JFrame mainFrame;

    private static HashMap<String, Double> categoryBudgets = new HashMap<>();
    private static HashMap<String, Double> expenseByCategory = new HashMap<>();
    private static double balance = 0, totalIncome = 0, totalExpense = 0;

    public static void main(String[] args){
        SwingUtilities.invokeLater(() -> launchMainApp("guest@example.com"));
    }

    public static void launchMainApp(String email) {
        userEmail = email;
        loadUserSettings();
        
        mainFrame = new JFrame("💰 Personal Finance Tracker - Dashboard");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(1400, 800);
        mainFrame.setLayout(new BorderLayout());

        createMenuBar();
        createDashboard();
        setupAutoLogout();

        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
    }

    private static void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        JMenu fileMenu = new JMenu("File");
        JMenuItem exportItem = new JMenuItem("Export Data");
        JMenuItem importItem = new JMenuItem("Import Data");
        JMenuItem exitItem = new JMenuItem("Exit");
        
        exportItem.addActionListener(e -> showExportDialog());
        importItem.addActionListener(e -> DBHelper.importFromCSV(tableModel, userEmail));
        exitItem.addActionListener(e -> System.exit(0));
        
        fileMenu.add(exportItem);
        fileMenu.add(importItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        
        JMenu viewMenu = new JMenu("View");
        JMenuItem chartItem = new JMenuItem("Show Charts");
        JMenuItem summaryItem = new JMenuItem("Financial Summary");
        
        chartItem.addActionListener(e -> showPieChart());
        summaryItem.addActionListener(e -> showFinancialSummary());
        
        viewMenu.add(chartItem);
        viewMenu.add(summaryItem);
        
        JMenu toolsMenu = new JMenu("Tools");
        JMenuItem budgetItem = new JMenuItem("Manage Budgets");
        JMenuItem goalsItem = new JMenuItem("Financial Goals");
        
        budgetItem.addActionListener(e -> showBudgetManager());
        goalsItem.addActionListener(e -> showFinancialGoals());
        
        toolsMenu.add(budgetItem);
        toolsMenu.add(goalsItem);
        
        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(toolsMenu);
        
        mainFrame.setJMenuBar(menuBar);
    }

    private static void createDashboard() {
        // ===== Top Balance Panel =====
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(240, 248, 255));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel balancePanel = new JPanel(new FlowLayout());
        balanceLabel = new JLabel("Balance: " + userCurrency + "0.00");
        balanceLabel.setFont(new Font("Arial", Font.BOLD, 28));
        balanceLabel.setForeground(new Color(0, 128, 0));
        balancePanel.add(balanceLabel);
        
        JPanel monthPanel = new JPanel(new FlowLayout());
        monthPanel.add(new JLabel("Filter by Month:"));
        monthFilterBox = new JComboBox<>(getMonthOptions());
        monthFilterBox.setSelectedItem(YearMonth.now().toString());
        monthFilterBox.addActionListener(e -> filterByMonth());
        monthPanel.add(monthFilterBox);
        
        topPanel.add(balancePanel, BorderLayout.WEST);
        topPanel.add(monthPanel, BorderLayout.EAST);
        mainFrame.add(topPanel, BorderLayout.NORTH);

        // ===== Left Input Panel =====
        JPanel inputPanel = new JPanel(new GridLayout(15, 2, 5, 5));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Add Transaction"));
        inputPanel.setBackground(new Color(240, 248, 255));

        inputPanel.add(new JLabel("Type:"));
        typeBox = new JComboBox<>(new String[]{"Income", "Expense"});
        inputPanel.add(typeBox);

        inputPanel.add(new JLabel("Category:"));
        categoryBox = new JComboBox<>(new String[]{
            "Food", "Transport", "Shopping", "Entertainment", 
            "Bills", "Rent", "Salary", "Business", "Investment", "Other"
        });
        inputPanel.add(categoryBox);

        inputPanel.add(new JLabel("Tag:"));
        tagBox = new JComboBox<>(new String[]{"None", "Work", "Personal", "Bonus", "Gift", "Emergency", "Travel"});
        inputPanel.add(tagBox);

        inputPanel.add(new JLabel("Amount (" + userCurrency + "):"));
        amountField = new JTextField();
        inputPanel.add(amountField);

        inputPanel.add(new JLabel("Date:"));
        dateField = new JFormattedTextField(new SimpleDateFormat("yyyy-MM-dd"));
        dateField.setValue(new Date());
        inputPanel.add(dateField);

        inputPanel.add(new JLabel("Recurring:"));
        recurringBox = new JComboBox<>(new String[]{"One-time", "Daily", "Weekly", "Monthly", "Yearly"});
        inputPanel.add(recurringBox);

        inputPanel.add(new JLabel("Description:"));
        descriptionField = new JTextField();
        inputPanel.add(descriptionField);

        inputPanel.add(new JLabel("Budget for Category:"));
        budgetField = new JTextField();
        inputPanel.add(budgetField);

        JButton setBudgetBtn = new JButton("💡 Set Budget");
        setBudgetBtn.setBackground(Color.ORANGE);
        setBudgetBtn.setForeground(Color.WHITE);
        inputPanel.add(setBudgetBtn);

        JButton addTxnBtn = new JButton("💾 Add Transaction");
        addTxnBtn.setBackground(new Color(0, 128, 0));
        addTxnBtn.setForeground(Color.WHITE);
        inputPanel.add(addTxnBtn);

        JButton clearBtn = new JButton("🗑️ Clear");
        clearBtn.setBackground(Color.RED);
        clearBtn.setForeground(Color.WHITE);
        inputPanel.add(clearBtn);

        JPanel westPanel = new JPanel(new BorderLayout());
        westPanel.add(inputPanel, BorderLayout.NORTH);
        mainFrame.add(westPanel, BorderLayout.WEST);

        // ===== Center Table Panel =====
        String[] columns = {"Date", "Type", "Category", "Tag", "Amount", "Description", "Actions"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 6; // Only actions column is editable
            }
        };
        
        transactionTable = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                String type = (String) getValueAt(row, 1);
                if ("Income".equals(type)) {
                    c.setBackground(new Color(220, 255, 220));
                } else {
                    c.setBackground(new Color(255, 220, 220));
                }
                return c;
            }
        };
        
        transactionTable.setRowHeight(30);
        transactionTable.getColumn("Actions").setCellRenderer(new ButtonRenderer());
        transactionTable.getColumn("Actions").setCellEditor(new ButtonEditor(new JCheckBox()));
        
        JScrollPane scrollPane = new JScrollPane(transactionTable);
        mainFrame.add(scrollPane, BorderLayout.CENTER);

        // ===== Bottom Summary Panel =====
        JPanel summaryPanel = new JPanel(new GridLayout(2, 4, 10, 10));
        summaryPanel.setBorder(BorderFactory.createTitledBorder("Financial Summary"));
        summaryPanel.setBackground(new Color(240, 248, 255));
        
        incomeLabel = new JLabel("Total Income: " + userCurrency + "0.00", JLabel.CENTER);
        expenseLabel = new JLabel("Total Expense: " + userCurrency + "0.00", JLabel.CENTER);
        savingsLabel = new JLabel("Savings: " + userCurrency + "0.00", JLabel.CENTER);
        
        JPanel searchPanel = new JPanel(new FlowLayout());
        searchPanel.add(new JLabel("Search:"));
        searchField = new JTextField(20);
        searchPanel.add(searchField);
        
        summaryPanel.add(incomeLabel);
        summaryPanel.add(expenseLabel);
        summaryPanel.add(savingsLabel);
        summaryPanel.add(searchPanel);
        
        JButton reportBtn = new JButton("📊 Generate Report");
        JButton chartBtn = new JButton("📈 View Charts");
        JButton budgetBtn = new JButton("💰 Budget Overview");
        JButton settingsBtn = new JButton("⚙ Settings");
        
        summaryPanel.add(reportBtn);
        summaryPanel.add(chartBtn);
        summaryPanel.add(budgetBtn);
        summaryPanel.add(settingsBtn);
        
        mainFrame.add(summaryPanel, BorderLayout.SOUTH);

        // ===== Right Chart Panel =====
        chartPanel = new JPanel(new BorderLayout());
        chartPanel.setPreferredSize(new Dimension(400, 400));
        chartPanel.setBackground(Color.WHITE);
        chartPanel.setBorder(BorderFactory.createTitledBorder("Quick Overview"));
        mainFrame.add(chartPanel, BorderLayout.EAST);

        // ===== Actions =====
        addTxnBtn.addActionListener(e -> addTransaction());
        setBudgetBtn.addActionListener(e -> setBudget());
        clearBtn.addActionListener(e -> clearForm());
        chartBtn.addActionListener(e -> showPieChart());
        budgetBtn.addActionListener(e -> showBudgetManager());
        settingsBtn.addActionListener(e -> openSettings(mainFrame));
        reportBtn.addActionListener(e -> generateReport());

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { filterTable(); }
            public void removeUpdate(DocumentEvent e) { filterTable(); }
            public void insertUpdate(DocumentEvent e) { filterTable(); }
        });

        // Load initial data
        refreshData();
    }

    private static String[] getMonthOptions() {
        // FIXED: Use fully qualified List
        java.util.List<String> months = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = 0; i < 12; i++) {
            months.add(current.minusMonths(i).toString());
        }
        return months.toArray(new String[0]);
    }

    private static void filterByMonth() {
        String selectedMonth = (String) monthFilterBox.getSelectedItem();
        refreshData();
    }

    private static void refreshData() {
        HashMap<String, Double> expenseMap = new HashMap<>();
        DBHelper.loadTransactions(userEmail, tableModel, expenseMap);
        calculateTotals();
        updateSummary();
        updateChart();
    }

    private static void calculateTotals() {
        totalIncome = 0;
        totalExpense = 0;
        balance = 0;
        
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String type = (String) tableModel.getValueAt(i, 1);
            String amountStr = tableModel.getValueAt(i, 4).toString().replaceAll("[^\\d.]", "");
            double amount = Double.parseDouble(amountStr);
            
            if ("Income".equals(type)) {
                totalIncome += amount;
            } else {
                totalExpense += amount;
            }
        }
        
        balance = totalIncome - totalExpense;
    }

    private static void addTransaction() {
        String type = (String) typeBox.getSelectedItem();
        String category = (String) categoryBox.getSelectedItem();
        String tag = (String) tagBox.getSelectedItem();
        String recurring = (String) recurringBox.getSelectedItem();
        String date = dateField.getText();
        String description = descriptionField.getText();

        try {
            double amount = Double.parseDouble(amountField.getText());
            if (amount <= 0) {
                JOptionPane.showMessageDialog(null, "Amount must be positive!");
                return;
            }

            // Save to database
            DBHelper.saveTransaction(userEmail, date, type, category, tag, amount, recurring, description);

            // Refresh data
            refreshData();
            
            // Show success message
            JOptionPane.showMessageDialog(null, "✅ Transaction added successfully!");
            
            // Clear form
            clearForm();
            
            // Check budget alerts
            checkBudgetAlerts(category, amount);
            
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null, "Please enter a valid amount!");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Error: " + ex.getMessage());
        }
    }

    private static void checkBudgetAlerts(String category, double amount) {
        if (budgetAlerts && "Expense".equals((String) typeBox.getSelectedItem())) {
            String currentMonth = YearMonth.now().toString();
            double budget = DBHelper.getBudget(userEmail, category, currentMonth);
            double spent = DBHelper.getCategorySpending(userEmail, currentMonth).getOrDefault(category, 0.0);
            
            if (budget > 0) {
                if (spent > budget) {
                    JOptionPane.showMessageDialog(null,
                        "⚠️ Budget exceeded for " + category + "!\n" +
                        "Budget: " + userCurrency + String.format("%.2f", budget) + "\n" +
                        "Spent: " + userCurrency + String.format("%.2f", spent),
                        "Budget Warning", JOptionPane.WARNING_MESSAGE);
                } else if (spent >= budget * 0.8) {
                    JOptionPane.showMessageDialog(null,
                        "💡 You've reached 80% of your " + category + " budget!\n" +
                        "Budget: " + userCurrency + String.format("%.2f", budget) + "\n" +
                        "Spent: " + userCurrency + String.format("%.2f", spent),
                        "Budget Alert", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
    }

    private static void setBudget() {
        String category = (String) categoryBox.getSelectedItem();
        try {
            double budget = Double.parseDouble(budgetField.getText());
            if (budget < 0) {
                JOptionPane.showMessageDialog(null, "Budget must be positive!");
                return;
            }
            
            String currentMonth = YearMonth.now().toString();
            if (DBHelper.saveBudget(userEmail, category, budget, currentMonth)) {
                JOptionPane.showMessageDialog(null, "✅ Budget set for " + category + ": " + userCurrency + budget);
                budgetField.setText("");
            } else {
                JOptionPane.showMessageDialog(null, "❌ Failed to set budget!");
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null, "Please enter a valid budget amount!");
        }
    }

    private static void clearForm() {
        amountField.setText("");
        descriptionField.setText("");
        dateField.setValue(new Date());
    }

    private static void updateSummary() {
        incomeLabel.setText("Total Income: " + userCurrency + String.format("%.2f", totalIncome));
        expenseLabel.setText("Total Expense: " + userCurrency + String.format("%.2f", totalExpense));
        savingsLabel.setText("Savings: " + userCurrency + String.format("%.2f", balance));
        balanceLabel.setText("Balance: " + userCurrency + String.format("%.2f", balance));
        
        // Update colors based on balance
        if (balance < 0) {
            balanceLabel.setForeground(Color.RED);
            savingsLabel.setForeground(Color.RED);
        } else {
            balanceLabel.setForeground(new Color(0, 128, 0));
            savingsLabel.setForeground(new Color(0, 128, 0));
        }
    }

    private static void filterTable() {
        String text = searchField.getText().toLowerCase();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            boolean match = false;
            for (int col = 0; col < tableModel.getColumnCount() - 1; col++) {
                if (tableModel.getValueAt(i, col).toString().toLowerCase().contains(text)) {
                    match = true;
                    break;
                }
            }
            transactionTable.setRowHeight(i, match ? 30 : 0);
        }
    }

    private static void updateChart() {
        chartPanel.removeAll();
        
        DefaultPieDataset dataset = new DefaultPieDataset();
        HashMap<String, Double> spending = DBHelper.getCategorySpending(userEmail, YearMonth.now().toString());
        
        for (Map.Entry<String, Double> entry : spending.entrySet()) {
            if (entry.getValue() > 0) {
                dataset.setValue(entry.getKey(), entry.getValue());
            }
        }
        
        if (dataset.getItemCount() > 0) {
            JFreeChart chart = ChartFactory.createPieChart(
                "Monthly Spending",
                dataset,
                true, true, false
            );
            
            PiePlot plot = (PiePlot) chart.getPlot();
            plot.setSectionOutlinesVisible(false);
            plot.setLabelGenerator(null);
            
            ChartPanel chartPanelComponent = new ChartPanel(chart);
            chartPanelComponent.setPreferredSize(new Dimension(380, 350));
            chartPanel.add(chartPanelComponent, BorderLayout.CENTER);
        } else {
            JLabel noDataLabel = new JLabel("No data available for chart", JLabel.CENTER);
            noDataLabel.setFont(new Font("Arial", Font.ITALIC, 14));
            chartPanel.add(noDataLabel, BorderLayout.CENTER);
        }
        
        chartPanel.revalidate();
        chartPanel.repaint();
    }

    private static void showPieChart() {
        JFrame chartFrame = new JFrame("Spending Analysis");
        chartFrame.setSize(600, 500);
        chartFrame.setLocationRelativeTo(mainFrame);
        
        DefaultPieDataset dataset = new DefaultPieDataset();
        HashMap<String, Double> spending = DBHelper.getCategorySpending(userEmail, YearMonth.now().toString());
        
        for (Map.Entry<String, Double> entry : spending.entrySet()) {
            if (entry.getValue() > 0) {
                dataset.setValue(entry.getKey(), entry.getValue());
            }
        }
        
        JFreeChart chart = ChartFactory.createPieChart3D(
            "Spending Distribution - " + YearMonth.now().toString(),
            dataset,
            true, true, false
        );
        
        ChartPanel chartPanel = new ChartPanel(chart);
        chartFrame.add(chartPanel);
        chartFrame.setVisible(true);
    }

    private static void showFinancialSummary() {
        String currentMonth = YearMonth.now().toString();
        double[] summary = DBHelper.getFinancialSummary(userEmail, currentMonth);
        
        String message = String.format(
            "📊 Financial Summary for %s\n\n" +
            "💰 Total Income: %s%.2f\n" +
            "💸 Total Expenses: %s%.2f\n" +
            "💵 Net Savings: %s%.2f\n" +
            "📈 Savings Rate: %.1f%%",
            currentMonth,
            userCurrency, summary[0],
            userCurrency, summary[1],
            userCurrency, summary[2],
            summary[0] > 0 ? (summary[2] / summary[0]) * 100 : 0
        );
        
        JOptionPane.showMessageDialog(mainFrame, message, "Financial Summary", JOptionPane.INFORMATION_MESSAGE);
    }

    private static void showBudgetManager() {
        JFrame budgetFrame = new JFrame("Budget Management");
        budgetFrame.setSize(500, 400);
        budgetFrame.setLayout(new BorderLayout());
        budgetFrame.setLocationRelativeTo(mainFrame);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Budget input panel
        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Set Budget"));
        
        JComboBox<String> budgetCategoryBox = new JComboBox<>(new String[]{
            "Food", "Transport", "Shopping", "Entertainment", "Bills", "Rent"
        });
        JTextField budgetAmountField = new JTextField();
        JButton saveBudgetBtn = new JButton("Save Budget");
        
        inputPanel.add(new JLabel("Category:"));
        inputPanel.add(budgetCategoryBox);
        inputPanel.add(new JLabel("Amount:"));
        inputPanel.add(budgetAmountField);
        inputPanel.add(new JLabel(""));
        inputPanel.add(saveBudgetBtn);
        
        // Budget overview panel
        JTextArea budgetTextArea = new JTextArea(15, 30);
        budgetTextArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(budgetTextArea);
        
        updateBudgetOverview(budgetTextArea);
        
        saveBudgetBtn.addActionListener(e -> {
            try {
                String category = (String) budgetCategoryBox.getSelectedItem();
                double amount = Double.parseDouble(budgetAmountField.getText());
                String month = YearMonth.now().toString();
                
                if (DBHelper.saveBudget(userEmail, category, amount, month)) {
                    JOptionPane.showMessageDialog(budgetFrame, "Budget saved successfully!");
                    budgetAmountField.setText("");
                    updateBudgetOverview(budgetTextArea);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(budgetFrame, "Please enter a valid amount!");
            }
        });
        
        mainPanel.add(inputPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        budgetFrame.add(mainPanel);
        budgetFrame.setVisible(true);
    }

    private static void updateBudgetOverview(JTextArea textArea) {
        String currentMonth = YearMonth.now().toString();
        HashMap<String, Double> spending = DBHelper.getCategorySpending(userEmail, currentMonth);
        StringBuilder sb = new StringBuilder();
        
        sb.append("Budget Overview for ").append(currentMonth).append("\n\n");
        
        String[] categories = {"Food", "Transport", "Shopping", "Entertainment", "Bills", "Rent"};
        for (String category : categories) {
            double budget = DBHelper.getBudget(userEmail, category, currentMonth);
            double spent = spending.getOrDefault(category, 0.0);
            double remaining = budget - spent;
            double percentage = budget > 0 ? (spent / budget) * 100 : 0;
            
            sb.append(category).append(":\n");
            sb.append("  Budget: ").append(userCurrency).append(String.format("%.2f", budget)).append("\n");
            sb.append("  Spent: ").append(userCurrency).append(String.format("%.2f", spent)).append("\n");
            sb.append("  Remaining: ").append(userCurrency).append(String.format("%.2f", remaining)).append("\n");
            sb.append("  Usage: ").append(String.format("%.1f", percentage)).append("%\n\n");
        }
        
        textArea.setText(sb.toString());
    }

    private static void showFinancialGoals() {
        JOptionPane.showMessageDialog(mainFrame, 
            "Financial Goals feature coming soon!\n\n" +
            "Planned features:\n" +
            "• Set savings targets\n" +
            "• Track progress towards goals\n" +
            "• Goal achievement alerts\n" +
            "• Visual goal tracking",
            "Financial Goals", JOptionPane.INFORMATION_MESSAGE);
    }

    private static void showExportDialog() {
        String[] options = {"CSV", "PDF", "Excel"};
        int choice = JOptionPane.showOptionDialog(mainFrame,
            "Choose export format:",
            "Export Data",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            options,
            options[0]);
        
        switch (choice) {
            case 0:
                DBHelper.exportToCSVFromTable(tableModel);
                break;
            case 1:
                DBHelper.exportToPDF(tableModel);
                break;
            case 2:
                // Excel export would go here
                JOptionPane.showMessageDialog(mainFrame, "Excel export feature coming soon!");
                break;
        }
    }

    private static void generateReport() {
        String currentMonth = YearMonth.now().toString();
        double[] summary = DBHelper.getFinancialSummary(userEmail, currentMonth);
        HashMap<String, Double> spending = DBHelper.getCategorySpending(userEmail, currentMonth);
        
        StringBuilder report = new StringBuilder();
        report.append("FINANCIAL REPORT - ").append(currentMonth).append("\n\n");
        report.append("SUMMARY:\n");
        report.append("Total Income: ").append(userCurrency).append(String.format("%.2f", summary[0])).append("\n");
        report.append("Total Expenses: ").append(userCurrency).append(String.format("%.2f", summary[1])).append("\n");
        report.append("Net Savings: ").append(userCurrency).append(String.format("%.2f", summary[2])).append("\n");
        report.append("Savings Rate: ").append(String.format("%.1f", summary[0] > 0 ? (summary[2] / summary[0]) * 100 : 0)).append("%\n\n");
        
        report.append("CATEGORY BREAKDOWN:\n");
        for (Map.Entry<String, Double> entry : spending.entrySet()) {
            report.append(entry.getKey()).append(": ").append(userCurrency).append(String.format("%.2f", entry.getValue())).append("\n");
        }
        
        JTextArea textArea = new JTextArea(report.toString(), 20, 40);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        
        JOptionPane.showMessageDialog(mainFrame, scrollPane, "Financial Report", JOptionPane.INFORMATION_MESSAGE);
    }

    private static void loadUserSettings() {
        Object[] settings = DBHelper.getUserSettings(userEmail);
        if (settings != null) {
            userName = (String) settings[0];
            userPhone = (String) settings[1];
            userTheme = (String) settings[2];
            userCurrency = (String) settings[3];
            budgetAlerts = (Boolean) settings[4];
            autoLogoutMinutes = (Integer) settings[5];
            darkMode = "Dark".equals(userTheme);
        }
    }

    // FIXED: Use fully qualified Timer class
    private static void setupAutoLogout() {
        if (autoLogoutMinutes > 0) {
            javax.swing.Timer logoutTimer = new javax.swing.Timer(autoLogoutMinutes * 60 * 1000, e -> {
                int result = JOptionPane.showConfirmDialog(mainFrame,
                    "Session timeout. Would you like to stay logged in?",
                    "Auto Logout",
                    JOptionPane.YES_NO_OPTION);
                
                if (result == JOptionPane.NO_OPTION) {
                    mainFrame.dispose();
                    // You'll need to handle the login redirection here
                    JOptionPane.showMessageDialog(mainFrame, "Please restart the application to login again.");
                } else {
                    setupAutoLogout(); // Reset timer
                }
            });
            logoutTimer.setRepeats(false);
            logoutTimer.start();
        }
    }

    // Button renderer and editor for action column
    static class ButtonRenderer extends JButton implements javax.swing.table.TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }
        
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText("Delete");
            return this;
        }
    }

    static class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private int row;
        
        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> {
                if (DBHelper.deleteTransaction(row, tableModel, userEmail)) {
                    refreshData();
                    JOptionPane.showMessageDialog(button, "Transaction deleted successfully!");
                }
                fireEditingStopped();
            });
        }
        
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            this.row = row;
            button.setText("Delete");
            return button;
        }
    }

    private static void toggleTheme() {
        darkMode = !darkMode;
        Color bg = darkMode ? Color.DARK_GRAY : Color.WHITE;
        Color fg = darkMode ? Color.WHITE : Color.BLACK;
        
        Component[] components = {transactionTable, chartPanel, incomeLabel, expenseLabel, savingsLabel};
        for (Component comp : components) {
            comp.setBackground(bg);
            comp.setForeground(fg);
        }
        
        balanceLabel.setForeground(darkMode ? Color.GREEN : new Color(0, 128, 0));
    }

    private static void openSettings(JFrame parent) {
        JFrame settingsFrame = new JFrame("⚙ Settings");
        settingsFrame.setSize(500, 400);
        settingsFrame.setLayout(new GridLayout(10, 2, 10, 10));
        settingsFrame.setLocationRelativeTo(parent);

        JTextField nameField = new JTextField(userName);
        JTextField phoneField = new JTextField(userPhone);
        JTextField emailField = new JTextField(userEmail);
        JPasswordField passwordField = new JPasswordField();

        JComboBox<String> themeBox = new JComboBox<>(new String[]{"Light", "Dark"});
        themeBox.setSelectedItem(darkMode ? "Dark" : "Light");

        JComboBox<String> currencyBox = new JComboBox<>(new String[]{"₹", "$", "€", "£"});
        currencyBox.setSelectedItem(userCurrency);

        JCheckBox budgetAlertBox = new JCheckBox("Enable Budget Alerts", budgetAlerts);

        JComboBox<String> autoLogoutBox = new JComboBox<>(new String[]{"Never", "5 min", "10 min", "30 min"});
        autoLogoutBox.setSelectedIndex(autoLogoutMinutes == 0 ? 0 : (autoLogoutMinutes == 5 ? 1 : (autoLogoutMinutes == 10 ? 2 : 3)));

        JButton exportBtn = new JButton("📤 Export Data (CSV)");
        JButton importBtn = new JButton("📥 Import Data (CSV)");
        JButton saveBtn = new JButton("Save");

        settingsFrame.add(new JLabel("Name:"));
        settingsFrame.add(nameField);
        settingsFrame.add(new JLabel("Phone:"));
        settingsFrame.add(phoneField);
        settingsFrame.add(new JLabel("Email:"));
        settingsFrame.add(emailField);
        settingsFrame.add(new JLabel("Password:"));
        settingsFrame.add(passwordField);
        settingsFrame.add(new JLabel("Theme:"));
        settingsFrame.add(themeBox);
        settingsFrame.add(new JLabel("Currency:"));
        settingsFrame.add(currencyBox);
        settingsFrame.add(new JLabel(""));
        settingsFrame.add(budgetAlertBox);
        settingsFrame.add(new JLabel("Auto-Logout:"));
        settingsFrame.add(autoLogoutBox);
        settingsFrame.add(exportBtn);
        settingsFrame.add(importBtn);
        settingsFrame.add(saveBtn);

        saveBtn.addActionListener(e -> {
            userName = nameField.getText();
            userPhone = phoneField.getText();
            userEmail = emailField.getText();
            userTheme = (String) themeBox.getSelectedItem();
            userCurrency = (String) currencyBox.getSelectedItem();
            budgetAlerts = budgetAlertBox.isSelected();
            String selected = (String) autoLogoutBox.getSelectedItem();
            autoLogoutMinutes = selected.equals("5 min") ? 5 : selected.equals("10 min") ? 10 : selected.equals("30 min") ? 30 : 0;

            // Save to database
            DBHelper.updateUserSettings(userEmail, userName, userPhone, userTheme, userCurrency, budgetAlerts, autoLogoutMinutes);
            
            darkMode = "Dark".equals(userTheme);
            toggleTheme();
            
            JOptionPane.showMessageDialog(settingsFrame, "✅ Settings Saved!");
            settingsFrame.dispose();
        });

        exportBtn.addActionListener(e -> DBHelper.exportToCSVFromTable(tableModel));
        importBtn.addActionListener(e -> DBHelper.importFromCSV(tableModel, userEmail));

        settingsFrame.setVisible(true);
    }
}