package vn.edu.fpt.fashionstore.repository.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.fpt.fashionstore.entity.Product;

import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {
    public static Specification<Product> filter(List<String> categories, Double minPrice, Double maxPrice, List<String> sizes) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Category
            if (categories != null && !categories.isEmpty()) {
                predicates.add(root.get("category").in(categories));
            }

            // Price range
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("price"), maxPrice));
            }

            // Size
            if (sizes != null && !sizes.isEmpty()) {
                predicates.add(root.get("size").in(sizes));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}