// Extra MongoDB reviews for JVMart UI demos (optional).
// Run: mongosh < database/mongo_seed_demo_reviews.js
// Requires existing jvmart.reviews collection (see mongo_schema.js).

use('jvmart');

db.reviews.insertMany([
  { productId: 4, userId: 2, username: 'johndoe', rating: 5, comment: 'USB hub works perfectly with my laptop setup.', createdAt: new Date() },
  { productId: 5, userId: 2, username: 'johndoe', rating: 4, comment: 'Monitor colors are vivid; stand is a bit wobbly.', createdAt: new Date() },
  { productId: 3, userId: 2, username: 'johndoe', rating: 3, comment: 'Keyboard is loud but satisfying for typing.', createdAt: new Date() }
], { ordered: false });
