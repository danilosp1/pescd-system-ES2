package br.ufscar.pescd.service;

import br.ufscar.pescd.dto.*;
import br.ufscar.pescd.entity.*;
import br.ufscar.pescd.enums.*;
import br.ufscar.pescd.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SupervisorProfessorServiceTest {
    private final OfferStudentRepository enrollments = mock(OfferStudentRepository.class);
    private final WorkPlanRepository plans = mock(WorkPlanRepository.class);
    private final ReportRepository reports = mock(ReportRepository.class);
    private final StatusChangeLogRepository logs = mock(StatusChangeLogRepository.class);
    private final OfferApiMapper mapper = mock(OfferApiMapper.class);
    private final SupervisorProfessorService service = new SupervisorProfessorService(enrollments, plans, reports, logs, mapper);
    private final User professor = new User();
    private final OfferStudent enrollment = new OfferStudent();
    private final Offer offer = new Offer();

    @BeforeEach
    void setUp() {
        offer.setSemester("2026/2");
        enrollment.setOffer(offer);
        enrollment.setSupervisor(professor);
        enrollment.setStatus(StudentOfferStatus.PLANO_ENVIADO);
    }

    private void enrolled() {
        when(enrollments.findByOffer_IdAndStudent_Id(1L, 2L)).thenReturn(Optional.of(enrollment));
    }

    private void assertTransition(StudentOfferStatus previous, StudentOfferStatus next) {
        ArgumentCaptor<StatusChangeLog> captor = ArgumentCaptor.forClass(StatusChangeLog.class);
        verify(logs).save(captor.capture());
        assertSame(enrollment, captor.getValue().getOfferStudent());
        assertEquals(previous, captor.getValue().getPreviousStatus());
        assertEquals(next, captor.getValue().getNewStatus());
        assertEquals(next, enrollment.getStatus());
        verify(enrollments).save(enrollment);
    }

    @Test
    void groupsEnrollmentsAndSortsDashboardByDescendingSemester() {
        Offer older = new Offer();
        older.setSemester("2025/2");
        OfferStudent other = new OfferStudent();
        other.setOffer(older);
        when(enrollments.findBySupervisor(professor)).thenReturn(List.of(other, enrollment));
        List<SupervisorDashboardDTO> result = service.getDashboard(professor);
        assertEquals(2, result.size());
        assertSame(offer, result.get(0).getOffer());
        assertEquals(List.of(enrollment), result.get(0).getSupervisedStudents());
        OfferDetailsResponse latest = mock(OfferDetailsResponse.class);
        OfferDetailsResponse previous = mock(OfferDetailsResponse.class);
        when(mapper.toOfferDetails(offer, List.of(enrollment))).thenReturn(latest);
        when(mapper.toOfferDetails(older, List.of(other))).thenReturn(previous);
        assertEquals(List.of(latest, previous), service.getDashboardForApi(professor));
    }

    @Test
    void rejectsMissingEnrollmentAndOtherSupervisors() {
        assertThrows(IllegalArgumentException.class, () -> service.getEnrollmentForSupervisor(1L, 2L, professor));
        enrolled();
        assertThrows(AccessDeniedException.class, () -> service.getEnrollmentForSupervisor(1L, 2L, new User()));
        enrollment.setSupervisor(null);
        assertThrows(AccessDeniedException.class, () -> service.getEnrollmentForSupervisor(1L, 2L, professor));
    }

    @Test
    void loadsDetailsAndHistoryAndRejectsMissingFiles() {
        enrolled();
        assertThrows(IllegalArgumentException.class, () -> service.getWorkPlan(enrollment));
        assertThrows(IllegalArgumentException.class, () -> service.getReport(enrollment));
        WorkPlan plan = new WorkPlan();
        Report report = new Report();
        when(plans.findByOfferStudent(enrollment)).thenReturn(Optional.of(plan));
        when(reports.findByOfferStudent(enrollment)).thenReturn(Optional.of(report));
        OfferStudentDetailsResponse details = mock(OfferStudentDetailsResponse.class);
        WorkPlanDetailsResponse planDetails = mock(WorkPlanDetailsResponse.class);
        ReportDetailsResponse reportDetails = mock(ReportDetailsResponse.class);
        when(mapper.toOfferStudentDetails(enrollment)).thenReturn(details);
        when(mapper.toWorkPlanDetails(plan)).thenReturn(planDetails);
        when(mapper.toReportDetails(report)).thenReturn(reportDetails);
        assertSame(details, service.getEnrollmentDetailsForApi(1L, 2L, professor));
        assertSame(planDetails, service.getWorkPlanDetailsForApi(1L, 2L, professor));
        assertSame(reportDetails, service.getReportDetailsForApi(1L, 2L, professor));
        StatusChangeLog log = new StatusChangeLog();
        when(logs.findByOfferStudentOrderByChangedAtAsc(enrollment)).thenReturn(List.of(log));
        assertEquals(List.of(log), service.getStatusLogs(enrollment));
    }

    @Test
    void approvesPlanAndRecordsReviewAndStatusHistory() {
        enrolled();
        WorkPlan plan = new WorkPlan();
        when(plans.findByOfferStudent(enrollment)).thenReturn(Optional.of(plan));
        ApprovePlanFormDTO form = new ApprovePlanFormDTO();
        form.setParecer("Plano adequado");
        OfferStudentSummaryResponse response = mock(OfferStudentSummaryResponse.class);
        when(mapper.toOfferStudentSummary(enrollment)).thenReturn(response);
        assertSame(response, service.approvePlanForApi(1L, 2L, professor, form));
        verify(plans).save(plan);
        assertEquals(form.getParecer(), plan.getSupervisorParecer());
        assertNotNull(plan.getSupervisorApprovedAt());
        assertTransition(StudentOfferStatus.PLANO_ENVIADO, StudentOfferStatus.PLANO_APROVADO);
    }


    @Test
    void approvesReportAndPersistsFrequencyGradeAndReview() {
        enrolled();
        enrollment.setStatus(StudentOfferStatus.RELATORIO_ENVIADO);
        Report report = new Report();
        when(reports.findByOfferStudent(enrollment)).thenReturn(Optional.of(report));
        ApproveReportFormDTO form = new ApproveReportFormDTO();
        form.setParecer("Aprovado");
        form.setFrequencia(95);
        form.setNotaSugestao(GradeOption.A);
        OfferStudentSummaryResponse response = mock(OfferStudentSummaryResponse.class);
        when(mapper.toOfferStudentSummary(enrollment)).thenReturn(response);
        assertSame(response, service.approveReportForApi(1L, 2L, professor, form));
        verify(reports).save(report);
        assertEquals("Aprovado", report.getSupervisorParecer());
        assertEquals(95, report.getSupervisorFrequencia());
        assertEquals(GradeOption.A, report.getSupervisorNotaSugestao());
        assertNotNull(report.getSupervisorApprovedAt());
        assertTransition(StudentOfferStatus.RELATORIO_ENVIADO, StudentOfferStatus.RELATORIO_APROVADO_SUPERVISOR);
    }

    @ParameterizedTest
    @EnumSource(value = StudentOfferStatus.class, names = "RELATORIO_ENVIADO", mode = EnumSource.Mode.EXCLUDE)
    void refusesReportApprovalAtEveryOtherStatus(StudentOfferStatus status) {
        enrolled();
        enrollment.setStatus(status);
        assertThrows(IllegalArgumentException.class, () -> service.approveReport(1L, 2L, professor, new ApproveReportFormDTO()));
        verifyNoInteractions(reports, logs);
    }
}
