package br.ufscar.pescd.service;

import br.ufscar.pescd.dto.RejectReportFormDTO;
import br.ufscar.pescd.entity.Offer;
import br.ufscar.pescd.entity.OfferStudent;
import br.ufscar.pescd.entity.Report;
import br.ufscar.pescd.entity.StatusChangeLog;
import br.ufscar.pescd.entity.User;
import br.ufscar.pescd.enums.OfferStatus;
import br.ufscar.pescd.enums.StudentOfferStatus;
import br.ufscar.pescd.repository.OfferStudentRepository;
import br.ufscar.pescd.repository.ReportRepository;
import br.ufscar.pescd.repository.StatusChangeLogRepository;
import br.ufscar.pescd.repository.WorkPlanRepository;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReportRejectionTest {

    private final OfferStudentRepository enrollments = mock(OfferStudentRepository.class);
    private final ReportRepository reports = mock(ReportRepository.class);
    private final StatusChangeLogRepository history = mock(StatusChangeLogRepository.class);
    private final OfferApiMapper mapper = mock(OfferApiMapper.class);
    private final SupervisorProfessorService service = new SupervisorProfessorService(
            enrollments, mock(WorkPlanRepository.class), reports, history, mapper);

    private final User supervisor = new User();
    private final OfferStudent enrollment = new OfferStudent();
    private final RejectReportFormDTO form = new RejectReportFormDTO();

    @BeforeEach
    void setUp() {
        Offer offer = new Offer();
        offer.setStatus(OfferStatus.EM_ANDAMENTO);
        enrollment.setOffer(offer);
        enrollment.setSupervisor(supervisor);
        enrollment.setStatus(StudentOfferStatus.RELATORIO_ENVIADO);
        form.setParecer("  Detalhe os resultados obtidos.  ");
        when(enrollments.findForUpdateByOffer_IdAndStudent_Id(1L, 2L))
                .thenReturn(Optional.of(enrollment));
    }

    @Test
    void rejectsPendingReportAndRecordsOpinionAndHistory() {
        Report report = new Report();
        report.setFileFid("original");
        when(reports.findByOfferStudent(enrollment)).thenReturn(Optional.of(report));

        service.rejectReport(1L, 2L, supervisor, form);

        assertEquals("Detalhe os resultados obtidos.", report.getSupervisorParecer());
        assertEquals("original", report.getFileFid());
        assertNull(report.getSupervisorApprovedAt());
        assertEquals(StudentOfferStatus.RELATORIO_REPROVADO, enrollment.getStatus());
        verify(reports).save(report);
        verify(enrollments).save(enrollment);
        verify(mapper).toOfferStudentSummary(enrollment);

        ArgumentCaptor<StatusChangeLog> log = ArgumentCaptor.forClass(StatusChangeLog.class);
        verify(history).save(log.capture());
        assertSame(enrollment, log.getValue().getOfferStudent());
        assertEquals(StudentOfferStatus.RELATORIO_ENVIADO, log.getValue().getPreviousStatus());
        assertEquals(StudentOfferStatus.RELATORIO_REPROVADO, log.getValue().getNewStatus());
    }

    @Test
    void disallowsAnotherProfessor() {
        assertThrows(AccessDeniedException.class, () -> service.rejectReport(1L, 2L, new User(), form));
        verifyNoInteractions(reports, history);
    }

    @Test
    void disallowsAnAlreadyReviewedReport() {
        enrollment.setStatus(StudentOfferStatus.RELATORIO_APROVADO_SUPERVISOR);

        assertThrows(IllegalArgumentException.class, () -> service.rejectReport(1L, 2L, supervisor, form));
        verifyNoInteractions(reports, history);
    }

    @Test
    void disallowsRejectionWhenTheOfferDoesNotAcceptResubmission() {
        enrollment.getOffer().setStatus(OfferStatus.CONCLUIDA);

        assertThrows(IllegalArgumentException.class, () -> service.rejectReport(1L, 2L, supervisor, form));
        verifyNoInteractions(reports, history);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "  ")
    void requiresAnOpinion(String opinion) {
        form.setParecer(opinion);

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(form);
            assertEquals(1, violations.size());
            assertEquals("parecer", violations.iterator().next().getPropertyPath().toString());
        }
    }
}
