package com.example.mealmate.model

data class MealResponse(
    val meals: List<Meal>
)

data class Meal(
    val idMeal: String,
    val strMeal: String,
    val strInstructions: String,
    val strMealThumb: String,
    val strCategory: String? = null,
    val strTags: String? = null,
    val strIngredient1: String? = null,
    val strIngredient2: String? = null,
    val strIngredient3: String? = null,
    val strIngredient4: String? = null,
    val strIngredient5: String? = null,
    val strIngredient6: String? = null,
    val strIngredient7: String? = null,
    val strIngredient8: String? = null,
    val strIngredient9: String? = null,
    val strIngredient10: String? = null,
    val strIngredient11: String? = null,
    val strIngredient12: String? = null,
    val strIngredient13: String? = null,
    val strIngredient14: String? = null,
    val strIngredient15: String? = null,
    val strIngredient16: String? = null,
    val strIngredient17: String? = null,
    val strIngredient18: String? = null,
    val strIngredient19: String? = null,
    val strIngredient20: String? = null,
    val strMeasure1: String? = null,
    val strMeasure2: String? = null,
    val strMeasure3: String? = null,
    val strMeasure4: String? = null,
    val strMeasure5: String? = null,
    val strMeasure6: String? = null,
    val strMeasure7: String? = null,
    val strMeasure8: String? = null,
    val strMeasure9: String? = null,
    val strMeasure10: String? = null,
    val strMeasure11: String? = null,
    val strMeasure12: String? = null,
    val strMeasure13: String? = null,
    val strMeasure14: String? = null,
    val strMeasure15: String? = null,
    val strMeasure16: String? = null,
    val strMeasure17: String? = null,
    val strMeasure18: String? = null,
    val strMeasure19: String? = null,
    val strMeasure20: String? = null
)
 {
     fun getIngredientsWithMeasures(): List<Pair<String, String>> {
         val ingredientsWithMeasures = mutableListOf<Pair<String, String>>()

         for (i in 1..20) {
             val ingredient = this::class.java.getDeclaredField("strIngredient$i").get(this) as? String
             val measure = this::class.java.getDeclaredField("strMeasure$i").get(this) as? String

             if (!ingredient.isNullOrBlank()) {
                 ingredientsWithMeasures.add(measure.orEmpty() to ingredient)
             }
         }

         return ingredientsWithMeasures
     }

}


data class CategoryResponse(
    val categories: List<Category>
)

data class Category(
    val idCategory: String,
    val strCategory: String,
    val strCategoryThumb: String,
    val strCategoryDescription: String
)

data class MealBriefResponse(
    val meals: List<MealBrief>
)

data class MealBrief(
    val strMeal: String,
    val strMealThumb: String,
    val idMeal: String
)
