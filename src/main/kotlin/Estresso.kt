package com.josiwhitlock.estresso

import kotlin.math.exp

/**
 * Built based on estrannaise.js
 * https://github.com/WHSAH/estrannaise.js
 */
object Estresso {
    val PKParameters: Map<String, List<Double>> = mapOf(
        "EV" to listOf(478.0, 0.236, 4.85, 1.24),
        "EEn" to listOf(191.4, 0.119, 0.601, 0.402),
        "EC" to listOf(246.0, 0.0825, 3.57, 0.669),
        "EB" to listOf(1893.1, 0.67, 61.5, 4.34),
        "EUn" to listOf(471.5, 0.01729, 6.528, 2.285),
        "EUn casubq" to listOf(16.15, 0.046, 0.022, 0.101),
        "patch tw" to listOf(16.792, 0.283, 5.592, 4.3),
        "patch ow" to listOf(59.481, 0.107, 7.842, 5.193),
    )

    fun PKFunctions(conversionFactor: Double = 1.0): Map<String, (Double, Double, Boolean, Double) -> Double> {
        fun callE2Curve3C(
            key: String,
            t: Double,
            dose: Double,
            steadystate: Boolean = false,
            T: Double = 1.0
        ): Double {
            val parameters = PKParameters[key] ?: throw IllegalArgumentException("Key $key not a valid key.")

            return e2Curve3C(
                t,
                dose,
                parameters[0],
                parameters[1],
                parameters[2],
                parameters[3],
                Ds = 0.0,
                D2 = 0.0,
                steadystate = steadystate,
                T = T
            )
        }

        return mapOf(
            "EV" to { t: Double, dose: Double, steadystate: Boolean, T: Double ->
                callE2Curve3C("EV", t, dose, steadystate, T)
            },
            "EEn im" to { t: Double, dose: Double, steadystate: Boolean, T: Double ->
                callE2Curve3C("EEn", t, dose, steadystate, T)
            },
            "EC im" to { t: Double, dose: Double, steadystate: Boolean, T: Double ->
                callE2Curve3C("EC", t, dose, steadystate, T)
            },
            "EUn im" to { t: Double, dose: Double, steadystate: Boolean, T: Double ->
                callE2Curve3C("EUn", t, dose, steadystate, T)
            },
            "EUn casubq" to { t: Double, dose: Double, steadystate: Boolean, T: Double ->
                callE2Curve3C("EUn casubq", t, dose, steadystate, T)
            },
            "EB im" to { t: Double, dose: Double, steadystate: Boolean, T: Double ->
                callE2Curve3C("EB", t, dose, steadystate, T)
            },
            // todo: tackle patches later
//            "patch tw" to { t: Double, dose: Double, steadystate: Boolean, T: Double ->
//                e2Patch3C(t, conversionFactor * dose, *PKParameters["patch tw"]!!.toDoubleArray(), 3.5, steadystate, T)
//            },
//            "patch ow" to { t: Double, dose: Double, steadystate: Boolean, T: Double ->
//                e2Patch3C(t, conversionFactor * dose, *PKParameters["patch ow"]!!.toDoubleArray(), 7.0, steadystate, T)
//            }
        )
    }

    /**
     * Calculate a given set of multi-doses
     * Offset values of `doses`, `times`, and `types` need to match.
     * @param t time offset for dose calculation (time value for formula)
     * @param doses Dose amounts, in mg
     * @param times Dosing intervals, in days, relative to the t
     * @param models Ester/types, see `methodList` for values
     * @param conversionFactor conversion factor for conversion from pg/mL to other
     * @param random if values need uncertainty applied
     * @param intervals true if days are set as interval
     */
    fun e2multidose3C(
        t: Double,
        doses: List<Double>,
        times: List<Double>,
        models: List<String>,
        conversionFactor: Double = 1.0,
        random: Boolean = false,
        intervals: Boolean = false
    ): Double {
        val newTimes: List<Double>
        if (intervals) {
            var totalEstrogen = -times[0]

            newTimes = times.map { value ->
                totalEstrogen += value
                totalEstrogen
            }
        }

        var sum = 0.0

        for (i in doses.indices) {
            if (!random) {
                sum += PKFunctions(conversionFactor)[models[i]]?.invoke(t - times[i], doses[i], false, 1.0) ?: 0.0 // ensure we dont add null to sum
            } else {
                error("Random functions are not supported yet.")
            }
        }

        return sum
    }

    private fun e2Curve3C(
        t: Double,
        dose: Double,
        d: Double,
        k1: Double,
        k2: Double,
        k3: Double,
        Ds: Double = 0.0,
        D2: Double = 0.0,
        steadystate: Boolean = false,
        T: Double = 1.0
    ): Double {

        if (!steadystate) {
            if (t < 0) {
                return 0.0;
            }

            var ret = 0.0;

            if (D2 > 0) {
                ret += D2 * exp(-k3 * t);
            }

            if (Ds > 0) {
                ret += if (k2 == k3) {
                    Ds * k2 * t * exp(-k2 * t)
                } else {
                    Ds * k2 / (k2 - k3) * (exp(-k3 * t) - exp(-k2 * t))
                }
            }

            // When one or more rate is equal the single-dose solution
            // is ill-defined because one or more denominators are zero.
            // In these cases we must first take the limit the recover
            // the correct solution.

            // ...buuut meh, we could just as well simply
            // perturb the rates by a tiny amount instead of doing
            // this lengthy limit thing.

            if (dose > 0 && d > 0) {
                ret += when {
                    k1 == k2 && k2 == k3 -> {
                        dose * d * k1 * k1 * t * t * exp(-k1 * t) / 2
                    }

                    k1 == k2 && k2 != k3 -> {
                        dose * d * k1 * k1 * (exp(-k3 * t) - exp(-k1 * t) * (1 + (k1 - k3) * t)) / (k1 - k3) / (k1 - k3)
                    }

                    k1 != k2 && k1 == k3 -> {
                        dose * d * k1 * k2 * (exp(-k2 * t) - exp(-k1 * t) * (1 + (k1 - k2) * t)) / (k1 - k2) / (k1 - k2)
                    }

                    k1 != k2 && k2 == k3 -> {
                        dose * d * k1 * k2 * (exp(-k1 * t) - exp(-k2 * t) * (1 - (k1 - k2) * t)) / (k1 - k2) / (k1 - k2)
                    }

                    else -> {
                        dose * d * k1 * k2 * (exp(-k1 * t) / (k1 - k2) / (k1 - k3) - exp(-k2 * t) / (k1 - k2) / (k2 - k3) + exp(
                            -k3 * t
                        ) / (k1 - k3) / (k2 - k3))
                    }
                }
            }

            return if (ret.isNaN()) {
                0.0
            } else {
                ret
            }
        } else {
            // todo: handle steadystate
            return -1.0//e2SteadyState3C(t, dose, T, d, k1, k2, k3);
        }
    }
}