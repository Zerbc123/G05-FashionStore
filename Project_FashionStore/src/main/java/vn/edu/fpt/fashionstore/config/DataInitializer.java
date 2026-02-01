package vn.edu.fpt.fashionstore.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import vn.edu.fpt.fashionstore.entity.Product;
import vn.edu.fpt.fashionstore.repository.ProductRepository;

import java.util.Arrays;

@Component
public class DataInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;

    public DataInitializer(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Chỉ khởi tạo dữ liệu nếu database trống
        if (productRepository.count() == 0) {
            initializeSampleData();
        }
    }

    private void initializeSampleData() {
        // Tạo dữ liệu mẫu cho sản phẩm (không set ID - để DB tự generate)
        Product[] products = {
            new Product("Classic White T-Shirt", "M", 29.99, "men", false, true),
            new Product("Blue Denim Jeans", "L", 79.99, "men", false, false),
            new Product("Black Leather Jacket", "XL", 199.99, "men", true, false),
            new Product("Striped Polo Shirt", "M", 49.99, "men", false, true),
            new Product("Cargo Shorts", "L", 39.99, "men", false, false),
            new Product("Sneakers", "M", 89.99, "men", true, true),
            new Product("Wool Sweater", "L", 69.99, "men", false, false),
            new Product("Baseball Cap", "One Size", 24.99, "men", false, true),
            new Product("Sports Watch", "One Size", 149.99, "men", true, false),
            new Product("Chinos", "M", 59.99, "men", false, true),
            new Product("Hoodie", "L", 54.99, "men", false, false),
            new Product("Canvas Belt", "One Size", 34.99, "men", false, true),

            new Product("Floral Summer Dress", "M", 89.99, "women", true, false),
            new Product("High Heels", "S", 129.99, "women", false, true),
            new Product("Leather Handbag", "One Size", 199.99, "women", true, false),
            new Product("Silk Blouse", "M", 79.99, "women", false, true),
            new Product("Skinny Jeans", "S", 69.99, "women", false, false),
            new Product("Running Shoes", "M", 99.99, "women", true, true),
            new Product("Wool Coat", "M", 249.99, "women", false, false),
            new Product("Scarf", "One Size", 39.99, "women", false, true),
            new Product("Earrings", "One Size", 49.99, "women", true, false),
            new Product("Yoga Pants", "S", 54.99, "women", true, false),
            new Product("Maxi Dress", "M", 119.99, "women", true, false),
            new Product("Tank Top", "S", 24.99, "women", true, false),

            new Product("Kids T-Shirt", "S", 19.99, "kids", false, true),
            new Product("Kids Jeans", "M", 34.99, "kids", false, false),
            new Product("School Backpack", "One Size", 44.99, "kids", true, false),
            new Product("Sneakers Kids", "S", 49.99, "kids", false, true),
            new Product("Rain Jacket", "M", 39.99, "kids", false, false),
            new Product("Toy Set", "One Size", 29.99, "kids", true, true),
            new Product("Kids Dress", "S", 29.99, "kids", false, false),
            new Product("Baseball Cap Kids", "M", 19.99, "kids", false, true),
            new Product("Socks Pack", "One Size", 14.99, "kids", false, false),
            new Product("Pencil Case", "One Size", 12.99, "kids", true, false),
            new Product("Kids Sunglasses", "One Size", 19.99, "kids", false, true),
            new Product("Kids Watch", "M", 34.99, "kids", true, false),

            new Product("Leather Wallet", "One Size", 59.99, "accessories", false, true),
            new Product("Sunglasses", "One Size", 89.99, "accessories", true, false),
            new Product("Watch", "One Size", 299.99, "accessories", false, false),
            new Product("Belt", "One Size", 44.99, "accessories", false, true),
            new Product("Hat", "One Size", 34.99, "accessories", true, false),
            new Product("Gloves", "M", 24.99, "accessories", false, false),
            new Product("Scarf Silk", "One Size", 69.99, "accessories", false, true),
            new Product("Phone Case", "One Size", 24.99, "accessories", false, false),
            new Product("Keychain", "One Size", 14.99, "accessories", true, false),
            new Product("Backpack", "One Size", 119.99, "accessories", true, false),
            new Product("Clutch Bag", "One Size", 89.99, "accessories", true, false),
            new Product("Tie", "One Size", 44.99, "accessories", false, false)
        };

        productRepository.saveAll(Arrays.asList(products));
        System.out.println("✓ Đã khởi tạo " + products.length + " sản phẩm mẫu");
    }
}
