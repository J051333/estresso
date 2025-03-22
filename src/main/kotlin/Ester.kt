package com.josiwhitlock.estresso

enum class Ester(val d: Double, val k1: Double, val k2: Double, val k3: Double) {
    VALERATE(478.0, 0.236, 4.85, 1.24),
    ENANTHATE(191.4, 0.119, 0.601, 0.402),
    CYPIONATE(246.0, 0.0825, 3.57, 0.669),
    BENZOATE(1893.1, 0.67, 61.5, 4.34),
    UNDECYLATE(471.5, 0.01729, 6.528, 2.285),
}

//        "EUn casubq" to listOf(16.15, 0.046, 0.022, 0.101),
//        "patch tw" to listOf(16.792, 0.283, 5.592, 4.3),
//        "patch ow" to listOf(59.481, 0.107, 7.842, 5.193),