package com.example.myfitnessapp.data.model

import kotlin.math.roundToInt

enum class DietGoalType(val label: String, val summary: String) {
    CUT("减脂", "优先高蛋白和较低热量密度，帮助控制总摄入。"),
    MAINTAIN("维持", "保持均衡饮食结构，让热量和营养更稳定。"),
    GAIN("增肌", "提高优质碳水和蛋白质占比，支持恢复和增肌。")
}

data class FoodCatalogItem(
    val code: String,
    val name: String,
    val caloriesPer100g: Int,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val mealTypes: Set<MealType>,
    val tags: Set<String> = emptySet()
)

data class PlannedFoodPortion(
    val food: FoodCatalogItem,
    val grams: Int
) {
    val calories: Int
        get() = HealthyDietPlanner.calculateCalories(food, grams)

    val protein: Double
        get() = HealthyDietPlanner.calculateMacro(food.proteinPer100g, grams)

    val carbs: Double
        get() = HealthyDietPlanner.calculateMacro(food.carbsPer100g, grams)

    val fat: Double
        get() = HealthyDietPlanner.calculateMacro(food.fatPer100g, grams)
}

data class MealPlanRecommendation(
    val mealType: MealType,
    val title: String,
    val targetCalories: Int,
    val items: List<PlannedFoodPortion>
) {
    val totalCalories: Int
        get() = items.sumOf { it.calories }

    fun summary(): String {
        val foods = items.joinToString(" + ") { "${it.food.name} ${it.grams}g" }
        return "$foods，约 ${totalCalories} kcal"
    }
}

data class DailyMealPlan(
    val remainingCalories: Int,
    val goalType: DietGoalType,
    val nextMeal: MealPlanRecommendation,
    val recommendations: List<MealPlanRecommendation>,
    val targetMacros: MacroSummary
)

data class MacroSummary(
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0
)

data class SavedPortionSnapshot(
    val name: String,
    val grams: Int,
    val caloriesPer100g: Int,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double
) {
    fun toPortion(mealType: MealType): PlannedFoodPortion {
        return PlannedFoodPortion(
            food = FoodCatalogItem(
                code = "saved_${name}",
                name = name,
                caloriesPer100g = caloriesPer100g,
                proteinPer100g = proteinPer100g,
                carbsPer100g = carbsPer100g,
                fatPer100g = fatPer100g,
                mealTypes = setOf(mealType)
            ),
            grams = grams
        )
    }

    fun serialize(): String {
        return listOf(
            name,
            grams.toString(),
            caloriesPer100g.toString(),
            proteinPer100g.toString(),
            carbsPer100g.toString(),
            fatPer100g.toString()
        ).joinToString("^")
    }

    companion object {
        fun deserialize(value: String): SavedPortionSnapshot? {
            val parts = value.split("^")
            if (parts.size != 6) return null
            return SavedPortionSnapshot(
                name = parts[0],
                grams = parts[1].toIntOrNull() ?: return null,
                caloriesPer100g = parts[2].toIntOrNull() ?: return null,
                proteinPer100g = parts[3].toDoubleOrNull() ?: return null,
                carbsPer100g = parts[4].toDoubleOrNull() ?: return null,
                fatPer100g = parts[5].toDoubleOrNull() ?: return null
            )
        }
    }
}

object BasicFoodCatalog {

    private val foods = listOf(
        FoodCatalogItem("oats", "燕麦", 389, 16.9, 66.3, 6.9, setOf(MealType.BREAKFAST)),
        FoodCatalogItem("milk", "牛奶", 54, 3.4, 5.0, 3.2, setOf(MealType.BREAKFAST, MealType.SNACK)),
        FoodCatalogItem("egg", "鸡蛋", 155, 13.0, 1.1, 11.0, setOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER)),
        FoodCatalogItem("banana", "香蕉", 89, 1.1, 22.8, 0.3, setOf(MealType.BREAKFAST, MealType.SNACK)),
        FoodCatalogItem("whole_wheat_bread", "全麦面包", 247, 12.0, 41.0, 4.0, setOf(MealType.BREAKFAST, MealType.SNACK)),
        FoodCatalogItem("yogurt", "酸奶", 62, 3.5, 6.0, 2.8, setOf(MealType.BREAKFAST, MealType.SNACK)),
        FoodCatalogItem("apple", "苹果", 52, 0.3, 13.8, 0.2, setOf(MealType.SNACK, MealType.BREAKFAST)),
        FoodCatalogItem("nuts", "坚果", 607, 20.0, 21.0, 54.0, setOf(MealType.SNACK), setOf("dense")),
        FoodCatalogItem("brown_rice", "糙米饭", 116, 2.6, 24.0, 0.9, setOf(MealType.LUNCH, MealType.DINNER)),
        FoodCatalogItem("quinoa", "藜麦", 120, 4.4, 21.3, 1.9, setOf(MealType.LUNCH, MealType.DINNER)),
        FoodCatalogItem("sweet_potato", "红薯", 86, 1.6, 20.1, 0.1, setOf(MealType.LUNCH, MealType.DINNER, MealType.SNACK)),
        FoodCatalogItem("chicken_breast", "鸡胸肉", 165, 31.0, 0.0, 3.6, setOf(MealType.LUNCH, MealType.DINNER)),
        FoodCatalogItem("salmon", "三文鱼", 208, 20.4, 0.0, 13.4, setOf(MealType.LUNCH, MealType.DINNER)),
        FoodCatalogItem("shrimp", "虾仁", 99, 24.0, 0.2, 0.3, setOf(MealType.LUNCH, MealType.DINNER)),
        FoodCatalogItem("lean_beef", "牛肉", 179, 26.0, 0.0, 8.0, setOf(MealType.LUNCH, MealType.DINNER)),
        FoodCatalogItem("tofu", "豆腐", 81, 8.1, 1.9, 4.2, setOf(MealType.LUNCH, MealType.DINNER)),
        FoodCatalogItem("broccoli", "西兰花", 34, 2.8, 6.6, 0.4, setOf(MealType.LUNCH, MealType.DINNER)),
        FoodCatalogItem("spinach", "菠菜", 23, 2.9, 3.6, 0.4, setOf(MealType.LUNCH, MealType.DINNER)),
        FoodCatalogItem("corn", "玉米", 106, 3.4, 22.8, 1.5, setOf(MealType.LUNCH, MealType.DINNER, MealType.SNACK)),
        FoodCatalogItem("avocado", "牛油果", 160, 2.0, 8.5, 14.7, setOf(MealType.BREAKFAST, MealType.SNACK))
    )

    fun foodsForMeal(mealType: MealType): List<FoodCatalogItem> =
        foods.filter { mealType in it.mealTypes }

    fun findByCode(code: String): FoodCatalogItem? = foods.firstOrNull { it.code == code }

    fun findByName(name: String): FoodCatalogItem? = foods.firstOrNull { it.name == name }
}

object HealthyDietPlanner {

    private val mealTemplates = mapOf(
        MealType.BREAKFAST to listOf(
            Template("轻盈早餐", listOf("yogurt" to 200, "banana" to 120, "egg" to 60)),
            Template("标准早餐", listOf("oats" to 55, "milk" to 250, "egg" to 100, "banana" to 100)),
            Template("高饱腹早餐", listOf("whole_wheat_bread" to 100, "egg" to 100, "milk" to 250, "avocado" to 60))
        ),
        MealType.LUNCH to listOf(
            Template("轻负担午餐", listOf("quinoa" to 160, "shrimp" to 150, "broccoli" to 220)),
            Template("均衡午餐", listOf("brown_rice" to 180, "chicken_breast" to 150, "broccoli" to 220)),
            Template("高蛋白午餐", listOf("brown_rice" to 160, "lean_beef" to 150, "spinach" to 220, "tofu" to 100))
        ),
        MealType.DINNER to listOf(
            Template("轻晚餐", listOf("sweet_potato" to 220, "salmon" to 120, "spinach" to 220)),
            Template("恢复晚餐", listOf("brown_rice" to 150, "chicken_breast" to 140, "broccoli" to 200, "tofu" to 120)),
            Template("控卡晚餐", listOf("corn" to 150, "shrimp" to 160, "spinach" to 220))
        ),
        MealType.SNACK to listOf(
            Template("水果加餐", listOf("apple" to 180, "yogurt" to 150)),
            Template("能量加餐", listOf("banana" to 120, "nuts" to 20, "yogurt" to 180)),
            Template("轻食加餐", listOf("sweet_potato" to 150, "milk" to 250))
        )
    )

    fun calculateCalories(food: FoodCatalogItem, grams: Int): Int {
        return (food.caloriesPer100g * grams / 100.0).roundToInt()
    }

    fun calculateMacro(per100g: Double, grams: Int): Double {
        return ((per100g * grams / 100.0) * 10.0).roundToInt() / 10.0
    }

    fun portionOptions(food: FoodCatalogItem): List<Int> {
        return when {
            "dense" in food.tags -> listOf(15, 20, 25, 30, 40)
            food.code == "egg" -> listOf(50, 100, 150, 200)
            else -> listOf(50, 100, 150, 200, 250, 300)
        }
    }

    fun buildDailyPlan(
        budgetCalories: Int,
        consumedCalories: Int,
        burnedCalories: Int,
        completedMeals: Set<MealType>,
        currentHour: Int,
        goalType: DietGoalType
    ): DailyMealPlan {
        val remainingCalories = (budgetCalories - consumedCalories + burnedCalories).coerceAtLeast(250)
        val nextMeal = suggestNextMeal(completedMeals, currentHour)
        val mealRatios = mealRatios(goalType)
        val recommendations = MealType.entries.map { mealType ->
            val target = if (mealType == nextMeal) {
                (remainingCalories * nextMealRatio(goalType)).roundToInt().coerceAtLeast(baseTarget(mealType, budgetCalories, mealRatios))
            } else {
                baseTarget(mealType, budgetCalories, mealRatios)
            }
            createRecommendation(mealType, target, goalType)
        }
        val primary = recommendations.first { it.mealType == nextMeal }
        return DailyMealPlan(
            remainingCalories = remainingCalories,
            goalType = goalType,
            nextMeal = primary,
            recommendations = recommendations,
            targetMacros = targetMacros(budgetCalories, goalType)
        )
    }

    fun recommendedSnack(remainingCalories: Int, goalType: DietGoalType): PlannedFoodPortion {
        val template = when {
            goalType == DietGoalType.CUT -> mealTemplates.getValue(MealType.SNACK)[0]
            goalType == DietGoalType.GAIN -> mealTemplates.getValue(MealType.SNACK)[1]
            remainingCalories < 300 -> mealTemplates.getValue(MealType.SNACK)[0]
            remainingCalories < 500 -> mealTemplates.getValue(MealType.SNACK)[2]
            else -> mealTemplates.getValue(MealType.SNACK)[1]
        }
        return template.items.first().toPortion()
    }

    fun serializeFavoriteItems(items: List<PlannedFoodPortion>): String {
        return items.joinToString("~") { portion ->
            SavedPortionSnapshot(
                name = portion.food.name,
                grams = portion.grams,
                caloriesPer100g = portion.food.caloriesPer100g,
                proteinPer100g = portion.food.proteinPer100g,
                carbsPer100g = portion.food.carbsPer100g,
                fatPer100g = portion.food.fatPer100g
            ).serialize()
        }
    }

    fun deserializeFavoriteItems(payload: String, mealType: MealType): List<PlannedFoodPortion> {
        if (payload.isBlank()) return emptyList()
        return payload.split("~")
            .mapNotNull { SavedPortionSnapshot.deserialize(it) }
            .map { it.toPortion(mealType) }
    }

    fun summarizeMacros(portions: List<PlannedFoodPortion>): MacroSummary {
        return MacroSummary(
            protein = portions.sumOf { it.protein },
            carbs = portions.sumOf { it.carbs },
            fat = portions.sumOf { it.fat }
        )
    }

    fun targetMacros(budgetCalories: Int, goalType: DietGoalType): MacroSummary {
        val calorieBase = budgetCalories.coerceAtLeast(1200)
        val (proteinRatio, carbsRatio, fatRatio) = when (goalType) {
            DietGoalType.CUT -> Triple(0.32, 0.38, 0.30)
            DietGoalType.MAINTAIN -> Triple(0.26, 0.44, 0.30)
            DietGoalType.GAIN -> Triple(0.28, 0.47, 0.25)
        }
        return MacroSummary(
            protein = ((calorieBase * proteinRatio) / 4.0 * 10.0).roundToInt() / 10.0,
            carbs = ((calorieBase * carbsRatio) / 4.0 * 10.0).roundToInt() / 10.0,
            fat = ((calorieBase * fatRatio) / 9.0 * 10.0).roundToInt() / 10.0
        )
    }

    private fun createRecommendation(mealType: MealType, targetCalories: Int, goalType: DietGoalType): MealPlanRecommendation {
        val template = pickTemplate(mealType, targetCalories, goalType)
        val portions = template.items.map { it.toPortion() }
        return MealPlanRecommendation(
            mealType = mealType,
            title = template.title,
            targetCalories = targetCalories,
            items = portions
        )
    }

    private fun pickTemplate(mealType: MealType, targetCalories: Int, goalType: DietGoalType): Template {
        val templates = mealTemplates.getValue(mealType)
        return when {
            goalType == DietGoalType.CUT -> templates[goalTemplateIndex(mealType, goalType)]
            goalType == DietGoalType.GAIN -> templates[goalTemplateIndex(mealType, goalType)]
            targetCalories < 420 -> templates.first()
            targetCalories > 640 -> templates.last()
            else -> templates[goalTemplateIndex(mealType, goalType)]
        }
    }

    private fun baseTarget(mealType: MealType, budgetCalories: Int, mealRatios: Map<MealType, Double>): Int =
        (budgetCalories * mealRatios.getValue(mealType)).roundToInt().coerceAtLeast(160)

    private fun suggestNextMeal(completedMeals: Set<MealType>, currentHour: Int): MealType {
        return when {
            currentHour < 10 && MealType.BREAKFAST !in completedMeals -> MealType.BREAKFAST
            currentHour < 15 && MealType.LUNCH !in completedMeals -> MealType.LUNCH
            currentHour < 20 && MealType.DINNER !in completedMeals -> MealType.DINNER
            else -> MealType.SNACK
        }
    }

    private fun Pair<String, Int>.toPortion(): PlannedFoodPortion {
        val food = requireNotNull(BasicFoodCatalog.findByCode(first))
        return PlannedFoodPortion(food = food, grams = second)
    }

    private fun mealRatios(goalType: DietGoalType): Map<MealType, Double> {
        return when (goalType) {
            DietGoalType.CUT -> mapOf(
                MealType.BREAKFAST to 0.25,
                MealType.LUNCH to 0.35,
                MealType.DINNER to 0.25,
                MealType.SNACK to 0.15
            )
            DietGoalType.MAINTAIN -> mapOf(
                MealType.BREAKFAST to 0.28,
                MealType.LUNCH to 0.37,
                MealType.DINNER to 0.25,
                MealType.SNACK to 0.10
            )
            DietGoalType.GAIN -> mapOf(
                MealType.BREAKFAST to 0.27,
                MealType.LUNCH to 0.33,
                MealType.DINNER to 0.28,
                MealType.SNACK to 0.12
            )
        }
    }

    private fun nextMealRatio(goalType: DietGoalType): Double {
        return when (goalType) {
            DietGoalType.CUT -> 0.34
            DietGoalType.MAINTAIN -> 0.40
            DietGoalType.GAIN -> 0.46
        }
    }

    private fun goalTemplateIndex(mealType: MealType, goalType: DietGoalType): Int {
        return when (goalType) {
            DietGoalType.CUT -> when (mealType) {
                MealType.BREAKFAST -> 0
                MealType.LUNCH -> 0
                MealType.DINNER -> 2
                MealType.SNACK -> 0
            }
            DietGoalType.MAINTAIN -> 1
            DietGoalType.GAIN -> when (mealType) {
                MealType.BREAKFAST -> 2
                MealType.LUNCH -> 2
                MealType.DINNER -> 1
                MealType.SNACK -> 1
            }
        }
    }

    private data class Template(
        val title: String,
        val items: List<Pair<String, Int>>
    )
}
