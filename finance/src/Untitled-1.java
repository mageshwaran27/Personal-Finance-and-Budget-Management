172    pieChartBtn.addActionListener(e -> updatePieChart());

285    private static void updatePieChart(){
        DefaultPieDataset dataset = new DefaultPieDataset();
        HashMap<String, Double> categoryTotals = new HashMap<>();
        
        for(int i=0;i<tableModel.getRowCount();i++){
            String category = (String)tableModel.getValueAt(i,2);
            double amount = Double.parseDouble(tableModel.getValueAt(i,4).toString().replace(userCurrency,""));
            categoryTotals.put(category, categoryTotals.getOrDefault(category,0.0) + amount);
        }
        
        for(Map.Entry<String, Double> entry : categoryTotals.entrySet()){
            dataset.setValue(entry.getKey(), entry.getValue());
        }
        
        JFreeChart chart = ChartFactory.createPieChart("Expense Breakdown", dataset, true, true, false);
        chartPanel.removeAll();
        ChartPanel cPanel = new ChartPanel(chart);
        cPanel.setPreferredSize(new Dimension(380, 380));
        chartPanel.add(cPanel); 
        chartPanel.revalidate(); 
        chartPanel.repaint();
    }
300