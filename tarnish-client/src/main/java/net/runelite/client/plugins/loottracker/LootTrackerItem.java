package net.runelite.client.plugins.loottracker;

public class LootTrackerItem {

    private final int id;
    private final String name;
    private int quantity;
    private int pricePerItem;

    public LootTrackerItem(int id, String name, int quantity, int pricePerItem) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.pricePerItem = pricePerItem;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    public int getPricePerItem() { return pricePerItem; }

    public void addQuantity(int amount) { this.quantity += amount; }
    public void setPricePerItem(int price) { this.pricePerItem = price; }

    public int getTotalValue() { return pricePerItem * quantity; }
}