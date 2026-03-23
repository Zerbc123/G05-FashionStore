package vn.edu.fpt.fashionstore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.fashionstore.entity.CartItem;
import vn.edu.fpt.fashionstore.entity.Customer;
import vn.edu.fpt.fashionstore.entity.ProductVariant;
import vn.edu.fpt.fashionstore.repository.CartRepository;
import vn.edu.fpt.fashionstore.repository.ProductVariantRepository;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    // Lấy tất cả sản phẩm trong giỏ hàng của một khách hàng
    @Transactional(readOnly = true)
    public List<CartItem> getCartItems(Customer customer) {
        if (customer == null) {
            return Collections.emptyList();
        }
        return cartRepository.findByCustomer(customer);
    }

    // Thêm sản phẩm vào giỏ hàng
    @Transactional
    public void addToCart(Customer customer, Integer variantId, int quantity) {
        if (quantity <= 0) {
            throw new RuntimeException("Số lượng sản phẩm phải lớn hơn 0!");
        }
        try {
        // Tìm ProductVariant từ ID
        ProductVariant productVariant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể sản phẩm!"));

        // Kiểm tra stock trước khi thêm vào giỏ
        validateStock(productVariant, quantity);

        // Kiểm tra xem sản phẩm này đã có trong giỏ hàng của khách chưa
        Optional<CartItem> existingCartItem = cartRepository.findByCustomerAndProductVariant(customer, productVariant);

        if (existingCartItem.isPresent()) {
            // Nếu đã có, kiểm tra tổng số lượng sau khi cập nhật
            CartItem cartItem = existingCartItem.get();
            int newQuantity = cartItem.getQuantity() + quantity;
            
            if (productVariant.getStock() < newQuantity) {
                throw new RuntimeException("Sản phẩm chỉ còn " + productVariant.getStock() + " sản phẩm. Bạn đã có " + cartItem.getQuantity() + " trong giỏ hàng, không thể thêm " + quantity + " sản phẩm nữa.");
            }
            
            cartItem.setQuantity(newQuantity);
            cartRepository.save(cartItem);
        } else {
            // Nếu chưa có, tạo mới một CartItem
            CartItem newCartItem = new CartItem();
            newCartItem.setCustomer(customer);
            newCartItem.setProductVariant(productVariant);
            newCartItem.setQuantity(quantity);
            cartRepository.save(newCartItem);
        }
    } catch (Exception e) {
        throw new RuntimeException("Lỗi khi thêm vào giỏ hàng: " + e.getMessage(), e);
    }
}

    // Cập nhật số lượng
    @Transactional
    public void updateQuantity(Integer cartItemId, int quantity) {
        CartItem cartItem = cartRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Cart item not found with id: " + cartItemId));

        if (quantity <= 0) {
            // Nếu số lượng là 0 hoặc âm, xóa sản phẩm
            cartRepository.delete(cartItem);
        } else {
            // Kiểm tra stock trước khi cập nhật số lượng
            ProductVariant variant = cartItem.getProductVariant();
            if (variant.getStock() < quantity) {
                throw new RuntimeException("Sản phẩm chỉ còn " + variant.getStock() + " sản phẩm. Bạn không thể cập nhật lên " + quantity + " sản phẩm.");
            }
            
            cartItem.setQuantity(quantity);
            cartRepository.save(cartItem);
        }
    }

    // Xóa sản phẩm khỏi giỏ hàng
    @Transactional
    public void removeFromCart(Integer cartItemId) {
        cartRepository.deleteById(cartItemId);
    }

    // Xóa toàn bộ giỏ hàng của khách hàng
    @Transactional
    public void clearCart(Customer customer) {
        List<CartItem> cartItems = cartRepository.findByCustomer(customer);
        cartRepository.deleteAll(cartItems);
    }

    // Tính tổng tiền giỏ hàng
    public double getCartTotal(List<CartItem> cartItems) {
        return cartItems.stream()
                .mapToDouble(item -> item.getProductVariant().getPrice() * item.getQuantity())
                .sum();
    }

    // Tìm variant dựa trên productId, sizeId và colorId
    public Integer findVariantByProductSizeColor(Long productId, Integer sizeId, Integer colorId) {
        try {
            List<ProductVariant> variants = productVariantRepository.findAll();
            
            Integer result = variants.stream()
                    .filter(v -> v.getProduct() != null && v.getProduct().getProductId().equals(productId))
                    .filter(v -> v.getCategorySize() != null && Objects.equals(v.getCategorySize().getCategorySizeId(), sizeId))
                    .filter(v -> v.getColor() != null && Objects.equals(v.getColor().getColorId(), colorId))
                    .map(ProductVariant::getVariantId)
                    .findFirst()
                    .orElse(null);
            
            return result;
        } catch (Exception e) {
            System.err.println("Error finding variant: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Lấy cart item theo ID
    public CartItem getCartItemById(Integer cartItemId) {
        return cartRepository.findById(cartItemId).orElse(null);
    }

    // Kiểm tra tồn kho
    public void validateStock(Integer variantId, int quantity) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể sản phẩm!"));
        validateStock(variant, quantity);
    }

    private void validateStock(ProductVariant variant, int quantity) {
        if (variant.getStock() < quantity) {
            throw new RuntimeException("Sản phẩm chỉ còn " + variant.getStock() + " sản phẩm. Bạn không thể mua " + quantity + " sản phẩm.");
        }
    }
}
