package vn.edu.fpt.fashionstore.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.fashionstore.entity.Customer;
import vn.edu.fpt.fashionstore.entity.Product;
import vn.edu.fpt.fashionstore.entity.Wishlist;
import vn.edu.fpt.fashionstore.repository.WishlistRepository;

import java.util.List;

@Service
public class WishlistService {
    
    @Autowired
    private WishlistRepository wishlistRepository;
    
    public List<Wishlist> getWishlistByCustomer(Customer customer) {
        List<Wishlist> wishlists = wishlistRepository.findByCustomer(customer);
        // Eagerly fetch variants to avoid lazy loading issues
        wishlists.forEach(w -> {
            if (w.getProduct() != null && w.getProduct().getVariants() != null) {
                w.getProduct().getVariants().size(); // Force initialization
            }
        });
        return wishlists;
    }
    
    @Transactional
    public boolean addToWishlist(Customer customer, Product product) {
        if (wishlistRepository.existsByCustomerAndProduct(customer, product)) {
            return false; // Already in wishlist
        }
        Wishlist wishlist = new Wishlist(customer, product);
        wishlistRepository.save(wishlist);
        return true;
    }
    
    @Transactional
    public boolean removeFromWishlist(Customer customer, Product product) {
        if (wishlistRepository.existsByCustomerAndProduct(customer, product)) {
            wishlistRepository.deleteByCustomerAndProduct(customer, product);
            return true;
        }
        return false;
    }
    
    public boolean isInWishlist(Customer customer, Product product) {
        return wishlistRepository.existsByCustomerAndProduct(customer, product);
    }
    
    public long getWishlistCount(Customer customer) {
        return wishlistRepository.countByCustomer(customer);
    }
}
