package com.ledgerline.transfer;

import com.ledgerline.domain.AppUser;
import com.ledgerline.domain.Transfer;
import com.ledgerline.repository.TransferRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferHistoryService {

    private final TransferRepository transferRepository;

    public TransferHistoryService(TransferRepository transferRepository) {
        this.transferRepository = transferRepository;
    }

    @Transactional(readOnly = true)
    public List<TransferDtos.TransferResponse> findForCustomer(AppUser user) {
        return transferRepository.findHistoryForUser(user.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TransferDtos.TransferResponse> findAll() {
        return transferRepository.findAllHistory()
                .stream().map(this::toResponse).toList();
    }

    private TransferDtos.TransferResponse toResponse(Transfer transfer) {
        return new TransferDtos.TransferResponse(
                transfer.getReference(),
                transfer.getSenderAccount().getAccountNumber(),
                transfer.getRecipientAccount().getAccountNumber(),
                transfer.getAmount(), transfer.getCurrency(),
                transfer.getStatus(), transfer.getCreatedAt());
    }
}
