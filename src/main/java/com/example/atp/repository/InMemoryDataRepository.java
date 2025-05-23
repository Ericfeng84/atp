// src/main/java/com/example/atp/repository/
package com.example.atp.repository;

import com.example.atp.model.*; // Import all models
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryDataRepository {
    // 使用ConcurrentHashMap保证线程安全，虽然ATP检查本身可能需要更复杂的并发控制
    public final Map<String, Product> products = new ConcurrentHashMap<>();
    public final Map<String, Warehouse> warehouses = new ConcurrentHashMap<>();
    public final Map<String, Customer> customers = new ConcurrentHashMap<>();
    public final List<InventoryItem> inventory = new ArrayList<>(); // 需要同步访问
    public final List<SourcingRule> sourcingRules = new ArrayList<>();
    public final List<SubstitutionRule> substitutionRules = new ArrayList<>();

    @PostConstruct
    public void initData() {
        // Products
        products.put("PART-A", new Product("PART-A", "Standard Part A", PartMarking.NONE));
        products.put("PART-B", new Product("PART-B", "Critical Part B", PartMarking.CRITICAL));
        products.put("PART-C", new Product("PART-C", "Obsolete Part C (use PART-D)", PartMarking.OBSOLETE));
        products.put("PART-D", new Product("PART-D", "Substitute for Part C", PartMarking.NONE));
        products.put("PART-E", new Product("PART-E", "Part with no stock", PartMarking.NONE));


        // Warehouses
        warehouses.put("WH-SH", new Warehouse("WH-SH", "Shanghai Warehouse", Region.CN_EAST, 2));
        warehouses.put("WH-BJ", new Warehouse("WH-BJ", "Beijing Warehouse", Region.CN_NORTH, 3));
        warehouses.put("WH-GZ", new Warehouse("WH-GZ", "Guangzhou Warehouse", Region.CN_SOUTH, 2));
        warehouses.put("WH-CD", new Warehouse("WH-CD", "Chengdu Warehouse", Region.CN_WEST, 4));
        warehouses.put("WH-GLOBAL", new Warehouse("WH-GLOBAL", "Global Backup Warehouse", null, 5)); // null region for fallback

        // Customers
        customers.put("CUST-001", new Customer("CUST-001", "East China Customer", Region.CN_EAST));
        customers.put("CUST-002", new Customer("CUST-002", "North China Customer", Region.CN_NORTH));

        // Inventory (productId, warehouseId, quantity) - synchronized access needed for updates
        synchronized (inventory) {
            inventory.add(new InventoryItem("PART-A", "WH-SH", 100));
            inventory.add(new InventoryItem("PART-A", "WH-BJ", 50));
            inventory.add(new InventoryItem("PART-B", "WH-SH", 20)); // Critical part in Shanghai
            inventory.add(new InventoryItem("PART-B", "WH-GLOBAL", 5)); // Critical part backup
            inventory.add(new InventoryItem("PART-D", "WH-SH", 200)); // Substitute for PART-C
            inventory.add(new InventoryItem("PART-D", "WH-GZ", 150));
        }


        // Sourcing Rules
        // Rule 1: East China customer, standard order, non-critical part -> SH, then GZ, then BJ
        sourcingRules.add(new SourcingRule(Region.CN_EAST, OrderType.STANDARD, PartMarking.NONE,
                Arrays.asList("WH-SH", "WH-GZ", "WH-BJ", "WH-GLOBAL")));
        // Rule 2: East China customer, urgent order, non-critical part -> SH, GZ, BJ, CD (wider search)
        sourcingRules.add(new SourcingRule(Region.CN_EAST, OrderType.URGENT, PartMarking.NONE,
                Arrays.asList("WH-SH", "WH-GZ", "WH-BJ", "WH-CD", "WH-GLOBAL")));
        // Rule 3: North China customer, any order type, non-critical part -> BJ, then SH
        sourcingRules.add(new SourcingRule(Region.CN_NORTH, OrderType.STANDARD, PartMarking.NONE,
                Arrays.asList("WH-BJ", "WH-SH", "WH-GLOBAL")));
         sourcingRules.add(new SourcingRule(Region.CN_NORTH, OrderType.URGENT, PartMarking.NONE,
                Arrays.asList("WH-BJ", "WH-SH", "WH-GZ", "WH-GLOBAL")));
        // Rule 4: Critical Part B for East China urgent orders -> SH first, then Global (specific part marking)
        sourcingRules.add(new SourcingRule(Region.CN_EAST, OrderType.URGENT, PartMarking.CRITICAL,
                Arrays.asList("WH-SH", "WH-GLOBAL")));
        // Fallback rule (less specific, could be a global rule if no region match)
        sourcingRules.add(new SourcingRule(null, OrderType.STANDARD, PartMarking.NONE,
                Arrays.asList("WH-GLOBAL", "WH-SH", "WH-BJ")));


        // Substitution Rules
        substitutionRules.add(new SubstitutionRule("PART-C", "PART-D"));
    }

    public Product getProduct(String id) { return products.get(id); }
    public Warehouse getWarehouse(String id) { return warehouses.get(id); }
    public Customer getCustomer(String id) { return customers.get(id); }

    public int getStock(String productId, String warehouseId) {
        synchronized (inventory) {
            return inventory.stream()
                    .filter(item -> item.getProductId().equals(productId) && item.getWarehouseId().equals(warehouseId))
                    .mapToInt(InventoryItem::getQuantity)
                    .sum(); // Should ideally be unique, sum handles if not
        }
    }

    // IMPORTANT: This is a simplified stock reduction.
    // In a real system, this needs to be transactional and handle concurrency properly.
    public boolean reduceStock(String productId, String warehouseId, int quantityToReduce) {
        synchronized (inventory) {
            for (InventoryItem item : inventory) {
                if (item.getProductId().equals(productId) && item.getWarehouseId().equals(warehouseId)) {
                    if (item.getQuantity() >= quantityToReduce) {
                        item.setQuantity(item.getQuantity() - quantityToReduce);
                        return true;
                    } else {
                        return false; // Not enough stock
                    }
                }
            }
            return false; // Item not found in that warehouse
        }
    }

    public List<String> findSubstitutes(String originalProductId) {
        return substitutionRules.stream()
                .filter(rule -> rule.getOriginalProductId().equals(originalProductId))
                .map(SubstitutionRule::getSubstituteProductId)
                .collect(Collectors.toList());
    }

    public List<String> findSourcingWarehouses(Region customerRegion, OrderType orderType, PartMarking partMarking) {
        // Find most specific rule first
        // 1. Match all: customerRegion, orderType, partMarking
        // 2. Match customerRegion, orderType (partMarking is NONE/null in rule)
        // 3. Match customerRegion (orderType is any, partMarking is NONE/null) - not implemented for brevity
        // 4. Match orderType (customerRegion is null, partMarking is NONE/null) - not implemented for brevity
        // 5. Default global rule (all null)

        // Simplified:
        // Priority 1: Match customerRegion, orderType, partMarking
        List<String> warehouses = sourcingRules.stream()
                .filter(r -> r.getCustomerRegion() == customerRegion &&
                              r.getOrderType() == orderType &&
                              r.getPartMarking() == partMarking)
                .findFirst()
                .map(SourcingRule::getPreferredWarehouseIds)
                .orElse(null);

        if (warehouses != null) return warehouses;

        // Priority 2: Match customerRegion, orderType (and rule's partMarking is general like NONE)
        warehouses = sourcingRules.stream()
                .filter(r -> r.getCustomerRegion() == customerRegion &&
                              r.getOrderType() == orderType &&
                              (r.getPartMarking() == PartMarking.NONE || r.getPartMarking() == null))
                .findFirst()
                .map(SourcingRule::getPreferredWarehouseIds)
                .orElse(null);
        
        if (warehouses != null) return warehouses;

        // Fallback to a generic rule if any (e.g., customerRegion is null, orderType is standard)
         warehouses = sourcingRules.stream()
                .filter(r -> r.getCustomerRegion() == null &&
                              r.getOrderType() == OrderType.STANDARD && // Example fallback
                              (r.getPartMarking() == PartMarking.NONE || r.getPartMarking() == null))
                .findFirst()
                .map(SourcingRule::getPreferredWarehouseIds)
                .orElse(List.of("WH-GLOBAL")); // Ultimate fallback

        return warehouses;
    }
}
