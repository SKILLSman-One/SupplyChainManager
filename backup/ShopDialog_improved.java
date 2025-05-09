class ShopDialog extends JDialog {
    public ShopDialog(JFrame parent, Customer customer, java.util.List<Market> markets) {
        super(parent, "Shop", true);
        setSize(400, 250);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        JLabel balanceLabel = new JLabel("Balance: " + String.format("%.2f", customer.balance));
        JComboBox<String> productSelector = new JComboBox<>();
        Map<String, Market> productMarketMap = new HashMap<>();

        for (Market m : markets) {
            for (Map.Entry<String, Integer> entry : m.stock.entrySet()) {
                String label = entry.getKey() + " (" + m.name + ")";
                productSelector.addItem(label);
                productMarketMap.put(label, m);
            }
        }

        JLabel stockLabel = new JLabel("Stock: -");
        JTextField amountField = new JTextField(5);
        JButton buyBtn = new JButton("Buy");

        productSelector.addActionListener(e -> {
            String selected = (String) productSelector.getSelectedItem();
            if (selected == null) return;
            Market m = productMarketMap.get(selected);
            String product = selected.split(" \\(")[0];
            int stock = m.stock.getOrDefault(product, 0);
            stockLabel.setText("Stock: " + stock);
        });

        buyBtn.addActionListener(e -> {
            String selected = (String) productSelector.getSelectedItem();
            if (selected == null) return;
            Market m = productMarketMap.get(selected);
            String product = selected.split(" \\(")[0];

            try {
                int amount = Integer.parseInt(amountField.getText());
                if (amount <= 0) {
                    JOptionPane.showMessageDialog(this, "Amount must be greater than zero.");
                    return;
                }
                
                double price = m.getPrice(product);
                if (price <= 0) {
                    JOptionPane.showMessageDialog(this, "Price not set for this product. Please contact the market manager.");
                    return;
                }
                
                // Use the enhanced buyProduct method with proper exception handling
                try {
                    // This will throw appropriate exceptions with detailed messages
                    customer.buyProduct(product, amount, price, m);
                    
                    // Update UI after successful purchase
                    balanceLabel.setText("Balance: " + String.format("%.2f", customer.balance));
                    int newStock = m.stock.getOrDefault(product, 0);
                    stockLabel.setText("Stock: " + newStock);
                    
                    // Show success message
                    JOptionPane.showMessageDialog(this, "Purchase successful!");
                } catch (Exception ex) {
                    // Display the specific error from our economic logic
                    JOptionPane.showMessageDialog(this, "Purchase failed: " + ex.getMessage());
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid number for the amount.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        JPanel center = new JPanel(new GridLayout(3, 2, 5, 5));
        center.add(new JLabel("Select Product:"));
        center.add(productSelector);
        center.add(stockLabel);
        center.add(new JLabel("Buy Amount:"));
        center.add(amountField);
        center.add(buyBtn);

        add(balanceLabel, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
    }
}