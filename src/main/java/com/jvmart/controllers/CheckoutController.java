package com.jvmart.controllers;

import com.jvmart.models.CartItem;
import com.jvmart.services.OrderService;
import com.jvmart.services.ActivityLogService;
import com.jvmart.session.SessionManager;
import com.jvmart.utils.AlertHelper;
import com.jvmart.utils.ImageHelper;
import com.jvmart.utils.SceneRouter;
import com.jvmart.utils.GlobalRefresh;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Modern Checkout Controller
 * Handles secure checkout process with validation and multiple payment methods
 */
public class CheckoutController implements GlobalRefresh.Refreshable {
    
    // Cart Summary
    @FXML private VBox cartItemsList;
    @FXML private ScrollPane cartItemsScroll;
    @FXML private Label cartItemCount;
    @FXML private Label subtotalLabel;
    @FXML private Label shippingLabel;
    @FXML private Label taxLabel;
    @FXML private Label totalLabel;
    @FXML private Label checkoutSubtotal;
    @FXML private Label checkoutTotal;
    
    // Shipping Information
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField addressField;
    @FXML private TextField cityField;
    @FXML private TextField postalCodeField;
    @FXML private ComboBox<String> deliveryMethodCombo;
    
    // Payment Information
    @FXML private ComboBox<String> paymentMethodCombo;
    @FXML private VBox cardPaymentFields;
    @FXML private VBox mobileMoneyFields;
    @FXML private TextField cardNumberField;
    @FXML private TextField expiryField;
    @FXML private TextField cvvField;
    @FXML private TextField cardholderNameField;
    @FXML private TextField mobileNumberField;
    @FXML private ComboBox<String> networkProviderCombo;
    
    // Promo Code
    @FXML private TextField promoCodeField;
    @FXML private Label promoMessage;
    
    // Actions
    @FXML private Button placeOrderBtn;

    private static final Logger LOGGER = Logger.getLogger(CheckoutController.class.getName());
    private final OrderService orderService = new OrderService();
    private final ActivityLogService activityLogService = new ActivityLogService();
    
    // Validation patterns
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9]{10,15}$");
    private static final Pattern CARD_PATTERN = Pattern.compile("^[0-9]{13,19}$");
    private static final Pattern CVV_PATTERN = Pattern.compile("^[0-9]{3,4}$");
    
    private double shippingCost = 15.0;
    private double discountAmount = 0.0;

    @FXML
    public void initialize() {
        SessionManager session = SessionManager.getInstance();
        List<CartItem> cart = session.getCart();

        if (cartItemsList == null) {
            LOGGER.severe("cartItemsList is null - FXML injection failed");
            return;
        }

        // Setup payment method listener
        if (paymentMethodCombo != null) {
            paymentMethodCombo.setOnAction(e -> onPaymentMethodChange());
        }

        if (firstNameField == null) {
            shippingCost = 0;
        }

        // Setup delivery method listener
        if (deliveryMethodCombo != null) {
            deliveryMethodCombo.setOnAction(e -> updateShippingCost());
        }

        loadCartItems();
        updateOrderSummary();
        setupFormValidation();
    }

    private void loadCartItems() {
        SessionManager session = SessionManager.getInstance();
        List<CartItem> cart = session.getCart();

        if (cartItemsList != null) {
            cartItemsList.getChildren().clear();
            for (CartItem item : cart) {
                cartItemsList.getChildren().add(createCartItemRow(item));
            }
        }

        if (cartItemCount != null) {
            cartItemCount.setText(cart.size() + " items");
        }
    }

    private void updateOrderSummary() {
        SessionManager session = SessionManager.getInstance();
        double subtotal = session.getCartTotal();
        double tax = firstNameField != null ? subtotal * 0.03 : 0;
        double total = subtotal + shippingCost + tax - discountAmount;

        String subStr = String.format("GHS %.2f", subtotal);
        if (subtotalLabel != null) {
            subtotalLabel.setText(subStr);
        }
        if (checkoutSubtotal != null) {
            checkoutSubtotal.setText(subStr);
        }
        if (shippingLabel != null) {
            shippingLabel.setText(String.format("GHS %.2f", shippingCost));
        }
        if (taxLabel != null) {
            taxLabel.setText(String.format("GHS %.2f", tax));
        }
        String totalStr = String.format("GHS %.2f", total);
        if (totalLabel != null) {
            totalLabel.setText(totalStr);
        }
        if (checkoutTotal != null) {
            checkoutTotal.setText(totalStr);
        }
    }

    private void setupFormValidation() {
        // Add input validation
        if (emailField != null) {
            emailField.textProperty().addListener((obs, old, newVal) -> validateEmail(newVal));
        }
        if (phoneField != null) {
            phoneField.textProperty().addListener((obs, old, newVal) -> validatePhone(newVal));
        }
        if (cardNumberField != null) {
            cardNumberField.textProperty().addListener((obs, old, newVal) -> validateCardNumber(newVal));
        }
    }

    private void onPaymentMethodChange() {
        if (paymentMethodCombo == null) return;

        String method = paymentMethodCombo.getValue();
        if (cardPaymentFields != null && mobileMoneyFields != null) {
            boolean isCard = "Credit/Debit Card".equals(method);
            boolean isMobileMoney = method != null && method.contains("Mobile Money");

            cardPaymentFields.setVisible(isCard);
            cardPaymentFields.setManaged(isCard);
            mobileMoneyFields.setVisible(isMobileMoney);
            mobileMoneyFields.setManaged(isMobileMoney);
        }
    }

    private void updateShippingCost() {
        if (deliveryMethodCombo == null) return;

        String method = deliveryMethodCombo.getValue();
        if (method != null) {
            if (method.contains("Standard")) {
                shippingCost = 15.0;
            } else if (method.contains("Express")) {
                shippingCost = 35.0;
            } else if (method.contains("Same Day")) {
                shippingCost = 50.0;
            }
            updateOrderSummary();
        }
    }

    private VBox createCartItemRow(CartItem item) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12));

        // Product image
        ImageView imageView = new ImageView();
        imageView.setFitHeight(60);
        imageView.setFitWidth(60);
        imageView.setPreserveRatio(true);
        
        Image image = ImageHelper.loadProductImage(item.getProduct().getImagePath());
        if (image != null) {
            imageView.setImage(image);
        }

        // Product details
        VBox details = new VBox(4);
        Label nameLabel = new Label(item.getProduct().getName());
        nameLabel.getStyleClass().add("text-sm font-medium");
        Label quantityLabel = new Label("Qty: " + item.getQuantity());
        quantityLabel.getStyleClass().add("text-xs");
        quantityLabel.setTextFill(javafx.scene.paint.Color.web("#9ca3af"));

        // Price
        Label priceLabel = new Label(String.format("GHS %.2f", item.getProduct().getPrice() * item.getQuantity()));
        priceLabel.getStyleClass().add("text-sm font-semibold");
        priceLabel.setTextFill(javafx.scene.paint.Color.web("#0ea5e9"));

        details.getChildren().addAll(nameLabel, quantityLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        row.getChildren().addAll(imageView, details, spacer, priceLabel);
        row.setStyle("-fx-background-color: #374151; -fx-background-radius: 8;");
        
        return new VBox(row);
    }

    private boolean validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) return false;
        return EMAIL_PATTERN.matcher(email).matches();
    }

    private boolean validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) return false;
        return PHONE_PATTERN.matcher(phone.replaceAll("[\\s\\-()]", "")).matches();
    }

    private boolean validateCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.trim().isEmpty()) return false;
        return CARD_PATTERN.matcher(cardNumber.replaceAll("[\\s\\-]", "")).matches();
    }

    // Action Methods
    @FXML private void editCart() {
        SceneRouter.navigateTo("cart.fxml");
    }

    @FXML private void applyPromoCode() {
        if (promoCodeField == null) return;
        
        String code = promoCodeField.getText().trim();
        if (code.isEmpty()) {
            AlertHelper.error("Please enter a promo code");
            return;
        }
        
        // Simple promo logic (in real app, would validate against database)
        if ("SAVE10".equalsIgnoreCase(code)) {
            discountAmount = SessionManager.getInstance().getCartTotal() * 0.1;
            if (promoMessage != null) {
                promoMessage.setText("✓ Promo code applied! You saved GHS " + String.format("%.2f", discountAmount));
                promoMessage.getStyleClass().add("text-success");
                promoMessage.setVisible(true);
                promoMessage.setManaged(true);
            }
            updateOrderSummary();
        } else {
            if (promoMessage != null) {
                promoMessage.setText("✗ Invalid promo code");
                promoMessage.getStyleClass().add("text-error");
                promoMessage.setVisible(true);
                promoMessage.setManaged(true);
            }
        }
    }

    @FXML private void placeOrder() {
        if (!validateCheckoutForm()) {
            return;
        }

        var result = orderService.placeOrder();
        switch (result) {
            case com.jvmart.services.ServiceResult.Success<?> success -> {
                activityLogService.logCurrentUser("PLACE_ORDER", "Order placed successfully");
                Integer placedId = (Integer) success.value();
                SceneRouter.navigateTo("order_confirmation.fxml", java.util.Map.of("lastOrderId", placedId));
            }
            case com.jvmart.services.ServiceResult.Failure<?> failure ->
                    AlertHelper.error("Order failed: " + failure.message());
            default -> AlertHelper.error("Unknown error occurred while placing order");
        }
    }

    @FXML private void backToCart() {
        SceneRouter.navigateTo("cart.fxml");
    }

    @FXML private void cancelCheckout() {
        AlertHelper.info("Checkout Cancelled", "Your cart has been saved. You can complete your purchase later.");
        SceneRouter.navigateTo("product_catalog.fxml");
    }

    private boolean validateCheckoutForm() {
        if (SessionManager.getInstance().getCart().isEmpty()) {
            AlertHelper.error("Your cart is empty");
            return false;
        }

        if (firstNameField == null) {
            return true;
        }

        // Validate shipping information
        if (firstNameField.getText().trim().isEmpty()) {
            AlertHelper.error("Please enter your first name");
            return false;
        }
        
        if (lastNameField == null || lastNameField.getText().trim().isEmpty()) {
            AlertHelper.error("Please enter your last name");
            return false;
        }
        
        if (emailField == null || !validateEmail(emailField.getText())) {
            AlertHelper.error("Please enter a valid email address");
            return false;
        }
        
        if (phoneField == null || !validatePhone(phoneField.getText())) {
            AlertHelper.error("Please enter a valid phone number");
            return false;
        }
        
        // Validate payment information
        String paymentMethod = paymentMethodCombo != null ? paymentMethodCombo.getValue() : null;
        if (paymentMethod == null) {
            AlertHelper.error("Please select a payment method");
            return false;
        }
        
        if ("Credit/Debit Card".equals(paymentMethod)) {
            if (cardNumberField == null || !validateCardNumber(cardNumberField.getText())) {
                AlertHelper.error("Please enter a valid card number");
                return false;
            }
            
            if (cvvField == null || !CVV_PATTERN.matcher(cvvField.getText()).matches()) {
                AlertHelper.error("Please enter a valid CVV");
                return false;
            }
        }
        
        return true;
    }

    @Override
    public void refresh() {
        loadCartItems();
        updateOrderSummary();
        AlertHelper.info("Checkout Refreshed", "Checkout page has been refreshed successfully.");
    }
}
