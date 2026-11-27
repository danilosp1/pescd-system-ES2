package br.ufscar.pescd.service;

import br.ufscar.pescd.dto.ReportForm;
import br.ufscar.pescd.entity.Offer;
import br.ufscar.pescd.entity.OfferStudent;
import br.ufscar.pescd.entity.Report;
import br.ufscar.pescd.entity.StatusChangeLog;
import br.ufscar.pescd.entity.User;
import br.ufscar.pescd.enums.OfferStatus;
import br.ufscar.pescd.enums.StudentOfferStatus;
import br.ufscar.pescd.repository.DocumentationRepository;
import br.ufscar.pescd.repository.OfferStudentRepository;
import br.ufscar.pescd.repository.ReportRepository;
import br.ufscar.pescd.repository.StatusChangeLogRepository;
import br.ufscar.pescd.repository.UserRepository;
import br.ufscar.pescd.repository.WorkPlanRepository;
import br.ufscar.pescd.storage.SeaweedFsStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReportResubmissionTest {

    private static final String STUDENT = "aluno";
    private static final byte[] NEW_PDF = {1, 2, 3};

    private final OfferStudentRepository enrollments = mock(OfferStudentRepository.class);
    private final ReportRepository reports = mock(ReportRepository.class);
    private final StatusChangeLogRepository history = mock(StatusChangeLogRepository.class);
    private final SeaweedFsStorageService storage = mock(SeaweedFsStorageService.class);
    private final StudentOfferService service = new StudentOfferService(
            enrollments, mock(UserRepository.class), mock(WorkPlanRepository.class),
            mock(DocumentationRepository.class), reports, history, mock(OfferApiMapper.class), storage);

    private final OfferStudent enrollment = new OfferStudent();
    private final Report report = new Report();
    private final ReportForm form = new ReportForm();
    private final LocalDateTime originalSubmission = LocalDateTime.of(2026, 1, 1, 12, 0);

    @BeforeEach
    void setUp() {
        User student = new User();
        student.setUsername(STUDENT);
        Offer offer = new Offer();
        offer.setStatus(OfferStatus.EM_ANDAMENTO);
        enrollment.setStudent(student);
        enrollment.setOffer(offer);
        enrollment.setStatus(StudentOfferStatus.RELATORIO_REPROVADO);

        report.setOfferStudent(enrollment);
        report.setFileFid("original");
        report.setSupervisorParecer("Detalhe os resultados.");
        report.setSubmittedAt(originalSubmission);

        form.setFrequency(92);
        form.setFile(new MockMultipartFile("file", "corrigido.pdf", "application/pdf", NEW_PDF));
        when(enrollments.findForUpdateById(1L)).thenReturn(Optional.of(enrollment));
        when(reports.findByOfferStudent(enrollment)).thenReturn(Optional.of(report));
        when(storage.store(NEW_PDF, "corrigido.pdf", "application/pdf")).thenReturn("corrigido");
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearSynchronization() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void resubmitsForReviewAndDeletesPreviousPdfOnlyAfterCommit() {
        service.submitReport(1L, STUDENT, form);

        assertEquals(StudentOfferStatus.RELATORIO_ENVIADO, enrollment.getStatus());
        assertEquals("corrigido", report.getFileFid());
        assertEquals("corrigido.pdf", report.getFileName());
        assertEquals(92, report.getFrequency());
        assertNull(report.getSupervisorParecer());
        assertTrue(report.getSubmittedAt().isAfter(originalSubmission));
        verify(reports).save(report);

        ArgumentCaptor<StatusChangeLog> log = ArgumentCaptor.forClass(StatusChangeLog.class);
        verify(history).save(log.capture());
        assertEquals(StudentOfferStatus.RELATORIO_REPROVADO, log.getValue().getPreviousStatus());
        assertEquals(StudentOfferStatus.RELATORIO_ENVIADO, log.getValue().getNewStatus());
        assertEquals("Relatório final reenviado", log.getValue().getDescription());

        verify(storage, never()).delete(any());
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> {
            sync.afterCommit();
            sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
        });
        verify(storage).delete("original");
        verify(storage, never()).delete("corrigido");
    }

    @Test
    void keepsOriginalPdfAndOpinionWhenUploadFails() {
        when(storage.store(NEW_PDF, "corrigido.pdf", "application/pdf"))
                .thenThrow(new IllegalStateException("Falha no armazenamento"));

        assertThrows(IllegalStateException.class, () -> service.submitReport(1L, STUDENT, form));

        assertEquals(StudentOfferStatus.RELATORIO_REPROVADO, enrollment.getStatus());
        assertEquals("original", report.getFileFid());
        assertEquals("Detalhe os resultados.", report.getSupervisorParecer());
        assertEquals(originalSubmission, report.getSubmittedAt());
        verify(reports, never()).save(any());
        verify(storage, never()).delete(any());
        verifyNoInteractions(history);
    }

    @Test
    void cleansUpNewPdfWhenTheDatabaseRollsBack() {
        when(reports.save(report)).thenThrow(new IllegalStateException("Falha no banco"));

        assertThrows(IllegalStateException.class, () -> service.submitReport(1L, STUDENT, form));
        TransactionSynchronizationManager.getSynchronizations().forEach(
                sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(storage).delete("corrigido");
        verify(storage, never()).delete("original");
    }

    @Test
    void acceptsFirstSubmissionAfterPlanApproval() {
        enrollment.setStatus(StudentOfferStatus.PLANO_APROVADO);
        when(reports.findByOfferStudent(enrollment)).thenReturn(Optional.empty());

        service.submitReport(1L, STUDENT, form);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        ArgumentCaptor<Report> saved = ArgumentCaptor.forClass(Report.class);
        verify(reports).save(saved.capture());
        assertEquals("corrigido", saved.getValue().getFileFid());
        assertEquals(StudentOfferStatus.RELATORIO_ENVIADO, enrollment.getStatus());
        verify(storage, never()).delete(any());
    }

    @Test
    void disallowsResubmissionWhileReportIsUnderReview() {
        enrollment.setStatus(StudentOfferStatus.RELATORIO_ENVIADO);

        assertThrows(IllegalArgumentException.class, () -> service.submitReport(1L, STUDENT, form));
        verifyNoInteractions(reports, history, storage);
    }
}
