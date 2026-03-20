package vn.edu.fpt.fashionstore.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.fashionstore.entity.Color;

public interface ColorRepository extends JpaRepository<Color, Integer> {
}
