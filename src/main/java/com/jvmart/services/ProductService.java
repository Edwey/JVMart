package com.jvmart.services;

import com.jvmart.dao.sql.ProductDAO;
import com.jvmart.models.Product;

import java.sql.SQLException;
import java.util.List;

public class ProductService {
    private final ProductDAO productDAO = new ProductDAO();

    public ServiceResult<List<Product>> getAllProducts() {
        try {
            return new ServiceResult.Success<>(productDAO.findAll());
        } catch (SQLException e) {
            return new ServiceResult.Failure<>("Failed to retrieve products: " + e.getMessage());
        }
    }

    public ServiceResult<List<Product>> getByCategory(String category) {
        try {
            List<Product> products = switch (category.toLowerCase()) {
                case "all" -> productDAO.findAll();
                default -> productDAO.findByCategory(category);
            };
            return new ServiceResult.Success<>(products);
        } catch (SQLException e) {
            return new ServiceResult.Failure<>("Failed to retrieve products by category: " + e.getMessage());
        }
    }

    public ServiceResult<List<Product>> search(String keyword) {
        try {
            return new ServiceResult.Success<>(productDAO.findByName(keyword));
        } catch (SQLException e) {
            return new ServiceResult.Failure<>("Failed to search products: " + e.getMessage());
        }
    }

    public ServiceResult<Void> saveProduct(Product product) {
        try {
            if (product.getId() > 0) {
                productDAO.update(product);
            } else {
                productDAO.save(product);
            }
            return new ServiceResult.Success<>(null);
        } catch (SQLException e) {
            return new ServiceResult.Failure<>("Failed to save product: " + e.getMessage());
        }
    }

    public ServiceResult<Void> deleteProduct(int id) {
        try {
            productDAO.delete(id);
            return new ServiceResult.Success<>(null);
        } catch (SQLException e) {
            return new ServiceResult.Failure<>("Failed to delete product: " + e.getMessage());
        }
    }

    public ServiceResult<List<Product>> getLowStockProducts(int threshold) {
        try {
            List<Product> lowStock = productDAO.findLowStock(threshold);
            return new ServiceResult.Success<>(lowStock);
        } catch (SQLException e) {
            return new ServiceResult.Failure<>("Failed to retrieve low stock products: " + e.getMessage());
        }
    }

    public ServiceResult<Integer> countLowStock() {
        try {
            int count = productDAO.countLowStock();
            return new ServiceResult.Success<>(count);
        } catch (SQLException e) {
            return new ServiceResult.Failure<>("Failed to count low stock products: " + e.getMessage());
        }
    }
}
