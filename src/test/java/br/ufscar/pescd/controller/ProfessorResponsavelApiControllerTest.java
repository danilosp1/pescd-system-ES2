package br.ufscar.pescd.controller;

import br.ufscar.pescd.dto.*;
import br.ufscar.pescd.entity.*;
import br.ufscar.pescd.repository.UserRepository;
import br.ufscar.pescd.service.ProfessorResponsavelService;
import br.ufscar.pescd.storage.SeaweedFsStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProfessorResponsavelApiControllerTest {
    private final ProfessorResponsavelService service = mock(ProfessorResponsavelService.class);
    private final UserRepository users = mock(UserRepository.class);
    private final SeaweedFsStorageService storage = mock(SeaweedFsStorageService.class);
    private final ProfessorResponsavelApiController controller = new ProfessorResponsavelApiController(service, users, storage);
    private final Authentication auth = mock(Authentication.class);
    private final User professor = new User();

    @BeforeEach
    void setUp() {
        when(auth.getName()).thenReturn("responsavel");
        when(users.findByUsername("responsavel")).thenReturn(Optional.of(professor));
    }

    @Test
    void queriesReturnServiceResultsForResolvedProfessor() {
        OfferDetailsResponse offer = mock(OfferDetailsResponse.class);
        OfferStudentDetailsResponse enrollment = mock(OfferStudentDetailsResponse.class);
        OfferStudentSummaryResponse student = mock(OfferStudentSummaryResponse.class);
        ResponsibleCloseOfferSummaryResponse closure = mock(ResponsibleCloseOfferSummaryResponse.class);
        ReportDetailsResponse report = mock(ReportDetailsResponse.class);
        DocumentationDetailsResponse document = mock(DocumentationDetailsResponse.class);
        when(service.getDashboardForApi(professor)).thenReturn(List.of(offer));
        when(service.getOfferDetailsForApi(1L, professor)).thenReturn(offer);
        when(service.getEnrollmentDetailsForApi(1L, 2L, professor)).thenReturn(enrollment);
        when(service.searchStudentsByNameForApi(1L, professor, "Lucas")).thenReturn(List.of(student));
        when(service.getCloseOfferSummaryForApi(1L, professor)).thenReturn(closure);
        when(service.getReportDetailsForApi(1L, 2L, professor)).thenReturn(report);
        when(service.getDocumentationDetailsForApi(1L, 2L, professor)).thenReturn(document);
        assertEquals(List.of(offer), controller.listResponsibleOffers(auth));
        assertSame(offer, controller.offerDetails(1L, auth));
        assertSame(enrollment, controller.enrollmentDetails(1L, 2L, auth));
        assertEquals(List.of(student), controller.searchStudents(1L, "Lucas", auth));
        assertSame(closure, controller.closeSummary(1L, auth));
        assertSame(report, controller.reportDetails(1L, 2L, auth));
        assertSame(document, controller.documentationDetails(1L, 2L, auth));
    }

    @Test
    void acceptsStudentSearchWithoutANameParameter() throws Exception {
        OfferStudentSummaryResponse student = mock(OfferStudentSummaryResponse.class);
        when(student.enrollmentId()).thenReturn(12L);
        when(service.searchStudentsByNameForApi(1L, professor, null)).thenReturn(List.of(student));

        MockMvcBuilders.standaloneSetup(controller).build()
                .perform(get("/api/professor/responsavel/offers/1/students").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].enrollmentId").value(12));
    }

    @Test
    void delegatesFinalEvaluationsAndClosureWithOriginalForms() {
        ConcludeReportFormDTO report = new ConcludeReportFormDTO();
        AnalyzeDocumentationFormDTO document = new AnalyzeDocumentationFormDTO();
        CloseOfferFormDTO closure = new CloseOfferFormDTO();
        closure.setLessonsLearned("Acompanhamento periódico");
        OfferStudentSummaryResponse student = mock(OfferStudentSummaryResponse.class);
        OfferSummaryResponse offer = mock(OfferSummaryResponse.class);
        when(service.concludeReportForApi(1L, 2L, professor, report)).thenReturn(student);
        when(service.analyzeDocumentationForApi(1L, 2L, professor, document)).thenReturn(student);
        when(service.closeOfferForApi(1L, professor, closure.getLessonsLearned())).thenReturn(offer);
        assertSame(student, controller.concludeReport(1L, 2L, report, auth));
        assertSame(student, controller.analyzeDocumentation(1L, 2L, document, auth));
        assertSame(offer, controller.closeOffer(1L, closure, auth));
    }

    @Test
    void downloadsFilesAfterCheckingEnrollmentAccess() {
        OfferStudent enrollment = new OfferStudent();
        when(service.getEnrollmentForResponsavel(1L, 2L, professor)).thenReturn(enrollment);
        WorkPlan plan = new WorkPlan();
        plan.setFileName("plano.pdf");
        plan.setFileFid("plan");
        plan.setContentType("application/pdf");
        Report report = new Report();
        report.setFileName("relatorio.pdf");
        report.setFileFid("report");
        report.setContentType("application/pdf");
        Documentation document = new Documentation();
        document.setFileName("documentacao.pdf");
        document.setFileFid("document");
        document.setContentType("application/pdf");
        when(service.getWorkPlan(enrollment)).thenReturn(plan);
        when(service.getReport(enrollment)).thenReturn(report);
        when(service.getDocumentation(enrollment)).thenReturn(document);
        when(storage.read("plan")).thenReturn(new byte[]{1});
        when(storage.read("report")).thenReturn(new byte[]{2});
        when(storage.read("document")).thenReturn(new byte[]{3});
        ResponseEntity<byte[]> planResponse = controller.downloadWorkPlan(1L, 2L, auth);
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
        verify(service, times(3)).getEnrollmentForResponsavel(1L, 2L, professor);
    }

    @Test
    void missingAccountAndUnauthorizedEnrollmentPreventDownloads() {
        when(users.findByUsername("responsavel")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> controller.listResponsibleOffers(auth));
        verifyNoInteractions(service, storage);
        when(users.findByUsername("responsavel")).thenReturn(Optional.of(professor));
        when(service.getEnrollmentForResponsavel(1L, 2L, professor))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Sem acesso"));
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> controller.downloadDocumentation(1L, 2L, auth));
        verifyNoInteractions(storage);
    }
}
