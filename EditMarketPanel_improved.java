class EditMarketPanel extends JPanel {
    public EditMarketPanel(JFrame parent, Market market, java.util.List<FactoryExtended> factories) {
        setLayout(new BorderLayout());

        JLabel balanceLabel = new JLabel("Balance: " + String.format("%.2f", market.balance));
        JComboBox<String> productSelector = new JComboBox<>();
        JLabel stockLabel = new JLabel("Stock Quantity: -");

        // Populate productSelector from factory products
        Set<String> factoryProducts = new HashSet<>();
        for (FactoryExtended f : factories) {
            factoryProducts.addAll(f.products.keySet());
        }
        for (String product : factoryProducts) {
            productSelector.addItem(product);
        }

        JTextField amountField = new JTextField(5);
        JButton buyButton = new JButton("Buy");

        JTextField priceField = new JTextField(5);
        JButton updatePriceButton = new JButton("Update Price");

        productSelector.addActionListener(e -> {
            String selected = (String) productSelector.getSelectedItem();
            if (selected == null) return;
            
            int total = 0;
            for (FactoryExtended f : factories) {
                InventoryItem item = f.products.get(selected);
                if (item != null) total += item.quantity;
            }
            stockLabel.setText("Stock Quantity: " + total);
        });

        buyButton.addActionListener(e -> {
            String selected = (String) productSelector.getSelectedItem();
            if (selected == null) {
                JOptionPane.showMessageDialog(parent, "Please select a product first.");
                return;
            }
            
            try {
                int amount = Integer.parseInt(amountField.getText());
                if (amount <= 0) {
                    JOptionPane.showMessageDialog(parent, "Amount must be greater than zero.");
                    return;
                }
                
                // Try to find factory that can sell
                boolean purchaseSuccessful = false;
                for (FactoryExtended f : factories) {
                    InventoryItem item = f.products.get(selected);
                    if (item != null && item.quantity >= amount) {
                        // Find cost per unit
                        double costPerUnit = 0;
                        for (ProductDesign d : f.designs) {
                            if (d.name.equals(selected)) {
                                costPerUnit = d.cost;
                                break;
                            }
                        }
                        
                        try {
                            // Use the enhanced market.buyProduct with exceptions
                            market.buyProduct(selected, amount, costPerUnit, f);
                            
                            // Update UI
                            balanceLabel.setText("Balance: " + String.format("%.2f", market.balance));
                            
                            // Refresh product selection to update stock display
                            productSelector.setSelectedItem(selected);
                            
                            // Show simple success message
                            JOptionPane.showMessageDialog(parent, "Purchase successful!");
                            
                            purchaseSuccessful = true;
                            break;
                        } catch (Exception ex) {
                            // Show specific error message
                            JOptionPane.showMessageDialog(parent, "Purchase failed: " + ex.getMessage());
                            return;
                        }
                    }
                }
                
                if (!purchaseSuccessful) {
                    JOptionPane.showMessageDialog(parent, "Could not find a factory with enough stock.");
                }
                
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(parent, "Please enter a valid number for amount.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent, "Error: " + ex.getMessage());
            }
        });

        updatePriceButton.addActionListener(e -> {
            String selected = (String) productSelector.getSelectedItem();
            if (selected == null) {
                JOptionPane.showMessageDialog(parent, "Please select a product first.");
                return;
            }
            
            try {
                double price = Double.parseDouble(priceField.getText());
                
                try {
                    // Use enhanced setPrice with exception for negative values
                    market.setPrice(selected, price);
                    JOptionPane.showMessageDialog(parent, "Price updated for " + selected + " to " + price);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(parent, "Price update failed: " + ex.getMessage());
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(parent, "Please enter a valid number for price.");
            }
        });
        
        // No transaction history functionality

        JPanel top = new JPanel(new GridLayout(3, 2, 5, 5));
        top.add(new JLabel("Select Product:"));
        top.add(productSelector);
        top.add(stockLabel);
        top.add(new JLabel("Amount:"));
        top.add(amountField);
        top.add(buyButton);

        JPanel bottom = new JPanel(new FlowLayout());
        bottom.add(new JLabel("Price (for stock):"));
        bottom.add(priceField);
        bottom.add(updatePriceButton);

        add(balanceLabel, BorderLayout.NORTH);
        add(top, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }
}