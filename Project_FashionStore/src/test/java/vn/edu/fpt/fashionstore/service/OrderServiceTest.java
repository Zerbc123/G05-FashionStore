package vn.edu.fpt.fashionstore.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderServiceTest {

    private CartService cartService;

    // chạy trước mỗi test
    @BeforeEach
    void setUp() {
        cartService = new CartService();
    }

    @Test
    @DisplayName("UC01 - Price=100, Quantity=2 → Total=200")
    void UC01() {
        double result = cartService.calculate(100, 2);
        assertEquals(200, result);
    }

    @Test
    @DisplayName("UC02 - Price=0, Quantity=3 → Total=0")
    void UC02() {
        double result = cartService.calculate(0, 3);
        assertEquals(0, result);
    }

    @Test
    @DisplayName("UC03 - Price=50.5, Quantity=2 → Total=101")
    void UC03() {
        double result = cartService.calculate(50.5, 2);
        assertEquals(101, result);
    }

    @Test
    @DisplayName("UC04 - Price < 0 → IllegalArgumentException")
    void UC04() {
        assertThrows(IllegalArgumentException.class, () -> {
            cartService.calculate(-10, 2);
        });
    }

    @Test
    @DisplayName("UC05 - Quantity < 0 → IllegalArgumentException")
    void UC05() {
        assertThrows(IllegalArgumentException.class, () -> {
            cartService.calculate(100, -5);
        });
    }

    @Test
    @DisplayName("UC06 - Quantity = 0 → Total=0")
    void UC06() {
        double result = cartService.calculate(100, 0);
        assertEquals(0, result);
    }

    @Test
    @DisplayName("UC07 - Price null → NullPointerException")
    void UC07() {
        assertThrows(NullPointerException.class, () -> {
            Double price = null;
            cartService.calculate(price, 2);
        });
    }

    @Test
    @DisplayName("UC08 - Price format invalid (abc) → NumberFormatException")
    void UC08() {
        assertThrows(NumberFormatException.class, () -> {
            Double.parseDouble("abc");
        });
    }

    @Test
    @DisplayName("UC09 - Quantity null → NullPointerException")
    void UC09() {
        assertThrows(NullPointerException.class, () -> {
            Integer quantity = null;
            cartService.calculate(100, quantity);
        });
    }

    @Test
    @DisplayName("UC10 - Quantity format invalid (abc) → NumberFormatException")
    void UC10() {
        assertThrows(NumberFormatException.class, () -> {
            Integer.parseInt("abc");
        });
    }
}