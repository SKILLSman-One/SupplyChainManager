import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
// Main class to run the program
public class SupplyChainSystemGUI {
    public static void main(String[] args) {
        MainFrame frame = new MainFrame();
        frame.setVisible(true);
    }
}
class InputMaterial {
    Producer producer;
    int amount;

    public InputMaterial(Producer producer, int amount) {
        this.producer = producer;
        this.amount = amount;
    }

    public String toString() {
        return producer.name + " x" + amount;
    }
}

class Customer {
    String name;
    double balance;
    Map<String, Integer> inventory = new HashMap<>();

    public Customer(String name, double balance) {
        this.name = name;
        this.balance = balance;
    }

    public void addProduct(String product, int amount) {
        inventory.put(product, inventory.getOrDefault(product, 0) + amount);
    }
    
    // Enhanced buyProduct method with proper exception handling
    public void buyProduct(String product, int amount, double price, Market market) throws Exception {
        if (!market.stock.containsKey(product)) {
            throw new Exception("Product not available in this market");
        }
        
        int availableStock = market.stock.getOrDefault(product, 0);
        if (availableStock < amount) {
            throw new Exception("Not enough stock in market: requested " + amount + ", available " + availableStock);
        }

        double totalCost = amount * price;
        if (balance < totalCost) {
            throw new Exception("Not enough balance: cost " + String.format("%.2f", totalCost) + 
                               ", available " + String.format("%.2f", balance));
        }

        // Complete successful purchase
        balance -= totalCost;
        market.balance += totalCost;  // Economic transfer - market receives payment
        market.stock.put(product, availableStock - amount);
        addProduct(product, amount);
    }

    public String toString() {
        return name + " (Balance: " + String.format("%.2f", balance) + ")";
    }
}


class Market {
    String name;
    double balance;
    Map<String, Integer> stock = new HashMap<>();
    Map<String, Double> prices = new HashMap<>();

    public Market(String name, double balance) {
        this.name = name;
        this.balance = balance;
    }

    public void buyProduct(String product, int amount, double pricePerUnit, FactoryExtended factory) throws Exception {
        if (!factory.products.containsKey(product)) {
            throw new Exception("Product not available from this factory");
        }

        InventoryItem item = factory.products.get(product);
        if (item.quantity < amount) {
            throw new Exception("Not enough stock: requested " + amount + ", available " + item.quantity);
        }

        double totalCost = amount * pricePerUnit;
        if (balance < totalCost) {
            throw new Exception("Not enough balance: cost " + String.format("%.2f", totalCost) + 
                               ", available " + String.format("%.2f", balance));
        }

        // Successful transaction logic
        item.quantity -= amount;
        balance -= totalCost;
        factory.balance += totalCost; // Pay the factory (adding economic flow)
        
        // Update stock
        stock.put(product, stock.getOrDefault(product, 0) + amount);
    }

    public void setPrice(String product, double price) throws Exception {
        if (price <= 0) {
            throw new Exception("Price must be greater than zero");
        }
        prices.put(product, price);
    }

    public double getPrice(String product) {
        return prices.getOrDefault(product, 0.0);
    }

    public String toString() {
        return name + " (Balance: " + String.format("%.2f", balance) + ")";
    }
}class ShopDialog extends JDialog {
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
}class EditMarketPanel extends JPanel {
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