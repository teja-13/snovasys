package com.ledgerline.transfer;

import com.ledgerline.domain.AppUser;
import com.ledgerline.domain.Deposit;
import com.ledgerline.domain.Transfer;
import com.ledgerline.repository.DepositRepository;
import com.ledgerline.repository.TransferRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferHistoryService {

    private final TransferRepository transferRepository;
    private final DepositRepository depositRepository;

    public TransferHistoryService(TransferRepository transferRepository, DepositRepository depositRepository) {
        this.transferRepository = transferRepository;
        this.depositRepository = depositRepository;
    }

    @Transactional(readOnly = true)
    public List<TransactionDtos.TransactionResponse> findForCustomer(AppUser user) {
        return combine(transferRepository.findHistoryForUser(user.getId()),
                depositRepository.findHistoryForUser(user.getId()));
    }

    @Transactional(readOnly = true)
    public List<TransactionDtos.TransactionResponse> findAll() {
        return combine(transferRepository.findAllHistory(), depositRepository.findAllHistory());
    }

    private List<TransactionDtos.TransactionResponse> combine(List<Transfer> transfers, List<Deposit> deposits) {
        List<TransactionDtos.TransactionResponse> transactions = new ArrayList<>();
        transfers.stream().map(this::toTransferResponse).forEach(transactions::add);
        deposits.stream().map(this::toDepositResponse).forEach(transactions::add);
        transactions.sort(Comparator.comparing(TransactionDtos.TransactionResponse::createdAt).reversed());
        return transactions;
    }

    private TransactionDtos.TransactionResponse toTransferResponse(Transfer transfer) {
        return new TransactionDtos.TransactionResponse(
                transfer.getReference(),
                "TRANSFER",
                null,
                null,
                transfer.getSenderAccount().getAccountNumber(),
                transfer.getRecipientAccount().getAccountNumber(),
                transfer.getAmount(), transfer.getCurrency(),
                transfer.getStatus().name(), transfer.getCreatedAt());
    }

    private TransactionDtos.TransactionResponse toDepositResponse(Deposit deposit) {
        return new TransactionDtos.TransactionResponse(
                deposit.getReference(),
                "DEPOSIT",
                deposit.getSource().name(),
                deposit.getDescription(),
                deposit.getAccount().getAccountNumber(),
                null,
                deposit.getAmount(), deposit.getCurrency(),
                "COMPLETED", deposit.getCreatedAt());
    }
}
