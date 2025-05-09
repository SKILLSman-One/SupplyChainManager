import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/**
 * Supply Chain Management System
 * This application simulates a supply chain with factories, markets, and customers.
 * 
 * For CS1 Students:
 * This program demonstrates object-oriented programming concepts including:
 * - Classes and Objects
 * - Inheritance
 * - Java Swing GUI components
 * - Exception handling
 * - Collections (Maps, Lists)
 */
public class SupplyChainSystemGUI {
    public static void main(String[] args) {
        // Create and display the main application window
        try {
            // Set the look and feel to match the system
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // If setting look and feel fails, continue with default
            System.out.println("Could not set system look and feel: " + e.getMessage());
        }
        
        // Launch the application on the Event Dispatch Thread for thread safety
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}

/**
 * Represents a raw material input for production
 */
class InputMaterial {
    Producer producer;
    int amount;

    /**
     * Creates a new input material
     * @param producer The producer of this material
     * @param amount The amount required
     */
    public InputMaterial(Producer producer, int amount) {
        this.producer = producer;
        this.amount = amount;
    }

    @Override
    public String toString() {
        return producer.name + " x" + amount;
    }
}

/**
 * Represents a customer who can purchase products from markets
 */
class Customer {
    String name;
    double balance;
    Map<String, Integer> inventory = new HashMap<>();

    /**
     * Creates a new customer
     * @param name The customer's name
     * @param balance The customer's starting balance
     */
    public Customer(String name, double balance) {
        this.name = name;
        this.balance = balance;
    }

    /**
     * Adds a product to the customer's inventory
     * @param product The product name
     * @param amount The amount to add
     */
    public void addProduct(String product, int amount) {
        inventory.put(product, inventory.getOrDefault(product, 0) + amount);
    }

    @Override
    public String toString() {
        return name + " (Balance: " + String.format("%.2f", balance) + ")";
    }
}

/**
 * Represents a market where products can be bought and sold
 */
class Market {
    String name;
    double balance;
    Map<String, Integer> stock = new HashMap<>();
    Map<String, Double> prices = new HashMap<>();

    /**
     * Creates a new market
     * @param name The market's name
     * @param balance The market's starting balance
     */
    public Market(String name, double balance) {
        this.name = name;
        this.balance = balance;
    }

    /**
     * Buys a product from a factory and adds it to stock
     * @param product The product name
     * @param amount The amount to buy
     * @param pricePerUnit The price per unit
     * @param factory The factory to buy from
     * @return true if purchase was successful, false otherwise
     */
    public boolean buyProduct(String product, int amount, double pricePerUnit, FactoryExtended factory) {
        // Check if the factory produces this product
        if (!factory.products.containsKey(product)) {
            return false;
        }

        InventoryItem item = factory.products.get(product);
        // Check if factory has enough stock
        if (item.quantity < amount) {
            return false;
        }

        double totalCost = amount * pricePerUnit;
        // Check if market has enough money
        if (balance < totalCost) {
            return false;
        }

        // Execute the transaction
        item.quantity -= amount;
        balance -= totalCost;
        factory.balance += totalCost; // Pay the factory
        
        // Add to market stock
        stock.put(product, stock.getOrDefault(product, 0) + amount);
        return true;
    }

    /**
     * Sets the selling price for a product
     * @param product The product name
     * @param price The price to set
     */
    public void setPrice(String product, double price) {
        prices.put(product, price);
    }

    /**
     * Gets the selling price for a product
     * @param product The product name
     * @return The price of the product
     */
    public double getPrice(String product) {
        return prices.getOrDefault(product, 0.0);
    }

    @Override
    public String toString() {
        return name;
    }
}

/**
 * Dialog for editing customer information
 */
class EditCustomerDialog extends JDialog {
    /**
     * Creates a dialog for editing customer information
     * @param parent The parent frame
     * @param customer The customer to edit
     * @param markets The list of available markets
     */
    public EditCustomerDialog(JFrame parent, Customer customer, java.util.List<Market> markets) {
        super(parent, "Edit Customer", true);
        setSize(300, 150);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        JLabel nameLabel = new JLabel("Name: " + customer.name);
        JLabel balanceLabel = new JLabel("Balance: " + String.format("%.2f", customer.balance));
        JButton shopBtn = new JButton("Shop");
        JButton inventoryBtn = new JButton("View Inventory");

        JPanel top = new JPanel(new GridLayout(2, 1));
        top.add(nameLabel);
        top.add(balanceLabel);

        JPanel bottom = new JPanel();
        bottom.add(shopBtn);
        bottom.add(inventoryBtn);

        // Add action listeners to buttons
        shopBtn.addActionListener(e -> {
            try {
                new ShopDialog(parent, customer, markets).setVisible(true);
                // Update the balance label after shopping
                balanceLabel.setText("Balance: " + String.format("%.2f", customer.balance));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error opening shop: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        inventoryBtn.addActionListener(e -> {
            try {
                new CustomerInventoryDialog(parent, customer).setVisible(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error viewing inventory: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        add(top, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }
}

/**
 * Dialog for shopping at markets
 */
class ShopDialog extends JDialog {
    /**
     * Creates a dialog for shopping at markets
     * @param parent The parent frame
     * @param customer The customer who is shopping
     * @param markets The list of available markets
     */
    public ShopDialog(JFrame parent, Customer customer, java.util.List<Market> markets) {
        super(parent, "Shop", true);
        setSize(400, 250);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        JLabel balanceLabel = new JLabel("Balance: " + String.format("%.2f", customer.balance));
        JComboBox<String> productSelector = new JComboBox<>();
        Map<String, Market> productMarketMap = new HashMap<>();
        JLabel priceLabel = new JLabel("Price: -");

        // Populate product selector with available products from all markets
        for (Market m : markets) {
            for (Map.Entry<String, Integer> entry : m.stock.entrySet()) {
                String product = entry.getKey();
                // Only add products that have stock
                if (entry.getValue() > 0) {
                    String label = product + " (" + m.name + ")";
                    productSelector.addItem(label);
                    productMarketMap.put(label, m);
                }
            }
        }

        JLabel stockLabel = new JLabel("Stock: -");
        JTextField amountField = new JTextField(5);
        JButton buyBtn = new JButton("Buy");

        // Update stock display when product selection changes
        productSelector.addActionListener(e -> {
            String selected = (String) productSelector.getSelectedItem();
            if (selected == null) return;
            
            try {
                Market m = productMarketMap.get(selected);
                String product = selected.split(" \\(")[0];
                int stock = m.stock.getOrDefault(product, 0);
                double price = m.getPrice(product);
                
                stockLabel.setText("Stock: " + stock);
                priceLabel.setText("Price: " + String.format("%.2f", price) + " each");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error selecting product: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        // Handle the buy button click
        buyBtn.addActionListener(e -> {
            String selected = (String) productSelector.getSelectedItem();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, 
                    "Please select a product first.", 
                    "No Product Selected", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            try {
                Market m = productMarketMap.get(selected);
                String product = selected.split(" \\(")[0];

                // Validate amount input
                int amount;
                try {
                    amount = Integer.parseInt(amountField.getText());
                    if (amount <= 0) {
                        throw new NumberFormatException("Amount must be greater than zero");
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, 
                        "Please enter a valid positive number for amount.", 
                        "Invalid Amount", 
                        JOptionPane.WARNING_MESSAGE);
                    amountField.requestFocus();
                    return;
                }

                int available = m.stock.getOrDefault(product, 0);
                double price = m.getPrice(product);
                double total = price * amount;

                // Validate purchase conditions
                if (amount > available) {
                    JOptionPane.showMessageDialog(this, 
                        "Not enough stock available. Maximum available: " + available, 
                        "Insufficient Stock", 
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                if (customer.balance < total) {
                    JOptionPane.showMessageDialog(this, 
                        "Not enough balance. Required: " + String.format("%.2f", total) + 
                        ", Available: " + String.format("%.2f", customer.balance), 
                        "Insufficient Balance", 
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }

                // Execute purchase
                customer.balance -= total;
                m.stock.put(product, available - amount);
                m.balance += total; // Add money to market
                customer.addProduct(product, amount);

                // Update display
                balanceLabel.setText("Balance: " + String.format("%.2f", customer.balance));
                stockLabel.setText("Stock: " + (available - amount));
                
                JOptionPane.showMessageDialog(this, 
                    "Successfully purchased " + amount + " units of " + product + 
                    " for a total of " + String.format("%.2f", total), 
                    "Purchase Successful", 
                    JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error during purchase: " + ex.getMessage(), 
                    "Purchase Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        // Create and arrange UI components
        JPanel center = new JPanel(new GridLayout(4, 2, 5, 5));
        center.add(new JLabel("Select Product:"));
        center.add(productSelector);
        center.add(stockLabel);
        center.add(priceLabel);
        center.add(new JLabel("Buy Amount:"));
        center.add(amountField);
        center.add(new JLabel()); // Spacer
        center.add(buyBtn);

        add(balanceLabel, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        
        // Initialize with first product if available
        if (productSelector.getItemCount() > 0) {
            productSelector.setSelectedIndex(0);
        }
    }
}

/**
 * Dialog to display a customer's inventory
 */
class CustomerInventoryDialog extends JDialog {
    /**
     * Creates a dialog showing a customer's inventory
     * @param parent The parent frame
     * @param customer The customer whose inventory to display
     */
    public CustomerInventoryDialog(JFrame parent, Customer customer) {
        super(parent, "Inventory of " + customer.name, true);
        setSize(400, 300);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        JTextArea area = new JTextArea();
        area.setEditable(false);
        StringBuilder sb = new StringBuilder();
        sb.append("Balance: ").append(String.format("%.2f", customer.balance)).append("\n\n");
        sb.append("Inventory:\n");
        
        if (customer.inventory.isEmpty()) {
            sb.append("No items in inventory.\n");
        } else {
            for (Map.Entry<String, Integer> entry : customer.inventory.entrySet()) {
                sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }

        area.setText(sb.toString());
        add(new JScrollPane(area), BorderLayout.CENTER);
        
        // Add close button at the bottom
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(closeButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }
}

/**
 * Dialog for adding a new market
 */
class AddMarketDialog extends JDialog {
    /**
     * Creates a dialog for adding a new market
     * @param parent The parent frame
     * @param markets The list of markets to add to
     * @param updateCallback Callback to run after adding a market
     */
    public AddMarketDialog(JFrame parent, java.util.List<Market> markets, Runnable updateCallback) {
        super(parent, "Add New Market", true);
        setSize(300, 200);
        setLocationRelativeTo(parent);
        setLayout(new GridLayout(3, 2, 5, 5));

        JTextField nameField = new JTextField();
        JTextField balanceField = new JTextField();

        add(new JLabel("Market Name:"));
        add(nameField);
        add(new JLabel("Initial Balance:"));
        add(balanceField);

        JButton addBtn = new JButton("Add");
        JButton cancelBtn = new JButton("Cancel");
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addBtn);
        buttonPanel.add(cancelBtn);
        
        add(new JLabel()); // spacer
        add(buttonPanel);

        // Handle add button click
        addBtn.addActionListener(e -> {
            try {
                // Validate inputs
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    throw new IllegalArgumentException("Market name cannot be empty");
                }
                
                // Check for duplicate market names
                for (Market existingMarket : markets) {
                    if (existingMarket.name.equals(name)) {
                        throw new IllegalArgumentException("A market with this name already exists");
                    }
                }
                
                double balance;
                try {
                    balance = Double.parseDouble(balanceField.getText());
                    if (balance < 0) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Balance must be a positive number");
                }
                
                // Create and add the new market
                markets.add(new Market(name, balance));
                updateCallback.run();
                dispose();
                
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, 
                    ex.getMessage(), 
                    "Invalid Input", 
                    JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error adding market: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        // Handle cancel button click
        cancelBtn.addActionListener(e -> dispose());
    }
}

/**
 * Panel for editing market details
 */
class EditMarketPanel extends JPanel {
    /**
     * Creates a panel for editing market details
     * @param parent The parent frame
     * @param market The market to edit
     * @param factories The list of available factories
     */
    public EditMarketPanel(JFrame parent, Market market, java.util.List<FactoryExtended> factories) {
        setLayout(new BorderLayout());

        JLabel balanceLabel = new JLabel("Balance: " + String.format("%.2f", market.balance));
        JComboBox<String> productSelector = new JComboBox<>();
        JLabel stockLabel = new JLabel("Stock Quantity: -");
        JLabel marketStockLabel = new JLabel("Market Stock: -");
        JLabel priceLabel = new JLabel("Current Price: -");

        // Populate productSelector from factory products
        Set<String> factoryProducts = new HashSet<>();
        for (FactoryExtended f : factories) {
            factoryProducts.addAll(f.products.keySet());
        }
        
        // Sort products alphabetically for easier navigation
        java.util.List<String> sortedProducts = new ArrayList<>(factoryProducts);
        Collections.sort(sortedProducts);
        
        for (String product : sortedProducts) {
            productSelector.addItem(product);
        }

        JTextField amountField = new JTextField(5);
        JButton buyButton = new JButton("Buy");

        JTextField priceField = new JTextField(5);
        JButton updatePriceButton = new JButton("Update Price");

        // Update display when product selection changes
        productSelector.addActionListener(e -> {
            try {
                String selected = (String) productSelector.getSelectedItem();
                if (selected == null) return;
                
                // Calculate total available in factories
                int total = 0;
                for (FactoryExtended f : factories) {
                    InventoryItem item = f.products.get(selected);
                    if (item != null) total += item.quantity;
                }
                
                // Update labels
                stockLabel.setText("Factory Stock: " + total);
                int marketStock = market.stock.getOrDefault(selected, 0);
                marketStockLabel.setText("Market Stock: " + marketStock);
                
                double currentPrice = market.getPrice(selected);
                if (currentPrice > 0) {
                    priceLabel.setText("Current Price: " + String.format("%.2f", currentPrice));
                    priceField.setText(String.format("%.2f", currentPrice));
                } else {
                    priceLabel.setText("Current Price: Not set");
                    priceField.setText("");
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent, 
                    "Error selecting product: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        // Handle buy button click
        buyButton.addActionListener(e -> {
            String selected = (String) productSelector.getSelectedItem();
            if (selected == null) {
                JOptionPane.showMessageDialog(parent, 
                    "Please select a product first.", 
                    "No Product Selected", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            try {
                // Validate amount
                int amount;
                try {
                    amount = Integer.parseInt(amountField.getText());
                    if (amount <= 0) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(parent, 
                        "Please enter a valid positive number for amount.", 
                        "Invalid Amount", 
                        JOptionPane.WARNING_MESSAGE);
                    amountField.requestFocus();
                    return;
                }

                // Try to find factory that can sell
                boolean purchaseSuccessful = false;
                for (FactoryExtended f : factories) {
                    InventoryItem item = f.products.get(selected);
                    if (item != null && item.quantity >= amount) {
                        double costPerUnit = 0;
                        // Find the product design to get the cost
                        for (ProductDesign d : f.designs) {
                            if (d.name.equals(selected)) {
                                costPerUnit = d.cost;
                                break;
                            }
                        }
                        
                        // Check if market has enough money
                        double totalCost = costPerUnit * amount;
                        if (market.balance < totalCost) {
                            JOptionPane.showMessageDialog(parent, 
                                "Market does not have enough balance. Required: " + 
                                String.format("%.2f", totalCost) + ", Available: " + 
                                String.format("%.2f", market.balance), 
                                "Insufficient Balance", 
                                JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                        
                        // Execute purchase
                        if (market.buyProduct(selected, amount, costPerUnit, f)) {
                            balanceLabel.setText("Balance: " + String.format("%.2f", market.balance));
                            
                            // Update displayed stock
                            int newFactoryStock = 0;
                            for (FactoryExtended factory : factories) {
                                InventoryItem factoryItem = factory.products.get(selected);
                                if (factoryItem != null) newFactoryStock += factoryItem.quantity;
                            }
                            
                            stockLabel.setText("Factory Stock: " + newFactoryStock);
                            int marketStock = market.stock.getOrDefault(selected, 0);
                            marketStockLabel.setText("Market Stock: " + marketStock);
                            
                            JOptionPane.showMessageDialog(parent, 
                                "Successfully purchased " + amount + " units of " + selected + 
                                " for a total of " + String.format("%.2f", totalCost), 
                                "Purchase Successful", 
                                JOptionPane.INFORMATION_MESSAGE);
                            
                            purchaseSuccessful = true;
                            break;
                        }
                    }
                }
                
                if (!purchaseSuccessful) {
                    JOptionPane.showMessageDialog(parent, 
                        "Not enough stock in factories or purchase failed.", 
                        "Purchase Failed", 
                        JOptionPane.WARNING_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent, 
                    "Error during purchase: " + ex.getMessage(), 
                    "Purchase Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        // Handle update price button click
        updatePriceButton.addActionListener(e -> {
            String selected = (String) productSelector.getSelectedItem();
            if (selected == null) {
                JOptionPane.showMessageDialog(parent, 
                    "Please select a product first.", 
                    "No Product Selected", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            try {
                // Validate price
                double price;
                try {
                    price = Double.parseDouble(priceField.getText());
                    if (price < 0) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(parent, 
                        "Please enter a valid positive number for price.", 
                        "Invalid Price", 
                        JOptionPane.WARNING_MESSAGE);
                    priceField.requestFocus();
                    return;
                }
                
                // Update the price
                market.setPrice(selected, price);
                priceLabel.setText("Current Price: " + String.format("%.2f", price));
                
                JOptionPane.showMessageDialog(parent, 
                    "Price updated for " + selected + " to " + String.format("%.2f", price), 
                    "Price Updated", 
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent, 
                    "Error updating price: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        // Create and arrange UI components
        JPanel top = new JPanel(new GridLayout(5, 2, 5, 5));
        top.add(new JLabel("Select Product:"));
        top.add(productSelector);
        top.add(stockLabel);
        top.add(marketStockLabel);
        top.add(priceLabel);
        top.add(new JLabel("Amount to Buy:"));
        top.add(amountField);
        top.add(buyButton);

        JPanel bottom = new JPanel(new FlowLayout());
        bottom.add(new JLabel("Set New Price:"));
        bottom.add(priceField);
        bottom.add(updatePriceButton);

        add(balanceLabel, BorderLayout.NORTH);
        add(top, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        
        // Initialize with first product if available
        if (productSelector.getItemCount() > 0) {
            productSelector.setSelectedIndex(0);
        }
    }
}

/**
 * Dialog for editing market details
 */
class EditMarketDialog extends JDialog {
    /**
     * Creates a dialog for editing market details
     * @param parent The parent frame
     * @param market The market to edit
     * @param factories The list of available factories
     */
    public EditMarketDialog(JFrame parent, Market market, java.util.List<FactoryExtended> factories) {
        super(parent, "Edit Market: " + market.name, true);
        setSize(400, 350);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Buy Products", new EditMarketPanel(parent, market, factories));
        tabs.addTab("Market Stock", createMarketStockPanel(market));

        add(tabs, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(closeButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * Creates a panel showing the market's current stock
     * @param market The market to show stock for
     * @return A panel displaying market stock
     */
    private JPanel createMarketStockPanel(Market market) {
        JPanel panel = new JPanel(new BorderLayout());
        
        JTextArea stockArea = new JTextArea();
        stockArea.setEditable(false);
        
        StringBuilder sb = new StringBuilder();
        sb.append("Market: ").append(market.name).append("\n");
        sb.append("Balance: ").append(String.format("%.2f", market.balance)).append("\n\n");
        sb.append("Current Stock:\n");
        
        if (market.stock.isEmpty()) {
            sb.append("No items in stock.\n");
        } else {
            // Sort products alphabetically
            java.util.List<String> sortedProducts = new ArrayList<>(market.stock.keySet());
            Collections.sort(sortedProducts);
            
            for (String product : sortedProducts) {
                int quantity = market.stock.get(product);
                double price = market.getPrice(product);
                
                sb.append(product)
                  .append(": ")
                  .append(quantity)
                  .append(" units");
                
                if (price > 0) {
                    sb.append(" (Price: ")
                      .append(String.format("%.2f", price))
                      .append(")");
                } else {
                    sb.append(" (Price not set)");
                }
                
                sb.append("\n");
            }
        }
        
        stockArea.setText(sb.toString());
        panel.add(new JScrollPane(stockArea), BorderLayout.CENTER);
        
        return panel;
    }
}

/**
 * Dialog for adding a new customer
 */
class AddCustomerDialog extends JDialog {
    /**
     * Creates a dialog for adding a new customer
     * @param parent The parent frame
     * @param customers The list of customers to add to
     * @param updateCallback Callback to run after adding a customer
     */
    public AddCustomerDialog(JFrame parent, java.util.List<Customer> customers, Runnable updateCallback) {
        super(parent, "Add New Customer", true);
        setSize(300, 200);
        setLocationRelativeTo(parent);
        setLayout(new GridLayout(3, 2, 5, 5));

        JTextField nameField = new JTextField();
        JTextField balanceField = new JTextField();

        add(new JLabel("Customer Name:"));
        add(nameField);
        add(new JLabel("Initial Balance:"));
        add(balanceField);

        JButton addBtn = new JButton("Add");
        JButton cancelBtn = new JButton("Cancel");
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addBtn);
        buttonPanel.add(cancelBtn);
        
        add(new JLabel()); // spacer
        add(buttonPanel);

        // Handle add button click
        addBtn.addActionListener(e -> {
            try {
                // Validate inputs
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    throw new IllegalArgumentException("Customer name cannot be empty");
                }
                
                // Check for duplicate customer names
                for (Customer existingCustomer : customers) {
                    if (existingCustomer.name.equals(name)) {
                        throw new IllegalArgumentException("A customer with this name already exists");
                    }
                }
                
                double balance;
                try {
                    balance = Double.parseDouble(balanceField.getText());
                    if (balance < 0) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Balance must be a positive number");
                }
                
                // Create and add the new customer
                customers.add(new Customer(name, balance));
                updateCallback.run();
                dispose();
                
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, 
                    ex.getMessage(), 
                    "Invalid Input", 
                    JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error adding customer: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        // Handle cancel button click
        cancelBtn.addActionListener(e -> dispose());
    }
}

/**
 * Represents an item in a factory's inventory
 */
class InventoryItem {
    String name;
    int quantity;

    /**
     * Creates a new inventory item
     * @param name The item name
     * @param quantity The initial quantity
     */
    public InventoryItem(String name, int quantity) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Item name cannot be empty");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        this.name = name;
        this.quantity = quantity;
    }
    
    @Override
    public String toString() {
        return name + ": " + quantity;
    }
}

/**
 * Represents a producer of raw materials
 */
class Producer {
    String name;
    double cost;

    /**
     * Creates a new producer
     * @param name The producer's name
     * @param cost The cost per unit
     * @throws IllegalArgumentException if name is empty or cost is negative
     */
    public Producer(String name, double cost) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Producer name cannot be empty");
        }
        if (cost <= 0) {
            throw new IllegalArgumentException("Cost must be greater than zero");
        }
        this.name = name;
        this.cost = cost;
    }

    @Override
    public String toString() {
        return name + " (Cost: " + String.format("%.2f", cost) + ")";
    }
}

/**
 * Represents a product design blueprint
 */
class ProductDesign {
    String name;
    double cost;
    java.util.List<InputMaterial> inputs = new ArrayList<>();

    /**
     * Creates a new product design
     * @param name The product name
     * @param cost The production cost
     * @throws IllegalArgumentException if name is empty or cost is negative
     */
    public ProductDesign(String name, double cost) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be empty");
        }
        if (cost < 0) {
            throw new IllegalArgumentException("Cost cannot be negative");
        }
        this.name = name;
        this.cost = cost;
    }

    /**
     * Adds an input material to the design
     * @param producer The producer of the material
     * @param amount The amount required
     * @throws IllegalArgumentException if producer is null or amount is negative
     */
    public void addInput(Producer producer, int amount) {
        if (producer == null) {
            throw new IllegalArgumentException("Producer cannot be null");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        inputs.add(new InputMaterial(producer, amount));
    }

    @Override
    public String toString() {
        return name;
    }
}

/**
 * Represents a basic factory
 */
class Factory {
    String name;
    double balance;
    java.util.List<Producer> producers = new ArrayList<>();

    /**
     * Creates a new factory
     * @param name The factory's name
     * @param balance The initial balance
     */
    public Factory(String name, double balance) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Factory name cannot be empty");
        }
        if (balance < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative");
        }
        this.name = name;
        this.balance = balance;
    }

    @Override
    public String toString() {
        return name;
    }
}

/**
 * Extended factory with production capabilities
 */
class FactoryExtended extends Factory {
    java.util.List<ProductDesign> designs = new ArrayList<>();
    Map<String, InventoryItem> products = new HashMap<>();

    /**
     * Creates a new extended factory
     * @param name The factory's name
     * @param balance The initial balance
     */
    public FactoryExtended(String name, double balance) {
        super(name, balance);
    }

    /**
     * Produces a product from its design
     * @param design The product design to produce
     * @param amount The amount to produce
     * @return true if production was successful, false otherwise
     * @throws IllegalArgumentException if design is null or amount is not positive
     */
    public boolean produce(ProductDesign design, int amount) {
        if (design == null) {
            throw new IllegalArgumentException("Product design cannot be null");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Production amount must be positive");
        }
        
        double totalCost = design.cost * amount;
        
        // Check if factory has enough money
        if (balance < totalCost) {
            return false;
        }

        // Add to inventory
        InventoryItem item = products.getOrDefault(design.name, new InventoryItem(design.name, 0));
        item.quantity += amount;
        products.put(design.name, item);
        
        // Deduct cost
        balance -= totalCost;
        
        return true;
    }
}

/**
 * Dialog for adding a new producer
 */
class AddProducerDialog extends JDialog {
    /**
     * Creates a dialog for adding a new producer
     * @param parent The parent frame
     * @param factory The factory to add the producer to
     * @param updateCallback Callback to run after adding a producer
     */
    public AddProducerDialog(JFrame parent, Factory factory, Runnable updateCallback) {
        super(parent, "Add New Producer", true);
        setSize(300, 200);
        setLocationRelativeTo(parent);
        setLayout(new GridLayout(3, 2, 5, 5));

        JTextField nameField = new JTextField();
        JTextField costField = new JTextField();

        add(new JLabel("Producer Name:"));
        add(nameField);
        add(new JLabel("Cost per Unit:"));
        add(costField);

        JButton addBtn = new JButton("Add");
        JButton cancelBtn = new JButton("Cancel");
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addBtn);
        buttonPanel.add(cancelBtn);
        
        add(new JLabel()); // spacer
        add(buttonPanel);

        // Handle add button click
        addBtn.addActionListener(e -> {
            try {
                // Validate inputs
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    throw new IllegalArgumentException("Producer name cannot be empty");
                }
                
                // Check for duplicate producer names
                for (Producer existingProducer : factory.producers) {
                    if (existingProducer.name.equals(name)) {
                        throw new IllegalArgumentException("A producer with this name already exists");
                    }
                }
                
                double cost;
                try {
                    cost = Double.parseDouble(costField.getText());
                    if (cost <= 0) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Cost must be a positive number");
                }
                
                // Create and add the new producer
                factory.producers.add(new Producer(name, cost));
                updateCallback.run();
                dispose();
                
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, 
                    ex.getMessage(), 
                    "Invalid Input", 
                    JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error adding producer: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        // Handle cancel button click
        cancelBtn.addActionListener(e -> dispose());
    }
}

/**
 * Dialog for adding a new product design
 */
class AddDesignDialog extends JDialog {
    /**
     * Creates a dialog for adding a new product design
     * @param parent The parent frame
     * @param factory The factory to add the design to
     * @param updateCallback Callback to run after adding a design
     */
    public AddDesignDialog(JFrame parent, FactoryExtended factory, Runnable updateCallback) {
        super(parent, "Add New Product Design", true);
        setSize(400, 300);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        JTextField nameField = new JTextField();
        JTextField costField = new JTextField();
        JPanel inputsPanel = new JPanel();
        inputsPanel.setLayout(new BoxLayout(inputsPanel, BoxLayout.Y_AXIS));
        java.util.List<JComboBox<Producer>> producerSelectors = new ArrayList<>();
        java.util.List<JTextField> amountFields = new ArrayList<>();

        // Create the top panel
        JPanel topPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        topPanel.add(new JLabel("Product Name:"));
        topPanel.add(nameField);
        topPanel.add(new JLabel("Production Cost:"));
        topPanel.add(costField);

        // Create button to add input material
        JButton addInputBtn = new JButton("Add Input Material");
        addInputBtn.addActionListener(e -> {
            JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
            
            JComboBox<Producer> producerSelector = new JComboBox<>();
            for (Producer p : factory.producers) {
                producerSelector.addItem(p);
            }
            
            JTextField amountField = new JTextField(5);
            
            inputRow.add(producerSelector);
            inputRow.add(new JLabel("Amount:"));
            inputRow.add(amountField);
            
            producerSelectors.add(producerSelector);
            amountFields.add(amountField);
            
            inputsPanel.add(inputRow);
            inputsPanel.revalidate();
            inputsPanel.repaint();
        });

        // Create the buttons panel
        JPanel buttonsPanel = new JPanel();
        JButton saveBtn = new JButton("Save Design");
        JButton cancelBtn = new JButton("Cancel");
        buttonsPanel.add(saveBtn);
        buttonsPanel.add(cancelBtn);

        // Handle save button click
        saveBtn.addActionListener(e -> {
            try {
                // Validate basic inputs
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    throw new IllegalArgumentException("Product name cannot be empty");
                }
                
                // Check for duplicate design names
                for (ProductDesign existingDesign : factory.designs) {
                    if (existingDesign.name.equals(name)) {
                        throw new IllegalArgumentException("A product design with this name already exists");
                    }
                }
                
                double cost;
                try {
                    cost = Double.parseDouble(costField.getText());
                    if (cost < 0) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Production cost must be a positive number");
                }
                
                // Create the new design
                ProductDesign design = new ProductDesign(name, cost);
                
                // Add input materials
                for (int i = 0; i < producerSelectors.size(); i++) {
                    Producer selectedProducer = (Producer) producerSelectors.get(i).getSelectedItem();
                    
                    if (selectedProducer == null) {
                        throw new IllegalArgumentException("Please select a producer for all inputs");
                    }
                    
                    int amount;
                    try {
                        amount = Integer.parseInt(amountFields.get(i).getText());
                        if (amount <= 0) {
                            throw new NumberFormatException();
                        }
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("Amount must be a positive integer");
                    }
                    
                    design.addInput(selectedProducer, amount);
                }
                
                // Add the design to the factory
                factory.designs.add(design);
                
                // Initialize the product in inventory with zero quantity
                factory.products.put(name, new InventoryItem(name, 0));
                
                updateCallback.run();
                dispose();
                
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, 
                    ex.getMessage(), 
                    "Invalid Input", 
                    JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error adding design: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        // Handle cancel button click
        cancelBtn.addActionListener(e -> dispose());

        // Add panels to dialog
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(topPanel, BorderLayout.NORTH);
        centerPanel.add(new JLabel("Input Materials:"), BorderLayout.CENTER);
        centerPanel.add(new JScrollPane(inputsPanel), BorderLayout.SOUTH);
        
        add(centerPanel, BorderLayout.CENTER);
        add(addInputBtn, BorderLayout.NORTH);
        add(buttonsPanel, BorderLayout.SOUTH);
        
        // Check if factory has producers
        if (factory.producers.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "This factory has no producers defined. You should add producers first.", 
                "No Producers", 
                JOptionPane.WARNING_MESSAGE);
            addInputBtn.setEnabled(false);
        }
    }
}

/**
 * Dialog for editing a factory
 */
class EditFactoryDialog extends JDialog {
    /**
     * Creates a dialog for editing a factory
     * @param parent The parent frame
     * @param factory The factory to edit
     * @param updateCallback Callback to run after editing the factory
     */
    public EditFactoryDialog(JFrame parent, FactoryExtended factory, Runnable updateCallback) {
        super(parent, "Edit Factory: " + factory.name, true);
        setSize(500, 400);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        
        tabs.addTab("Overview", createOverviewPanel(factory));
        tabs.addTab("Producers", createProducersPanel(parent, factory, updateCallback));
        tabs.addTab("Product Designs", createDesignsPanel(parent, factory, updateCallback));
        tabs.addTab("Production", createProductionPanel(parent, factory));
        tabs.addTab("Inventory", createInventoryPanel(factory));

        add(tabs, BorderLayout.CENTER);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(closeButton);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    /**
     * Creates the overview panel
     * @param factory The factory to show
     * @return The overview panel
     */
    private JPanel createOverviewPanel(FactoryExtended factory) {
        JPanel panel = new JPanel(new BorderLayout());
        
        JTextArea infoArea = new JTextArea();
        infoArea.setEditable(false);
        
        StringBuilder sb = new StringBuilder();
        sb.append("Factory: ").append(factory.name).append("\n");
        sb.append("Balance: ").append(String.format("%.2f", factory.balance)).append("\n\n");
        
        sb.append("Producers: ").append(factory.producers.size()).append("\n");
        sb.append("Product Designs: ").append(factory.designs.size()).append("\n");
        sb.append("Products in Inventory: ").append(factory.products.size()).append("\n\n");
        
        sb.append("Inventory Summary:\n");
        for (Map.Entry<String, InventoryItem> entry : factory.products.entrySet()) {
            sb.append("- ").append(entry.getKey()).append(": ").append(entry.getValue().quantity).append(" units\n");
        }
        
        infoArea.setText(sb.toString());
        panel.add(new JScrollPane(infoArea), BorderLayout.CENTER);
        
        return panel;
    }

    /**
     * Creates the producers panel
     * @param parent The parent frame
     * @param factory The factory to show producers for
     * @param updateCallback Callback to run after updates
     * @return The producers panel
     */
    private JPanel createProducersPanel(JFrame parent, Factory factory, Runnable updateCallback) {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Create list model and JList
        DefaultListModel<Producer> listModel = new DefaultListModel<>();
        for (Producer p : factory.producers) {
            listModel.addElement(p);
        }
        
        JList<Producer> producerList = new JList<>(listModel);
        JScrollPane scrollPane = new JScrollPane(producerList);
        
        // Create buttons
        JButton addBtn = new JButton("Add Producer");
        
        addBtn.addActionListener(e -> {
            new AddProducerDialog(parent, factory, () -> {
                // Update the list after adding a producer
                listModel.clear();
                for (Producer p : factory.producers) {
                    listModel.addElement(p);
                }
                updateCallback.run();
            }).setVisible(true);
        });
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addBtn);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    /**
     * Creates the product designs panel
     * @param parent The parent frame
     * @param factory The factory to show designs for
     * @param updateCallback Callback to run after updates
     * @return The designs panel
     */
    private JPanel createDesignsPanel(JFrame parent, FactoryExtended factory, Runnable updateCallback) {
        JPanel panel = new JPanel(new BorderLayout());
        
        JComboBox<ProductDesign> designSelector = new JComboBox<>();
        for (ProductDesign d : factory.designs) {
            designSelector.addItem(d);
        }
        
        JTextArea designDetailsArea = new JTextArea();
        designDetailsArea.setEditable(false);
        
        // Update details when selection changes
        designSelector.addActionListener(e -> {
            ProductDesign selected = (ProductDesign) designSelector.getSelectedItem();
            if (selected == null) {
                designDetailsArea.setText("");
                return;
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("Product: ").append(selected.name).append("\n");
            sb.append("Production Cost: ").append(String.format("%.2f", selected.cost)).append("\n\n");
            sb.append("Required Inputs:\n");
            
            for (InputMaterial input : selected.inputs) {
                sb.append("- ").append(input.producer.name)
                  .append(" x").append(input.amount)
                  .append(" (Cost: ").append(String.format("%.2f", input.producer.cost * input.amount))
                  .append(")\n");
            }
            
            // Calculate total material cost
            double materialCost = 0;
            for (InputMaterial input : selected.inputs) {
                materialCost += input.producer.cost * input.amount;
            }
            
            sb.append("\nTotal Material Cost: ").append(String.format("%.2f", materialCost));
            sb.append("\nProduction Cost: ").append(String.format("%.2f", selected.cost));
            sb.append("\nTotal Cost: ").append(String.format("%.2f", materialCost + selected.cost));
            
            designDetailsArea.setText(sb.toString());
        });
        
        // Initialize with first design if available
        if (designSelector.getItemCount() > 0) {
            designSelector.setSelectedIndex(0);
        }
        
        // Create Add Design button
        JButton addBtn = new JButton("Add Design");
        addBtn.addActionListener(e -> {
            new AddDesignDialog(parent, factory, () -> {
                // Update the selector after adding a design
                designSelector.removeAllItems();
                for (ProductDesign d : factory.designs) {
                    designSelector.addItem(d);
                }
                updateCallback.run();
            }).setVisible(true);
        });
        
        // Layout the panel
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(new JLabel("Select Product Design:"), BorderLayout.WEST);
        topPanel.add(designSelector, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addBtn);
        
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(designDetailsArea), BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    /**
     * Creates the production panel
     * @param parent The parent frame
     * @param factory The factory to show production for
     * @return The production panel
     */
    private JPanel createProductionPanel(JFrame parent, FactoryExtended factory) {
        JPanel panel = new JPanel(new BorderLayout());
        
        JLabel balanceLabel = new JLabel("Factory Balance: " + String.format("%.2f", factory.balance));
        JComboBox<ProductDesign> designSelector = new JComboBox<>();
        
        for (ProductDesign d : factory.designs) {
            designSelector.addItem(d);
        }
        
        JLabel costLabel = new JLabel("Cost per Unit: -");
        JTextField amountField = new JTextField(5);
        JLabel totalCostLabel = new JLabel("Total Cost: -");
        JButton produceBtn = new JButton("Produce");
        
        // Update cost information when selection changes
        designSelector.addActionListener(e -> {
            ProductDesign selected = (ProductDesign) designSelector.getSelectedItem();
            if (selected == null) {
                costLabel.setText("Cost per Unit: -");
                totalCostLabel.setText("Total Cost: -");
                return;
            }
            
            costLabel.setText("Cost per Unit: " + String.format("%.2f", selected.cost));
            
            // Update total cost if amount is valid
            try {
                int amount = Integer.parseInt(amountField.getText());
                if (amount > 0) {
                    double totalCost = amount * selected.cost;
                    totalCostLabel.setText("Total Cost: " + String.format("%.2f", totalCost));
                }
            } catch (NumberFormatException ex) {
                // Ignore parsing errors when changing selection
            }
        });
        
        // Update total cost when amount changes
        amountField.addActionListener(e -> {
            ProductDesign selected = (ProductDesign) designSelector.getSelectedItem();
            if (selected == null) return;
            
            try {
                int amount = Integer.parseInt(amountField.getText());
                if (amount > 0) {
                    double totalCost = amount * selected.cost;
                    totalCostLabel.setText("Total Cost: " + String.format("%.2f", totalCost));
                } else {
                    totalCostLabel.setText("Total Cost: -");
                }
            } catch (NumberFormatException ex) {
                totalCostLabel.setText("Total Cost: -");
            }
        });
        
        // Handle produce button click
        produceBtn.addActionListener(e -> {
            ProductDesign selected = (ProductDesign) designSelector.getSelectedItem();
            if (selected == null) {
                JOptionPane.showMessageDialog(parent, 
                    "Please select a product design first.", 
                    "No Design Selected", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            try {
                // Validate amount
                int amount;
                try {
                    amount = Integer.parseInt(amountField.getText());
                    if (amount <= 0) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(parent, 
                        "Please enter a valid positive number for amount.", 
                        "Invalid Amount", 
                        JOptionPane.WARNING_MESSAGE);
                    amountField.requestFocus();
                    return;
                }
                
                // Calculate total cost
                double totalCost = amount * selected.cost;
                
                // Check if factory has enough balance
                if (factory.balance < totalCost) {
                    JOptionPane.showMessageDialog(parent, 
                        "Factory does not have enough balance. Required: " + 
                        String.format("%.2f", totalCost) + ", Available: " + 
                        String.format("%.2f", factory.balance), 
                        "Insufficient Balance", 
                        JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                // Produce the product
                if (factory.produce(selected, amount)) {
                    // Update balance display
                    balanceLabel.setText("Factory Balance: " + String.format("%.2f", factory.balance));
                    
                    JOptionPane.showMessageDialog(parent, 
                        "Successfully produced " + amount + " units of " + selected.name + 
                        " for a total cost of " + String.format("%.2f", totalCost), 
                        "Production Successful", 
                        JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(parent, 
                        "Production failed.", 
                        "Production Failed", 
                        JOptionPane.ERROR_MESSAGE);
                }
                
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(parent, 
                    "Error during production: " + ex.getMessage(), 
                    "Production Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        // Layout the panel
        JPanel inputPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        inputPanel.add(new JLabel("Select Product:"));
        inputPanel.add(designSelector);
        inputPanel.add(costLabel);
        inputPanel.add(new JLabel());
        inputPanel.add(new JLabel("Amount to Produce:"));
        inputPanel.add(amountField);
        inputPanel.add(totalCostLabel);
        inputPanel.add(new JLabel());
        inputPanel.add(new JLabel());
        inputPanel.add(produceBtn);
        
        panel.add(balanceLabel, BorderLayout.NORTH);
        panel.add(inputPanel, BorderLayout.CENTER);
        
        // Initialize with first design if available
        if (designSelector.getItemCount() > 0) {
            designSelector.setSelectedIndex(0);
        }
        
        return panel;
    }

    /**
     * Creates the inventory panel
     * @param factory The factory to show inventory for
     * @return The inventory panel
     */
    private JPanel createInventoryPanel(FactoryExtended factory) {
        JPanel panel = new JPanel(new BorderLayout());
        
        JTextArea inventoryArea = new JTextArea();
        inventoryArea.setEditable(false);
        
        StringBuilder sb = new StringBuilder();
        sb.append("Factory: ").append(factory.name).append("\n");
        sb.append("Balance: ").append(String.format("%.2f", factory.balance)).append("\n\n");
        sb.append("Current Inventory:\n");
        
        if (factory.products.isEmpty()) {
            sb.append("No products in inventory.\n");
        } else {
            // Sort products alphabetically
            java.util.List<String> sortedProducts = new ArrayList<>(factory.products.keySet());
            Collections.sort(sortedProducts);
            
            for (String productName : sortedProducts) {
                InventoryItem item = factory.products.get(productName);
                sb.append(productName).append(": ").append(item.quantity).append(" units\n");
            }
        }
        
        inventoryArea.setText(sb.toString());
        panel.add(new JScrollPane(inventoryArea), BorderLayout.CENTER);
        
        return panel;
    }
}

/**
 * Dialog for adding a new factory
 */
class AddFactoryDialog extends JDialog {
    /**
     * Creates a dialog for adding a new factory
     * @param parent The parent frame
     * @param factories The list of factories to add to
     * @param updateCallback Callback to run after adding a factory
     */
    public AddFactoryDialog(JFrame parent, java.util.List<FactoryExtended> factories, Runnable updateCallback) {
        super(parent, "Add New Factory", true);
        setSize(300, 200);
        setLocationRelativeTo(parent);
        setLayout(new GridLayout(3, 2, 5, 5));

        JTextField nameField = new JTextField();
        JTextField balanceField = new JTextField();

        add(new JLabel("Factory Name:"));
        add(nameField);
        add(new JLabel("Initial Balance:"));
        add(balanceField);

        JButton addBtn = new JButton("Add");
        JButton cancelBtn = new JButton("Cancel");
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addBtn);
        buttonPanel.add(cancelBtn);
        
        add(new JLabel()); // spacer
        add(buttonPanel);

        // Handle add button click
        addBtn.addActionListener(e -> {
            try {
                // Validate inputs
                String name = nameField.getText().trim();
                if (name.isEmpty()) {
                    throw new IllegalArgumentException("Factory name cannot be empty");
                }
                
                // Check for duplicate factory names
                for (Factory existingFactory : factories) {
                    if (existingFactory.name.equals(name)) {
                        throw new IllegalArgumentException("A factory with this name already exists");
                    }
                }
                
                double balance;
                try {
                    balance = Double.parseDouble(balanceField.getText());
                    if (balance < 0) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Balance must be a positive number");
                }
                
                // Create and add the new factory
                factories.add(new FactoryExtended(name, balance));
                updateCallback.run();
                dispose();
                
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(this, 
                    ex.getMessage(), 
                    "Invalid Input", 
                    JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error adding factory: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        // Handle cancel button click
        cancelBtn.addActionListener(e -> dispose());
    }
}

/**
 * Main application frame
 */
class MainFrame extends JFrame {
    private java.util.List<FactoryExtended> factories = new ArrayList<>();
    private java.util.List<Market> markets = new ArrayList<>();
    private java.util.List<Customer> customers = new ArrayList<>();

    private DefaultListModel<FactoryExtended> factoryListModel;
    private DefaultListModel<Market> marketListModel;
    private DefaultListModel<Customer> customerListModel;

    /**
     * Creates the main application frame
     */
    public MainFrame() {
        setTitle("Supply Chain Management System");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Create sample data
        initializeSampleData();

        // Create the main UI
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Factories", createFactoryPanel());
        tabs.addTab("Markets", createMarketPanel());
        tabs.addTab("Customers", createCustomerPanel());

        add(tabs, BorderLayout.CENTER);
    }

    /**
     * Initializes sample data for the application
     */
    private void initializeSampleData() {
        try {
            // Create sample factories
            FactoryExtended factory1 = new FactoryExtended("Furniture Factory", 10000);
            FactoryExtended factory2 = new FactoryExtended("Electronics Factory", 15000);
            
            // Add producers
            factory1.producers.add(new Producer("Wood", 5.0));
            factory1.producers.add(new Producer("Metal", 8.0));
            factory1.producers.add(new Producer("Fabric", 3.0));
            
            factory2.producers.add(new Producer("Plastic", 4.0));
            factory2.producers.add(new Producer("Circuits", 12.0));
            factory2.producers.add(new Producer("Glass", 6.0));
            
            // Create product designs
            ProductDesign chair = new ProductDesign("Wooden Chair", 10.0);
            chair.addInput(factory1.producers.get(0), 4); // 4 Wood
            chair.addInput(factory1.producers.get(1), 1); // 1 Metal
            
            ProductDesign table = new ProductDesign("Dining Table", 15.0);
            table.addInput(factory1.producers.get(0), 8); // 8 Wood
            table.addInput(factory1.producers.get(1), 2); // 2 Metal
            
            ProductDesign smartphone = new ProductDesign("Smartphone", 50.0);
            smartphone.addInput(factory2.producers.get(0), 2); // 2 Plastic
            smartphone.addInput(factory2.producers.get(1), 5); // 5 Circuits
            smartphone.addInput(factory2.producers.get(2), 1); // 1 Glass
            
            // Add designs to factories
            factory1.designs.add(chair);
            factory1.designs.add(table);
            factory2.designs.add(smartphone);
            
            // Initialize product inventory
            factory1.products.put("Wooden Chair", new InventoryItem("Wooden Chair", 0));
            factory1.products.put("Dining Table", new InventoryItem("Dining Table", 0));
            factory2.products.put("Smartphone", new InventoryItem("Smartphone", 0));
            
            // Produce some initial inventory
            factory1.produce(chair, 10);
            factory1.produce(table, 5);
            factory2.produce(smartphone, 20);
            
            // Add factories to list
            factories.add(factory1);
            factories.add(factory2);
            
            // Create sample markets
            Market market1 = new Market("Downtown Market", 5000);
            Market market2 = new Market("Online Store", 8000);
            
            // Set prices
            market1.setPrice("Wooden Chair", 75.0);
            market1.setPrice("Dining Table", 200.0);
            market1.setPrice("Smartphone", 500.0);
            
            market2.setPrice("Wooden Chair", 80.0);
            market2.setPrice("Dining Table", 220.0);
            market2.setPrice("Smartphone", 480.0);
            
            // Add some inventory to markets
            market1.buyProduct("Wooden Chair", 5, 40.0, factory1);
            market1.buyProduct("Dining Table", 2, 80.0, factory1);
            
            market2.buyProduct("Smartphone", 10, 200.0, factory2);
            market2.buyProduct("Wooden Chair", 3, 40.0, factory1);
            
            // Add markets to list
            markets.add(market1);
            markets.add(market2);
            
            // Create sample customers
            Customer customer1 = new Customer("John Smith", 2000);
            Customer customer2 = new Customer("Jane Doe", 1500);
            
            // Add some products to customers
            customer1.addProduct("Wooden Chair", 1);
            customer2.addProduct("Smartphone", 1);
            
            // Add customers to list
            customers.add(customer1);
            customers.add(customer2);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error initializing sample data: " + e.getMessage(), 
                "Initialization Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Creates the factory panel
     * @return The factory panel
     */
    private JPanel createFactoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        factoryListModel = new DefaultListModel<>();
        for (FactoryExtended f : factories) {
            factoryListModel.addElement(f);
        }

        JList<FactoryExtended> factoryList = new JList<>(factoryListModel);
        JScrollPane scrollPane = new JScrollPane(factoryList);

        JButton addBtn = new JButton("Add Factory");
        JButton editBtn = new JButton("Edit Factory");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addBtn);
        buttonPanel.add(editBtn);

        addBtn.addActionListener(e -> {
            try {
                new AddFactoryDialog(this, factories, this::updateFactoryList).setVisible(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error opening dialog: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        editBtn.addActionListener(e -> {
            FactoryExtended selected = factoryList.getSelectedValue();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, 
                    "Please select a factory first.", 
                    "No Factory Selected", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            try {
                new EditFactoryDialog(this, selected, this::updateFactoryList).setVisible(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error opening dialog: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Creates the market panel
     * @return The market panel
     */
    private JPanel createMarketPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        marketListModel = new DefaultListModel<>();
        for (Market m : markets) {
            marketListModel.addElement(m);
        }

        JList<Market> marketList = new JList<>(marketListModel);
        JScrollPane scrollPane = new JScrollPane(marketList);

        JButton addBtn = new JButton("Add Market");
        JButton editBtn = new JButton("Edit Market");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addBtn);
        buttonPanel.add(editBtn);

        addBtn.addActionListener(e -> {
            try {
                new AddMarketDialog(this, markets, this::updateMarketList).setVisible(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error opening dialog: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        editBtn.addActionListener(e -> {
            Market selected = marketList.getSelectedValue();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, 
                    "Please select a market first.", 
                    "No Market Selected", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            try {
                new EditMarketDialog(this, selected, factories).setVisible(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error opening dialog: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Creates the customer panel
     * @return The customer panel
     */
    private JPanel createCustomerPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        customerListModel = new DefaultListModel<>();
        for (Customer c : customers) {
            customerListModel.addElement(c);
        }

        JList<Customer> customerList = new JList<>(customerListModel);
        JScrollPane scrollPane = new JScrollPane(customerList);

        JButton addBtn = new JButton("Add Customer");
        JButton editBtn = new JButton("Edit Customer");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addBtn);
        buttonPanel.add(editBtn);

        addBtn.addActionListener(e -> {
            try {
                new AddCustomerDialog(this, customers, this::updateCustomerList).setVisible(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error opening dialog: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        editBtn.addActionListener(e -> {
            Customer selected = customerList.getSelectedValue();
            if (selected == null) {
                JOptionPane.showMessageDialog(this, 
                    "Please select a customer first.", 
                    "No Customer Selected", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            try {
                new EditCustomerDialog(this, selected, markets).setVisible(true);
                updateCustomerList(); // Update to show new balance
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error opening dialog: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Updates the factory list display
     */
    private void updateFactoryList() {
        factoryListModel.clear();
        for (FactoryExtended f : factories) {
            factoryListModel.addElement(f);
        }
    }

    /**
     * Updates the market list display
     */
    private void updateMarketList() {
        marketListModel.clear();
        for (Market m : markets) {
            marketListModel.addElement(m);
        }
    }

    /**
     * Updates the customer list display
     */
    private void updateCustomerList() {
        customerListModel.clear();
        for (Customer c : customers) {
            customerListModel.addElement(c);
        }
    }
}
