package com.example.myfitnessapp.data.model

enum class MealType(val label: String) {
    BREAKFAST("早餐"),
    LUNCH("午餐"),
    DINNER("晚餐"),
    SNACK("加餐")
}

data class FoodEntry(
    val id: String,
    val name: String,
    val calories: Int,
    val servingSize: String,
    val mealType: MealType
)

data class NutritionSummary(
    val budgetCalories: Int = 2000,
    val consumedCalories: Int = 0,
    val burnedCalories: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0
) {
    val remainingCalories: Int get() = budgetCalories - consumedCalories + burnedCalories
}

data class DietState(
    val todaySummary: NutritionSummary = NutritionSummary(),
    val meals: Map<MealType, List<FoodEntry>> = emptyMap(),
    val adviceText: String = "",
    val waterCount: Int = 0
)