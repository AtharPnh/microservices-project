package com.athar.inventoryservice.repository;

import com.athar.inventoryservice.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.List;
import java.util.Optional;
@EnableJpaRepositories
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
   List<Inventory> findBySkuCodeIn(List<String> skuCode);
}
