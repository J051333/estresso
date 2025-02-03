package com.josiwhitlock.estresso

import kotlin.math.exp

/**
 * Built based on estrannaise.js
 * https://github.com/WHSAH/estrannaise.js
 */
object Estresso {

    /**
     * Calculate a given set of doses
     * Offset values of `doses`, `times`, and `models` need to match.
     * @param t time offset for dose calculation (time value for formula)
     * @param doses Dose amounts, in mg
     * @param times Dosing intervals, in days, relative to the t
     * @param models Ester/types, see `Ester` for values
     * @param conversionFactor conversion factor for conversion from pg/mL to other
     * @param random if values need uncertainty applied
     * @param intervals true if days are set as interval
     */
    fun e2multidose3C(
        t: Double,
        doses: List<Double>,
        times: List<Double>,
        models: List<Ester>,
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
                sum += e2Curve3C(
                    t - times[i],
                    doses[i],
                    models[i].d,
                    models[i].k1,
                    models[i].k2,
                    models[i].k3,
                )
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