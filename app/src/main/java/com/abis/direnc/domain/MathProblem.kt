package com.abis.direnc.domain

import kotlin.random.Random

data class MathProblem(val question: String, val answer: Int)

object MathProblemGenerator {

    /**
     * Orta zorlukta, hesap makinesi gerektirmeyen ama düşündüren bir işlem üretir.
     * Üç tür: 2 basamaklı × 1 basamaklı, 3 basamaklı + 3 basamaklı, 3 basamaklı - 3 basamaklı.
     */
    fun generate(random: Random = Random.Default): MathProblem {
        return when (random.nextInt(3)) {
            0 -> {
                val a = random.nextInt(12, 100)   // 12..99
                val b = random.nextInt(3, 10)     // 3..9
                MathProblem("$a × $b = ?", a * b)
            }
            1 -> {
                val a = random.nextInt(100, 1000)
                val b = random.nextInt(100, 1000)
                MathProblem("$a + $b = ?", a + b)
            }
            else -> {
                val hi = random.nextInt(500, 1000)
                val lo = random.nextInt(100, 500)
                MathProblem("$hi - $lo = ?", hi - lo)
            }
        }
    }
}
