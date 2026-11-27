package br.ufscar.pescd.controller;

import br.ufscar.pescd.dto.ApproveReportFormDTO;
import br.ufscar.pescd.dto.OfferStudentSummaryResponse;
import br.ufscar.pescd.dto.RejectReportFormDTO;
import br.ufscar.pescd.dto.ReportForm;
import br.ufscar.pescd.entity.OfferStudent;
import br.ufscar.pescd.entity.Report;
import br.ufscar.pescd.entity.User;
import br.ufscar.pescd.enums.StudentOfferStatus;
import br.ufscar.pescd.repository.UserRepository;
import br.ufscar.pescd.service.StudentOfferService;
import br.ufscar.pescd.service.SupervisorProfessorService;
import br.ufscar.pescd.storage.SeaweedFsStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReportReviewControllerTest {

    private static final String REVIEW_VIEW = "professor/supervisor/approve-report";
    private static final String OPINION = "Detalhe a metodologia do estágio.";

    private final SupervisorProfessorService supervisorService = mock(SupervisorProfessorService.class);
    private final StudentOfferService studentService = mock(StudentOfferService.class);
    private final UserRepository users = mock(UserRepository.class);
    private final SeaweedFsStorageService storage = mock(SeaweedFsStorageService.class);

    private final SupervisorProfessorApiController api = new SupervisorProfessorApiController(
            supervisorService, users, storage);
    private final SupervisorProfessorController supervisorPage = new SupervisorProfessorController(
            supervisorService, users, storage);
    private final StudentOfferController studentPage = new StudentOfferController(studentService, storage);

    private final Authentication authentication = mock(Authentication.class);
    private final User professor = new User();
    private final OfferStudent enrollment = new OfferStudent();
    private final Report report = new Report();
    private final ExtendedModelMap model = new ExtendedModelMap();
    private final RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

    @BeforeEach
    void setUp() {
        when(authentication.getName()).thenReturn("professor");
        when(users.findByUsername("professor")).thenReturn(Optional.of(professor));
        when(supervisorService.getEnrollmentForSupervisor(1L, 2L, professor)).thenReturn(enrollment);
        report.setFrequency(92);
        when(supervisorService.getReport(enrollment)).thenReturn(report);
    }

    @Test
    void apiSendsTheAuthenticatedSupervisorAndReturnsTheUpdatedStatus() {
        RejectReportFormDTO form = new RejectReportFormDTO();
        form.setParecer(OPINION);
        StudentOfferStatus rejected = StudentOfferStatus.RELATORIO_REPROVADO;
        OfferStudentSummaryResponse summary = new OfferStudentSummaryResponse(
                3L, null, null, rejected, rejected.getDisplayName(), null,
                true, false, true, null, null, null, null);
        when(supervisorService.rejectReport(1L, 2L, professor, form)).thenReturn(summary);

        assertSame(summary, api.rejectReport(1L, 2L, form, authentication));
    }

    @Test
    void reviewShowsBothActionsAndPrefillsTheApprovalFrequency() {
        assertEquals(REVIEW_VIEW, supervisorPage.approveReportForm(1L, 2L, model, authentication));

        assertSame(enrollment, model.get("offerStudent"));
        assertSame(report, model.get("report"));
        ApproveReportFormDTO approval = assertInstanceOf(ApproveReportFormDTO.class, model.get("form"));
        assertEquals(92, approval.getFrequencia());
        assertInstanceOf(RejectReportFormDTO.class, model.get("rejectForm"));
    }

    @Test
    void rejectionWithValidationErrorsRendersTheSameFormAndDoesNotReject() {
        RejectReportFormDTO form = new RejectReportFormDTO();
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "rejectForm");
        errors.rejectValue("parecer", "NotBlank");
        model.addAllAttributes(errors.getModel());

        assertEquals(REVIEW_VIEW, supervisorPage.rejectReport(1L, 2L, form, errors, model, authentication, redirect));
        assertSame(form, model.get("rejectForm"));
        assertInstanceOf(ApproveReportFormDTO.class, model.get("form"));
        verify(supervisorService, never()).rejectReport(anyLong(), anyLong(), any(), any());
    }

    @Test
    void invalidApprovalPreservesTheEnteredFieldsAndTheRejectionAction() {
        ApproveReportFormDTO form = new ApproveReportFormDTO();
        form.setFrequencia(97);
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "form");
        errors.rejectValue("parecer", "NotBlank");
        model.addAllAttributes(errors.getModel());

        assertEquals(REVIEW_VIEW, supervisorPage.approveReport(1L, 2L, form, errors, model, authentication, redirect));
        assertSame(form, model.get("form"));
        assertInstanceOf(RejectReportFormDTO.class, model.get("rejectForm"));
        verify(supervisorService, never()).approveReport(anyLong(), anyLong(), any(), any());
    }

    @Test
    void successfulRejectionReturnsToTheDashboardWithConfirmation() {
        RejectReportFormDTO form = new RejectReportFormDTO();
        form.setParecer(OPINION);
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(form, "rejectForm");

        String view = supervisorPage.rejectReport(1L, 2L, form, errors, model, authentication, redirect);

        assertEquals("redirect:/professor/supervisor/dashboard", view);
        assertTrue(redirect.getFlashAttributes().containsKey("successMessage"));
        verify(supervisorService).rejectReport(1L, 2L, professor, form);
    }

    @Test
    void studentReportFormReceivesTheRejectedReportAndItsOpinion() {
        when(authentication.getName()).thenReturn("aluno");
        enrollment.setStatus(StudentOfferStatus.RELATORIO_REPROVADO);
        report.setSupervisorParecer(OPINION);
        when(studentService.getEnrollment(3L, "aluno")).thenReturn(enrollment);
        when(studentService.getReport(enrollment)).thenReturn(report);

        assertEquals("aluno/offers/relatorio", studentPage.reportForm(3L, authentication, model));
        assertSame(enrollment, model.get("enrollment"));
        assertSame(report, model.get("report"));
        assertInstanceOf(ReportForm.class, model.get("reportForm"));
    }
}
