package com.billing.app.domain.repository

import com.billing.app.data.local.entity.InvoiceEntity
import com.billing.app.data.local.entity.InvoiceItemEntity
import com.billing.app.data.local.entity.InvoiceStatus
import kotlinx.coroutines.flow.Flow

interface InvoiceRepository {
    fun getAllInvoices(): Flow<List<InvoiceEntity>>
    fun getInvoicesByStatus(status: InvoiceStatus): Flow<List<InvoiceEntity>>
    fun getInvoicesSince(startTime: Long): Flow<List<InvoiceEntity>>
    fun getInvoicesBetween(startTime: Long, endTime: Long): Flow<List<InvoiceEntity>>
    fun searchInvoices(query: String, status: InvoiceStatus? = null): Flow<List<InvoiceEntity>>
    fun getTotalInvoiceCount(): Flow<Int>
    fun getInvoiceCountBetween(startTime: Long, endTime: Long): Flow<Int>
    fun getTotalSalesBetween(startTime: Long, endTime: Long): Flow<Long>
    fun getAverageBillBetween(startTime: Long, endTime: Long): Flow<Double>
    fun getCreditDuesInvoices(): Flow<List<InvoiceEntity>>
    fun getTotalDuePaise(): Flow<Long>

    suspend fun getInvoiceByNumber(invoiceNumber: String): InvoiceEntity?
    suspend fun getInvoiceItems(invoiceNumber: String): List<InvoiceItemEntity>
    suspend fun getAllInvoiceItems(): List<InvoiceItemEntity>

    suspend fun generateNextInvoiceNumber(): String

    suspend fun completeSale(
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>,
        draftInvoiceNumberToDelete: String? = null
    ): Result<InvoiceEntity>

    suspend fun saveDraft(
        invoice: InvoiceEntity,
        items: List<InvoiceItemEntity>
    ): Result<InvoiceEntity>

    suspend fun voidInvoice(invoiceNumber: String): Result<Unit>
    suspend fun settleInvoiceDue(invoiceNumber: String): Result<Unit>
    suspend fun deleteInvoice(invoiceNumber: String): Result<Unit>

    suspend fun upsertInvoicesAndItems(
        invoices: List<InvoiceEntity>,
        items: List<InvoiceItemEntity>
    ): Result<Unit>
}
