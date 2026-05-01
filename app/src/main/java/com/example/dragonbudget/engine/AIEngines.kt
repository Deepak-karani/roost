package com.example.dragonbudget.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.util.Log
import com.example.dragonbudget.data.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// ──────────────────────────────────────────────
// LLM Engine Interface
// ──────────────────────────────────────────────

interface LocalLLMEngine {
    suspend fun generateBudgetAdvice(prompt: String): String
}

// ──────────────────────────────────────────────
// Receipt Vision Interface
// ──────────────────────────────────────────────

interface ReceiptVisionEngine {
    suspend fun extractPurchaseFromImage(imageUri: Uri): PurchaseDraft
    suspend fun extractReceiptItems(imageUri: Uri): ReceiptScanResult
}

// ──────────────────────────────────────────────
// LiteRT Gemma Engine (Production — no fallback)
// ──────────────────────────────────────────────

/**
 * Production LiteRT-LM Gemma Engine.
 * Runs Gemma 4 on the Qualcomm Snapdragon NPU via LiteRT-LM.
 */
class LiteRtGemmaEngine(private val liteRTLMManager: com.example.qnn_litertlm_gemma.LiteRTLMManager) : LocalLLMEngine {
    override suspend fun generateBudgetAdvice(prompt: String): String {
        if (!liteRTLMManager.isEngineReady()) {
            return "SnapDragon's brain is still waking up (Engine not initialized). Please wait a moment or check if the model file is in your Downloads folder."
        }
        return try {
            val response = StringBuilder()
            liteRTLMManager.startConversation()
            liteRTLMManager.sendMessage(prompt).collect { chunk ->
                response.append(chunk)
            }
            response.toString()
        } catch (e: Exception) {
            "SnapDragon encountered an error: ${e.message}"
        }
    }
}

// ──────────────────────────────────────────────
// ML Kit OCR Receipt Vision Engine (Production)
// ──────────────────────────────────────────────

class MLKitReceiptVisionEngine(
    private val context: Context,
    private val liteRTLMManager: com.example.qnn_litertlm_gemma.LiteRTLMManager
) : ReceiptVisionEngine {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.Builder().build())

    override suspend fun extractPurchaseFromImage(imageUri: Uri): PurchaseDraft {
        val result = extractReceiptItems(imageUri)
        return PurchaseDraft(
            merchant = result.merchant,
            amount = result.total,
            suggestedCategory = result.items.firstOrNull()?.suggestedCategory ?: "Other",
            confidence = result.confidence
        )
    }

    /**
     * Image preprocessing for folded/messy receipts:
     * 1. Convert to grayscale (removes color noise from folds/stains)
     * 2. Boost contrast (makes faded text from fold creases readable)
     * 3. Sharpen edges (helps with blurry text from camera shake)
     */
    private suspend fun preprocessImage(imageUri: Uri): Bitmap = withContext(Dispatchers.Default) {
        val inputStream = context.contentResolver.openInputStream(imageUri)
        val original = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        val width = original.width
        val height = original.height

        val enhanced = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(enhanced)
        val paint = Paint()

        // Grayscale + high contrast color matrix
        // This dramatically improves OCR on:
        //  - Folded receipts (shadows become less prominent)
        //  - Faded thermal paper (text gets darker)
        //  - Messy/stained receipts (color noise removed)
        val contrast = 1.8f  // Boost contrast
        val translate = (-(128f * contrast) + 128f)

        val colorMatrix = ColorMatrix(floatArrayOf(
            // Grayscale conversion (luminance weights) + contrast boost
            0.299f * contrast, 0.587f * contrast, 0.114f * contrast, 0f, translate,
            0.299f * contrast, 0.587f * contrast, 0.114f * contrast, 0f, translate,
            0.299f * contrast, 0.587f * contrast, 0.114f * contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(original, 0f, 0f, paint)
        original.recycle()

        Log.d("VisionEngine", "Preprocessed image: ${width}x${height}, contrast=1.8x")
        enhanced
    }

    override suspend fun extractReceiptItems(imageUri: Uri): ReceiptScanResult {
        Log.d("VisionEngine", "Starting receipt scan for URI: $imageUri")

        // Step 1: Preprocess image for better OCR on messy receipts
        val enhancedBitmap = try {
            preprocessImage(imageUri)
        } catch (e: Exception) {
            Log.w("VisionEngine", "Image preprocessing failed, using original: ${e.message}")
            null
        }

        // Step 2: Run OCR on the enhanced image (or original as fallback)
        val rawText = if (enhancedBitmap != null) {
            val enhancedImage = InputImage.fromBitmap(enhancedBitmap, 0)
            val enhancedResult = recognizer.process(enhancedImage).await()
            enhancedBitmap.recycle()

            val enhancedText = enhancedResult.text
            Log.d("VisionEngine", "Enhanced OCR: ${enhancedText.length} chars")

            // Also run on the original for comparison
            val originalImage = InputImage.fromFilePath(context, imageUri)
            val originalResult = recognizer.process(originalImage).await()
            val originalText = originalResult.text
            Log.d("VisionEngine", "Original OCR: ${originalText.length} chars")

            // Use whichever got more text (more = better for messy receipts)
            if (enhancedText.length >= originalText.length) {
                Log.d("VisionEngine", "Using ENHANCED image (more text)")
                enhancedText
            } else {
                Log.d("VisionEngine", "Using ORIGINAL image (more text)")
                originalText
            }
        } else {
            val image = InputImage.fromFilePath(context, imageUri)
            recognizer.process(image).await().text
        }

        Log.d("VisionEngine", "=== FINAL OCR TEXT (${rawText.length} chars) ===")
        Log.d("VisionEngine", rawText)
        Log.d("VisionEngine", "=== END OCR ===")

        if (rawText.isBlank()) {
            return ReceiptScanResult(merchant = "Unknown", items = emptyList(), total = 0.0, confidence = 0f)
        }

        // Try Gemma-based parsing first (if engine is ready)
        if (liteRTLMManager.isEngineReady()) {
            try {
                val prompt = PromptBuilder.buildOCRParsePrompt(rawText)
                val response = StringBuilder()
                liteRTLMManager.startConversation()
                liteRTLMManager.sendMessage(prompt).collect { chunk ->
                    response.append(chunk)
                }
                val parsed = parseGemmaResponse(response.toString())
                if (parsed != null && parsed.items.isNotEmpty()) {
                    Log.d("VisionEngine", "Gemma parsed: ${parsed.items.size} items")
                    return parsed
                }
            } catch (e: Exception) {
                Log.w("VisionEngine", "Gemma parsing failed: ${e.message}")
            }
        }

        // Smart multi-item regex parser
        return smartParseReceipt(rawText)
    }

    private fun parseGemmaResponse(response: String): ReceiptScanResult? {
        return try {
            // Find the main JSON block
            val jsonStr = Regex("""\{[\s\S]*\}""").find(response)?.value ?: return null
            
            // Extract merchant
            val merchant = Regex(""""merchant"\s*:\s*"([^"]+)"""").find(jsonStr)?.groupValues?.get(1) ?: "Unknown"
            
            // Extract total
            val total = Regex(""""total"\s*:\s*(\d+\.?\d*)""").find(jsonStr)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
            
            // Extract items array content
            val itemsMatch = Regex(""""items"\s*:\s*\[([\s\S]*?)\]""").find(jsonStr)
            val itemsList = mutableListOf<ReceiptLineItem>()
            
            if (itemsMatch != null) {
                val itemsArrayStr = itemsMatch.groupValues[1]
                // Match each item object: {"name": "...", "price": 0.00, "category": "..."}
                val itemRegex = Regex("""\{\s*"name"\s*:\s*"([^"]+)"\s*,\s*"price"\s*:\s*(\d+\.?\d*)\s*,\s*"category"\s*:\s*"([^"]+)"\s*\}""")
                itemRegex.findAll(itemsArrayStr).forEach { match ->
                    val name = match.groupValues[1]
                    val price = match.groupValues[2].toDoubleOrNull() ?: 0.0
                    val category = match.groupValues[3]
                    if (price > 0) {
                        itemsList.add(ReceiptLineItem(itemName = name, price = price, suggestedCategory = category))
                    }
                }
            }

            if (itemsList.isNotEmpty() || total > 0) {
                ReceiptScanResult(
                    merchant = merchant,
                    items = if (itemsList.isNotEmpty()) itemsList else listOf(ReceiptLineItem("Total", total, "Other")),
                    total = if (total > 0) total else itemsList.sumOf { it.price },
                    confidence = 0.95f
                )
            } else null
        } catch (e: Exception) {
            Log.e("VisionEngine", "Failed to parse Gemma response: ${e.message}")
            null
        }
    }

    // ──────────────────────────────────────────────
    // Improved Multi-Item Receipt Parser
    // ──────────────────────────────────────────────

    private fun smartParseReceipt(rawText: String): ReceiptScanResult {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        val merchant = extractMerchant(lines)
        val items = mutableListOf<ReceiptLineItem>()
        var totalAmount = 0.0

        // Lines to SKIP (metadata, not products)
        val skipWords = setOf(
            "subtotal", "sub total", "total", "tax", "change due",
            "cash", "credit", "debit", "visa", "mastercard", "amex",
            "card", "balance", "tender", "payment", "amount due",
            "thank", "welcome", "receipt", "store #", "phone", "tel",
            "date", "time", "trans", "ref #", "auth", "www.", ".com",
            "address", "city", "state", "zip", "savings", "discount",
            "member", "loyalty", "rewards", "coupon", "register",
            "cashier", "terminal", "approved", "return", "refund"
        )

        // Find the TOTAL first so we know the receipt's total
        val totalPatterns = listOf(
            Regex("""(?i)\b(?:grand\s*)?total\s*:?\s*\$?\s*(\d{1,6}\.\d{2})"""),
            Regex("""(?i)amount\s*due\s*:?\s*\$?\s*(\d{1,6}\.\d{2})"""),
            Regex("""(?i)balance\s*due\s*:?\s*\$?\s*(\d{1,6}\.\d{2})"""),
        )
        for (line in lines) {
            for (pat in totalPatterns) {
                pat.find(line)?.let {
                    val t = it.groupValues[1].toDoubleOrNull()
                    if (t != null && t > totalAmount) totalAmount = t
                }
            }
        }

        // ── Extract line items ──
        // Multiple patterns to handle different receipt formats

        for (line in lines) {
            val lower = line.lowercase()

            // Skip metadata lines
            if (skipWords.any { lower.contains(it) }) continue
            if (line.length < 3) continue
            // Skip lines that are only digits, dates, times, phone numbers
            if (lower.matches(Regex("""^[\d\s/:\\.-]+$"""))) continue
            // Skip lines that look like addresses
            if (lower.matches(Regex(""".*\d{5}(-\d{4})?$"""))) continue

            // Try to find a price on this line
            val price = extractPrice(line)
            if (price != null && price > 0.01 && price < 999) {
                val itemName = extractItemName(line, price)
                if (itemName.length >= 2) {
                    items.add(ReceiptLineItem(
                        itemName = itemName,
                        price = price,
                        suggestedCategory = inferCategory(itemName)
                    ))
                    Log.d("VisionEngine", "  ITEM: '$itemName' -> \$$price")
                }
            }
        }

        // If no total found, sum the items
        if (totalAmount == 0.0 && items.isNotEmpty()) {
            totalAmount = items.sumOf { it.price }
        }

        // Remove duplicate items (same name and price)
        val uniqueItems = items.distinctBy { "${it.itemName.lowercase()}_${it.price}" }

        val confidence = when {
            uniqueItems.size >= 5 -> 0.9f
            uniqueItems.size >= 3 -> 0.85f
            uniqueItems.isNotEmpty() -> 0.7f
            totalAmount > 0 -> 0.5f
            else -> 0.2f
        }

        Log.d("VisionEngine", "RESULT: merchant='$merchant', ${uniqueItems.size} items, total=\$$totalAmount")

        return ReceiptScanResult(
            merchant = merchant,
            items = uniqueItems,
            total = totalAmount,
            confidence = confidence
        )
    }

    /**
     * Extract the price from a line. Handles multiple formats:
     * - "MILK 2% GAL    3.99"
     * - "MILK 2% GAL    $3.99"
     * - "MILK 2% GAL    3.99 F"  (tax code suffix)
     * - "3.99 MILK 2% GAL"
     * - "$3.99   MILK 2% GAL"
     */
    private fun extractPrice(line: String): Double? {
        // Pattern 1: Price at the END of line (most common)
        // Captures: "anything   3.99" or "anything   $3.99" or "anything 3.99 F"
        val endPrice = Regex("""\$?\s*(\d{1,4}\.\d{2})\s*[A-Za-z]?\s*$""")
        endPrice.find(line)?.let {
            return it.groupValues[1].toDoubleOrNull()
        }

        // Pattern 2: Price at the START of line
        val startPrice = Regex("""^\s*\$?\s*(\d{1,4}\.\d{2})\s""")
        startPrice.find(line)?.let {
            return it.groupValues[1].toDoubleOrNull()
        }

        return null
    }

    /**
     * Extract the item name by removing the price and cleaning up.
     */
    private fun extractItemName(line: String, price: Double): String {
        val priceStr = String.format("%.2f", price)

        // Remove all price-related text from the line
        var name = line
            .replace("\$$priceStr", "")
            .replace(priceStr, "")
            .replace("$", "")
            // Remove common prefixes like "1x", "2 x", quantity codes
            .replace(Regex("""^\s*\d+\s*[xX@]\s*"""), "")
            // Remove trailing tax codes like "F", "T", "N"
            .replace(Regex("""\s+[FTNOAB]\s*$"""), "")
            // Remove item codes/SKUs at start (e.g., "004011  BANANA")
            .replace(Regex("""^\s*\d{4,}\s+"""), "")
            // Remove leading/trailing special chars
            .replace(Regex("""^[\s*#\-_]+"""), "")
            .replace(Regex("""[\s*#\-_]+$"""), "")
            .trim()

        // If the name is too short or just numbers, return empty
        if (name.length < 2 || name.matches(Regex("""^\d+$"""))) return ""

        // Capitalize nicely
        return name.split(Regex("""\s+""")).joinToString(" ") { word ->
            if (word.length <= 2 && word.all { it.isUpperCase() }) word  // Keep abbreviations like "2%"
            else word.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * Known store names for fuzzy matching against OCR text.
     * OCR often garbles logos, so we scan the ENTIRE receipt for these.
     */
    private val knownStores = listOf(
        "Trader Joe's", "Trader Joes", "Whole Foods", "Walmart", "Target",
        "Costco", "Kroger", "Safeway", "Albertsons", "Publix", "Aldi",
        "Wegmans", "H-E-B", "HEB", "Meijer", "WinCo", "Food Lion",
        "Stop & Shop", "Giant", "ShopRite", "Sprouts", "Fresh Market",
        "Sam's Club", "BJ's", "Ralph's", "Ralphs", "Vons", "Pavilions",
        "Harris Teeter", "Piggly Wiggly", "Hy-Vee", "Food 4 Less",
        "Dollar Tree", "Dollar General", "Family Dollar", "Five Below",
        "CVS", "Walgreens", "Rite Aid", "7-Eleven", "7 Eleven",
        "Starbucks", "Dunkin", "McDonald's", "McDonalds", "Subway",
        "Chipotle", "Chick-fil-A", "Wendy's", "Wendys", "Taco Bell",
        "Burger King", "Panda Express", "In-N-Out", "Five Guys",
        "Panera", "Jimmy John's", "Jimmy Johns", "Domino's", "Dominos",
        "Papa John's", "Papa Johns", "Pizza Hut", "Little Caesars",
        "Popeyes", "KFC", "Arby's", "Arbys", "Jack in the Box",
        "Home Depot", "Lowe's", "Lowes", "Menards", "Ace Hardware",
        "Best Buy", "Apple Store", "Amazon", "Nordstrom", "Macy's", "Macys",
        "TJ Maxx", "Marshalls", "Ross", "Old Navy", "Gap", "Nike",
        "Bath & Body Works", "Sephora", "Ulta", "GameStop",
        "Shell", "Chevron", "BP", "ExxonMobil", "Mobil", "Texaco",
        "76", "Circle K", "Wawa", "QuikTrip", "Sheetz", "RaceTrac",
        "Trader Joe", "TRADER JOE", "TJ", "WF Market", "Whole Fds"
    )

    /**
     * Extract merchant by prioritizing the very first clean line at the top,
     * then scanning for known store names, then falling back to smart scoring.
     */
    private fun extractMerchant(lines: List<String>): String {
        // Step 1: Immediate priority - check the first 3 lines for a "very clean" name
        // (Mostly letters, 4-30 chars, no numbers/addresses)
        for (line in lines.take(3)) {
            val cleaned = line.trim()
            val lower = cleaned.lowercase()
            
            // Criteria for a "perfect" top-of-receipt merchant name
            if (cleaned.length in 4..30 && 
                cleaned.count { it.isLetter() }.toFloat() / cleaned.length > 0.8 &&
                !cleaned.any { it.isDigit() } &&
                !lower.contains("welcome") && !lower.contains("receipt") && 
                !lower.contains("store") && !lower.contains("phone")
            ) {
                val name = cleaned.split(Regex("""\s+""")).joinToString(" ") { word ->
                    word.lowercase().replaceFirstChar { it.uppercase() }
                }
                Log.d("VisionEngine", "Found perfect merchant at top: '$name'")
                return name
            }
        }

        val fullText = lines.joinToString(" ")
        val fullLower = fullText.lowercase()

        // Step 2: Check for known store names anywhere in the receipt
        for (store in knownStores) {
            if (fullLower.contains(store.lowercase())) {
                Log.d("VisionEngine", "Matched known store: $store")
                return when {
                    store.lowercase().contains("trader joe") -> "Trader Joe's"
                    store.lowercase().contains("whole f") || store.lowercase().contains("wf market") -> "Whole Foods"
                    store.lowercase().contains("mcdonalds") -> "McDonald's"
                    store.lowercase().contains("wendys") -> "Wendy's"
                    else -> store
                }
            }
        }

        // Step 3: Look for the best candidate line in the first 10 lines (Smart Scoring)
        val candidates = mutableListOf<Pair<String, Int>>()
        for ((index, line) in lines.take(10).withIndex()) {
            val cleaned = line.trim()
            if (cleaned.length < 4 || cleaned.length > 50) continue
            if (cleaned.matches(Regex("""^[\d\s./$#,:\-()]+$"""))) continue

            val lower = cleaned.lowercase()
            if (lower.contains("receipt") || lower.contains("date:") ||
                lower.contains("time:") || lower.contains("tel") ||
                lower.contains("phone") || lower.contains("store #") ||
                lower.contains("www.") || lower.contains(".com") ||
                lower.contains("address") || lower.contains("welcome") ||
                lower.matches(Regex(""".*\d{3}[-.]\d{3}[-.]\d{4}.*""")) ||
                lower.matches(Regex(""".*\d{5}.*"""))) continue

            val letterRatio = cleaned.count { it.isLetter() }.toFloat() / cleaned.length
            val positionBonus = (10 - index) * 2 // Give even more weight to top lines
            val lengthBonus = cleaned.length.coerceAtMost(20)
            val score = (letterRatio * 30 + positionBonus + lengthBonus).toInt()

            candidates.add(cleaned to score)
        }

        val best = candidates.maxByOrNull { it.second }
        if (best != null) {
            val name = best.first.split(Regex("""\s+""")).joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
            Log.d("VisionEngine", "Best scored merchant candidate: '$name' (score=${best.second})")
            return name
        }

        return "Store"
    }

    /**
     * Infer budget category from item name keywords.
     */
    private fun inferCategory(itemName: String): String {
        val lower = itemName.lowercase()
        return when {
            listOf("milk", "bread", "egg", "cheese", "butter", "yogurt", "chicken", "beef",
                "pork", "fish", "salmon", "shrimp", "rice", "pasta", "cereal", "fruit", "apple",
                "banana", "orange", "lemon", "lime", "tomato", "lettuce", "onion", "potato",
                "carrot", "broccoli", "spinach", "pepper", "cucumber", "avocado", "mushroom",
                "garlic", "ginger", "celery", "corn", "grape", "berry", "melon", "mango",
                "vegetable", "organic", "produce", "frozen", "canned", "juice", "water",
                "soda", "snack", "chip", "cracker", "cookie", "flour", "sugar", "salt",
                "oil", "sauce", "soup", "bean", "deli", "ham", "turkey", "bacon", "sausage",
                "cream", "ice cream", "candy", "chocolate", "gum", "nut", "almond", "peanut"
            ).any { lower.contains(it) } -> Categories.GROCERIES

            listOf("coffee", "latte", "mocha", "espresso", "tea", "burger", "pizza", "taco",
                "sandwich", "wrap", "salad", "meal", "combo", "drink", "fries", "wing", "sub",
                "bowl", "plate", "entree", "appetizer", "dessert", "cake", "donut", "bagel",
                "smoothie", "boba", "sushi", "ramen", "noodle", "curry", "kebab", "gyro",
                "burrito", "quesadilla", "nachos"
            ).any { lower.contains(it) } -> Categories.FOOD

            listOf("gas", "fuel", "diesel", "unleaded", "premium", "regular", "gallon"
            ).any { lower.contains(it) } -> Categories.GAS

            listOf("movie", "ticket", "game", "play", "show", "concert", "museum",
                "netflix", "spotify", "subscription", "streaming", "arcade"
            ).any { lower.contains(it) } -> Categories.ENTERTAINMENT

            listOf("shirt", "pants", "shoe", "dress", "jacket", "clothes", "jeans",
                "accessory", "watch", "bag", "purse", "electronics", "phone", "case",
                "charger", "cable", "book", "toy", "gift"
            ).any { lower.contains(it) } -> Categories.SHOPPING

            listOf("soap", "detergent", "cleaner", "towel", "tissue", "toilet", "paper",
                "trash", "bag", "sponge", "broom", "mop", "bulb", "battery", "dish",
                "laundry", "bleach", "wipe", "spray"
            ).any { lower.contains(it) } -> Categories.HOUSEHOLD

            listOf("notebook", "pen", "pencil", "folder", "binder", "textbook",
                "calculator", "tuition", "school", "class"
            ).any { lower.contains(it) } -> Categories.SCHOOL

            else -> Categories.OTHER
        }
    }
}
