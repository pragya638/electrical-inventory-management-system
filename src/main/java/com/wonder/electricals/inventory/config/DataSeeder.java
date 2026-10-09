package com.wonder.electricals.inventory.config;

import com.wonder.electricals.inventory.dto.StockMovementRequest;
import com.wonder.electricals.inventory.entity.Category;
import com.wonder.electricals.inventory.entity.Plant;
import com.wonder.electricals.inventory.entity.Product;
import com.wonder.electricals.inventory.entity.Role;
import com.wonder.electricals.inventory.entity.Supplier;
import com.wonder.electricals.inventory.entity.User;
import com.wonder.electricals.inventory.repository.CategoryRepository;
import com.wonder.electricals.inventory.repository.PlantRepository;
import com.wonder.electricals.inventory.repository.ProductRepository;
import com.wonder.electricals.inventory.repository.SupplierRepository;
import com.wonder.electricals.inventory.repository.UserRepository;
import com.wonder.electricals.inventory.service.StockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads demonstration data on first start so the project can be demonstrated without
 * manual setup.
 *
 * <p>Two safety properties:
 * <ul>
 *   <li>It only runs when the users table is empty, so restarting the application never
 *       duplicates rows or overwrites user edits.</li>
 *   <li>Every organisation, plant, supplier and product name below is invented
 *       demonstration data. Nothing here reflects a real company record.</li>
 * </ul>
 *
 * <p>Opening balances and issues are created by calling {@link StockService}, so the
 * seeded stock passes through exactly the same business rules, validation and audit
 * trail as a movement made through the UI.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    /** Shared demo password for the three seeded accounts. Documented in the README. */
    public static final String DEMO_PASSWORD = "Admin@123";

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PlantRepository plantRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;
    private final StockService stockService;
    private final boolean enabled;

    public DataSeeder(UserRepository userRepository,
                      CategoryRepository categoryRepository,
                      PlantRepository plantRepository,
                      SupplierRepository supplierRepository,
                      ProductRepository productRepository,
                      PasswordEncoder passwordEncoder,
                      StockService stockService,
                      @Value("${app.seed.enabled:true}") boolean enabled) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.plantRepository = plantRepository;
        this.supplierRepository = supplierRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
        this.stockService = stockService;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Demo data seeding is disabled (app.seed.enabled=false)");
            return;
        }
        if (userRepository.count() > 0) {
            log.info("Database already contains data - skipping demo data seeding");
            return;
        }
        seed();
        log.info("Demo data seeded. Every seeded record is demonstration data only.");
    }

    private void seed() {
        seedUsers();

        Category cables = category("Cables", "Copper and aluminium power and control cables");
        Category wires = category("Wires", "Household and industrial wiring products");
        Category switches = category("Switches", "Modular switches, sockets and accessories");
        Category accessories = category("Electrical Accessories", "MCB, distribution boards and fittings");
        Category lighting = category("Lighting Products", "LED bulbs, tubes, panels and fittings");

        Plant plantA = plant("Plant A - Kashipur (DEMO)", "Kashipur, Uttarakhand",
                "Demo Supervisor A", "9000000001");
        Plant plantB = plant("Plant B - Kucharam (DEMO)", "Kucharam, Telangana",
                "Demo Supervisor B", "9000000002");
        Plant plantC = plant("Plant C - Haridwar (DEMO)", "Haridwar, Uttarakhand",
                "Demo Supervisor C", "9000000003");

        supplier("SUP-001", "Demo Cable Traders", "Demo Contact A",
                "orders@demo-cables.example", "9001000001", "Demo address, Kashipur");
        supplier("SUP-002", "Demo Switch Works", "Demo Contact B",
                "sales@demo-switches.example", "9001000002", "Demo address, Kucharam");
        supplier("SUP-003", "Demo Lighting Supply Co", "Demo Contact C",
                "info@demo-lighting.example", "9001000003", "Demo address, Haridwar");

        Product cbl1 = product("CBL-001", "1.5 sq mm Copper Cable (DEMO)", cables, "Meter", 200);
        Product cbl2 = product("CBL-002", "2.5 sq mm Copper Cable (DEMO)", cables, "Meter", 150);
        Product cbl3 = product("CBL-003", "4 sq mm Aluminium Cable (DEMO)", cables, "Meter", 100);
        Product wir1 = product("WIR-001", "Single Core 1.0 sq mm Wire (DEMO)", wires, "Meter", 300);
        Product wir2 = product("WIR-002", "Twin and Earth 2.5 sq mm Wire (DEMO)", wires, "Meter", 250);
        Product swt1 = product("SWT-001", "Modular 6A Switch (DEMO)", switches, "Piece", 120);
        Product swt2 = product("SWT-002", "5A 2-Pin Socket (DEMO)", switches, "Piece", 150);
        Product acc1 = product("ACC-001", "32A Single Pole MCB (DEMO)", accessories, "Piece", 80);
        Product acc2 = product("ACC-002", "4-Way Modular DB (DEMO)", accessories, "Piece", 40);
        Product lgt1 = product("LGT-001", "9W LED Bulb (DEMO)", lighting, "Piece", 250);
        Product lgt2 = product("LGT-002", "18W LED Tube Light (DEMO)", lighting, "Piece", 180);
        Product lgt3 = product("LGT-003", "12W LED Panel (DEMO)", lighting, "Piece", 60);

        // ---- Opening receipts -------------------------------------------------
        in(cbl1, plantA, 900, "GRN-DEMO-1001");
        in(cbl1, plantB, 600, "GRN-DEMO-1002");
        in(cbl1, plantC, 450, "GRN-DEMO-1003");
        in(cbl2, plantA, 700, "GRN-DEMO-1004");
        in(cbl2, plantB, 400, "GRN-DEMO-1005");
        in(cbl2, plantC, 120, "GRN-DEMO-1006");
        in(cbl3, plantA, 350, "GRN-DEMO-1007");
        in(cbl3, plantC, 90, "GRN-DEMO-1008");
        in(wir1, plantA, 1200, "GRN-DEMO-1009");
        in(wir1, plantB, 800, "GRN-DEMO-1010");
        in(wir1, plantC, 200, "GRN-DEMO-1011");
        in(wir2, plantA, 900, "GRN-DEMO-1012");
        in(wir2, plantB, 600, "GRN-DEMO-1013");
        in(wir2, plantC, 500, "GRN-DEMO-1014");
        in(swt1, plantA, 500, "GRN-DEMO-1015");
        in(swt1, plantB, 350, "GRN-DEMO-1016");
        in(swt1, plantC, 110, "GRN-DEMO-1017");
        in(swt2, plantA, 400, "GRN-DEMO-1018");
        in(swt2, plantB, 300, "GRN-DEMO-1019");
        in(swt2, plantC, 260, "GRN-DEMO-1020");
        in(acc1, plantA, 260, "GRN-DEMO-1021");
        in(acc1, plantB, 180, "GRN-DEMO-1022");
        in(acc1, plantC, 60, "GRN-DEMO-1023");
        in(acc2, plantA, 150, "GRN-DEMO-1024");
        in(acc2, plantB, 90, "GRN-DEMO-1025");
        in(acc2, plantC, 70, "GRN-DEMO-1026");
        in(lgt1, plantA, 900, "GRN-DEMO-1027");
        in(lgt1, plantB, 600, "GRN-DEMO-1028");
        in(lgt1, plantC, 230, "GRN-DEMO-1029");
        in(lgt2, plantA, 500, "GRN-DEMO-1030");
        in(lgt2, plantB, 320, "GRN-DEMO-1031");
        in(lgt2, plantC, 165, "GRN-DEMO-1032");
        in(lgt3, plantA, 220, "GRN-DEMO-1033");
        in(lgt3, plantB, 30, "GRN-DEMO-1034");

        // ---- Issues -----------------------------------------------------------
        out(cbl1, plantA, 180, "ISS-DEMO-2001");
        out(cbl1, plantB, 90, "ISS-DEMO-2002");
        out(cbl2, plantA, 140, "ISS-DEMO-2003");
        out(cbl2, plantB, 75, "ISS-DEMO-2004");
        out(cbl3, plantA, 60, "ISS-DEMO-2005");
        out(wir1, plantA, 200, "ISS-DEMO-2006");
        out(wir2, plantA, 260, "ISS-DEMO-2007");
        out(swt1, plantA, 120, "ISS-DEMO-2008");
        // 350 - 250 = 100, reorder level is 120 -> low stock
        out(swt1, plantB, 250, "ISS-DEMO-2009");
        out(swt2, plantA, 95, "ISS-DEMO-2010");
        out(acc1, plantA, 70, "ISS-DEMO-2011");
        // 150 - 115 = 35, reorder level is 40 -> low stock
        out(acc2, plantA, 115, "ISS-DEMO-2012");
        out(lgt1, plantA, 310, "ISS-DEMO-2013");
        // 600 - 430 = 170, reorder level is 180 -> low stock
        out(lgt1, plantB, 430, "ISS-DEMO-2014");
        out(lgt2, plantA, 140, "ISS-DEMO-2015");
        out(lgt3, plantA, 65, "ISS-DEMO-2016");

        // Issuing the whole balance of this row leaves a real OUT OF STOCK example.
        out(acc2, plantC, 70, "ISS-DEMO-2017");
    }

    private void seedUsers() {
        saveUser("Demo Admin", "admin@demo.local", Role.ADMIN);
        saveUser("Demo Inventory Manager", "manager@demo.local", Role.INVENTORY_MANAGER);
        saveUser("Demo Viewer", "viewer@demo.local", Role.VIEWER);
    }

    private void saveUser(String name, String email, Role role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(DEMO_PASSWORD));
        user.setRole(role);
        user.setActive(true);
        userRepository.save(user);
    }

    private Category category(String name, String description) {
        Category category = new Category();
        category.setName(name);
        category.setDescription(description);
        category.setActive(true);
        return categoryRepository.save(category);
    }

    private Plant plant(String name, String location, String contactPerson, String contactNumber) {
        Plant plant = new Plant();
        plant.setName(name);
        plant.setLocation(location);
        plant.setContactPerson(contactPerson);
        plant.setContactNumber(contactNumber);
        plant.setActive(true);
        return plantRepository.save(plant);
    }

    private Supplier supplier(String code, String name, String contactPerson, String email,
                              String phone, String address) {
        Supplier supplier = new Supplier();
        supplier.setSupplierCode(code);
        supplier.setName(name);
        supplier.setContactPerson(contactPerson);
        supplier.setEmail(email);
        supplier.setPhone(phone);
        supplier.setAddress(address);
        supplier.setActive(true);
        return supplierRepository.save(supplier);
    }

    private Product product(String code, String name, Category category, String unit,
                            int reorderLevel) {
        Product product = new Product();
        product.setProductCode(code);
        product.setName(name);
        product.setCategory(category);
        product.setUnit(unit);
        product.setReorderLevel(reorderLevel);
        product.setDescription("Demonstration product record");
        product.setActive(true);
        return productRepository.save(product);
    }

    private void in(Product product, Plant plant, int quantity, String reference) {
        stockService.stockIn(movement(product, plant, quantity, reference, "Demo opening receipt"));
    }

    private void out(Product product, Plant plant, int quantity, String reference) {
        stockService.stockOut(movement(product, plant, quantity, reference, "Demo site issue"));
    }

    private StockMovementRequest movement(Product product, Plant plant, int quantity,
                                         String reference, String remarks) {
        StockMovementRequest request = new StockMovementRequest();
        request.setProductId(product.getId());
        request.setPlantId(plant.getId());
        request.setQuantity(quantity);
        request.setReferenceNumber(reference);
        request.setRemarks(remarks);
        return request;
    }
}
