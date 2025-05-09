# Find and replace Customer class
/^class Customer \{/,/^}/ c\
class Customer {\
    String name;\
    double balance;\
    Map<String, Integer> inventory = new HashMap<>();\
\
    public Customer(String name, double balance) {\
        this.name = name;\
        this.balance = balance;\
    }\
\
    public void addProduct(String product, int amount) {\
        inventory.put(product, inventory.getOrDefault(product, 0) + amount);\
    }\
    \
    // Enhanced buyProduct method with proper exception handling\
    public void buyProduct(String product, int amount, double price, Market market) throws Exception {\
        if (!market.stock.containsKey(product)) {\
            throw new Exception("Product not available in this market");\
        }\
        \
        int availableStock = market.stock.getOrDefault(product, 0);\
        if (availableStock < amount) {\
            throw new Exception("Not enough stock in market: requested " + amount + ", available " + availableStock);\
        }\
\
        double totalCost = amount * price;\
        if (balance < totalCost) {\
            throw new Exception("Not enough balance: cost " + String.format("%.2f", totalCost) + \
                               ", available " + String.format("%.2f", balance));\
        }\
\
        // Complete successful purchase\
        balance -= totalCost;\
        market.balance += totalCost;  // Economic transfer - market receives payment\
        market.stock.put(product, availableStock - amount);\
        addProduct(product, amount);\
    }\
\
    public String toString() {\
        return name + " (Balance: " + String.format("%.2f", balance) + ")";\
    }\
}

# Find and replace Market class
/^class Market \{/,/^}/ c\
class Market {\
    String name;\
    double balance;\
    Map<String, Integer> stock = new HashMap<>();\
    Map<String, Double> prices = new HashMap<>();\
\
    public Market(String name, double balance) {\
        this.name = name;\
        this.balance = balance;\
    }\
\
    public void buyProduct(String product, int amount, double pricePerUnit, FactoryExtended factory) throws Exception {\
        if (!factory.products.containsKey(product)) {\
            throw new Exception("Product not available from this factory");\
        }\
\
        InventoryItem item = factory.products.get(product);\
        if (item.quantity < amount) {\
            throw new Exception("Not enough stock: requested " + amount + ", available " + item.quantity);\
        }\
\
        double totalCost = amount * pricePerUnit;\
        if (balance < totalCost) {\
            throw new Exception("Not enough balance: cost " + String.format("%.2f", totalCost) + \
                               ", available " + String.format("%.2f", balance));\
        }\
\
        // Successful transaction logic\
        item.quantity -= amount;\
        balance -= totalCost;\
        factory.balance += totalCost; // Pay the factory (adding economic flow)\
        \
        // Update stock\
        stock.put(product, stock.getOrDefault(product, 0) + amount);\
    }\
\
    public void setPrice(String product, double price) throws Exception {\
        if (price <= 0) {\
            throw new Exception("Price must be greater than zero");\
        }\
        prices.put(product, price);\
    }\
\
    public double getPrice(String product) {\
        return prices.getOrDefault(product, 0.0);\
    }\
\
    public String toString() {\
        return name + " (Balance: " + String.format("%.2f", balance) + ")";\
    }\
}
