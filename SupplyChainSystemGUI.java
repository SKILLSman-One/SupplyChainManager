import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.awt.geom.RoundRectangle2D;

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
        setTitle("Supply Chain System");
        setSize(700, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Create sample data
        initializeSampleData();

        // Create the main UI with a vertical button layout as shown in screenshots
        setLayout(new BorderLayout());
        
        // Main title at the top
        JLabel titleLabel = new JLabel("Supply Chain System", JLabel.CENTER);
        titleLabel.setFont(new Font("Sans-Serif", Font.BOLD, 24));
        add(titleLabel, BorderLayout.NORTH);
        
        // Create a panel for the main menu buttons with spacing between them
        JPanel mainPanel = new JPanel(new GridLayout(4, 1, 10, 30));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 100, 30, 100));
        
        // Create gradient panels with buttons for each section
        JPanel producersPanel = createGradientPanel("Raw Material Producers");
        producersPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showProducersPanel();
            }
        });
        
        JPanel factoriesPanel = createGradientPanel("Factories");
        factoriesPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showFactoriesPanel();
            }
        });
        
        JPanel marketsPanel = createGradientPanel("Markets");
        marketsPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showMarketsPanel();
            }
        });
        
        JPanel customersPanel = createGradientPanel("Customers");
        customersPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showCustomersPanel();
            }
        });
        
        // Add the panels to the main panel
        mainPanel.add(producersPanel);
        mainPanel.add(factoriesPanel);
        mainPanel.add(marketsPanel);
        mainPanel.add(customersPanel);
        
        // Add the main panel to the center of the frame
        add(mainPanel, BorderLayout.CENTER);
    }
    
    /**
     * Creates a gradient panel with the given title
     * @param title The title to display on the panel
     * @return A JPanel with gradient background and centered title
     */
    private JPanel createGradientPanel(String title) {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(220, 230, 255);
                Color color2 = new Color(200, 215, 240);
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        panel.setLayout(new BorderLayout());
        JLabel label = new JLabel(title, JLabel.CENTER);
        label.setFont(new Font("Sans-Serif", Font.BOLD, 16));
        panel.add(label, BorderLayout.CENTER);
        panel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return panel;
    }
    
    /**
     * Shows the producers panel
     */
    private void showProducersPanel() {
        getContentPane().removeAll();
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Create title panel with back button
        JPanel titlePanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Raw Material Producers", JLabel.CENTER);
        titleLabel.setFont(new Font("Sans-Serif", Font.BOLD, 18));
        JButton backButton = new JButton("Back");
        backButton.addActionListener(e -> {
            // Return to main menu
            getContentPane().removeAll();
            setTitle("Supply Chain System");
            // Create the main UI with a vertical button layout
            setLayout(new BorderLayout());
            
            // Main title at the top
            JLabel mainTitleLabel = new JLabel("Supply Chain System", JLabel.CENTER);
            mainTitleLabel.setFont(new Font("Sans-Serif", Font.BOLD, 24));
            add(mainTitleLabel, BorderLayout.NORTH);
            
            // Create a panel for the main menu buttons with spacing between them
            JPanel menuPanel = new JPanel(new GridLayout(4, 1, 10, 30));
            menuPanel.setBorder(BorderFactory.createEmptyBorder(30, 100, 30, 100));
            
            // Re-create all four panels
            JPanel producersPanel = createGradientPanel("Raw Material Producers");
            producersPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showProducersPanel();
                }
            });
            
            JPanel factoriesPanel = createGradientPanel("Factories");
            factoriesPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showFactoriesPanel();
                }
            });
            
            JPanel marketsPanel = createGradientPanel("Markets");
            marketsPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showMarketsPanel();
                }
            });
            
            JPanel customersPanel = createGradientPanel("Customers");
            customersPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showCustomersPanel();
                }
            });
            
            // Add the panels to the main panel
            menuPanel.add(producersPanel);
            menuPanel.add(factoriesPanel);
            menuPanel.add(marketsPanel);
            menuPanel.add(customersPanel);
            
            // Add the main panel to the center of the frame
            add(menuPanel, BorderLayout.CENTER);
            revalidate();
            repaint();
        });
        
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        titlePanel.add(backButton, BorderLayout.EAST);
        
        // Add the title panel to the main panel
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        
        // Create and add the producers panel content
        mainPanel.add(createProducersPanel(), BorderLayout.CENTER);
        
        // Add the main panel to the content pane
        add(mainPanel);
        revalidate();
        repaint();
    }
    
    /**
     * Creates the producers panel content
     * @return The producers panel
     */
    private JPanel createProducersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Create a panel for the list of producers
        JPanel listPanel = new JPanel(new BorderLayout());
        
        // Create a model for the list of producers
        DefaultListModel<Producer> producerListModel = new DefaultListModel<>();
        
        // Collect all producers from all factories
        Set<Producer> allProducers = new HashSet<>();
        for (FactoryExtended factory : factories) {
            allProducers.addAll(factory.producers);
        }
        
        // Add all producers to the list model
        for (Producer producer : allProducers) {
            producerListModel.addElement(producer);
        }
        
        // Create a list to display the producers
        JList<Producer> producerList = new JList<>(producerListModel);
        
        // If the list is empty, show a message
        if (producerListModel.isEmpty()) {
            JLabel emptyLabel = new JLabel("No producers. Add one!");
            emptyLabel.setFont(new Font("Sans-Serif", Font.ITALIC, 14));
            listPanel.add(emptyLabel, BorderLayout.CENTER);
        } else {
            JScrollPane scrollPane = new JScrollPane(producerList);
            listPanel.add(scrollPane, BorderLayout.CENTER);
        }
        
        // Add the list panel to the main panel
        panel.add(listPanel, BorderLayout.CENTER);
        
        // Create buttons for adding and editing producers
        JButton addBtn = new JButton("Add Producer");
        JButton editBtn = new JButton("Edit Producer");
        
        // Create a panel for the buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addBtn);
        buttonPanel.add(editBtn);
        
        // Add action listeners for the buttons
        addBtn.addActionListener(e -> {
            try {
                // Show dialog to add new producer
                JDialog addProducerDialog = new JDialog(this, "Add Producer", true);
                addProducerDialog.setSize(300, 250);
                addProducerDialog.setLocationRelativeTo(this);
                addProducerDialog.setLayout(new GridLayout(6, 2, 5, 5));
                
                // Create icon for the dialog
                JLabel iconLabel = new JLabel();
                iconLabel.setIcon(new ImageIcon(createQuestionMarkIcon()));
                
                // Create form fields
                JTextField nameField = new JTextField();
                JTextField costField = new JTextField();
                JTextField sellingPriceField = new JTextField();
                JTextField capacityField = new JTextField();
                JTextField fundField = new JTextField();
                
                // Add components to the dialog
                addProducerDialog.add(iconLabel);
                addProducerDialog.add(new JLabel()); // empty cell
                addProducerDialog.add(new JLabel("Name:"));
                addProducerDialog.add(nameField);
                addProducerDialog.add(new JLabel("Cost:"));
                addProducerDialog.add(costField);
                addProducerDialog.add(new JLabel("Selling Price:"));
                addProducerDialog.add(sellingPriceField);
                addProducerDialog.add(new JLabel("Capacity:"));
                addProducerDialog.add(capacityField);
                addProducerDialog.add(new JLabel("Fund:"));
                addProducerDialog.add(fundField);
                
                // Create buttons for the dialog
                JButton okBtn = new JButton("OK");
                JButton cancelBtn = new JButton("Cancel");
                
                // Create a panel for the buttons
                JPanel dialogButtonPanel = new JPanel();
                dialogButtonPanel.add(okBtn);
                dialogButtonPanel.add(cancelBtn);
                
                // Add the button panel to the dialog
                addProducerDialog.add(dialogButtonPanel);
                
                // Add action listeners for the buttons
                okBtn.addActionListener(okEvent -> {
                    try {
                        // Validate and create new producer
                        String name = nameField.getText().trim();
                        if (name.isEmpty()) {
                            throw new IllegalArgumentException("Name cannot be empty");
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
                        
                        // Create and add the new producer to all factories
                        Producer newProducer = new Producer(name, cost);
                        for (FactoryExtended factory : factories) {
                            if (!factory.producers.contains(newProducer)) {
                                factory.producers.add(newProducer);
                            }
                        }
                        
                        // Update the list and close the dialog
                        producerListModel.addElement(newProducer);
                        addProducerDialog.dispose();
                        
                    } catch (IllegalArgumentException ex) {
                        JOptionPane.showMessageDialog(addProducerDialog,
                            ex.getMessage(),
                            "Invalid Input",
                            JOptionPane.WARNING_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(addProducerDialog,
                            "Error adding producer: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                });
                
                // Add action listener for cancel button
                cancelBtn.addActionListener(cancelEvent -> addProducerDialog.dispose());
                
                // Show the dialog
                addProducerDialog.setVisible(true);
                
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Error creating dialog: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        editBtn.addActionListener(e -> {
            // Get the selected producer
            Producer selectedProducer = producerList.getSelectedValue();
            if (selectedProducer == null) {
                JOptionPane.showMessageDialog(this,
                    "Please select a producer to edit",
                    "No Selection",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            try {
                // Show dialog to edit producer
                JDialog editProducerDialog = new JDialog(this, "Edit Producer: " + selectedProducer.name, true);
                editProducerDialog.setSize(300, 200);
                editProducerDialog.setLocationRelativeTo(this);
                editProducerDialog.setLayout(new GridLayout(4, 2, 5, 5));
                
                // Create form fields with current values
                JTextField costField = new JTextField(String.valueOf(selectedProducer.cost));
                
                // Add components to the dialog
                editProducerDialog.add(new JLabel("Name:"));
                editProducerDialog.add(new JLabel(selectedProducer.name)); // Name is not editable
                editProducerDialog.add(new JLabel("Cost:"));
                editProducerDialog.add(costField);
                
                // Create buttons for the dialog
                JButton saveBtn = new JButton("Save");
                JButton cancelBtn = new JButton("Cancel");
                
                // Create a panel for the buttons
                JPanel dialogButtonPanel = new JPanel();
                dialogButtonPanel.add(saveBtn);
                dialogButtonPanel.add(cancelBtn);
                
                // Add the button panel to the dialog
                editProducerDialog.add(new JLabel()); // Empty cell
                editProducerDialog.add(dialogButtonPanel);
                
                // Add action listeners for the buttons
                saveBtn.addActionListener(saveEvent -> {
                    try {
                        // Validate and update producer
                        double cost;
                        try {
                            cost = Double.parseDouble(costField.getText());
                            if (cost <= 0) {
                                throw new NumberFormatException();
                            }
                        } catch (NumberFormatException ex) {
                            throw new IllegalArgumentException("Cost must be a positive number");
                        }
                        
                        // Update the producer
                        selectedProducer.cost = cost;
                        
                        // Refresh the list
                        producerList.repaint();
                        
                        // Close the dialog
                        editProducerDialog.dispose();
                        
                    } catch (IllegalArgumentException ex) {
                        JOptionPane.showMessageDialog(editProducerDialog,
                            ex.getMessage(),
                            "Invalid Input",
                            JOptionPane.WARNING_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(editProducerDialog,
                            "Error updating producer: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                });
                
                // Add action listener for cancel button
                cancelBtn.addActionListener(cancelEvent -> editProducerDialog.dispose());
                
                // Show the dialog
                editProducerDialog.setVisible(true);
                
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Error creating dialog: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        // Add the button panel to the main panel
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Creates a question mark icon for the Add Producer dialog
     * @return The icon as a BufferedImage
     */
    private BufferedImage createQuestionMarkIcon() {
        // Create a 32x32 icon with a green background and question mark
        BufferedImage icon = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = icon.createGraphics();
        
        // Set rendering hints for better quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw green background
        g2d.setColor(new Color(144, 238, 144)); // Light green
        g2d.fillRect(0, 0, 32, 32);
        
        // Draw question mark
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Sans-Serif", Font.BOLD, 24));
        g2d.drawString("?", 12, 24);
        
        g2d.dispose();
        return icon;
    }
    
    /**
     * Shows the factories panel
     */
    private void showFactoriesPanel() {
        getContentPane().removeAll();
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Create title panel with back button
        JPanel titlePanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Factories", JLabel.CENTER);
        titleLabel.setFont(new Font("Sans-Serif", Font.BOLD, 18));
        JButton backButton = new JButton("Back");
        backButton.addActionListener(e -> {
            // Return to main menu
            getContentPane().removeAll();
            setTitle("Supply Chain System");
            // Create the main UI with a vertical button layout
            setLayout(new BorderLayout());
            
            // Main title at the top
            JLabel mainTitleLabel = new JLabel("Supply Chain System", JLabel.CENTER);
            mainTitleLabel.setFont(new Font("Sans-Serif", Font.BOLD, 24));
            add(mainTitleLabel, BorderLayout.NORTH);
            
            // Create a panel for the main menu buttons with spacing between them
            JPanel menuPanel = new JPanel(new GridLayout(4, 1, 10, 30));
            menuPanel.setBorder(BorderFactory.createEmptyBorder(30, 100, 30, 100));
            
            // Re-create all four panels
            JPanel producersPanel = createGradientPanel("Raw Material Producers");
            producersPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showProducersPanel();
                }
            });
            
            JPanel factoriesPanel = createGradientPanel("Factories");
            factoriesPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showFactoriesPanel();
                }
            });
            
            JPanel marketsPanel = createGradientPanel("Markets");
            marketsPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showMarketsPanel();
                }
            });
            
            JPanel customersPanel = createGradientPanel("Customers");
            customersPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showCustomersPanel();
                }
            });
            
            // Add the panels to the main panel
            menuPanel.add(producersPanel);
            menuPanel.add(factoriesPanel);
            menuPanel.add(marketsPanel);
            menuPanel.add(customersPanel);
            
            // Add the main panel to the center of the frame
            add(menuPanel, BorderLayout.CENTER);
            revalidate();
            repaint();
        });
        
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        titlePanel.add(backButton, BorderLayout.EAST);
        
        // Add the title panel to the main panel
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        
        // Create the factory list panel with radio buttons
        JPanel listPanel = new JPanel(new BorderLayout());
        
        // If factories list is empty, show a message
        if (factories.isEmpty()) {
            JLabel emptyLabel = new JLabel("No factories. Add one!");
            emptyLabel.setFont(new Font("Sans-Serif", Font.ITALIC, 14));
            listPanel.add(emptyLabel, BorderLayout.CENTER);
        } else {
            // Create a panel for the factory list with radio buttons
            JPanel factoryRadioPanel = new JPanel(new GridLayout(factories.size(), 1));
            ButtonGroup factoryGroup = new ButtonGroup();
            
            // Create a radio button for each factory
            for (FactoryExtended factory : factories) {
                JRadioButton factoryRadio = new JRadioButton(factory.name);
                factoryRadio.setActionCommand(factory.name);
                factoryGroup.add(factoryRadio);
                factoryRadioPanel.add(factoryRadio);
                
                // Select the first factory by default
                if (factoryGroup.getSelection() == null) {
                    factoryRadio.setSelected(true);
                }
            }
            
            listPanel.add(factoryRadioPanel, BorderLayout.CENTER);
        }
        
        // Add the list panel to the main panel
        mainPanel.add(listPanel, BorderLayout.CENTER);
        
        // Create buttons for adding and editing factories
        JButton addBtn = new JButton("Add Factory");
        JButton editBtn = new JButton("Edit Factory");
        
        // Create a panel for the buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addBtn);
        buttonPanel.add(editBtn);
        
        // Add action listeners for the buttons
        addBtn.addActionListener(e -> {
            try {
                // Show dialog to add new factory
                JDialog addFactoryDialog = new JDialog(this, "Add Factory", true);
                addFactoryDialog.setSize(300, 200);
                addFactoryDialog.setLocationRelativeTo(this);
                addFactoryDialog.setLayout(new GridLayout(3, 2, 5, 5));
                
                // Create form fields
                JTextField nameField = new JTextField();
                JTextField balanceField = new JTextField();
                
                // Add components to the dialog
                addFactoryDialog.add(new JLabel("Factory Name:"));
                addFactoryDialog.add(nameField);
                addFactoryDialog.add(new JLabel("Initial Balance:"));
                addFactoryDialog.add(balanceField);
                
                // Create buttons for the dialog
                JButton okBtn = new JButton("Add");
                JButton cancelBtn = new JButton("Cancel");
                
                // Create a panel for the buttons
                JPanel dialogButtonPanel = new JPanel();
                dialogButtonPanel.add(okBtn);
                dialogButtonPanel.add(cancelBtn);
                
                // Add the button panel to the dialog
                addFactoryDialog.add(new JLabel()); // Empty cell
                addFactoryDialog.add(dialogButtonPanel);
                
                // Add action listeners for the buttons
                okBtn.addActionListener(okEvent -> {
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
                        FactoryExtended newFactory = new FactoryExtended(name, balance);
                        factories.add(newFactory);
                        
                        // Close the dialog and refresh the factories panel
                        addFactoryDialog.dispose();
                        showFactoriesPanel(); // Refresh the panel
                        
                    } catch (IllegalArgumentException ex) {
                        JOptionPane.showMessageDialog(addFactoryDialog, 
                            ex.getMessage(), 
                            "Invalid Input", 
                            JOptionPane.WARNING_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(addFactoryDialog, 
                            "Error adding factory: " + ex.getMessage(), 
                            "Error", 
                            JOptionPane.ERROR_MESSAGE);
                    }
                });
                
                // Add action listener for cancel button
                cancelBtn.addActionListener(cancelEvent -> addFactoryDialog.dispose());
                
                // Show the dialog
                addFactoryDialog.setVisible(true);
                
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error creating dialog: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        editBtn.addActionListener(e -> {
            // Get the selected factory
            ButtonGroup factoryGroup = new ButtonGroup();
            Enumeration<AbstractButton> buttons = factoryGroup.getElements();
            FactoryExtended selectedFactory = null;
            
            while (buttons.hasMoreElements()) {
                AbstractButton button = buttons.nextElement();
                if (button.isSelected()) {
                    String factoryName = button.getActionCommand();
                    for (FactoryExtended factory : factories) {
                        if (factory.name.equals(factoryName)) {
                            selectedFactory = factory;
                            break;
                        }
                    }
                    break;
                }
            }
            
            if (selectedFactory == null && !factories.isEmpty()) {
                // If no factory is selected but factories exist, select the first one
                selectedFactory = factories.get(0);
            }
            
            if (selectedFactory == null) {
                JOptionPane.showMessageDialog(this, 
                    "No factory available to edit. Please add a factory first.", 
                    "No Factory", 
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Now we have a selected factory, show the edit dialog
            FactoryExtended factory = selectedFactory; // Final reference for lambda
            
            try {
                // Show the edit factory dialog similar to the original EditFactoryDialog
                JDialog editFactoryDialog = new JDialog(this, "Edit Factory: " + factory.name, true);
                editFactoryDialog.setSize(400, 300);
                editFactoryDialog.setLocationRelativeTo(this);
                editFactoryDialog.setLayout(new BorderLayout());
                
                // Create a panel with factory information
                JPanel infoPanel = new JPanel(new GridLayout(4, 2, 5, 5));
                infoPanel.add(new JLabel("Name:"));
                infoPanel.add(new JLabel(factory.name));
                infoPanel.add(new JLabel("Balance:"));
                infoPanel.add(new JLabel(String.format("%.2f", factory.balance)));
                infoPanel.add(new JLabel("Capacity:"));
                infoPanel.add(new JLabel("1")); // Placeholder as in screenshots
                infoPanel.add(new JLabel("Stock:"));
                infoPanel.add(new JLabel("0")); // Placeholder as in screenshots
                
                // Create a panel for material selection and buying
                JPanel materialPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                materialPanel.add(new JLabel("Select Material:"));
                
                // Create a combo box with available producers/materials
                JComboBox<Producer> materialCombo = new JComboBox<>();
                for (Producer producer : factory.producers) {
                    materialCombo.addItem(producer);
                }
                
                materialPanel.add(materialCombo);
                materialPanel.add(new JLabel("Amount:"));
                JTextField amountField = new JTextField(5);
                materialPanel.add(amountField);
                JButton buyBtn = new JButton("Buy");
                materialPanel.add(buyBtn);
                
                // Add a label to display the current stock
                JLabel stockLabel = new JLabel("Stock: -");
                materialPanel.add(stockLabel);
                
                // Create buttons for other factory operations
                JButton createDesignBtn = new JButton("Create Design");
                JButton manufacturingBtn = new JButton("Manufacturing");
                JButton viewInventoryBtn = new JButton("View Inventory");
                JButton destroyByproductBtn = new JButton("Destroy Byproduct");
                
                // Create a panel for the operation buttons
                JPanel operationPanel = new JPanel(new GridLayout(2, 2, 5, 5));
                operationPanel.add(createDesignBtn);
                operationPanel.add(manufacturingBtn);
                operationPanel.add(viewInventoryBtn);
                operationPanel.add(destroyByproductBtn);
                
                // Add action listeners for the buttons
                // (For simplicity, we're not implementing all functionality in this update)
                
                // Add all panels to the dialog
                JPanel mainDialogPanel = new JPanel(new BorderLayout());
                mainDialogPanel.add(infoPanel, BorderLayout.NORTH);
                mainDialogPanel.add(materialPanel, BorderLayout.CENTER);
                mainDialogPanel.add(operationPanel, BorderLayout.SOUTH);
                
                editFactoryDialog.add(mainDialogPanel, BorderLayout.CENTER);
                
                // Add a close button at the bottom
                JButton closeBtn = new JButton("Back");
                closeBtn.addActionListener(closeEvent -> editFactoryDialog.dispose());
                
                JPanel closePanel = new JPanel();
                closePanel.add(closeBtn);
                editFactoryDialog.add(closePanel, BorderLayout.SOUTH);
                
                // Show the dialog
                editFactoryDialog.setVisible(true);
                
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Error editing factory: " + ex.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        // Add the button panel to the main panel
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Add the main panel to the content pane
        add(mainPanel);
        revalidate();
        repaint();
    }
    
    /**
     * Shows the markets panel
     */
    private void showMarketsPanel() {
        getContentPane().removeAll();
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        // Create title panel with back button
        JPanel titlePanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Markets", JLabel.CENTER);
        titleLabel.setFont(new Font("Sans-Serif", Font.BOLD, 18));
        JButton backButton = new JButton("Back");
        backButton.addActionListener(e -> {
            // Return to main menu
            getContentPane().removeAll();
            setTitle("Supply Chain System");
            // Create the main UI with a vertical button layout
            setLayout(new BorderLayout());
            
            // Main title at the top
            JLabel mainTitleLabel = new JLabel("Supply Chain System", JLabel.CENTER);
            mainTitleLabel.setFont(new Font("Sans-Serif", Font.BOLD, 24));
            add(mainTitleLabel, BorderLayout.NORTH);
            
            // Create a panel for the main menu buttons with spacing between them
            JPanel menuPanel = new JPanel(new GridLayout(4, 1, 10, 30));
            menuPanel.setBorder(BorderFactory.createEmptyBorder(30, 100, 30, 100));
            
            // Re-create all four panels
            JPanel producersPanel = createGradientPanel("Raw Material Producers");
            producersPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showProducersPanel();
                }
            });
            
            JPanel factoriesPanel = createGradientPanel("Factories");
            factoriesPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showFactoriesPanel();
                }
            });
            
            JPanel marketsPanel = createGradientPanel("Markets");
            marketsPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showMarketsPanel();
                }
            });
            
            JPanel customersPanel = createGradientPanel("Customers");
            customersPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showCustomersPanel();
                }
            });
            
            // Add the panels to the main panel
            menuPanel.add(producersPanel);
            menuPanel.add(factoriesPanel);
            menuPanel.add(marketsPanel);
            menuPanel.add(customersPanel);
            
            // Add the main panel to the center of the frame
            add(menuPanel, BorderLayout.CENTER);
            revalidate();
            repaint();
        });
        
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        titlePanel.add(backButton, BorderLayout.EAST);
        
        // Add the title panel to the main panel
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        
        // Create and add the markets panel content
        JPanel marketsListPanel = new JPanel(new BorderLayout());
        
        // If markets list is empty, show a message
        if (markets.isEmpty()) {
            JLabel emptyLabel = new JLabel("No markets. Add one!");
            emptyLabel.setFont(new Font("Sans-Serif", Font.ITALIC, 14));
            marketsListPanel.add(emptyLabel, BorderLayout.CENTER);
        } else {
            // Create a panel for the market list
            JPanel marketButtonsPanel = new JPanel(new GridLayout(markets.size(), 1));
            
            // Create a button for each market
            for (Market market : markets) {
                JButton marketButton = new JButton(market.name);
                marketButton.addActionListener(actionEvent -> {
                    try {
                        // Show dialog to edit market
                        JDialog editMarketDialog = new JDialog(this, "Edit Market: " + market.name, true);
                        editMarketDialog.setSize(400, 300);
                        editMarketDialog.setLocationRelativeTo(this);
                        editMarketDialog.setLayout(new BorderLayout());
                        
                        // Create a panel with market information
                        JPanel infoPanel = new JPanel(new GridLayout(1, 2, 5, 5));
                        infoPanel.add(new JLabel("Balance:"));
                        infoPanel.add(new JLabel(String.format("%.1f", market.balance)));
                        
                        // Create a panel for product selection and buying
                        JPanel buyPanel = new JPanel(new GridLayout(4, 2, 5, 5));
                        buyPanel.add(new JLabel("Select Product:"));
                        
                        // Create a combo box with available products
                        JComboBox<String> productCombo = new JComboBox<>();
                        Set<String> allProducts = new HashSet<>();
                        
                        // Get all products from all factories
                        for (FactoryExtended factory : factories) {
                            allProducts.addAll(factory.products.keySet());
                        }
                        
                        // Add products to the combo box
                        for (String product : allProducts) {
                            productCombo.addItem(product);
                        }
                        
                        buyPanel.add(productCombo);
                        
                        // Add stock quantity label
                        buyPanel.add(new JLabel("Stock Quantity:"));
                        JLabel stockLabel = new JLabel("-");
                        buyPanel.add(stockLabel);
                        
                        // Update stock display when product selection changes
                        productCombo.addActionListener(comboEvent -> {
                            String selectedProduct = (String) productCombo.getSelectedItem();
                            if (selectedProduct != null) {
                                int quantity = market.stock.getOrDefault(selectedProduct, 0);
                                stockLabel.setText(String.valueOf(quantity));
                            }
                        });
                        
                        // Add amount field and buy button
                        buyPanel.add(new JLabel("Amount:"));
                        JTextField amountField = new JTextField();
                        buyPanel.add(amountField);
                        buyPanel.add(new JLabel(""));
                        JButton buyBtn = new JButton("Buy");
                        buyPanel.add(buyBtn);
                        
                        // Add price setting section
                        JPanel pricePanel = new JPanel(new GridLayout(1, 3, 5, 5));
                        pricePanel.add(new JLabel("Price (for stock):"));
                        JTextField priceField = new JTextField();
                        pricePanel.add(priceField);
                        JButton updatePriceBtn = new JButton("Update Price");
                        pricePanel.add(updatePriceBtn);
                        
                        // Add action listener for buy button
                        buyBtn.addActionListener(buyEvent -> {
                            try {
                                String selectedProduct = (String) productCombo.getSelectedItem();
                                if (selectedProduct == null) {
                                    JOptionPane.showMessageDialog(editMarketDialog,
                                        "Please select a product first.",
                                        "No Product Selected",
                                        JOptionPane.WARNING_MESSAGE);
                                    return;
                                }
                                
                                // Validate amount
                                int amount;
                                try {
                                    amount = Integer.parseInt(amountField.getText());
                                    if (amount <= 0) {
                                        throw new NumberFormatException();
                                    }
                                } catch (NumberFormatException ex) {
                                    JOptionPane.showMessageDialog(editMarketDialog,
                                        "Please enter a valid positive number for amount.",
                                        "Invalid Amount",
                                        JOptionPane.WARNING_MESSAGE);
                                    amountField.requestFocus();
                                    return;
                                }
                                
                                // Find factory with enough stock
                                boolean purchaseSuccessful = false;
                                for (FactoryExtended factory : factories) {
                                    InventoryItem item = factory.products.get(selectedProduct);
                                    if (item != null && item.quantity >= amount) {
                                        double costPerUnit = 0;
                                        // Find the product design to get the cost
                                        for (ProductDesign design : factory.designs) {
                                            if (design.name.equals(selectedProduct)) {
                                                costPerUnit = design.cost;
                                                break;
                                            }
                                        }
                                        
                                        // Check if market has enough money
                                        double totalCost = costPerUnit * amount;
                                        if (market.balance < totalCost) {
                                            JOptionPane.showMessageDialog(editMarketDialog,
                                                "Market does not have enough balance. Required: " +
                                                String.format("%.2f", totalCost) + ", Available: " +
                                                String.format("%.2f", market.balance),
                                                "Insufficient Balance",
                                                JOptionPane.WARNING_MESSAGE);
                                            return;
                                        }
                                        
                                        // Execute purchase
                                        if (market.buyProduct(selectedProduct, amount, costPerUnit, factory)) {
                                            // Update displayed information
                                            infoPanel.removeAll();
                                            infoPanel.add(new JLabel("Balance:"));
                                            infoPanel.add(new JLabel(String.format("%.1f", market.balance)));
                                            
                                            int newStock = market.stock.getOrDefault(selectedProduct, 0);
                                            stockLabel.setText(String.valueOf(newStock));
                                            
                                            JOptionPane.showMessageDialog(editMarketDialog,
                                                "Successfully purchased " + amount + " units of " + selectedProduct +
                                                " for a total of " + String.format("%.2f", totalCost),
                                                "Purchase Successful",
                                                JOptionPane.INFORMATION_MESSAGE);
                                            
                                            purchaseSuccessful = true;
                                            break;
                                        }
                                    }
                                }
                                
                                if (!purchaseSuccessful) {
                                    JOptionPane.showMessageDialog(editMarketDialog,
                                        "Not enough stock in factories or purchase failed.",
                                        "Purchase Failed",
                                        JOptionPane.WARNING_MESSAGE);
                                }
                                
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(editMarketDialog,
                                    "Error during purchase: " + ex.getMessage(),
                                    "Purchase Error",
                                    JOptionPane.ERROR_MESSAGE);
                            }
                        });
                        
                        // Add action listener for update price button
                        updatePriceBtn.addActionListener(priceEvent -> {
                            try {
                                String selectedProduct = (String) productCombo.getSelectedItem();
                                if (selectedProduct == null) {
                                    JOptionPane.showMessageDialog(editMarketDialog,
                                        "Please select a product first.",
                                        "No Product Selected",
                                        JOptionPane.WARNING_MESSAGE);
                                    return;
                                }
                                
                                // Validate price
                                double price;
                                try {
                                    price = Double.parseDouble(priceField.getText());
                                    if (price < 0) {
                                        throw new NumberFormatException();
                                    }
                                } catch (NumberFormatException ex) {
                                    JOptionPane.showMessageDialog(editMarketDialog,
                                        "Please enter a valid positive number for price.",
                                        "Invalid Price",
                                        JOptionPane.WARNING_MESSAGE);
                                    priceField.requestFocus();
                                    return;
                                }
                                
                                // Update the price
                                market.setPrice(selectedProduct, price);
                                
                                JOptionPane.showMessageDialog(editMarketDialog,
                                    "Price updated for " + selectedProduct + " to " + String.format("%.2f", price),
                                    "Price Updated",
                                    JOptionPane.INFORMATION_MESSAGE);
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(editMarketDialog,
                                    "Error updating price: " + ex.getMessage(),
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
                            }
                        });
                        
                        // Create main panel and add components
                        JPanel mainDialogPanel = new JPanel(new BorderLayout());
                        mainDialogPanel.add(infoPanel, BorderLayout.NORTH);
                        mainDialogPanel.add(buyPanel, BorderLayout.CENTER);
                        mainDialogPanel.add(pricePanel, BorderLayout.SOUTH);
                        
                        editMarketDialog.add(mainDialogPanel, BorderLayout.CENTER);
                        
                        // Add a close button at the bottom
                        JButton closeBtn = new JButton("Close");
                        closeBtn.addActionListener(closeEvent -> editMarketDialog.dispose());
                        
                        JPanel closePanel = new JPanel();
                        closePanel.add(closeBtn);
                        editMarketDialog.add(closePanel, BorderLayout.SOUTH);
                        
                        // Show the dialog
                        editMarketDialog.setVisible(true);
                        
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(this,
                            "Error editing market: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                });
                
                marketButtonsPanel.add(marketButton);
            }
            
            marketsListPanel.add(marketButtonsPanel, BorderLayout.CENTER);
        }
        
        // Add the markets list panel to the main panel
        mainPanel.add(marketsListPanel, BorderLayout.CENTER);
        
        // Create buttons for adding markets
        JButton addBtn = new JButton("Add Market");
        JButton editBtn = new JButton("Edit Market");
        
        // Create a panel for the buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(addBtn);
        buttonPanel.add(editBtn);
        
        // Add action listener for add button
        addBtn.addActionListener(e -> {
            try {
                // Create a dialog for adding a new market
                JDialog addMarketDialog = new JDialog(this, "Add New Market", true);
                addMarketDialog.setSize(300, 150);
                addMarketDialog.setLocationRelativeTo(this);
                addMarketDialog.setLayout(new GridLayout(3, 2, 5, 5));
                
                // Add components to the dialog
                JLabel nameLabel = new JLabel("Market Name:");
                JTextField nameField = new JTextField();
                JLabel balanceLabel = new JLabel("Initial Balance:");
                JTextField balanceField = new JTextField();
                JButton dialogAddBtn = new JButton("Add");
                
                addMarketDialog.add(nameLabel);
                addMarketDialog.add(nameField);
                addMarketDialog.add(balanceLabel);
                addMarketDialog.add(balanceField);
                addMarketDialog.add(new JLabel()); // Empty cell
                addMarketDialog.add(dialogAddBtn);
                
                // Add action listener for add button
                dialogAddBtn.addActionListener(dialogEvent -> {
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
                        Market newMarket = new Market(name, balance);
                        markets.add(newMarket);
                        
                        // Close the dialog and refresh the markets panel
                        addMarketDialog.dispose();
                        showMarketsPanel(); // Refresh the panel
                        
                    } catch (IllegalArgumentException ex) {
                        JOptionPane.showMessageDialog(addMarketDialog,
                            ex.getMessage(),
                            "Invalid Input",
                            JOptionPane.WARNING_MESSAGE);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(addMarketDialog,
                            "Error adding market: " + ex.getMessage(),
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
                    }
                });
                
                // Show the dialog
                addMarketDialog.setVisible(true);
                
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Error creating dialog: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        // Add action listener for edit button (similar to clicking a market button)
        editBtn.addActionListener(e -> {
            if (markets.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                    "No markets available to edit. Please add a market first.",
                    "No Markets",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // For simplicity, just edit the first market
            Market market = markets.get(0);
            
            try {
                // Show dialog to edit market (simplified version)
                JDialog editMarketDialog = new JDialog(this, "Edit Market: " + market.name, true);
                editMarketDialog.setSize(400, 300);
                editMarketDialog.setLocationRelativeTo(this);
                editMarketDialog.setLayout(new BorderLayout());
                
                // Create a panel with market information
                JPanel infoPanel = new JPanel(new GridLayout(1, 2, 5, 5));
                infoPanel.add(new JLabel("Balance:"));
                infoPanel.add(new JLabel(String.format("%.1f", market.balance)));
                
                editMarketDialog.add(infoPanel, BorderLayout.NORTH);
                
                // Add a close button at the bottom
                JButton closeBtn = new JButton("Close");
                closeBtn.addActionListener(closeEvent -> editMarketDialog.dispose());
                
                JPanel closePanel = new JPanel();
                closePanel.add(closeBtn);
                editMarketDialog.add(closePanel, BorderLayout.SOUTH);
                
                // Show the dialog
                editMarketDialog.setVisible(true);
                
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Error editing market: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
        
        // Add the button panel to the main panel
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Add the main panel to the content pane
        add(mainPanel);
        revalidate();
        repaint();
    }
    
    /**
     * Shows the customers panel
     */
    private void showCustomersPanel() {
        getContentPane().removeAll();
        getContentPane().add(createCustomerPanel(), BorderLayout.CENTER);
        revalidate();
        repaint();
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
