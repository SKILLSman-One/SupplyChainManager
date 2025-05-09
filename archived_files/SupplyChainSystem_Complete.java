import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

class SupplyChainSystemGUI {
    static ArrayList<Producer> producers = new ArrayList<>();
    static ArrayList<FactoryExtended> factories = new ArrayList<>();
    static ArrayList<Market> markets = new ArrayList<>();
    static ArrayList<Customer> customers = new ArrayList<>();

    static java.util.List<ProductDesign> allDesigns = new ArrayList<>();
    
    static Color backgroundColor = new Color(240, 240, 245);
    static Color accentColor = new Color(70, 130, 180);
    static Color lightAccentColor = new Color(173, 216, 230);
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            setUpDemoData();
            new MainFrame();
        });
    }
    
    private static void setUpDemoData() {
        // Set up demo producers
        Producer p1 = new Producer("Farm", 1000);
        p1.materials.put("Wood", 20);
        p1.materials.put("Iron", 15);
        p1.materials.put("Plastic", 30);
        producers.add(p1);
        
        Producer p2 = new Producer("Mine", 1500);
        p2.materials.put("Stone", 40);
        p2.materials.put("Gold", 5);
        p2.materials.put("Silver", 10);
        producers.add(p2);
        
        // Set up demo factories
        FactoryExtended f1 = new FactoryExtended("Furniture Factory", 2000);
        ProductDesign chair = new ProductDesign("Chair", 50.0);
        chair.addMaterial(new InputMaterial("Wood", 4));
        f1.designs.add(chair);
        factories.add(f1);
        
        FactoryExtended f2 = new FactoryExtended("Electronics Factory", 3000);
        ProductDesign phone = new ProductDesign("Phone", 200.0);
        phone.addMaterial(new InputMaterial("Plastic", 2));
        phone.addMaterial(new InputMaterial("Gold", 1));
        f2.designs.add(phone);
        factories.add(f2);
        
        allDesigns.add(chair);
        allDesigns.add(phone);
        
        // Set up demo markets
        Market m1 = new Market("Downtown Mall", 5000);
        markets.add(m1);
        
        Market m2 = new Market("Online Store", 4000);
        markets.add(m2);
        
        // Set up demo customers
        Customer c1 = new Customer("John", 500);
        customers.add(c1);
        
        Customer c2 = new Customer("Alice", 800);
        customers.add(c2);
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
    
    // Enhanced buyProduct method with proper error handling
    public boolean buyProduct(String product, int amount, double price, Market market) {
        if (!market.stock.containsKey(product)) {
            JOptionPane.showMessageDialog(null, "Product not available in this market");
            return false;
        }
        
        int availableStock = market.stock.getOrDefault(product, 0);
        if (availableStock < amount) {
            JOptionPane.showMessageDialog(null, "Not enough stock in market: requested " + amount + ", available " + availableStock);
            return false;
        }

        double totalCost = amount * price;
        if (balance < totalCost) {
            JOptionPane.showMessageDialog(null, "Not enough balance: cost " + String.format("%.2f", totalCost) + 
                               ", available " + String.format("%.2f", balance));
            return false;
        }

        // Complete successful purchase
        balance -= totalCost;
        market.balance += totalCost;  // Economic transfer - market receives payment
        market.stock.put(product, availableStock - amount);
        addProduct(product, amount);
        return true;
    }

    public String toString() {
        return name + " (Balance: " + String.format("%.2f", balance) + ")";
    }
}

class Producer {
    String name;
    double balance;
    Map<String, Integer> materials = new HashMap<>();
    
    public Producer(String name, double balance) {
        this.name = name;
        this.balance = balance;
    }
    
    public String toString() {
        return name + " (Balance: " + String.format("%.2f", balance) + ")";
    }
}

class InputMaterial {
    String name;
    int amount;
    
    public InputMaterial(String name, int amount) {
        this.name = name;
        this.amount = amount;
    }
}

class ProductDesign {
    String name;
    double cost;
    ArrayList<InputMaterial> materials = new ArrayList<>();
    
    public ProductDesign(String name, double cost) {
        this.name = name;
        this.cost = cost;
    }
    
    public void addMaterial(InputMaterial material) {
        materials.add(material);
    }
}

class InventoryItem {
    String name;
    int quantity;
    
    public InventoryItem(String name, int quantity) {
        this.name = name;
        this.quantity = quantity;
    }
}

class Factory {
    String name;
    double balance;
    
    public Factory(String name, double balance) {
        this.name = name;
        this.balance = balance;
    }
    
    public String toString() {
        return name + " (Balance: " + String.format("%.2f", balance) + ")";
    }
}

class FactoryExtended extends Factory {
    ArrayList<ProductDesign> designs = new ArrayList<>();
    Map<String, InventoryItem> products = new HashMap<>();
    Map<String, Integer> materials = new HashMap<>();
    
    public FactoryExtended(String name, double balance) {
        super(name, balance);
    }
    
    public void addMaterial(String material, int amount) {
        materials.put(material, materials.getOrDefault(material, 0) + amount);
    }
    
    public void manufacture(ProductDesign design, int amount) {
        // Check if we have the materials
        boolean canManufacture = true;
        for (InputMaterial material : design.materials) {
            int available = materials.getOrDefault(material.name, 0);
            if (available < material.amount * amount) {
                canManufacture = false;
                JOptionPane.showMessageDialog(null, "Not enough " + material.name + ". Need " + (material.amount * amount) + ", have " + available);
                break;
            }
        }
        
        if (canManufacture) {
            // Consume materials
            for (InputMaterial material : design.materials) {
                int current = materials.get(material.name);
                materials.put(material.name, current - (material.amount * amount));
            }
            
            // Create product
            InventoryItem item = products.getOrDefault(design.name, new InventoryItem(design.name, 0));
            item.quantity += amount;
            products.put(design.name, item);
            
            JOptionPane.showMessageDialog(null, "Successfully manufactured " + amount + " " + design.name + "(s)!");
        }
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
    
    // Enhanced buyProduct method with better error handling
    public boolean buyProduct(String product, int amount, double pricePerUnit, FactoryExtended factory) {
        if (!factory.products.containsKey(product)) {
            JOptionPane.showMessageDialog(null, "Product not available from this factory");
            return false;
        }

        InventoryItem item = factory.products.get(product);
        if (item.quantity < amount) {
            JOptionPane.showMessageDialog(null, "Not enough stock: requested " + amount + ", available " + item.quantity);
            return false;
        }

        double totalCost = amount * pricePerUnit;
        if (balance < totalCost) {
            JOptionPane.showMessageDialog(null, "Not enough balance: cost " + String.format("%.2f", totalCost) + 
                           ", available " + String.format("%.2f", balance));
            return false;
        }

        // Successful transaction logic
        item.quantity -= amount;
        balance -= totalCost;
        factory.balance += totalCost; // Pay the factory (adding economic flow)
        
        // Update stock
        stock.put(product, stock.getOrDefault(product, 0) + amount);
        return true;
    }
    
    // Enhanced setPrice method with validation
    public boolean setPrice(String product, double price) {
        if (price <= 0) {
            JOptionPane.showMessageDialog(null, "Price must be greater than zero");
            return false;
        }
        prices.put(product, price);
        return true;
    }
    
    public double getPrice(String product) {
        return prices.getOrDefault(product, 0.0);
    }
    
    public String toString() {
        return name + " (Balance: " + String.format("%.2f", balance) + ")";
    }
}

// Main UI Classes
class MainFrame extends JFrame {
    private JPanel contentPanel;
    private JPanel navPanel;
    private JPanel displayPanel;
    
    private JPanel producerPanel;
    private JPanel factoryPanel;
    private JPanel marketPanel;
    private JPanel customerPanel;
    
    DefaultListModel<Producer> producerListModel = new DefaultListModel<>();
    DefaultListModel<FactoryExtended> factoryListModel = new DefaultListModel<>();
    DefaultListModel<Market> marketListModel = new DefaultListModel<>();
    DefaultListModel<Customer> customerListModel = new DefaultListModel<>();
    
    JList<Producer> producerList;
    JList<FactoryExtended> factoryList;
    JList<Market> marketList;
    JList<Customer> customerList;
    
    public MainFrame() {
        setTitle("Supply Chain Management System");
        setSize(900, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(SupplyChainSystemGUI.backgroundColor);
        
        setupNavigation();
        setupContentPanels();
        
        add(contentPanel);
        
        // Populate list models
        for (Producer p : SupplyChainSystemGUI.producers) {
            producerListModel.addElement(p);
        }
        
        for (FactoryExtended f : SupplyChainSystemGUI.factories) {
            factoryListModel.addElement(f);
        }
        
        for (Market m : SupplyChainSystemGUI.markets) {
            marketListModel.addElement(m);
        }
        
        for (Customer c : SupplyChainSystemGUI.customers) {
            customerListModel.addElement(c);
        }
        
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private void setupNavigation() {
        navPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        navPanel.setBackground(SupplyChainSystemGUI.backgroundColor);
        navPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JButton producersBtn = createGradientButton("Producers");
        JButton factoriesBtn = createGradientButton("Factories");
        JButton marketsBtn = createGradientButton("Markets");
        JButton customersBtn = createGradientButton("Customers");
        
        producersBtn.addActionListener(e -> showPanel("producers"));
        factoriesBtn.addActionListener(e -> showPanel("factories"));
        marketsBtn.addActionListener(e -> showPanel("markets"));
        customersBtn.addActionListener(e -> showPanel("customers"));
        
        navPanel.add(producersBtn);
        navPanel.add(factoriesBtn);
        navPanel.add(marketsBtn);
        navPanel.add(customersBtn);
        
        contentPanel.add(navPanel, BorderLayout.WEST);
    }
    
    private JButton createGradientButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                if (!isOpaque() && getBorder() instanceof javax.swing.plaf.UIResource) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setPaint(new GradientPaint(
                        0, 0, SupplyChainSystemGUI.accentColor,
                        0, getHeight(), SupplyChainSystemGUI.lightAccentColor));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                    g2.dispose();
                }
                super.paintComponent(g);
            }
            
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(150, 50);
            }
        };
        
        button.setOpaque(false);
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        
        return button;
    }
    
    private void setupContentPanels() {
        displayPanel = new JPanel(new CardLayout());
        displayPanel.setBackground(SupplyChainSystemGUI.backgroundColor);
        
        // Producer Panel
        setupProducerPanel();
        setupFactoryPanel();
        setupMarketPanel();
        setupCustomerPanel();
        
        contentPanel.add(displayPanel, BorderLayout.CENTER);
        
        // Set initial panel
        showPanel("producers");
    }
    
    private void setupProducerPanel() {
        producerPanel = new JPanel(new BorderLayout());
        producerPanel.setBackground(SupplyChainSystemGUI.backgroundColor);
        producerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel titleLabel = new JLabel("Producers");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(SupplyChainSystemGUI.accentColor);
        
        producerList = new JList<>(producerListModel);
        producerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        producerList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (isSelected) {
                    c.setBackground(SupplyChainSystemGUI.accentColor);
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(list.getBackground());
                    c.setForeground(list.getForeground());
                }
                return c;
            }
        });
        
        JScrollPane listScroller = new JScrollPane(producerList);
        
        // Buttons panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(SupplyChainSystemGUI.backgroundColor);
        
        JButton inventoryBtn = new JButton("View Inventory");
        JButton editBtn = new JButton("Edit Producer");
        
        inventoryBtn.addActionListener(e -> {
            Producer selected = producerList.getSelectedValue();
            if (selected != null) {
                // Show inventory dialog
                JDialog dialog = new InventoryDialog(this, selected);
                dialog.setVisible(true);
            }
        });
        
        editBtn.addActionListener(e -> {
            // Edit dialog for producer
        });
        
        buttonPanel.add(inventoryBtn);
        buttonPanel.add(editBtn);
        
        producerPanel.add(titleLabel, BorderLayout.NORTH);
        producerPanel.add(listScroller, BorderLayout.CENTER);
        producerPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        displayPanel.add(producerPanel, "producers");
    }
    
    private void setupFactoryPanel() {
        factoryPanel = new JPanel(new BorderLayout());
        factoryPanel.setBackground(SupplyChainSystemGUI.backgroundColor);
        factoryPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel titleLabel = new JLabel("Factories");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(SupplyChainSystemGUI.accentColor);
        
        factoryList = new JList<>(factoryListModel);
        factoryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        factoryList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (isSelected) {
                    c.setBackground(SupplyChainSystemGUI.accentColor);
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(list.getBackground());
                    c.setForeground(list.getForeground());
                }
                return c;
            }
        });
        
        JScrollPane listScroller = new JScrollPane(factoryList);
        
        // Buttons panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(SupplyChainSystemGUI.backgroundColor);
        
        JButton inventoryBtn = new JButton("View Inventory");
        JButton manufacturBtn = new JButton("Manufacture");
        JButton designBtn = new JButton("Create Design");
        JButton editBtn = new JButton("Edit Factory");
        
        inventoryBtn.addActionListener(e -> {
            FactoryExtended selected = factoryList.getSelectedValue();
            if (selected != null) {
                // Show inventory dialog
                // First for materials
                JOptionPane.showMessageDialog(this, "Materials Inventory:\n" + 
                                          formatInventory(selected.materials),
                                          selected.name + " Materials",
                                          JOptionPane.INFORMATION_MESSAGE);
                
                // Then for products
                StringBuilder products = new StringBuilder();
                for (Map.Entry<String, InventoryItem> entry : selected.products.entrySet()) {
                    products.append(entry.getKey()).append(": ").append(entry.getValue().quantity).append("\n");
                }
                
                if (products.length() == 0) {
                    products.append("No products in inventory.");
                }
                
                JOptionPane.showMessageDialog(this, "Products Inventory:\n" + 
                                          products.toString(),
                                          selected.name + " Products",
                                          JOptionPane.INFORMATION_MESSAGE);
            }
        });
        
        manufacturBtn.addActionListener(e -> {
            FactoryExtended selected = factoryList.getSelectedValue();
            if (selected != null) {
                // Show manufacture dialog
                JDialog dialog = new ManufactureDialog(this, selected);
                dialog.setVisible(true);
            }
        });
        
        designBtn.addActionListener(e -> {
            FactoryExtended selected = factoryList.getSelectedValue();
            if (selected != null) {
                // Show design creation dialog
                JDialog dialog = new CreateDesignDialog(this, selected);
                dialog.setVisible(true);
            }
        });
        
        editBtn.addActionListener(e -> {
            // Edit dialog for factory
        });
        
        buttonPanel.add(inventoryBtn);
        buttonPanel.add(manufacturBtn);
        buttonPanel.add(designBtn);
        buttonPanel.add(editBtn);
        
        factoryPanel.add(titleLabel, BorderLayout.NORTH);
        factoryPanel.add(listScroller, BorderLayout.CENTER);
        factoryPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        displayPanel.add(factoryPanel, "factories");
    }
    
    private void setupMarketPanel() {
        marketPanel = new JPanel(new BorderLayout());
        marketPanel.setBackground(SupplyChainSystemGUI.backgroundColor);
        marketPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel titleLabel = new JLabel("Markets");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(SupplyChainSystemGUI.accentColor);
        
        marketList = new JList<>(marketListModel);
        marketList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        marketList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (isSelected) {
                    c.setBackground(SupplyChainSystemGUI.accentColor);
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(list.getBackground());
                    c.setForeground(list.getForeground());
                }
                return c;
            }
        });
        
        JScrollPane listScroller = new JScrollPane(marketList);
        
        // Buttons panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(SupplyChainSystemGUI.backgroundColor);
        
        JButton inventoryBtn = new JButton("View Inventory");
        JButton editBtn = new JButton("Edit Market");
        JButton addBtn = new JButton("Add Market");
        
        inventoryBtn.addActionListener(e -> {
            Market selected = marketList.getSelectedValue();
            if (selected != null) {
                // Show inventory dialog
                StringBuilder inventory = new StringBuilder();
                for (Map.Entry<String, Integer> entry : selected.stock.entrySet()) {
                    String priceStr = selected.prices.containsKey(entry.getKey()) ? 
                                    String.format("%.2f", selected.prices.get(entry.getKey())) : 
                                    "Not set";
                    inventory.append(entry.getKey()).append(": ").append(entry.getValue())
                             .append(" (Price: ").append(priceStr).append(")\n");
                }
                
                if (inventory.length() == 0) {
                    inventory.append("No products in inventory.");
                }
                
                JOptionPane.showMessageDialog(this, "Inventory:\n" + 
                                          inventory.toString(),
                                          selected.name + " Inventory",
                                          JOptionPane.INFORMATION_MESSAGE);
            }
        });
        
        editBtn.addActionListener(e -> {
            Market selected = marketList.getSelectedValue();
            if (selected != null) {
                // Show edit dialog
                JDialog dialog = new EditMarketDialog(this, selected);
                dialog.setVisible(true);
            }
        });
        
        addBtn.addActionListener(e -> {
            // Show add market dialog
            JDialog dialog = new AddMarketDialog(this);
            dialog.setVisible(true);
        });
        
        buttonPanel.add(inventoryBtn);
        buttonPanel.add(editBtn);
        buttonPanel.add(addBtn);
        
        marketPanel.add(titleLabel, BorderLayout.NORTH);
        marketPanel.add(listScroller, BorderLayout.CENTER);
        marketPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        displayPanel.add(marketPanel, "markets");
    }
    
    private void setupCustomerPanel() {
        customerPanel = new JPanel(new BorderLayout());
        customerPanel.setBackground(SupplyChainSystemGUI.backgroundColor);
        customerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JLabel titleLabel = new JLabel("Customers");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(SupplyChainSystemGUI.accentColor);
        
        customerList = new JList<>(customerListModel);
        customerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        customerList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (isSelected) {
                    c.setBackground(SupplyChainSystemGUI.accentColor);
                    c.setForeground(Color.WHITE);
                } else {
                    c.setBackground(list.getBackground());
                    c.setForeground(list.getForeground());
                }
                return c;
            }
        });
        
        JScrollPane listScroller = new JScrollPane(customerList);
        
        // Buttons panel
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(SupplyChainSystemGUI.backgroundColor);
        
        JButton inventoryBtn = new JButton("View Inventory");
        JButton shopBtn = new JButton("Shop");
        JButton editBtn = new JButton("Edit Customer");
        
        inventoryBtn.addActionListener(e -> {
            Customer selected = customerList.getSelectedValue();
            if (selected != null) {
                // Show inventory dialog
                JDialog dialog = new CustomerInventoryDialog(this, selected);
                dialog.setVisible(true);
            }
        });
        
        shopBtn.addActionListener(e -> {
            Customer selected = customerList.getSelectedValue();
            if (selected != null) {
                // Show shop dialog
                JDialog dialog = new ShopDialog(this, selected);
                dialog.setVisible(true);
            }
        });
        
        editBtn.addActionListener(e -> {
            Customer selected = customerList.getSelectedValue();
            if (selected != null) {
                // Show edit dialog
                JDialog dialog = new EditCustomerDialog(this, selected);
                dialog.setVisible(true);
            }
        });
        
        buttonPanel.add(inventoryBtn);
        buttonPanel.add(shopBtn);
        buttonPanel.add(editBtn);
        
        customerPanel.add(titleLabel, BorderLayout.NORTH);
        customerPanel.add(listScroller, BorderLayout.CENTER);
        customerPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        displayPanel.add(customerPanel, "customers");
    }
    
    private void showPanel(String name) {
        CardLayout cl = (CardLayout) displayPanel.getLayout();
        cl.show(displayPanel, name);
    }
    
    private String formatInventory(Map<String, Integer> inventory) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        
        if (sb.length() == 0) {
            sb.append("No items in inventory.");
        }
        
        return sb.toString();
    }
}

class InventoryDialog extends JDialog {
    public InventoryDialog(JFrame parent, Producer producer) {
        super(parent, "Inventory: " + producer.name, true);
        setSize(300, 300);
        setLocationRelativeTo(parent);
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(SupplyChainSystemGUI.backgroundColor);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        DefaultListModel<String> model = new DefaultListModel<>();
        for (Map.Entry<String, Integer> entry : producer.materials.entrySet()) {
            model.addElement(entry.getKey() + ": " + entry.getValue());
        }
        
        JList<String> list = new JList<>(model);
        JScrollPane scrollPane = new JScrollPane(list);
        
        JLabel balanceLabel = new JLabel("Balance: " + String.format("%.2f", producer.balance));
        
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(balanceLabel, BorderLayout.SOUTH);
        
        add(panel);
    }
}

class ManufactureDialog extends JDialog {
    public ManufactureDialog(JFrame parent, FactoryExtended factory) {
        super(parent, "Manufacture: " + factory.name, true);
        setSize(400, 300);
        setLocationRelativeTo(parent);
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(SupplyChainSystemGUI.backgroundColor);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JComboBox<ProductDesign> designSelector = new JComboBox<>();
        for (ProductDesign design : factory.designs) {
            designSelector.addItem(design);
        }
        
        designSelector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof ProductDesign) {
                    value = ((ProductDesign) value).name + " (Cost: " + ((ProductDesign) value).cost + ")";
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        
        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        inputPanel.setBackground(SupplyChainSystemGUI.backgroundColor);
        
        JLabel amountLabel = new JLabel("Amount: ");
        JTextField amountField = new JTextField("1");
        
        inputPanel.add(new JLabel("Design: "));
        inputPanel.add(designSelector);
        inputPanel.add(amountLabel);
        inputPanel.add(amountField);
        
        JButton manufactureBtn = new JButton("Manufacture");
        
        manufactureBtn.addActionListener(e -> {
            ProductDesign selected = (ProductDesign) designSelector.getSelectedItem();
            if (selected != null) {
                try {
                    int amount = Integer.parseInt(amountField.getText());
                    if (amount > 0) {
                        factory.manufacture(selected, amount);
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(this, "Amount must be greater than 0.");
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Please enter a valid number for amount.");
                }
            }
        });
        
        panel.add(inputPanel, BorderLayout.CENTER);
        panel.add(manufactureBtn, BorderLayout.SOUTH);
        
        add(panel);
    }
}

class CreateDesignDialog extends JDialog {
    public CreateDesignDialog(JFrame parent, FactoryExtended factory) {
        super(parent, "Create Design: " + factory.name, true);
        setSize(400, 400);
        setLocationRelativeTo(parent);
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(SupplyChainSystemGUI.backgroundColor);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        inputPanel.setBackground(SupplyChainSystemGUI.backgroundColor);
        
        JTextField nameField = new JTextField();
        JTextField costField = new JTextField();
        
        inputPanel.add(new JLabel("Name: "));
        inputPanel.add(nameField);
        inputPanel.add(new JLabel("Cost: "));
        inputPanel.add(costField);
        
        DefaultListModel<InputMaterial> materialsModel = new DefaultListModel<>();
        JList<InputMaterial> materialsList = new JList<>(materialsModel);
        
        materialsList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (value instanceof InputMaterial) {
                    InputMaterial im = (InputMaterial) value;
                    value = im.name + " (x" + im.amount + ")";
                }
                return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            }
        });
        
        JScrollPane materialsScroller = new JScrollPane(materialsList);
        
        JPanel materialsPanel = new JPanel(new BorderLayout());
        materialsPanel.setBackground(SupplyChainSystemGUI.backgroundColor);
        materialsPanel.setBorder(BorderFactory.createTitledBorder("Materials"));
        
        JPanel addMaterialPanel = new JPanel(new GridLayout(1, 4, 5, 5));
        addMaterialPanel.setBackground(SupplyChainSystemGUI.backgroundColor);
        
        JTextField materialNameField = new JTextField();
        JTextField materialAmountField = new JTextField("1");
        JButton addMaterialBtn = new JButton("+");
        
        addMaterialPanel.add(materialNameField);
        addMaterialPanel.add(materialAmountField);
        addMaterialPanel.add(addMaterialBtn);
        
        addMaterialBtn.addActionListener(e -> {
            String name = materialNameField.getText().trim();
            if (!name.isEmpty()) {
                try {
                    int amount = Integer.parseInt(materialAmountField.getText());
                    if (amount > 0) {
                        InputMaterial material = new InputMaterial(name, amount);
                        materialsModel.addElement(material);
                        materialNameField.setText("");
                        materialAmountField.setText("1");
                    } else {
                        JOptionPane.showMessageDialog(this, "Amount must be greater than 0.");
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Please enter a valid number for amount.");
                }
            }
        });
        
        materialsPanel.add(materialsScroller, BorderLayout.CENTER);
        materialsPanel.add(addMaterialPanel, BorderLayout.SOUTH);
        
        JButton createBtn = new JButton("Create Design");
        
        createBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a name for the design.");
                return;
            }
            
            try {
                double cost = Double.parseDouble(costField.getText());
                if (cost <= 0) {
                    JOptionPane.showMessageDialog(this, "Cost must be greater than 0.");
                    return;
                }
                
                if (materialsModel.size() == 0) {
                    JOptionPane.showMessageDialog(this, "Please add at least one material.");
                    return;
                }
                
                ProductDesign design = new ProductDesign(name, cost);
                for (int i = 0; i < materialsModel.size(); i++) {
                    design.addMaterial(materialsModel.getElementAt(i));
                }
                
                factory.designs.add(design);
                SupplyChainSystemGUI.allDesigns.add(design);
                
                JOptionPane.showMessageDialog(this, "Design created successfully!");
                dispose();
                
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid number for cost.");
            }
        });
        
        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(materialsPanel, BorderLayout.CENTER);
        panel.add(createBtn, BorderLayout.SOUTH);
        
        add(panel);
    }
}

class AddMarketDialog extends JDialog {
    public AddMarketDialog(JFrame parent) {
        super(parent, "Add Market", true);
        setSize(300, 150);
        setLocationRelativeTo(parent);
        
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        panel.setBackground(SupplyChainSystemGUI.backgroundColor);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JTextField nameField = new JTextField();
        JTextField balanceField = new JTextField("1000");
        
        panel.add(new JLabel("Name: "));
        panel.add(nameField);
        panel.add(new JLabel("Initial Balance: "));
        panel.add(balanceField);
        
        JButton addBtn = new JButton("Add Market");
        
        addBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a name for the market.");
                return;
            }
            
            try {
                double balance = Double.parseDouble(balanceField.getText());
                if (balance < 0) {
                    JOptionPane.showMessageDialog(this, "Balance cannot be negative.");
                    return;
                }
                
                Market market = new Market(name, balance);
                SupplyChainSystemGUI.markets.add(market);
                ((MainFrame) parent).marketListModel.addElement(market);
                
                dispose();
                
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid number for balance.");
            }
        });
        
        panel.add(new JLabel(""));
        panel.add(addBtn);
        
        add(panel);
    }
}

class EditMarketDialog extends JDialog {
    public EditMarketDialog(JFrame parent, Market market) {
        super(parent, "Edit Market: " + market.name, true);
        setSize(700, 500);
        setLocationRelativeTo(parent);
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(SupplyChainSystemGUI.backgroundColor);
        
        EditMarketPanel panel = new EditMarketPanel(this, market);
        mainPanel.add(panel);
        
        add(mainPanel);
    }
}

class EditMarketPanel extends JPanel {
    JDialog parent;
    Market market;
    ArrayList<FactoryExtended> factories = SupplyChainSystemGUI.factories;
    
    JComboBox<String> productSelector;
    JLabel stockLabel;
    JLabel priceLabel;
    JLabel balanceLabel;
    
    public EditMarketPanel(JDialog parent, Market market) {
        this.parent = parent;
        this.market = market;
        
        setLayout(new BorderLayout());
        setBackground(SupplyChainSystemGUI.backgroundColor);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Market Info Panel
        JPanel infoPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        infoPanel.setBackground(SupplyChainSystemGUI.backgroundColor);
        
        infoPanel.add(new JLabel("Market Name: "));
        infoPanel.add(new JLabel(market.name));
        
        balanceLabel = new JLabel("Balance: " + String.format("%.2f", market.balance));
        infoPanel.add(new JLabel("Balance: "));
        infoPanel.add(balanceLabel);
        
        // Product Selection
        JPanel selectionPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        selectionPanel.setBackground(SupplyChainSystemGUI.backgroundColor);
        selectionPanel.setBorder(BorderFactory.createTitledBorder("Product Information"));
        
        productSelector = new JComboBox<>();
        for (String product : market.stock.keySet()) {
            productSelector.addItem(product);
        }
        
        for (FactoryExtended factory : factories) {
            for (String product : factory.products.keySet()) {
                if (!market.stock.containsKey(product)) {
                    productSelector.addItem(product);
                }
            }
        }
        
        stockLabel = new JLabel("Stock: 0");
        priceLabel = new JLabel("Price: 0.00");
        
        productSelector.addActionListener(e -> {
            String selected = (String) productSelector.getSelectedItem();
            if (selected != null) {
                int stock = market.stock.getOrDefault(selected, 0);
                stockLabel.setText("Stock: " + stock);
                
                double price = market.prices.getOrDefault(selected, 0.0);
                priceLabel.setText("Price: " + String.format("%.2f", price));
            }
        });
        
        selectionPanel.add(new JLabel("Product: "));
        selectionPanel.add(productSelector);
        selectionPanel.add(new JLabel("Stock: "));
        selectionPanel.add(stockLabel);
        selectionPanel.add(new JLabel("Price: "));
        selectionPanel.add(priceLabel);
        
        // Buy Panel
        JPanel buyPanel = new JPanel();
        buyPanel.setBackground(SupplyChainSystemGUI.backgroundColor);
        buyPanel.setBorder(BorderFactory.createTitledBorder("Buy Products"));
        
        JLabel amountLabel = new JLabel("Amount: ");
        JTextField amountField = new JTextField("1", 5);
        JButton buyButton = new JButton("Buy from Factory");
        
        buyPanel.add(amountLabel);
        buyPanel.add(amountField);
        buyPanel.add(buyButton);
        
        // Price Panel
        JPanel pricePanel = new JPanel();
        pricePanel.setBackground(SupplyChainSystemGUI.backgroundColor);
        pricePanel.setBorder(BorderFactory.createTitledBorder("Set Price"));
        
        JLabel setPriceLabel = new JLabel("Price: ");
        JTextField priceField = new JTextField("0.00", 5);
        JButton updatePriceButton = new JButton("Update Price");
        
        pricePanel.add(setPriceLabel);
        pricePanel.add(priceField);
        pricePanel.add(updatePriceButton);
        
        // Buy button action
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
                        
                        // Use the enhanced market.buyProduct with better error handling
                        boolean success = market.buyProduct(selected, amount, costPerUnit, f);
                        if (success) {
                            // Update UI
                            balanceLabel.setText("Balance: " + String.format("%.2f", market.balance));
                            
                            // Refresh product selection to update stock display
                            productSelector.setSelectedItem(selected);
                            
                            // Show simple success message
                            JOptionPane.showMessageDialog(parent, "Purchase successful!");
                            
                            purchaseSuccessful = true;
                            break;
                        }
                    }
                }
                
                if (!purchaseSuccessful) {
                    JOptionPane.showMessageDialog(parent, "Could not find a factory with enough stock.");
                }
                
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(parent, "Please enter a valid number for amount.");
            }
        });
        
        // Update price button action
        updatePriceButton.addActionListener(e -> {
            String selected = (String) productSelector.getSelectedItem();
            if (selected == null) {
                JOptionPane.showMessageDialog(parent, "Please select a product first.");
                return;
            }
            
            try {
                double price = Double.parseDouble(priceField.getText());
                
                // Use enhanced setPrice with better validation
                boolean success = market.setPrice(selected, price);
                if (success) {
                    JOptionPane.showMessageDialog(parent, "Price updated for " + selected + " to " + price);
                    // Update the price label
                    priceLabel.setText("Price: " + String.format("%.2f", price));
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(parent, "Please enter a valid number for price.");
            }
        });
        
        // Main Layout
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(SupplyChainSystemGUI.backgroundColor);
        
        centerPanel.add(selectionPanel, BorderLayout.NORTH);
        
        JPanel actionsPanel = new JPanel(new GridLayout(2, 1));
        actionsPanel.setBackground(SupplyChainSystemGUI.backgroundColor);
        
        actionsPanel.add(buyPanel);
        actionsPanel.add(pricePanel);
        
        centerPanel.add(actionsPanel, BorderLayout.CENTER);
        
        add(infoPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        
        // Initialize the display for the first product if available
        if (productSelector.getItemCount() > 0) {
            productSelector.setSelectedIndex(0);
        }
    }
}

class EditCustomerDialog extends JDialog {
    public EditCustomerDialog(JFrame parent, Customer customer) {
        super(parent, "Edit Customer: " + customer.name, true);
        setSize(300, 150);
        setLocationRelativeTo(parent);
        
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        panel.setBackground(SupplyChainSystemGUI.backgroundColor);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JTextField nameField = new JTextField(customer.name);
        JTextField balanceField = new JTextField(String.format("%.2f", customer.balance));
        
        panel.add(new JLabel("Name: "));
        panel.add(nameField);
        panel.add(new JLabel("Balance: "));
        panel.add(balanceField);
        
        JButton updateBtn = new JButton("Update");
        
        updateBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Name cannot be empty.");
                return;
            }
            
            try {
                double balance = Double.parseDouble(balanceField.getText());
                if (balance < 0) {
                    JOptionPane.showMessageDialog(this, "Balance cannot be negative.");
                    return;
                }
                
                customer.name = name;
                customer.balance = balance;
                
                // Update the list model to reflect changes
                ((MainFrame) parent).customerList.repaint();
                
                dispose();
                
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid number for balance.");
            }
        });
        
        panel.add(new JLabel(""));
        panel.add(updateBtn);
        
        add(panel);
    }
}

class CustomerInventoryDialog extends JDialog {
    public CustomerInventoryDialog(JFrame parent, Customer customer) {
        super(parent, "Inventory: " + customer.name, true);
        setSize(300, 300);
        setLocationRelativeTo(parent);
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(SupplyChainSystemGUI.backgroundColor);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        DefaultListModel<String> model = new DefaultListModel<>();
        for (Map.Entry<String, Integer> entry : customer.inventory.entrySet()) {
            model.addElement(entry.getKey() + ": " + entry.getValue());
        }
        
        JList<String> list = new JList<>(model);
        JScrollPane scrollPane = new JScrollPane(list);
        
        JLabel balanceLabel = new JLabel("Balance: " + String.format("%.2f", customer.balance));
        
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(balanceLabel, BorderLayout.SOUTH);
        
        add(panel);
    }
}

class ShopDialog extends JDialog {
    Customer customer;
    Map<String, Market> productMarketMap = new HashMap<>();
    JComboBox<String> productSelector;
    JLabel stockLabel;
    JLabel priceLabel;
    JLabel balanceLabel;
    
    public ShopDialog(JFrame parent, Customer customer) {
        super(parent, "Shop: " + customer.name, true);
        setSize(400, 250);
        setLocationRelativeTo(parent);
        
        this.customer = customer;
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(SupplyChainSystemGUI.backgroundColor);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Customer Info
        JPanel customerPanel = new JPanel(new GridLayout(1, 2));
        customerPanel.setBackground(SupplyChainSystemGUI.backgroundColor);
        
        balanceLabel = new JLabel("Balance: " + String.format("%.2f", customer.balance));
        customerPanel.add(new JLabel("Customer: " + customer.name));
        customerPanel.add(balanceLabel);
        
        // Product selector
        JPanel selectorPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        selectorPanel.setBackground(SupplyChainSystemGUI.backgroundColor);
        
        productSelector = new JComboBox<>();
        populateProductSelector();
        
        stockLabel = new JLabel("Stock: 0");
        priceLabel = new JLabel("Price: 0.00");
        
        productSelector.addActionListener(e -> {
            String selected = (String) productSelector.getSelectedItem();
            if (selected != null) {
                Market m = productMarketMap.get(selected);
                String product = selected.split(" \\(")[0];
                int stock = m.stock.getOrDefault(product, 0);
                double price = m.getPrice(product);
                
                stockLabel.setText("Stock: " + stock);
                priceLabel.setText("Price: " + String.format("%.2f", price));
            }
        });
        
        selectorPanel.add(new JLabel("Product: "));
        selectorPanel.add(productSelector);
        selectorPanel.add(new JLabel("Stock: "));
        selectorPanel.add(stockLabel);
        selectorPanel.add(new JLabel("Price: "));
        selectorPanel.add(priceLabel);
        
        // Buy Panel
        JPanel buyPanel = new JPanel();
        buyPanel.setBackground(SupplyChainSystemGUI.backgroundColor);
        
        JLabel amountLabel = new JLabel("Amount: ");
        JTextField amountField = new JTextField("1", 5);
        JButton buyBtn = new JButton("Buy");
        
        buyPanel.add(amountLabel);
        buyPanel.add(amountField);
        buyPanel.add(buyBtn);
        
        // Buy Button Action
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
                
                // Use our enhanced Customer buyProduct method
                boolean success = customer.buyProduct(product, amount, price, m);
                if (success) {
                    // Update UI after successful purchase
                    balanceLabel.setText("Balance: " + String.format("%.2f", customer.balance));
                    int newStock = m.stock.getOrDefault(product, 0);
                    stockLabel.setText("Stock: " + newStock);
                    
                    // Show success message
                    JOptionPane.showMessageDialog(this, "Purchase successful!");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid number for the amount.");
            }
        });
        
        panel.add(customerPanel, BorderLayout.NORTH);
        panel.add(selectorPanel, BorderLayout.CENTER);
        panel.add(buyPanel, BorderLayout.SOUTH);
        
        add(panel);
        
        // Initialize display
        if (productSelector.getItemCount() > 0) {
            productSelector.setSelectedIndex(0);
        }
    }
    
    private void populateProductSelector() {
        for (Market m : SupplyChainSystemGUI.markets) {
            for (Map.Entry<String, Integer> entry : m.stock.entrySet()) {
                String product = entry.getKey();
                if (entry.getValue() > 0 && m.getPrice(product) > 0) {
                    String item = product + " (" + m.name + ")";
                    productSelector.addItem(item);
                    productMarketMap.put(item, m);
                }
            }
        }
    }
}

class DestroyByProductDialog extends JDialog {
    public DestroyByProductDialog(JFrame parent, Customer customer) {
        super(parent, "Destroy Products: " + customer.name, true);
        setSize(300, 200);
        setLocationRelativeTo(parent);
        
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(SupplyChainSystemGUI.backgroundColor);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Product selector
        JComboBox<String> productSelector = new JComboBox<>();
        for (String product : customer.inventory.keySet()) {
            productSelector.addItem(product + " (" + customer.inventory.get(product) + ")");
        }
        
        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        inputPanel.setBackground(SupplyChainSystemGUI.backgroundColor);
        
        JLabel amountLabel = new JLabel("Amount: ");
        JTextField amountField = new JTextField("1");
        
        inputPanel.add(new JLabel("Product: "));
        inputPanel.add(productSelector);
        inputPanel.add(amountLabel);
        inputPanel.add(amountField);
        
        JButton destroyBtn = new JButton("Destroy");
        
        destroyBtn.addActionListener(e -> {
            String selected = (String) productSelector.getSelectedItem();
            if (selected != null) {
                String product = selected.split(" \\(")[0];
                try {
                    int amount = Integer.parseInt(amountField.getText());
                    int available = customer.inventory.getOrDefault(product, 0);
                    
                    if (amount <= 0) {
                        JOptionPane.showMessageDialog(this, "Amount must be greater than 0.");
                        return;
                    }
                    
                    if (amount > available) {
                        JOptionPane.showMessageDialog(this, "Not enough products. Have " + available + ", trying to destroy " + amount);
                        return;
                    }
                    
                    // Destroy products
                    int newAmount = available - amount;
                    if (newAmount > 0) {
                        customer.inventory.put(product, newAmount);
                    } else {
                        customer.inventory.remove(product);
                    }
                    
                    JOptionPane.showMessageDialog(this, "Successfully destroyed " + amount + " " + product + "(s).");
                    dispose();
                    
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Please enter a valid number for amount.");
                }
            }
        });
        
        panel.add(inputPanel, BorderLayout.CENTER);
        panel.add(destroyBtn, BorderLayout.SOUTH);
        
        add(panel);
    }
}

class FactoryPanelHelper {
    public static void destroyMaterials(FactoryExtended factory, String material, int amount) {
        int available = factory.materials.getOrDefault(material, 0);
        if (amount > available) {
            JOptionPane.showMessageDialog(null, "Not enough materials. Have " + available + ", trying to destroy " + amount);
            return;
        }
        
        int newAmount = available - amount;
        if (newAmount > 0) {
            factory.materials.put(material, newAmount);
        } else {
            factory.materials.remove(material);
        }
        
        JOptionPane.showMessageDialog(null, "Successfully destroyed " + amount + " " + material + ".");
    }
    
    public static void destroyProducts(FactoryExtended factory, String product, int amount) {
        InventoryItem item = factory.products.get(product);
        if (item == null || item.quantity < amount) {
            JOptionPane.showMessageDialog(null, "Not enough products to destroy.");
            return;
        }
        
        item.quantity -= amount;
        if (item.quantity == 0) {
            factory.products.remove(product);
        }
        
        JOptionPane.showMessageDialog(null, "Successfully destroyed " + amount + " " + product + "(s).");
    }
}