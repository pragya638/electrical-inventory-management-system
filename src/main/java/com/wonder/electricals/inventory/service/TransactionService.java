package com.wonder.electricals.inventory.service;

import com.wonder.electricals.inventory.dto.filter.TransactionFilter;
import com.wonder.electricals.inventory.dto.response.StockTransactionResponse;
import com.wonder.electricals.inventory.entity.StockTransaction;
import com.wonder.electricals.inventory.entity.TransactionType;
import com.wonder.electricals.inventory.exception.ResourceNotFoundException;
import com.wonder.electricals.inventory.repository.StockTransactionRepository;
import com.wonder.electricals.inventory.util.DtoMapper;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only access to the stock movement audit trail. */
@Service
@Transactional(readOnly = true)
public class TransactionService {

    private static final int MAX_PAGE_SIZE = 200;

    private final StockTransactionRepository transactionRepository;
    private final DtoMapper mapper;

    public TransactionService(StockTransactionRepository transactionRepository, DtoMapper mapper) {
        this.transactionRepository = transactionRepository;
        this.mapper = mapper;
    }

    public List<StockTransactionResponse> search(TransactionFilter filter) {
        Pageable pageable = PageRequest.of(
                Math.max(filter.getPage(), 0),
                Math.min(Math.max(filter.getSize(), 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "transactionDate", "id"));
        return mapper.toTransactionResponses(transactionRepository.search(
                filter.getType(), filter.getProductId(), filter.getPlantId(),
                filter.getFrom(), filter.getTo(), pageable));
    }

    public StockTransactionResponse findById(Long id) {
        return mapper.toTransactionResponse(transactionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Transaction", id)));
    }

    public List<StockTransactionResponse> findRecent(TransactionType type, int limit) {
        return mapper.toTransactionResponses(
                transactionRepository.findRecentByType(type, PageRequest.of(0, limit)));
    }

    public List<StockTransactionResponse> findForReport(TransactionType type,
                                                        LocalDate from, LocalDate to) {
        List<StockTransaction> transactions =
                transactionRepository.findForReport(type, from, to);
        return mapper.toTransactionResponses(transactions);
    }
}
