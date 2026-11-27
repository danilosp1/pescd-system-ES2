package br.ufscar.pescd.controller;

import br.ufscar.pescd.dto.*;
import br.ufscar.pescd.entity.*;
import br.ufscar.pescd.repository.UserRepository;
import br.ufscar.pescd.service.SupervisorProfessorService;
import br.ufscar.pescd.storage.SeaweedFsStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SupervisorProfessorApiControllerTest {
    private final SupervisorProfessorService service = mock(SupervisorProfessorService.class);
    private final UserRepository users = mock(UserRepository.class);
    private final SeaweedFsStorageService storage = mock(SeaweedFsStorageService.class);
    private final SupervisorProfessorApiController controller = new SupervisorProfessorApiController(service, users, storage);
    private final Authentication auth = mock(Authentication.class);
    private final User professor = new User();

    @BeforeEach
    void setUp() {
        when(auth.getName()).thenReturn("supervisor");
        when(users.findByUsername("supervisor")).thenReturn(Optional.of(professor));
    }

    @Test
    void delegatesQueriesWithResolvedProfessorAndPathIds() {
        OfferDetailsResponse offer = mock(OfferDetailsResponse.class);
        OfferStudentDetailsResponse enrollment = mock(OfferStudentDetailsResponse.class);
        WorkPlanDetailsResponse plan = mock(WorkPlanDetailsResponse.class);
        ReportDetailsResponse report = mock(ReportDetailsResponse.class);
        when(service.getDashboardForApi(professor)).thenReturn(List.of(offer));
        when(service.getEnrollmentDetailsForApi(1L, 2L, professor)).thenReturn(enrollment);
        when(service.getWorkPlanDetailsForApi(1L, 2L, professor)).thenReturn(plan);
        when(service.getReportDetailsForApi(1L, 2L, professor)).thenReturn(report);
        assertEquals(List.of(offer), controller.listSupervisedOffers(auth));
        assertSame(enrollment, controller.enrollmentDetails(1L, 2L, auth));
        assertSame(plan, controller.workPlanDetails(1L, 2L, auth));
        assertSame(report, controller.reportDetails(1L, 2L, auth));
    }

    @Test
    void delegatesApprovalsWithUnchangedFormAndIdentity() {
        ApprovePlanFormDTO plan = new ApprovePlanFormDTO();
        ApproveReportFormDTO report = new ApproveReportFormDTO();
        OfferStudentSummaryResponse response = mock(OfferStudentSummaryResponse.class);
        when(service.approvePlanForApi(1L, 2L, professor, plan)).thenReturn(response);
        when(service.approveReportForApi(1L, 2L, professor, report)).thenReturn(response);
        assertSame(response, controller.approvePlan(1L, 2L, plan, auth));
        assertSame(response, controller.approveReport(1L, 2L, report, auth));
    }

    @Test
    void delegatesPlanAndReportRejectionsWithResolvedProfessor() {
        RejectPlanFormDTO plan = new RejectPlanFormDTO();
        RejectReportFormDTO report = new RejectReportFormDTO();
        OfferStudentSummaryResponse response = mock(OfferStudentSummaryResponse.class);
        when(service.rejectPlanForApi(1L, 2L, professor, plan)).thenReturn(response);
        when(service.rejectReport(1L, 2L, professor, report)).thenReturn(response);

        assertSame(response, controller.rejectPlan(1L, 2L, plan, auth));
        assertSame(response, controller.rejectReport(1L, 2L, report, auth));
    }

    @Test
    void downloadsAuthorizedFilesWithTheirNamesAndContents() {
        OfferStudent enrollment = new OfferStudent();
        when(service.getEnrollmentForSupervisor(1L, 2L, professor)).thenReturn(enrollment);
        WorkPlan plan = new WorkPlan();
        plan.setFileName("plano.pdf");
        plan.setFileFid("plan");
        plan.setContentType("application/pdf");
        Report report = new Report();
        report.setFileName("relatorio.pdf");
        report.setFileFid("report");
        report.setContentType("application/pdf");
        when(service.getWorkPlan(enrollment)).thenReturn(plan);
        when(service.getReport(enrollment)).thenReturn(report);
        byte[] planBytes = {1,2};
        byte[] reportBytes = {3,4};
        when(storage.read("plan")).thenReturn(planBytes);
        when(storage.read("report")).thenReturn(reportBytes);
        ResponseEntity<byte[]> planResponse = controller.downloadWorkPlan(1L, 2L, auth);
        ResponseEntity<byte[]> reportResponse = controller.downloadReport(1L, 2L, auth);
        assertEquals(HttpStatus.OK, planResponse.getStatusCode());
        assertEquals(MediaType.APPLICATION_PDF, reportResponse.getHeaders().getContentType());
        assertEquals("inline; filename=\"plano.pdf\"", planResponse.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertEquals("inline; filename=\"relatorio.pdf\"", reportResponse.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertArrayEquals(planBytes, planResponse.getBody());
        assertArrayEquals(reportBytes, reportResponse.getBody());
    }

    @Test
    void missingAuthenticatedAccountStopsBeforeServiceCalls() {
        when(users.findByUsername("supervisor")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> controller.listSupervisedOffers(auth));
        verifyNoInteractions(service, storage);
    }
}
