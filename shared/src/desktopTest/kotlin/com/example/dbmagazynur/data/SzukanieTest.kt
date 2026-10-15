package com.example.dbmagazynur.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SzukanieTest {
    @Test
    fun bezWielkosciLiterIPolskichZnakow() {
        assertEquals("zolc lodz", doPorownania("ŻÓŁĆ Łódź"))
        assertEquals("sruba", doPorownania("Śruba"))
    }

    @Test
    fun kazdeSlowoMusiWystapic() {
        val maszyna = "Prasa krawędziowa 2 PL01-L2-PR02"
        assertTrue(pasujeDoFrazy(maszyna, "prasa krawedziowa"))
        assertTrue(pasujeDoFrazy(maszyna, "pr02  prasa"))
        assertTrue(pasujeDoFrazy(maszyna, ""))
        assertFalse(pasujeDoFrazy(maszyna, "prasa tokarka"))
    }
}
