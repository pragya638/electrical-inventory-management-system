package com.wonder.electricals.inventory.service;

import com.wonder.electricals.inventory.dto.StockMovementRequest;
import com.wonder.electricals.inventory.dto.response.InventoryResponse;
import com.wonder.electricals.inventory.dto.response.StockTransactionResponse;
import com.wonder.electricals.inventory.entity.Inventory;
import com.wonder.electricals.inventory.entity.Plant;
import com.wonder.electricals.inventory.entity.Product;
import com.wonder.electricals.inventory.entity.StockTransaction;
import com.wonder.electricals.inventory.entity.TransactionType;
import com.wonder.electricals.inventory.exception.InsufficientStockException;
import com.wonder.electricals.inventory.exception.OperationNotAllowedException;
import com.wonder.electricals.inventory.exception.ResourceNotFoundException;
import com.wonder.electricals.inventory.repository.InventoryRepository;
import com.wonder.electricals.inventory.repository.PlantRepository;
import com.wonder.electricals.inventory.repository.ProductRepository;
import com.wonder.electricals.inventory.repository.StockTransactionRepository;
import com.wonder.electricals.inventory.security.SecurityUtils;
import com.wonder.electricals.inventory.util.DtoMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The only place where stock quantities are allowed to change.
 *
 * <p>Both movements run inside one database transaction: the inventory row and the
 * matching {@link StockTransaction} audit row are written together, so the two can
 * never drift apart. Any failure rolls the whole unit of work back.
 */
@Service
public class StockService {

    private static final Logger log = LoggerFactory.getLogger(StockService.class);

    private final ProductRepository productRepository;
    private final PlantRepository plantRepository;
    private final InventoryRepository inventoryRepository;
    private final StockTransactionRepository transactionRepository;
    private final DtoMapper mapper;

    public StockService(ProductRepository productRepository,
                        PlantRepository plantRepository,
                        InventoryRepository inventoryRepository,
                        StockTransactionRepository transactionRepository,
                        DtoMapper mapper) {
        this.productRepository = productRepository;
        this.plantRepository = plantRepository;
        this.inventoryRepository = inventoryRepository;
        this.transactionRepository = transactionRepository;
        this.mapper = mapper;
    }

    /**
     * Business rule 1: stock IN increases the quantity, creating the inventory row when
     * the product has never been stocked at that plant before.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public StockTransactionResponse stockIn(StockMovementRequest request) {
        Product product = requireActiveProduct(request.getProductId());
        Plant plant = requireActivePlant(request.getPlantId());
        int quantity = requirePositiveQuantity(request.getQuantity());

        Inventory inventory = inventoryRepository
                .findByProductIdAndPlantId(product.getId(), plant.getId())
                .orElseGet(() -> newInventory(product, plant));

        inventory.increaseBy(quantity);
        Inventory saved = inventoryRepository.save(inventory);

        StockTransaction transaction = recordMovement(product, plant, TransactionType.IN,
                quantity, request);

        log.info("Stock IN: {} x{} at {} -> new quantity {}",
                product.getProductCode(), quantity, plant.getName(), saved.getQuantity());
        return mapper.toTransactionResponse(transaction);
    }

    /**
     * Business rules 2, 3 and 4: stock OUT decreases the quantity but is rejected when
     * the request exceeds what is on hand, which is what stops stock from going negative.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public StockTransactionResponse stockOut(StockMovementRequest request) {
        Product product = requireActiveProduct(request.getProductId());
        Plant plant = requireActivePlant(request.getPlantId());
        int quantity = requirePositiveQuantity(request.getQuantity());

        Inventory inventory = inventoryRepository
                .findByProductIdAndPlantId(product.getId(), plant.getId())
                .orElseThrow(() -> new InsufficientStockException(
                        product.getProductCode(), plant.getName(), 0, quantity));

        if (quantity > inventory.getQuantity()) {
            // Nothing is written: the transaction rolls back untouched.
            throw new InsufficientStockException(product.getProductCode(), plant.getName(),
                    inventory.getQuantity(), quantity);
        }

        inventory.decreaseBy(quantity);
        Inventory saved = inventoryRepository.save(inventory);

        StockTransaction transaction = recordMovement(product, plant, TransactionType.OUT,
                quantity, request);

        log.info("Stock OUT: {} x{} at {} -> new quantity {}",
                product.getProductCode(), quantity, plant.getName(), saved.getQuantity());
        return mapper.toTransactionResponse(transaction);
    }

    /** Current stock of a product at a plant, or null when it was never stocked. */
    @Transactional(readOnly = true)
    public InventoryResponse currentStock(Long productId, Long plantId) {
        return inventoryRepository.findByProductIdAndPlantId(productId, plantId)
                .map(mapper::toInventoryResponse)
                .orElse(null);
    }

    private Inventory newInventory(Product product, Plant plant) {
        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setPlant(plant);
        inventory.initialiseQuantity(0);
        return inventory;
    }

    private StockTransaction recordMovement(Product product, Plant plant, TransactionType type,
                                           int quantity, StockMovementRequest request) {
        StockTransaction transaction = new StockTransaction();
        transaction.setProduct(product);
        transaction.setPlant(plant);
        transaction.setTransactionType(type);
        transaction.setQuantity(quantity);
        transaction.setReferenceNumber(request.getReferenceNumber());
        transaction.setRemarks(request.getRemarks());
        transaction.setPerformedBy(SecurityUtils.currentUsername());
        return transactionRepository.save(transaction);
    }

    private Product requireActiveProduct(Long productId) {
        Product product = productRepository.findByIdWithCategory(productId)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", productId));
        if (!product.isActive()) {
            throw new OperationNotAllowedException(
                    "Product " + product.getProductCode() + " is deactivated and cannot be moved");
        }
        return product;
    }

    private Plant requireActivePlant(Long plantId) {
        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> ResourceNotFoundException.of("Plant", plantId));
        if (!plant.isActive()) {
            throw new OperationNotAllowedException(
                    "Plant " + plant.getName() + " is deactivated and cannot be used");
        }
        return plant;
    }

    /**
     * Defence in depth: the DTO already enforces {@code @Positive}, but a non-positive
     * quantity must never reach the arithmetic below even if validation is bypassed.
     */
    private int requirePositiveQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new OperationNotAllowedException("Quantity must be greater than zero");
        }
        return quantity;
    }
}
