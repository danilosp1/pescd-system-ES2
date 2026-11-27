package br.ufscar.pescd.controller;

import br.ufscar.pescd.dto.*;
import br.ufscar.pescd.entity.*;
import br.ufscar.pescd.enums.GradeOption;
import br.ufscar.pescd.repository.UserRepository;
import br.ufscar.pescd.service.ProfessorResponsavelService;
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

class ProfessorResponsavelControllerTest {
    private final ProfessorResponsavelService service = mock(ProfessorResponsavelService.class);
    private final UserRepository users = mock(UserRepository.class);
    private final SeaweedFsStorageService storage = mock(SeaweedFsStorageService.class);
    private final ProfessorResponsavelController controller = new ProfessorResponsavelController(service, users, storage);
    private final Authentication auth = mock(Authentication.class);
    private final BindingResult binding = mock(BindingResult.class);
    private final ExtendedModelMap model = new ExtendedModelMap();
    private final RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
    private final User professor = new User();
    private final Offer offer = new Offer();
    private final OfferStudent enrollment = new OfferStudent();
    private final WorkPlan plan = new WorkPlan();
    private final Report report = new Report();
    private final Documentation document = new Documentation();

    @BeforeEach
    void setUp() {
        professor.setId(3L);
        offer.setResponsibleProfessor(professor);
        enrollment.setOffer(offer);
        User student = new User();
        student.setFullName("Lucas Ferreira");
        enrollment.setStudent(student);
        when(auth.getName()).thenReturn("professor");
        when(users.findByUsername("professor")).thenReturn(Optional.of(professor));
        when(service.getOffer(1L)).thenReturn(offer);
        when(service.getOfferStudents(1L)).thenReturn(List.of(enrollment));
        when(service.getEnrollmentForResponsavel(1L, 2L, professor)).thenReturn(enrollment);
        when(service.getWorkPlan(enrollment)).thenReturn(plan);
        when(service.getReport(enrollment)).thenReturn(report);
        when(service.getDocumentation(enrollment)).thenReturn(document);
        report.setSupervisorFrequencia(95);
        report.setSupervisorNotaSugestao(GradeOption.A);
    }

    @Test
    void rendersDashboardAndPrefillsFinalEvaluationFromSupervisor() {
        ResponsavelDashboardDTO item = mock(ResponsavelDashboardDTO.class);
        when(service.getDashboard(professor)).thenReturn(List.of(item));
        assertEquals("professor/responsavel/dashboard", controller.dashboard(model, auth));
        assertEquals(List.of(item), model.get("dashboard"));
        assertSame(professor, model.get("professor"));
        assertEquals("professor/responsavel/conclude-report", controller.concludeReportForm(1L, 2L, model, auth));
        ConcludeReportFormDTO form = (ConcludeReportFormDTO) model.get("form");
        assertEquals(95, form.getFrequencia());
        assertEquals(GradeOption.A, form.getNota());
        assertSame(plan, model.get("workPlan"));
        assertSame(report, model.get("report"));
        assertEquals("professor/responsavel/analyze-documentation", controller.analyzeDocumentationForm(1L, 2L, model, auth));
        assertInstanceOf(AnalyzeDocumentationFormDTO.class, model.get("form"));
        assertSame(document, model.get("documentation"));
        assertTrue(model.containsKey("statusLogs"));
        assertTrue(model.containsKey("gradeOptions"));
        assertEquals("professor/responsavel/close-offer", controller.closeOfferForm(1L, model, auth));
        assertSame(offer, model.get("offer"));
        assertInstanceOf(CloseOfferFormDTO.class, model.get("form"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"LUCAS", "ferreira"})
    void filtersOfferStudentsByCaseInsensitiveName(String name) {
        assertEquals("professor/responsavel/offer-details", controller.offerDetails(1L, name, auth, model));
        assertEquals(List.of(enrollment), model.get("students"));
        assertEquals(name, model.get("nome"));
        controller.offerDetails(1L, "Marina", auth, model);
        assertEquals(List.of(), model.get("students"));
    }

    @Test
    void deniesOfferDetailsForAnotherResponsibleProfessor() {
        User other = new User();
        other.setId(9L);
        offer.setResponsibleProfessor(other);
        assertThrows(IllegalArgumentException.class, () -> controller.offerDetails(1L, null, auth, model));
        verify(service, never()).getOfferStudents(anyLong());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void reportValidationControlsFinalEvaluation(boolean errors) {
        when(binding.hasErrors()).thenReturn(errors);
        ConcludeReportFormDTO form = new ConcludeReportFormDTO();
        String view = controller.concludeReport(1L, 2L, form, binding, model, auth, redirect);
        if (errors) {
            assertEquals("professor/responsavel/conclude-report", view);
            assertSame(report, model.get("report"));
            assertTrue(model.containsKey("gradeOptions"));
            verify(service, never()).concludeReport(anyLong(), anyLong(), any(), any());
        } else {
            assertEquals("redirect:/professor/responsavel/dashboard", view);
            verify(service).concludeReport(1L, 2L, professor, form);
            assertTrue(redirect.getFlashAttributes().containsKey("successMessage"));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void documentationValidationControlsFinalEvaluation(boolean errors) {
        when(binding.hasErrors()).thenReturn(errors);
        AnalyzeDocumentationFormDTO form = new AnalyzeDocumentationFormDTO();
        String view = controller.analyzeDocumentation(1L, 2L, form, binding, model, auth, redirect);
        if (errors) {
            assertEquals("professor/responsavel/analyze-documentation", view);
            assertSame(document, model.get("documentation"));
            assertSame(enrollment, model.get("offerStudent"));
            verify(service, never()).analyzeDocumentation(anyLong(), anyLong(), any(), any());
        } else {
            assertEquals("redirect:/professor/responsavel/dashboard", view);
            verify(service).analyzeDocumentation(1L, 2L, professor, form);
            assertTrue(redirect.getFlashAttributes().containsKey("successMessage"));
        }
    }


    @Test
    void closureSuccessAndBusinessFailureProduceAppropriateFlashMessages() {
        CloseOfferFormDTO form = new CloseOfferFormDTO();
        form.setLessonsLearned("Manter registros");
        assertEquals("redirect:/professor/responsavel/dashboard",
                controller.closeOffer(1L, form, binding, auth, model, redirect));
        verify(service).closeOffer(1L, professor, "Manter registros");
        assertTrue(redirect.getFlashAttributes().containsKey("successMessage"));
        RedirectAttributesModelMap failed = new RedirectAttributesModelMap();
        doThrow(new IllegalArgumentException("Alunos pendentes")).when(service).closeOffer(1L, professor, "Manter registros");
        assertEquals("redirect:/professor/responsavel/dashboard",
                controller.closeOffer(1L, form, binding, auth, model, failed));
        assertEquals("Alunos pendentes", failed.getFlashAttributes().get("errorMessage"));
        assertFalse(failed.getFlashAttributes().containsKey("successMessage"));
    }

    @Test
    void downloadsAllThreeAuthorizedFiles() {
        plan.setContentType("application/pdf");
        plan.setFileName("plano.pdf");
        plan.setFileFid("plan");
        report.setContentType("application/pdf");
        report.setFileName("relatorio.pdf");
        report.setFileFid("report");
        document.setContentType("application/pdf");
        document.setFileName("documentacao.pdf");
        document.setFileFid("document");
        when(storage.read("plan")).thenReturn(new byte[]{1});
        when(storage.read("report")).thenReturn(new byte[]{2});
        when(storage.read("document")).thenReturn(new byte[]{3});
        ResponseEntity<byte[]> planResponse = controller.downloadPlan(1L, 2L, auth);
        ResponseEntity<byte[]> reportResponse = controller.downloadReport(1L, 2L, auth);
        ResponseEntity<byte[]> documentResponse = controller.downloadDocumentation(1L, 2L, auth);
        assertArrayEquals(new byte[]{1}, planResponse.getBody());
        assertArrayEquals(new byte[]{2}, reportResponse.getBody());
        assertArrayEquals(new byte[]{3}, documentResponse.getBody());
        for (ResponseEntity<byte[]> response : List.of(planResponse, reportResponse, documentResponse)) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
            assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).startsWith("inline; filename="));
        }
    }

    @Test
    void rejectsAuthenticatedUserMissingFromRepository() {
        when(users.findByUsername("professor")).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> controller.dashboard(model, auth));
        verify(service, never()).getDashboard(any());
    }
}
