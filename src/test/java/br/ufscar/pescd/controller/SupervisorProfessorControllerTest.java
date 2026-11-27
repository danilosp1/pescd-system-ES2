package br.ufscar.pescd.controller;

import br.ufscar.pescd.dto.*;
import br.ufscar.pescd.entity.*;
import br.ufscar.pescd.enums.GradeOption;
import br.ufscar.pescd.repository.UserRepository;
import br.ufscar.pescd.service.SupervisorProfessorService;
import br.ufscar.pescd.storage.SeaweedFsStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SupervisorProfessorControllerTest {
    private final SupervisorProfessorService service = mock(SupervisorProfessorService.class);
    private final UserRepository users = mock(UserRepository.class);
    private final SeaweedFsStorageService storage = mock(SeaweedFsStorageService.class);
    private final SupervisorProfessorController controller = new SupervisorProfessorController(service, users, storage);
    private final Authentication auth = mock(Authentication.class);
    private final BindingResult binding = mock(BindingResult.class);
    private final ExtendedModelMap model = new ExtendedModelMap();
    private final RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
    private final User professor = new User();
    private final OfferStudent enrollment = new OfferStudent();
    private final WorkPlan plan = new WorkPlan();
    private final Report report = new Report();

    @BeforeEach
    void setUp() {
        when(auth.getName()).thenReturn("professor");
        when(users.findByUsername("professor")).thenReturn(Optional.of(professor));
        when(service.getEnrollmentForSupervisor(1L, 2L, professor)).thenReturn(enrollment);
        when(service.getWorkPlan(enrollment)).thenReturn(plan);
        when(service.getReport(enrollment)).thenReturn(report);
        report.setFrequency(95);
    }

    @Test
    void rendersDashboardAndApprovalFormsWithRequiredAttributes() {
        SupervisorDashboardDTO item = mock(SupervisorDashboardDTO.class);
        when(service.getDashboard(professor)).thenReturn(List.of(item));
        assertEquals("professor/supervisor/dashboard", controller.dashboard(model, auth));
        assertEquals(List.of(item), model.get("dashboard"));
        assertSame(professor, model.get("professor"));
        assertEquals("professor/supervisor/approve-plan", controller.approvePlanForm(1L, 2L, model, auth));
        assertSame(enrollment, model.get("offerStudent"));
        assertSame(plan, model.get("workPlan"));
        assertInstanceOf(ApprovePlanFormDTO.class, model.get("form"));
        assertEquals("professor/supervisor/approve-report", controller.approveReportForm(1L, 2L, model, auth));
        assertSame(report, model.get("report"));
        assertEquals(95, ((ApproveReportFormDTO) model.get("form")).getFrequencia());
        assertArrayEquals(GradeOption.values(), (GradeOption[]) model.get("gradeOptions"));
        assertTrue(model.containsKey("statusLogs"));
    }

    @Test
    void approvesPlanAndRedirectsWithConfirmation() {
        ApprovePlanFormDTO form = new ApprovePlanFormDTO();
        form.setParecer("Aprovado");
        assertEquals("redirect:/professor/supervisor/dashboard",
                controller.approvePlan(1L, 2L, form, binding, model, auth, redirect));
        verify(service).approvePlan(1L, 2L, professor, "Aprovado");
        assertTrue(redirect.getFlashAttributes().containsKey("successMessage"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void reportValidationControlsSubmissionAndRedirect(boolean errors) {
        when(binding.hasErrors()).thenReturn(errors);
        ApproveReportFormDTO form = new ApproveReportFormDTO();
        String view = controller.approveReport(1L, 2L, form, binding, model, auth, redirect);
        if (errors) {
            assertEquals("professor/supervisor/approve-report", view);
            assertSame(enrollment, model.get("offerStudent"));
            assertSame(report, model.get("report"));
            assertTrue(model.containsKey("gradeOptions"));
            verify(service, never()).approveReport(anyLong(), anyLong(), any(), any());
        } else {
            assertEquals("redirect:/professor/supervisor/dashboard", view);
            verify(service).approveReport(1L, 2L, professor, form);
            assertTrue(redirect.getFlashAttributes().containsKey("successMessage"));
        }
    }

    @Test
    void downloadsPlanAndReportAsInlinePdf() {
        plan.setContentType("application/pdf");
        plan.setFileName("plano.pdf");
        plan.setFileFid("plan");
        report.setContentType("application/pdf");
        report.setFileName("relatorio.pdf");
        report.setFileFid("report");
        when(storage.read("plan")).thenReturn(new byte[]{1});
        when(storage.read("report")).thenReturn(new byte[]{2});
        ResponseEntity<byte[]> planResponse = controller.downloadPlan(1L, 2L, auth);
        ResponseEntity<byte[]> reportResponse = controller.downloadReport(1L, 2L, auth);
        assertArrayEquals(new byte[]{1}, planResponse.getBody());
        assertArrayEquals(new byte[]{2}, reportResponse.getBody());
        assertEquals(HttpStatus.OK, planResponse.getStatusCode());
        assertEquals(MediaType.APPLICATION_PDF, reportResponse.getHeaders().getContentType());
        assertEquals("inline; filename=\"plano.pdf\"", planResponse.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertEquals("inline; filename=\"relatorio.pdf\"", reportResponse.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
    }

    @Test
    void rejectsAuthenticatedUserMissingFromRepository() {
        when(users.findByUsername("professor")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> controller.dashboard(model, auth));
        verifyNoInteractions(storage);
        verify(service, never()).getDashboard(any());
    }
}
