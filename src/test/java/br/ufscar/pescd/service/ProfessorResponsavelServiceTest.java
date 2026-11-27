package br.ufscar.pescd.service;

import br.ufscar.pescd.dto.*;
import br.ufscar.pescd.entity.*;
import br.ufscar.pescd.enums.*;
import br.ufscar.pescd.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProfessorResponsavelServiceTest {
    private final OfferStudentRepository enrollments = mock(OfferStudentRepository.class);
    private final WorkPlanRepository plans = mock(WorkPlanRepository.class);
    private final ReportRepository reports = mock(ReportRepository.class);
    private final DocumentationRepository documents = mock(DocumentationRepository.class);
    private final StatusChangeLogRepository logs = mock(StatusChangeLogRepository.class);
    private final OfferRepository offers = mock(OfferRepository.class);
    private final OfferApiMapper mapper = mock(OfferApiMapper.class);
    private final ProfessorResponsavelService service = new ProfessorResponsavelService(
            enrollments, plans, reports, documents, logs, offers, mapper);
    private final User professor = new User();
    private final Offer offer = new Offer();
    private final OfferStudent enrollment = new OfferStudent();

    @BeforeEach
    void setUp() {
        professor.setId(3L);
        offer.setResponsibleProfessor(professor);
        offer.setSemester("2026/2");
        offer.setStatus(OfferStatus.EM_ANDAMENTO);
        enrollment.setOffer(offer);
        User student = new User();
        student.setFullName("Lucas Ferreira");
        enrollment.setStudent(student);
        enrollment.setStatus(StudentOfferStatus.RELATORIO_APROVADO_SUPERVISOR);
    }

    private void enrolled() {
        when(enrollments.findByOffer_IdAndStudent_Id(1L, 2L)).thenReturn(Optional.of(enrollment));
    }

    private void offered() {
        when(offers.findById(1L)).thenReturn(Optional.of(offer));
        when(enrollments.findByOffer(offer)).thenReturn(List.of(enrollment));
    }

    private void assertConcluded(StudentOfferStatus previous) {
        ArgumentCaptor<StatusChangeLog> captor = ArgumentCaptor.forClass(StatusChangeLog.class);
        verify(logs).save(captor.capture());
        assertSame(enrollment, captor.getValue().getOfferStudent());
        assertEquals(previous, captor.getValue().getPreviousStatus());
        assertEquals(StudentOfferStatus.CONCLUIDO_RESPONSAVEL, captor.getValue().getNewStatus());
        assertEquals(StudentOfferStatus.CONCLUIDO_RESPONSAVEL, enrollment.getStatus());
        verify(enrollments).save(enrollment);
    }

    @Test
    void groupsAndOrdersResponsibleDashboard() {
        Offer older = new Offer();
        older.setSemester("2025/1");
        OfferStudent other = new OfferStudent();
        other.setOffer(older);
        when(enrollments.findByOffer_ResponsibleProfessor(professor)).thenReturn(List.of(other, enrollment));
        List<ResponsavelDashboardDTO> dashboard = service.getDashboard(professor);
        assertSame(offer, dashboard.get(0).getOffer());
        assertEquals(List.of(enrollment), dashboard.get(0).getStudents());
        OfferDetailsResponse latest = mock(OfferDetailsResponse.class);
        OfferDetailsResponse previous = mock(OfferDetailsResponse.class);
        when(mapper.toOfferDetails(offer, List.of(enrollment))).thenReturn(latest);
        when(mapper.toOfferDetails(older, List.of(other))).thenReturn(previous);
        assertEquals(List.of(latest, previous), service.getDashboardForApi(professor));
    }

    @Test
    void deniesMissingEnrollmentAndDifferentResponsibleProfessor() {
        assertThrows(IllegalArgumentException.class, () -> service.getEnrollmentForResponsavel(1L, 2L, professor));
        enrolled();
        assertThrows(AccessDeniedException.class, () -> service.getEnrollmentForResponsavel(1L, 2L, new User()));
        OfferStudentDetailsResponse response = mock(OfferStudentDetailsResponse.class);
        when(mapper.toOfferStudentDetails(enrollment)).thenReturn(response);
        assertSame(response, service.getEnrollmentDetailsForApi(1L, 2L, professor));
    }

    @Test
    void loadsFilesAndHistoryAndRejectsMissingDocuments() {
        assertThrows(IllegalArgumentException.class, () -> service.getWorkPlan(enrollment));
        assertThrows(IllegalArgumentException.class, () -> service.getReport(enrollment));
        assertThrows(IllegalArgumentException.class, () -> service.getDocumentation(enrollment));
        WorkPlan plan = new WorkPlan();
        Report report = new Report();
        Documentation document = new Documentation();
        when(plans.findByOfferStudent(enrollment)).thenReturn(Optional.of(plan));
        when(reports.findByOfferStudent(enrollment)).thenReturn(Optional.of(report));
        when(documents.findByOfferStudent(enrollment)).thenReturn(Optional.of(document));
        assertSame(plan, service.getWorkPlan(enrollment));
        enrolled();
        ReportDetailsResponse reportResponse = mock(ReportDetailsResponse.class);
        DocumentationDetailsResponse documentResponse = mock(DocumentationDetailsResponse.class);
        when(mapper.toReportDetails(report)).thenReturn(reportResponse);
        when(mapper.toDocumentationDetails(document)).thenReturn(documentResponse);
        assertSame(reportResponse, service.getReportDetailsForApi(1L, 2L, professor));
        assertSame(documentResponse, service.getDocumentationDetailsForApi(1L, 2L, professor));
        StatusChangeLog log = new StatusChangeLog();
        when(logs.findByOfferStudentOrderByChangedAtAsc(enrollment)).thenReturn(List.of(log));
        assertEquals(List.of(log), service.getStatusLogs(enrollment));
    }

    @Test
    void concludesReportWithFinalGradeAndHistory() {
        enrolled();
        Report report = new Report();
        when(reports.findByOfferStudent(enrollment)).thenReturn(Optional.of(report));
        ConcludeReportFormDTO form = new ConcludeReportFormDTO();
        form.setParecer("Concluído");
        form.setFrequencia(95);
        form.setNota(GradeOption.A);
        OfferStudentSummaryResponse response = mock(OfferStudentSummaryResponse.class);
        when(mapper.toOfferStudentSummary(enrollment)).thenReturn(response);
        assertSame(response, service.concludeReportForApi(1L, 2L, professor, form));
        assertEquals("Concluído", report.getResponsavelParecer());
        assertEquals(95, report.getResponsavelFrequencia());
        assertEquals(GradeOption.A, report.getResponsavelNota());
        assertNotNull(report.getResponsavelApprovedAt());
        verify(reports).save(report);
        assertConcluded(StudentOfferStatus.RELATORIO_APROVADO_SUPERVISOR);
    }

    @ParameterizedTest
    @EnumSource(value = StudentOfferStatus.class, names = "RELATORIO_APROVADO_SUPERVISOR", mode = EnumSource.Mode.EXCLUDE)
    void refusesReportConclusionAtOtherStatuses(StudentOfferStatus status) {
        enrolled();
        enrollment.setStatus(status);
        assertThrows(IllegalArgumentException.class, () -> service.concludeReport(1L, 2L, professor, new ConcludeReportFormDTO()));
        verifyNoInteractions(reports, logs);
    }

    @Test
    void concludesDocumentationWithReviewFrequencyGradeAndHistory() {
        enrolled();
        enrollment.setStatus(StudentOfferStatus.DOCUMENTACAO_ENVIADA);
        Documentation document = new Documentation();
        when(documents.findByOfferStudent(enrollment)).thenReturn(Optional.of(document));
        AnalyzeDocumentationFormDTO form = new AnalyzeDocumentationFormDTO();
        form.setParecer("Documentação conferida");
        form.setFrequencia(100);
        form.setNota(GradeOption.A);
        OfferStudentSummaryResponse response = mock(OfferStudentSummaryResponse.class);
        when(mapper.toOfferStudentSummary(enrollment)).thenReturn(response);
        assertSame(response, service.analyzeDocumentationForApi(1L, 2L, professor, form));
        assertEquals(form.getParecer(), document.getResponsavelParecer());
        assertEquals(100, document.getResponsavelFrequencia());
        assertEquals(GradeOption.A, document.getResponsavelNota());
        assertNotNull(document.getResponsavelApprovedAt());
        verify(documents).save(document);
        assertConcluded(StudentOfferStatus.DOCUMENTACAO_ENVIADA);
    }


    @ParameterizedTest
    @ValueSource(strings = {" LUCAS ", "ferreira"})
    void searchesStudentsByCaseInsensitiveTrimmedName(String query) {
        offered();
        OfferStudentSummaryResponse response = mock(OfferStudentSummaryResponse.class);
        when(mapper.toOfferStudentSummary(enrollment)).thenReturn(response);
        assertEquals(List.of(response), service.searchStudentsByNameForApi(1L, professor, query));
        assertTrue(service.searchStudentsByNameForApi(1L, professor, "Marina").isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void returnsAllStudentsWhenTheSearchHasNoName(String query) {
        User secondStudent = new User();
        secondStudent.setFullName("Marina Costa");
        OfferStudent secondEnrollment = new OfferStudent();
        secondEnrollment.setStudent(secondStudent);
        secondEnrollment.setOffer(offer);
        when(offers.findById(1L)).thenReturn(Optional.of(offer));
        when(enrollments.findByOffer(offer)).thenReturn(List.of(enrollment, secondEnrollment));
        OfferStudentSummaryResponse first = mock(OfferStudentSummaryResponse.class);
        OfferStudentSummaryResponse second = mock(OfferStudentSummaryResponse.class);
        when(mapper.toOfferStudentSummary(enrollment)).thenReturn(first);
        when(mapper.toOfferStudentSummary(secondEnrollment)).thenReturn(second);

        assertEquals(List.of(first, second), service.searchStudentsByNameForApi(1L, professor, query));
    }

    @Test
    void checksOfferOwnershipForDetailsAndSearch() {
        assertThrows(IllegalArgumentException.class, () -> service.getOffer(1L));
        offered();
        User another = new User();
        another.setId(99L);
        offer.setResponsibleProfessor(another);
        assertThrows(AccessDeniedException.class, () -> service.searchStudentsByNameForApi(1L, professor, null));
        offer.setResponsibleProfessor(professor);
        OfferDetailsResponse response = mock(OfferDetailsResponse.class);
        when(mapper.toOfferDetails(offer, List.of(enrollment))).thenReturn(response);
        assertSame(response, service.getOfferDetailsForApi(1L, professor));
        assertEquals(List.of(enrollment), service.getOfferStudents(1L));
    }

    @Test
    void closureSummaryAllowsFinishedStudents() {
        offered();
        enrollment.setStatus(StudentOfferStatus.CONCLUIDO_RESPONSAVEL);
        service.getCloseOfferSummaryForApi(1L, professor);
        verify(mapper).toResponsibleCloseSummary(offer, List.of(enrollment), true);
    }

    @Test
    void rejectsClosureWithUnfinishedStudents() {
        offered();
        assertThrows(IllegalArgumentException.class, () -> service.closeOffer(1L, professor, "Lições"));
        verify(offers, never()).save(any());
        assertEquals(OfferStatus.EM_ANDAMENTO, offer.getStatus());
    }

    @Test
    void requestsSecretaryClosureAfterAllStudentsConclude() {
        offered();
        enrollment.setStatus(StudentOfferStatus.CONCLUIDO_RESPONSAVEL);
        OfferSummaryResponse response = mock(OfferSummaryResponse.class);
        when(mapper.toOfferSummary(offer)).thenReturn(response);
        assertSame(response, service.closeOfferForApi(1L, professor, "Manter acompanhamento"));
        assertEquals(OfferStatus.AGUARDANDO_ENCERRAMENTO_SECRETARIO, offer.getStatus());
        assertEquals("Manter acompanhamento", offer.getLessonsLearned());
        assertSame(professor, offer.getClosureRequestedBy());
        assertNotNull(offer.getClosureRequestedAt());
        verify(offers).save(offer);
    }
}
