package com.billing.app.data.local.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InvoiceStatusTest {

    @Test
    fun testInvoiceStatusEnumValues() {
        val statuses = InvoiceStatus.values().map { it.name }
        assertTrue(statuses.contains("DRAFT"))
        assertTrue(statuses.contains("COMPLETED"))
        assertTrue(statuses.contains("CANCELLED"))
    }

    @Test
    fun testDraftFilteringLogic() {
        val invoices = listOf(
            InvoiceEntity(
                invoiceNumber = "INV-001",
                subtotalPaise = 10000L,
                grandTotalPaise = 10000L,
                status = InvoiceStatus.COMPLETED
            ),
            InvoiceEntity(
                invoiceNumber = "DRAFT-001",
                subtotalPaise = 50000L,
                grandTotalPaise = 50000L,
                status = InvoiceStatus.DRAFT
            ),
            InvoiceEntity(
                invoiceNumber = "INV-002",
                subtotalPaise = 25000L,
                grandTotalPaise = 25000L,
                status = InvoiceStatus.COMPLETED
            )
        )

        val completedOnly = invoices.filter { it.status == InvoiceStatus.COMPLETED }
        val draftsOnly = invoices.filter { it.status == InvoiceStatus.DRAFT }

        assertEquals(2, completedOnly.size)
        assertEquals(35000L, completedOnly.sumOf { it.grandTotalPaise })
        assertEquals(1, draftsOnly.size)
        assertEquals(50000L, draftsOnly.sumOf { it.grandTotalPaise })
    }
}