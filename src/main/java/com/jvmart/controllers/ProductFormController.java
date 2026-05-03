package com.jvmart.controllers;

import com.jvmart.models.Product;
import com.jvmart.services.ProductService;
import com.jvmart.utils.AlertHelper;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import net.synedra.validatorfx.Validator;


public class ProductFormController {
    @FXML private Label dialogTitle;
    @FXML private TextField nameField;
    @FXML private TextArea descriptionField;
    @FXML private TextField priceField;
    @FXML private TextField stockField;
    @FXML private TextField categoryField;
    @FXML private TextField imagePathField;

    private Product product;
    private final ProductService productService = new ProductService();
    private final Validator validator = new Validator();
    private Runnable onSaveCallback;

    @FXML
    public void initialize() {
        validator.createCheck()
                .dependsOn("name", nameField.textProperty())
                .withMethod(c -> {
                    if (c.get("name").toString().trim().isEmpty()) c.error("Name is required");
                })
                .decorates(nameField);

        validator.createCheck()
                .dependsOn("price", priceField.textProperty())
                .withMethod(c -> {
                    try {
                        if (Double.parseDouble(c.get("price").toString()) < 0) c.error("Price must be positive");
                    } catch (NumberFormatException e) {
                        c.error("Invalid price");
                    }
                })
                .decorates(priceField);
    }

    public void setProduct(Product product) {
        this.product = product;
        if (product != null) {
            dialogTitle.setText("Edit Product");
            nameField.setText(product.getName());
            descriptionField.setText(product.getDescription());
            priceField.setText(String.valueOf(product.getPrice()));
            stockField.setText(String.valueOf(product.getStock()));
            categoryField.setText(product.getCategory());
            imagePathField.setText(product.getImagePath());
        } else {
            dialogTitle.setText("Add New Product");
            this.product = new Product();
        }
    }

    public void setOnSave(Runnable callback) {
        this.onSaveCallback = callback;
    }

    @FXML
    private void onSave() {
        if (!validator.validate()) return;

        product.setName(nameField.getText());
        product.setDescription(descriptionField.getText());
        product.setPrice(Double.parseDouble(priceField.getText()));
        product.setStock(Integer.parseInt(stockField.getText()));
        product.setCategory(categoryField.getText());
        product.setImagePath(imagePathField.getText());

        var result = productService.saveProduct(product);
        switch (result) {
            case com.jvmart.services.ServiceResult.Success<?> ignored -> {
                if (onSaveCallback != null) onSaveCallback.run();
                close();
            }
            case com.jvmart.services.ServiceResult.Failure<?> failure ->
                AlertHelper.error("Error saving product: " + failure.message());
        }
    }

    @FXML
    private void onCancel() {
        close();
    }

    private void close() {
        ((Stage) nameField.getScene().getWindow()).close();
    }
}
