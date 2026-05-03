// =====================================================
// JVMart Database Schema - MongoDB
// =====================================================
// Database: jvmart
// Version: 1.0
// Compatible with MongoDB 6.0+

// Switch to jvmart database
use jvmart;

// =====================================================
// Collections and Indexes
// =====================================================

// =====================================================
// Reviews Collection
// =====================================================
// Structure: Product reviews with ratings and comments
db.createCollection("reviews");

// Create indexes for reviews collection
db.reviews.createIndex({ "productId": 1 });
db.reviews.createIndex({ "userId": 1 });
db.reviews.createIndex({ "createdAt": -1 });
db.reviews.createIndex({ "productId": 1, "rating": -1 });
db.reviews.createIndex({ "productId": 1, "createdAt": -1 });

// Sample review documents
db.reviews.insertMany([
    {
        "productId": 1,
        "userId": 2,
        "username": "johndoe",
        "rating": 5,
        "comment": "Excellent laptop! Very fast and great display quality.",
        "createdAt": new Date("2023-12-01T10:30:00Z")
    },
    {
        "productId": 1,
        "userId": 2,
        "username": "johndoe",
        "rating": 4,
        "comment": "Good value for money, but could have better battery life.",
        "createdAt": new Date("2023-12-02T14:15:00Z")
    },
    {
        "productId": 2,
        "userId": 2,
        "username": "johndoe",
        "rating": 4,
        "comment": "Comfortable mouse, works well with my laptop.",
        "createdAt": new Date("2023-12-03T09:20:00Z")
    }
]);

// =====================================================
// Activity Logs Collection
// =====================================================
// Structure: User activity tracking for analytics and security
db.createCollection("activity_logs");

// Create indexes for activity_logs collection
db.activity_logs.createIndex({ "userId": 1 });
db.activity_logs.createIndex({ "action": 1 });
db.activity_logs.createIndex({ "timestamp": -1 });
db.activity_logs.createIndex({ "userId": 1, "timestamp": -1 });
db.activity_logs.createIndex({ "action": 1, "timestamp": -1 });

// Sample activity log documents
db.activity_logs.insertMany([
    {
        "userId": 2,
        "action": "LOGIN",
        "detail": "User logged in successfully",
        "timestamp": new Date("2023-12-01T08:00:00Z")
    },
    {
        "userId": 2,
        "action": "VIEW_PRODUCT",
        "detail": "Viewed product #1: Laptop Pro 15\"",
        "timestamp": new Date("2023-12-01T08:15:00Z")
    },
    {
        "userId": 2,
        "action": "ADD_TO_CART",
        "detail": "Added product #1 to cart",
        "timestamp": new Date("2023-12-01T08:20:00Z")
    },
    {
        "userId": 2,
        "action": "PLACE_ORDER",
        "detail": "Placed order #1",
        "timestamp": new Date("2023-12-01T08:30:00Z")
    },
    {
        "userId": 2,
        "action": "SUBMIT_REVIEW",
        "detail": "Reviewed product #1",
        "timestamp": new Date("2023-12-01T10:30:00Z")
    }
]);

// =====================================================
// Aggregation Pipelines and Views
// =====================================================

// =====================================================
// Product Review Analytics
// =====================================================

// Function to get average rating for a product
function getProductAverageRating(productId) {
    const pipeline = [
        { $match: { productId: productId } },
        {
            $group: {
                _id: null,
                avgRating: { $avg: "$rating" },
                totalReviews: { $sum: 1 },
                ratingDistribution: {
                    $push: "$rating"
                }
            }
        },
        {
            $addFields: {
                rating5: {
                    $size: {
                        $filter: {
                            input: "$ratingDistribution",
                            cond: { $eq: ["$$this", 5] }
                        }
                    }
                },
                rating4: {
                    $size: {
                        $filter: {
                            input: "$ratingDistribution",
                            cond: { $eq: ["$$this", 4] }
                        }
                    }
                },
                rating3: {
                    $size: {
                        $filter: {
                            input: "$ratingDistribution",
                            cond: { $eq: ["$$this", 3] }
                        }
                    }
                },
                rating2: {
                    $size: {
                        $filter: {
                            input: "$ratingDistribution",
                            cond: { $eq: ["$$this", 2] }
                        }
                    }
                },
                rating1: {
                    $size: {
                        $filter: {
                            input: "$ratingDistribution",
                            cond: { $eq: ["$$this", 1] }
                        }
                    }
                }
            }
        },
        {
            $project: {
                _id: 0,
                avgRating: { $round: ["$avgRating", 2] },
                totalReviews: 1,
                rating5: 1,
                rating4: 1,
                rating3: 1,
                rating2: 1,
                rating1: 1
            }
        }
    ];
    
    return db.reviews.aggregate(pipeline).toArray();
}

// =====================================================
// User Activity Analytics
// =====================================================

// Function to get user activity summary
function getUserActivitySummary(userId, days = 30) {
    const pipeline = [
        {
            $match: {
                userId: userId,
                timestamp: {
                    $gte: new Date(Date.now() - days * 24 * 60 * 60 * 1000)
                }
            }
        },
        {
            $group: {
                _id: "$action",
                count: { $sum: 1 },
                lastOccurrence: { $max: "$timestamp" }
            }
        },
        { $sort: { count: -1 } }
    ];
    
    return db.activity_logs.aggregate(pipeline).toArray();
}

// =====================================================
// Product Performance Analytics
// =====================================================

// Function to get product review trends
function getProductReviewTrends(productId, months = 6) {
    const pipeline = [
        {
            $match: {
                productId: productId,
                createdAt: {
                    $gte: new Date(Date.now() - months * 30 * 24 * 60 * 60 * 1000)
                }
            }
        },
        {
            $group: {
                _id: {
                    year: { $year: "$createdAt" },
                    month: { $month: "$createdAt" }
                },
                avgRating: { $avg: "$rating" },
                reviewCount: { $sum: 1 }
            }
        },
        { $sort: { "_id.year": 1, "_id.month": 1 } }
    ];
    
    return db.reviews.aggregate(pipeline).toArray();
}

// =====================================================
// Text Search Configuration
// =====================================================

// Create text index for review comments
db.reviews.createIndex({
    "comment": "text",
    "username": "text"
}, {
    weights: {
        "comment": 10,
        "username": 1
    },
    name: "review_text_search"
});

// Function to search reviews
function searchReviews(searchText, productId = null) {
    const matchStage = {
        $text: { $search: searchText }
    };
    
    if (productId) {
        matchStage.productId = productId;
    }
    
    const pipeline = [
        { $match: matchStage },
        { $sort: { score: { $meta: "textScore" } } },
        {
            $project: {
                productId: 1,
                userId: 1,
                username: 1,
                rating: 1,
                comment: 1,
                createdAt: 1,
                score: { $meta: "textScore" }
            }
        }
    ];
    
    return db.reviews.aggregate(pipeline).toArray();
}

// =====================================================
// Data Validation Rules
// =====================================================

// Validation rules for reviews collection
db.runCommand({
    collMod: "reviews",
    validator: {
        $jsonSchema: {
            bsonType: "object",
            required: ["productId", "userId", "username", "rating", "comment", "createdAt"],
            properties: {
                productId: { bsonType: "int", minimum: 1 },
                userId: { bsonType: "int", minimum: 1 },
                username: { bsonType: "string", minLength: 1, maxLength: 100 },
                rating: { bsonType: "int", minimum: 1, maximum: 5 },
                comment: { bsonType: "string", minLength: 1, maxLength: 1000 },
                createdAt: { bsonType: "date" }
            }
        }
    },
    validationAction: "error"
});

// Validation rules for activity_logs collection
db.runCommand({
    collMod: "activity_logs",
    validator: {
        $jsonSchema: {
            bsonType: "object",
            required: ["userId", "action", "detail", "timestamp"],
            properties: {
                userId: { bsonType: "int", minimum: 1 },
                action: { bsonType: "string", enum: ["LOGIN", "LOGOUT", "VIEW_PRODUCT", "ADD_TO_CART", "UPDATE_CART", "REMOVE_FROM_CART", "PLACE_ORDER", "SUBMIT_REVIEW", "UPDATE_PROFILE", "CHANGE_PASSWORD"] },
                detail: { bsonType: "string", minLength: 1, maxLength: 500 },
                timestamp: { bsonType: "date" }
            }
        }
    },
    validationAction: "error"
});

// =====================================================
// Performance Optimization
// =====================================================

// Create compound indexes for common query patterns
db.reviews.createIndex({ "productId": 1, "rating": -1, "createdAt": -1 });
db.activity_logs.createIndex({ "userId": 1, "action": 1, "timestamp": -1 });

// Create partial indexes for better performance
db.reviews.createIndex(
    { "createdAt": -1 },
    { partialFilterExpression: { "rating": { $gte: 4 } } }
);

// =====================================================
// Backup and Recovery
// =====================================================

// Backup command:
// mongodump --uri="mongodb://localhost:27017" --db=jvmart --out=/backup/mongodb/$(date +%Y%m%d_%H%M%S)

// Restore command:
// mongorestore --uri="mongodb://localhost:27017" --db=jvmart /backup/mongodb/20231201_120000/jvmart/

// =====================================================
// Monitoring and Maintenance
// =====================================================

// Get collection statistics
function getCollectionStats() {
    return {
        reviews: db.reviews.stats(),
        activityLogs: db.activity_logs.stats()
    };
}

// Get index usage statistics
function getIndexStats() {
    return {
        reviews: db.reviews.aggregate([{ $indexStats: {} }]).toArray(),
        activityLogs: db.activity_logs.aggregate([{ $indexStats: {} }]).toArray()
    };
}

// Clean up old activity logs (older than 1 year)
function cleanupOldActivityLogs() {
    const cutoffDate = new Date(Date.now() - 365 * 24 * 60 * 60 * 1000);
    const result = db.activity_logs.deleteMany({
        timestamp: { $lt: cutoffDate }
    });
    return result;
}

// =====================================================
// Security Configuration
// =====================================================

// Create read-only user for reporting
// use admin
// db.createUser({
//     user: "jvmart_readonly",
//     pwd: "readonly_password",
//     roles: [
//         { role: "read", db: "jvmart" }
//     ]
// });

// Create application user with read/write permissions
// use admin
// db.createUser({
//     user: "jvmart_app",
//     pwd: "app_password",
//     roles: [
//         { role: "readWrite", db: "jvmart" }
//     ]
// });

// Enable authentication in mongod.conf:
// security:
//   authorization: enabled

// =====================================================
// Replication and Sharding Considerations
// =====================================================

// For production, consider:
// 1. Replica set for high availability
// 2. Sharding for horizontal scaling
// 3. Change streams for real-time updates

// Replica set initialization:
// rs.initiate({
//     _id: "jvmart_rs",
//     members: [
//         { _id: 0, host: "localhost:27017" },
//         { _id: 1, host: "localhost:27018" },
//         { _id: 2, host: "localhost:27019" }
//     ]
// });

// Shard key recommendations:
// sh.shardCollection("jvmart.reviews", { "productId": 1, "createdAt": 1 });
// sh.shardCollection("jvmart.activity_logs", { "userId": 1, "timestamp": 1 });

// =====================================================
// Sample Queries for Testing
// =====================================================

// Test queries to verify the setup
print("=== Testing MongoDB Setup ===");

// Get all reviews for product 1
print("Reviews for product 1:");
db.reviews.find({ productId: 1 }).pretty();

// Get average rating for product 1
print("Average rating for product 1:");
getProductAverageRating(1);

// Get user activity summary
print("User activity summary for user 2:");
getUserActivitySummary(2);

// Search reviews
print("Search results for 'laptop':");
searchReviews("laptop");

print("=== MongoDB Schema Setup Complete ===");
