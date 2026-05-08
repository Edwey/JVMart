# JVMart - Simple Guide (Layman Version)

This guide explains JVMart in everyday language.
No technical background is needed.

## What is JVMart?

JVMart is a shopping app for desktop.
It has two sides:

- **Customer side**: for shopping, reviews, cart, and orders
- **Admin side**: for managing products, orders, and users

## How to Open the App

From the project folder, run:

```bash
mvnw.cmd javafx:run
```

If you are not on Windows, use:

```bash
./mvnw javafx:run
```

## What You Can Do as a Customer

### 1) Browse products
- Open the **Products** section
- Use categories on the left (Electronics, Clothing, etc.)
- Use search to find items faster

### 2) View product details
- Click a product to open its page
- See price, stock, and reviews
- Rate with stars and write your own review

### 3) Save items to wishlist
- On a product page, use **Save/Remove**
- In cart, tap **View Wishlist** to manage saved items
- You can remove items or move them to cart

### 4) Use the cart and checkout
- Add items to cart
- Change quantities or remove items
- Continue to checkout and place your order

### 5) Track your orders
- Open **My Orders**
- See order status (pending, shipped, completed, etc.)

### 6) Update your profile
- Edit your personal details
- Change your password

## What You Can Do as an Admin

### 1) View dashboard
- See overall shop activity and quick stats

### 2) Manage products
- Add new products
- Edit product details
- Remove products
- Update stock levels

### 3) Manage orders
- View customer orders
- Update order status

### 4) Manage reviews and users
- Check customer reviews
- Review customer accounts and activity

## Light Mode and Dark Mode

JVMart supports both themes.

- The app should look the same in structure
- Only colors should change between light and dark

So buttons, spacing, and text sizes should feel consistent.

## Easy Demo Flow (for presentation)

If you want to show the app quickly, do this:

1. Login as customer
2. Go to Products and search/filter
3. Open a product, add to cart, save to wishlist
4. Open wishlist and move item to cart
5. Checkout
6. Show My Orders
7. Switch theme (light/dark)
8. Login as admin and show product/order management

## In One Sentence

JVMart is a full shopping app where customers can shop and track orders, while admins manage store operations in one place.
