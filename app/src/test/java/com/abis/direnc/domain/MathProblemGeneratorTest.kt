package com.abis.direnc.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MathProblemGeneratorTest {

    @Test
    fun `answer her zaman question ile tutarli`() {
        // 200 farklı tohumda üretilen sorunun cevabı doğru olmalı
        repeat(200) { seed ->
            val p = MathProblemGenerator.generate(Random(seed.toLong()))
            assertEquals(
                "Soru: ${p.question}",
                evaluate(p.question),
                p.answer
            )
        }
    }

    @Test
    fun `orta zorluk araliginda kalir`() {
        repeat(200) { seed ->
            val p = MathProblemGenerator.generate(Random(seed.toLong()))
            // Sonuç negatif olmamalı ve makul büyüklükte kalmalı (kafadan/kâğıtla)
            assertTrue("Cevap negatif olmamalı: ${p.question}", p.answer >= 0)
            assertTrue("Cevap çok büyük olmamalı: ${p.question}", p.answer <= 100000)
        }
    }

    // Test yardımcı: "a op b" stringini değerlendirir (yalnızca test amaçlı)
    private fun evaluate(q: String): Int {
        val cleaned = q.removeSuffix(" = ?").trim()
        return when {
            cleaned.contains('×') -> {
                val (a, b) = cleaned.split('×').map { it.trim().toInt() }
                a * b
            }
            cleaned.contains('+') -> {
                val (a, b) = cleaned.split('+').map { it.trim().toInt() }
                a + b
            }
            cleaned.contains('-') -> {
                val (a, b) = cleaned.split('-').map { it.trim().toInt() }
                a - b
            }
            else -> error("Bilinmeyen işlem: $cleaned")
        }
    }
}
