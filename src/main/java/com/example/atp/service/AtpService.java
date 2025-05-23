// src/main/java/com/example/atp/service/
package com.example.atp.service;

import com.example.atp.model.AtpRequest;
import com.example.atp.model.AtpRequestItem;
import com.example.atp.model.AtpResponse;
import com.example.atp.model.AtpResultItem;
import com.example.atp.model.Customer;
import com.example.atp.model.Product;
import com.example.atp.model.Warehouse;
import com.example.atp.repository.InMemoryDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service

public class AtpService {
    @Autowired
    private InMemoryDataRepository dataRepository;

    public AtpResponse checkAvailability(AtpRequest request) {
        Customer customer = dataRepository.getCustomer(request.getCustomerId());
        if (customer == null) {
            // Handle error: customer not found
            return new AtpResponse(UUID.randomUUID().toString(), new ArrayList<>(), "CUSTOMER_NOT_FOUND");
        }

        List<AtpResultItem> results = new ArrayList<>();
        int totalConfirmedItems = 0;
        int totalRequestedItems = 0;

        for (AtpRequestItem item : request.getItems()) {
            totalRequestedItems++;
            Product product = dataRepository.getProduct(item.getProductId());
            if (product == null) {
                results.add(new AtpResultItem(item.getProductId(), null, item.getRequestedQuantity(), 0, null, null, "PRODUCT_NOT_FOUND"));
                continue;
            }

            List<String> preferredWarehouses = dataRepository.findSourcingWarehouses(
                    customer.getRegion(),
                    request.getOrderType(),
                    product.getMarking()
            );

            AtpResultItem resultItem = processSingleItem(item, product, preferredWarehouses);
            results.add(resultItem);
            if (resultItem.getConfirmedQuantity() > 0) {
                totalConfirmedItems++;
            }
        }

        String overallStatus;
        if (totalConfirmedItems == 0 && totalRequestedItems > 0) {
            overallStatus = "NONE_CONFIRMED";
        } else if (totalConfirmedItems < totalRequestedItems) {
            overallStatus = "PARTIALLY_CONFIRMED";
        } else if (totalRequestedItems == 0) {
            overallStatus = "NO_ITEMS_REQUESTED";
        }
        else {
            overallStatus = "ALL_CONFIRMED";
        }

        return new AtpResponse(UUID.randomUUID().toString(), results, overallStatus);
    }

    private AtpResultItem processSingleItem(AtpRequestItem requestItem, Product originalProduct, List<String> preferredWarehouseIds) {
        int remainingQuantityToFulfill = requestItem.getRequestedQuantity();
        
        // Try original product first
        for (String whId : preferredWarehouseIds) {
            Warehouse warehouse = dataRepository.getWarehouse(whId);
            if (warehouse == null) continue;

            int stock = dataRepository.getStock(originalProduct.getId(), whId);
            if (stock > 0) {
                int fulfillable = Math.min(stock, remainingQuantityToFulfill);
                if (dataRepository.reduceStock(originalProduct.getId(), whId, fulfillable)) { // Attempt to reserve stock
                    LocalDate shipDate = LocalDate.now().plusDays(warehouse.getProcessingLeadTimeDays());
                    return new AtpResultItem(originalProduct.getId(), originalProduct.getId(),
                            requestItem.getRequestedQuantity(), fulfillable, whId, shipDate,
                            fulfillable == requestItem.getRequestedQuantity() ? "Fulfilled" : "Partially fulfilled");
                }
            }
        }

        // If original product not found or not enough, try substitutes
        List<String> substituteProductIds = dataRepository.findSubstitutes(originalProduct.getId());
        for (String subProductId : substituteProductIds) {
            Product substituteProduct = dataRepository.getProduct(subProductId);
            if (substituteProduct == null) continue;

            for (String whId : preferredWarehouseIds) {
                Warehouse warehouse = dataRepository.getWarehouse(whId);
                if (warehouse == null) continue;

                int stock = dataRepository.getStock(substituteProduct.getId(), whId);
                if (stock > 0) {
                    int fulfillable = Math.min(stock, remainingQuantityToFulfill);
                     if (dataRepository.reduceStock(substituteProduct.getId(), whId, fulfillable)) {
                        LocalDate shipDate = LocalDate.now().plusDays(warehouse.getProcessingLeadTimeDays());
                        return new AtpResultItem(originalProduct.getId(), substituteProduct.getId(),
                                requestItem.getRequestedQuantity(), fulfillable, whId, shipDate,
                                "Fulfilled with substitute " + substituteProduct.getId());
                    }
                }
            }
        }

        // If no stock found for original or substitutes
        return new AtpResultItem(originalProduct.getId(), null, requestItem.getRequestedQuantity(),
                0, null, null, "No stock available for " + originalProduct.getId() + " or its substitutes.");
    }
}
