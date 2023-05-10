package com.athar.productservice.service;

import com.athar.productservice.dto.ProductRequest;
import com.athar.productservice.dto.ProductResponse;
import com.athar.productservice.model.Product;
import com.athar.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Stream;


@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    //inject ProductRepository by constructor in service class to save product in database
    private final ProductRepository productRepository;

    public void createProduct(ProductRequest productRequest){
        Product product = Product.builder()
                .name(productRequest.getName())
                .description(productRequest.getDescription())
                .price(productRequest.getPrice())
                .build();

        productRepository.save(product);
        log.info("Product {} is saved.", product.getId());
    }


    public List<ProductResponse> getAllProducts() {

        /*first read all the products inside the database
        then store them inside a variable called products
         */
        List<Product> products =  productRepository.findAll();
        
        /*
        then map this Product class into response class
         */
        return products.stream().map(this::mapToProductResponse).toList();

    }

    private ProductResponse mapToProductResponse(Product product) {
        return  ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .build();
    }
}
