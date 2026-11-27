package br.ufscar.pescd.controller;

import br.ufscar.pescd.dto.*;
import br.ufscar.pescd.entity.*;
import br.ufscar.pescd.service.StudentOfferService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StudentOfferControllerTest {
    private final StudentOfferService service = mock(StudentOfferService.class);
    private final SeaweedFsStorageService storage = mock(SeaweedFsStorageService.class);
    private final StudentOfferController controller = new StudentOfferController(service, storage);
    private final Authentication auth = mock(Authentication.class);
    private final BindingResult binding = mock(BindingResult.class);
    private final OfferStudent enrollment = new OfferStudent();
    private final ExtendedModelMap model = new ExtendedModelMap();
    private final RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

    @BeforeEach
    void setUp() {
        when(auth.getName()).thenReturn("lucas");
        when(service.getEnrollment(1L, "lucas")).thenReturn(enrollment);
    }

    @Test
    void displaysListAndEnrollmentDetails() {
        when(service.findStudentEnrollments("lucas")).thenReturn(List.of(enrollment));
        assertEquals("aluno/offers/list", controller.listEnrollments(auth, model));
        assertEquals(List.of(enrollment), model.get("enrollments"));
        WorkPlan plan = new WorkPlan();
        when(service.getWorkPlan(enrollment)).thenReturn(plan);
        assertEquals("aluno/offers/details", controller.enrollmentDetails(1L, auth, model));
        assertSame(enrollment, model.get("enrollment"));
        assertSame(plan, model.get("workPlan"));
        assertTrue(model.containsKey("documentation"));
        assertTrue(model.containsKey("report"));
        assertTrue(model.containsKey("logs"));
    }

    @Test
    void initializesSubmissionForms() {
        assertEquals("aluno/offers/plano", controller.workPlanForm(1L, auth, model));
        Object plan = model.get("workPlanForm");
        assertInstanceOf(WorkPlanForm.class, plan);
        assertEquals("aluno/offers/documentacao", controller.documentationForm(1L, auth, model));
        Object document = model.get("documentationForm");
        assertInstanceOf(DocumentationForm.class, document);
        assertEquals("aluno/offers/relatorio", controller.reportForm(1L, auth, model));
        Object report = model.get("reportForm");
        assertInstanceOf(ReportForm.class, report);
    }

    @Test
    void redisplaysWorkPlanWithTheStudentsInput() {
        WorkPlanForm form = new WorkPlanForm();
        form.setDisciplineName("Engenharia de Software");
        model.addAttribute("workPlanForm", form);

        assertEquals("aluno/offers/plano", controller.workPlanForm(1L, auth, model));
        assertSame(form, model.get("workPlanForm"));
        assertEquals("Engenharia de Software", form.getDisciplineName());
    }

    @Test
    void redisplaysDocumentationWithTheStudentsInput() {
        DocumentationForm form = new DocumentationForm();
        form.setInstitutionName("UFSCar");
        model.addAttribute("documentationForm", form);

        assertEquals("aluno/offers/documentacao", controller.documentationForm(1L, auth, model));
        assertSame(form, model.get("documentationForm"));
        assertEquals("UFSCar", form.getInstitutionName());
    }

    @Test
    void validationErrorsRedisplayFormsWithoutSubmitting() {
        when(binding.hasErrors()).thenReturn(true);
        assertEquals("aluno/offers/plano", controller.submitWorkPlan(1L, new WorkPlanForm(), binding, auth, model, redirect));
        assertEquals("aluno/offers/documentacao", controller.submitDocumentation(1L, new DocumentationForm(), binding, auth, model, redirect));
        assertEquals("aluno/offers/relatorio", controller.submitReport(1L, new ReportForm(), binding, auth, model, redirect));
        verify(service, never()).submitWorkPlan(anyLong(), any(), any());
        verify(service, never()).submitDocumentation(anyLong(), any(), any());
        verify(service, never()).submitReport(anyLong(), any(), any());
        assertTrue(redirect.getFlashAttributes().isEmpty());
    }

    @Test
    void successfulSubmissionsRedirectWithConfirmation() {
        WorkPlanForm plan = new WorkPlanForm();
        DocumentationForm document = new DocumentationForm();
        ReportForm report = new ReportForm();
        assertEquals("redirect:/aluno/ofertas/1", controller.submitWorkPlan(1L, plan, binding, auth, model, redirect));
        assertTrue(redirect.getFlashAttributes().containsKey("successMessage"));
        assertEquals("redirect:/aluno/ofertas/1", controller.submitDocumentation(1L, document, binding, auth, model, redirect));
        assertTrue(redirect.getFlashAttributes().containsKey("successMessage"));
        assertEquals("redirect:/aluno/ofertas/1", controller.submitReport(1L, report, binding, auth, model, redirect));
        verify(service).submitWorkPlan(1L, "lucas", plan);
        verify(service).submitDocumentation(1L, "lucas", document);
        verify(service).submitReport(1L, "lucas", report);
    }

    @Test
    void businessErrorsRedisplayInputAndErrorMessage() {
        doThrow(new IllegalArgumentException("Plano inválido")).when(service).submitWorkPlan(anyLong(), any(), any());
        doThrow(new IllegalArgumentException("Documento inválido")).when(service).submitDocumentation(anyLong(), any(), any());
        doThrow(new IllegalArgumentException("Relatório inválido")).when(service).submitReport(anyLong(), any(), any());
        assertEquals("aluno/offers/plano", controller.submitWorkPlan(1L, new WorkPlanForm(), binding, auth, model, redirect));
        assertEquals("Plano inválido", model.get("errorMessage"));
        assertEquals("aluno/offers/documentacao", controller.submitDocumentation(1L, new DocumentationForm(), binding, auth, model, redirect));
        assertEquals("Documento inválido", model.get("errorMessage"));
        assertEquals("aluno/offers/relatorio", controller.submitReport(1L, new ReportForm(), binding, auth, model, redirect));
        assertEquals("Relatório inválido", model.get("errorMessage"));
        assertSame(enrollment, model.get("enrollment"));
        assertTrue(redirect.getFlashAttributes().isEmpty());
    }

    @Test
    void missingDocumentsReturn404() {
        assertEquals(HttpStatus.NOT_FOUND, controller.downloadWorkPlan(1L, auth).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.downloadDocumentation(1L, auth).getStatusCode());
        assertEquals(HttpStatus.NOT_FOUND, controller.downloadReport(1L, auth).getStatusCode());
        verifyNoInteractions(storage);
    }

    @ParameterizedTest
    @ValueSource(strings = "documento.pdf")
    void downloadsAvailableFilesWithPdfHeaders(String name) {
        byte[] bytes = {1,2,3};
        when(storage.read("fid")).thenReturn(bytes);
        WorkPlan plan = new WorkPlan();
        plan.setFileName(name);
        plan.setFileFid("fid");
        Documentation document = new Documentation();
        document.setFileName(name);
        document.setFileFid("fid");
        Report report = new Report();
        report.setFileName(name);
        report.setFileFid("fid");
        when(service.getWorkPlan(enrollment)).thenReturn(plan);
        when(service.getDocumentation(enrollment)).thenReturn(document);
        when(service.getReport(enrollment)).thenReturn(report);
        for (ResponseEntity<byte[]> response : List.of(controller.downloadWorkPlan(1L, auth),
                controller.downloadDocumentation(1L, auth), controller.downloadReport(1L, auth))) {
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
            assertEquals("inline; filename=\"" + name + "\"",
                    response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
            assertArrayEquals(bytes, response.getBody());
        }
    }
}
