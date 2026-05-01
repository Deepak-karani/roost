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
        userQuestion: String,
        overallWeeklyBudget: Double = 0.0
    ): String {
        // Pre-compute every number we want Gemma to quote back. Gemma shouldn't
        // do arithmetic itself — small LMs get sums and ratios wrong frequently.
        //
        // Two distinct budget concepts go into the prompt:
        //
        //   • overallWeeklyBudget — the top-level number the user set on
        //     first launch ("Welcome to Roost — what's your weekly budget?").
        //     This is what they think of as "my budget".
        //   • allocated — the sum of per-category weekly limits (allocations
        //     the user has split out of the overall). Categories without an
        //     allocation are still trackable; their spending counts against
        //     the unallocated remainder.
        //
        // The model needs both so it can answer "what's my budget" with the
        // overall *and* answer "how much do I have for food" with the
        // category line.
        val allocated = categories.sumOf { it.weeklyLimit }
        val unallocated = (overallWeeklyBudget - allocated).coerceAtLeast(0.0)
        val totalSpent = categories.sumOf { it.spentAmount }
        val remainingOverall = (overallWeeklyBudget - totalSpent).coerceAtLeast(0.0)
        val percentUsed = if (overallWeeklyBudget > 0.0) (totalSpent / overallWeeklyBudget) * 100 else 0.0

        // Show every category that has a limit OR has been spent against —
        // hide truly empty ones so Gemma doesn't list a wall of $0 buckets.
        val activeCats = categories.filter { it.weeklyLimit > 0 || it.spentAmount > 0 }
        val budgetLines = if (activeCats.isEmpty()) {
            "(no categories with allocations yet)"
        } else {
            activeCats.joinToString("\n") { cat ->
                val pct = if (cat.weeklyLimit > 0) ((cat.spentAmount / cat.weeklyLimit) * 100).toInt() else 0
                "- ${cat.name}: \$${String.format("%.2f", cat.spentAmount)} spent of \$${String.format("%.2f", cat.weeklyLimit)} allocated ($pct%)"
            }
        }

        // Flag categories that are over or near limit so Gemma can prioritise them.
        val overLimit = activeCats.filter { it.weeklyLimit > 0 && it.spentAmount > it.weeklyLimit }
            .joinToString(", ") { it.name }
        val nearLimit = activeCats.filter {
            it.weeklyLimit > 0 &&
                it.spentAmount <= it.weeklyLimit &&
                it.spentAmount / it.weeklyLimit >= 0.8
        }.joinToString(", ") { it.name }

        val flags = buildString {
            if (overallWeeklyBudget <= 0.0) append("No overall budget set yet. ")
            if (overLimit.isNotEmpty()) append("Over budget: $overLimit. ")
            if (nearLimit.isNotEmpty()) append("Close to limit: $nearLimit. ")
            if (isEmpty()) append("All categories on track.")
        }

        return """
You are ${dragon.name}, an offline budgeting companion. Be brief, friendly, and use ONLY the numbers below — do not invent transactions, do not recompute totals, do not round. Reply in 40 words or fewer.

Dragon state:
- Health: ${dragon.health}/100
- Mood: ${dragon.mood}
- Level: ${dragon.level}
- Logging streak: ${dragon.streakDays} days

Top-level budget (the user's "weekly budget"):
- Overall weekly budget: ${'$'}${String.format("%.2f", overallWeeklyBudget)}
- Total spent this week: ${'$'}${String.format("%.2f", totalSpent)}
- Remaining of overall: ${'$'}${String.format("%.2f", remainingOverall)}
- ${String.format("%.0f", percentUsed)}% of overall budget used

Category allocations (split out of the overall):
- Allocated across categories: ${'$'}${String.format("%.2f", allocated)}
- Unallocated remainder of overall: ${'$'}${String.format("%.2f", unallocated)}

Per category:
$budgetLines

Status flags: $flags

User question: $userQuestion

Answering rules:
- If the user asks about THEIR BUDGET, MY BUDGET, or "weekly budget", quote the OVERALL weekly budget and what's remaining of it — NOT the sum of category allocations.
- If the user asks about a specific category (food, gas, etc.), quote that category's line.
- Never recompute. Never round.
        """.trimIndent()
    }

    /**
     * Build a prompt for parsing raw OCR text into a structured list of items.
     */
    fun buildOCRParsePrompt(rawText: String, availableCategories: List<String> = listOf(Categories.FOOD)): String {
        val catsLine = if (availableCategories.isEmpty()) Categories.FOOD
        else availableCategories.joinToString(", ")
        return """
You are a receipt parser. Extract EVERY line item from this OCR text.

LAYOUT WARNING: phone-camera OCR commonly returns the receipt in TWO COLUMNS,
where ALL ITEM NAMES appear first (top of the OCR text), then ALL PRICES
appear together at the bottom. When you see this pattern:
  • Take the list of N item names in order they appear.
  • Take the list of N+ price numbers at the bottom in order they appear.
  • Pair them position-by-position (1st name <-> 1st price, etc.).
  • The LAST price in the bottom block is usually the receipt TOTAL — do
    not pair it with an item.
  • A price line that follows "TIP", "GRATUITY", "SERVICE", "SUBTOTAL", or
    "TOTAL" labels is its own field, not an item price.

Skip these from the items list (they are NOT items):
- Subtotal, tax, total, balance, change, amount due, payment method
- Phone numbers, addresses, table numbers, "thank you", store hours, URLs
- Any standalone integer with no decimal point that is clearly a check
  number, table number, or transaction ID (e.g. "22395", "Tbl: H17")

A real price has a decimal point (e.g. "29.00", "311.20"). Numbers with no
decimal in this dataset are usually NOT prices — be conservative.

ALSO extract these as separate fields when present:
- "tip" / "gratuity" -> tip
- "service charge" / "auto-gratuity" / "svc fee" / "X% service" -> service_charge

Categories: $catsLine.
For each item pick the closest category from that list. If nothing fits, pick the first one.

Return ONLY a valid JSON object. Include EVERY item you find — do not truncate.
Use 0.00 for tip / service_charge if they aren't on the receipt.

{
  "merchant": "Store Name",
  "items": [
    {"name": "Item Name", "price": 0.00, "category": "Category"}
  ],
  "tip": 0.00,
  "service_charge": 0.00,
  "total": 0.00
}

OCR Text:
$rawText
        """.trimIndent()
    }
}
