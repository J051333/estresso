import com.josiwhitlock.estresso.Ester
import org.junit.jupiter.api.Test
import com.josiwhitlock.estresso.Estresso.e2multidose3C
import kotlin.test.assertEquals

class EstressoTest {

    @Test
    fun e2multidose3CTest() {
        val expected = 61; // expected result by estrannai.se

        val result = e2multidose3C(
            t = 2.0,
            doses = listOf(1.0),
            times = listOf(0.0),
            models = listOf(Ester.VALERATE),
        )

        assertEquals(expected, result.toInt())
    }
}