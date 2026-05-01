package com.example.dragonbudget.engine

import com.example.dragonbudget.data.*

/**
 * PromptBuilder — Builds structured prompts for LiteRT-LM Gemma.
 *
 * Design decisions for NPU efficiency:
 * - System prompt is SHORT (fewer tokens = faster TTFT)
 * - Spending data is structured with clear labels
 * - Output is constrained to 40 words max
 */
object PromptBuilder {

    fun buildAdvicePrompt(
        dragon: DragonState,
        categories: List<BudgetCategoryWithSpent>,
        userQuestion: String
    ): String {
        val budgetLines = categories.joinToString("\n") { cat ->
            "${cat.name}: \$${String.format("%.2f", cat.spentAmount)} / \$${String.format("%.2f", cat.weeklyLimit)}"
        }

        return """
You are SnapDragon, an offline budgeting companion.
Use only the local spending data below.
Do not invent transactions.
Keep response under 40 words.

Dragon state:
Health: ${dragon.health}/100
Mood: ${dragon.mood}
Level: ${dragon.level}
Streak: ${dragon.streakDays} days

Budget summary:
$budgetLines

User question:
$userQuestion

Return a short friendly answer.
        """.trimIndent()
    }

    /**
     * Build a prompt for parsing raw OCR text into a structured list of items.
     */
    fun buildOCRParsePrompt(rawText: String): String {
        return """
Extract the merchant name and a list of individual line items from this receipt OCR text.
For each item, identify the name, price, and suggested category.
Categories: Food, Groceries, Gas, School, Entertainment, Shopping, Household, Other.

Return ONLY a valid JSON object in this exact format:
{
  "merchant": "Store Name",
  "items": [
    {"name": "Item Name", "price": 0.00, "category": "Category"},
    ...
  ],
  "total": 0.00
}

OCR Text:
$rawText
        """.trimIndent()
    }
}
