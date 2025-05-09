class Customer {
    String name;
    double balance;
    Map<String, Integer> inventory = new HashMap<>();
    List<String> purchaseHistory = new ArrayList<>();

    public Customer(String name, double balance) {
        this.name = name;
        this.balance = balance;
    }

    public void addProduct(String product, int amount) {
        inventory.put(product, inventory.getOrDefault(product, 0) + amount);
    }
    
    public void buyProduct(String product, int amount, double pricePerUnit, Market market) throws Exception {
        int availableStock = market.stock.getOrDefault(product, 0);
        if (availableStock < amount) {
            throw new Exception("Not enough stock in market: requested " + amount + ", available " + availableStock);
        }

        double totalCost = amount * pricePerUnit;
        if (balance < totalCost) {
            throw new Exception("Not enough balance: cost " + String.format("%.2f", totalCost) + 
                               ", available " + String.format("%.2f", balance));
        }

        // Successful purchase
        balance -= totalCost;
        market.balance += totalCost;
        market.stock.put(product, availableStock - amount);
        addProduct(product, amount);
        
        // Record purchase
        String purchase = String.format("Bought %d units of %s from %s for %.2f", 
                                      amount, product, market.name, totalCost);
        purchaseHistory.add(purchase);
    }
    
    public List<String> getPurchaseHistory() {
        return purchaseHistory;
    }

    public String toString() {
        return name + " (Balance: " + String.format("%.2f", balance) + ")";
    }
}
